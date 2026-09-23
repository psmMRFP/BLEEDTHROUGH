#!/usr/bin/env bash
set -euo pipefail

repo_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
soak_dir="$repo_dir/run/phase7-soak"
minutes="${1:-180}"
if ! [[ "$minutes" =~ ^[0-9]+$ ]]; then
    printf 'Usage: %s [total online minutes; 0 tests startup and restart]\n' "$0" >&2
    exit 2
fi
if ! rg -q '^eula=true$' "$repo_dir/run/eula.txt"; then
    printf 'Accept the Minecraft EULA in run/eula.txt before running this test.\n' >&2
    exit 2
fi
mkdir -p "$soak_dir/config" "$soak_dir/evidence"
evidence_dir="$soak_dir/evidence/session-$(date -u +%Y%m%dT%H%M%SZ)-$$"
mkdir -p "$evidence_dir"
if [[ ! -e "$soak_dir/eula.txt" ]]; then cp "$repo_dir/run/eula.txt" "$soak_dir/eula.txt"; fi
if [[ ! -e "$soak_dir/server.properties" ]]; then
    printf 'level-name=world\nlevel-seed=76507650\nserver-ip=127.0.0.1\nserver-port=25570\nonline-mode=true\nspawn-protection=0\nview-distance=6\nsimulation-distance=6\nmax-players=2\n' > "$soak_dir/server.properties"
fi
if [[ ! -e "$soak_dir/config/meatscape-common.toml" ]]; then
    printf '[evolution]\nglobalTickBudget=64\nperRiftTickBudget=8\n[diagnostics]\nsoakTelemetryEnabled=true\nsoakTelemetryIntervalTicks=200\n' > "$soak_dir/config/meatscape-common.toml"
fi
git -C "$repo_dir" rev-parse HEAD > "$evidence_dir/build-commit.txt"
git -C "$repo_dir" status --porcelain > "$evidence_dir/worktree-status.txt"
/usr/lib/jvm/java-17-openjdk/bin/java -version 2> "$evidence_dir/java-version.txt"
printf 'seed=76507650\nport=25570\nfixture=overworld 8 70 8 and 520 70 520; local and distant forceload\nplanned_minutes=%s\n' "$minutes" > "$evidence_dir/manifest.txt"
csv_file="$soak_dir/world/data/meatscape-soak.csv"
previous_rows=0
if [[ -f "$csv_file" ]]; then previous_rows="$(wc -l < "$csv_file")"; fi

server_pid=''
fifo=''
cleanup() {
    if [[ -n "$server_pid" ]] && kill -0 "$server_pid" 2>/dev/null; then
        printf 'stop\n' >&3 || true
        wait "$server_pid" || true
    fi
    exec 3>&- || true
    if [[ -n "$fifo" && -p "$fifo" ]]; then rm "$fifo"; fi
}
trap cleanup EXIT INT TERM

send() {
    printf '%s %s\n' "$(date -u +%FT%TZ)" "$1" >> "$evidence_dir/commands.log"
    printf '%s\n' "$1" >&3
}

run_segment() {
    local segment="$1" duration="$2" log_file="$evidence_dir/server-$1.log"
    fifo="$evidence_dir/console-$segment.fifo"
    mkfifo "$fifo"
    exec 3<> "$fifo"
    printf '%s segment=%s start\n' "$(date -u +%FT%TZ)" "$segment" >> "$evidence_dir/timeline.log"
    ( cd "$repo_dir"; env JAVA_HOME=/usr/lib/jvm/java-17-openjdk PATH=/usr/lib/jvm/java-17-openjdk/bin:$PATH ./gradlew runServer -PmeatscapeSoak=true --console=plain < "$fifo" > "$log_file" 2>&1 ) &
    server_pid=$!
    local ready=0
    for (( i=0; i<240; i++ )); do
        if rg -q 'Done \(' "$log_file" 2>/dev/null; then ready=1; break; fi
        if ! kill -0 "$server_pid" 2>/dev/null; then break; fi
        sleep 1
    done
    if (( ready == 0 )); then
        printf 'Server did not reach Done; inspect %s\n' "$log_file" >&2
        return 1
    fi
    send 'meatscape debug stats'
    if [[ "$segment" == 'first' && ! -e "$soak_dir/evidence/fixture-created" ]]; then
        send 'forceload add -16 -16 31 31'
        send 'execute in minecraft:overworld positioned 8 70 8 run meatscape rift create 2 64'
        send 'setblock 14 70 8 meatscape:rift_core[active=false]'
        send 'setblock 10 70 8 meatscape:heart_pump'
        send 'setblock 12 70 8 minecraft:chest'
        touch "$soak_dir/evidence/fixture-created"
    fi
    if [[ "$segment" == 'first' && ! -e "$soak_dir/evidence/fixture-v2-created" ]]; then
        send 'setblock 11 70 8 meatscape:regenerative_membrane[wounded=true]'
        send 'data merge block 11 70 8 {Nutrition:4}'
        send 'forceload add 512 512 527 527'
        send 'execute in minecraft:overworld positioned 520 70 520 run meatscape rift create 2 32'
        touch "$soak_dir/evidence/fixture-v2-created"
    elif [[ "$segment" == 'restart' ]]; then
        send 'forceload add 512 512 527 527'
    fi
    send 'meatscape pause true'
    sleep 2
    send 'meatscape pause false'
    send 'reload'
    for (( i=0; i<duration; i++ )); do
        sleep 60
        send 'meatscape debug stats'
        send 'meatscape rollback status'
    done
    send 'forceload remove 512 512 527 527'
    if [[ "$segment" == 'first' ]]; then
        send 'execute in minecraft:overworld positioned 8 70 8 run meatscape rollback start 2 4 true'
    else
        send 'execute in minecraft:overworld positioned 8 70 8 run meatscape rollback start 2 4 false'
    fi
    send 'save-all flush'
    send 'stop'
    local status=0
    wait "$server_pid" || status=$?
    printf '%s segment=%s stop exit=%s\n' "$(date -u +%FT%TZ)" "$segment" "$status" >> "$evidence_dir/timeline.log"
    server_pid=''
    exec 3>&-
    rm "$fifo"
    fifo=''
    if (( status != 0 )); then return "$status"; fi
    if ! rg -q 'Stopping server|Stopping the server' "$log_file"; then
        printf 'No normal shutdown marker in %s\n' "$log_file" >&2
        return 1
    fi
    return 0
}

first=$(( minutes / 2 ))
second=$(( minutes - first ))
run_segment first "$first"
run_segment restart "$second"
if (( minutes > 0 )); then
    awk -F, -v start="$(( previous_rows + 1 ))" 'NR >= start && NR > 1 {print $2}' "$csv_file" | sort -u > "$evidence_dir/session-ids.txt"
    if [[ "$(wc -l < "$evidence_dir/session-ids.txt")" -lt 2 ]]; then
        printf 'Expected telemetry from both online segments; inspect %s\n' "$csv_file" >&2
        exit 1
    fi
    awk -F, -v start="$(( previous_rows + 1 ))" 'NR >= start && NR > 1 {rows++; violations += $26; if (NF != 29) malformed++} END {printf "rows=%d\nbudget_violations=%d\nmalformed_rows=%d\n", rows, violations, malformed}' "$csv_file" > "$evidence_dir/csv-summary.txt"
fi
printf 'Soak completed; inspect %s and the world CSV before claiming acceptance.\n' "$evidence_dir"
