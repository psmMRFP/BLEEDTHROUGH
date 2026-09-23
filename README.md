# BLEEDTHROUGH

BLEEDTHROUGH is a Minecraft 1.20.1 bio-punk survival and ecological cosmic-horror project. Its single Forge core mod, Meatscape, models the gradual overlap between the Overworld and a complete biological reality called The Maw.

Phase 8's planned minimum systems are implemented: The Maw and its survival loop, natural Nether and Outer End entrances, a representative Maw scene and ecology, Hematic and Enzymatic industry, Living Architecture, Compatibility, a bounded Neural circuit, and local End Revelation research. This is a technical milestone, not a release claim. Real-player and multiplayer testing, long-running server load, balance, and final visual assets remain open. See the [Phase 8 acceptance report](docs/PHASE_8_ACCEPTANCE.md) for the exact evidence and gaps. Phase 7's separate Core Alpha gates also remain open.

## Play and research

- A Maw Gateway provides a developer entry path. Rare natural Burning Wounds in the Nether and Wormholes on new Outer End islands also lead to The Maw; they do not require a quest or dragon kill.
- Raw Tissue, Nutrient Paste, and Dermal Panels support an early Maw trip. Heart Pumps, Arteries, Enzyme Vats, regenerative membranes, and Neural components provide small independent Core-only industry loops.
- A suitable new Outer End Wormhole may have an Ancient Anchor nearby. Reading it and carrying End Stone and Chorus samples records three independent observations. The End Revelation advancement page and messages show progress in any discovery order. Existing End chunks are never retroactively modified.

## Requirements and verification

- Minecraft 1.20.1, Forge 47.4.22, and JDK 17.
- Build dependencies and caches belong in your persistent user-level Gradle directory or this workspace, never a system temporary directory.

With JDK 17 active, run these commands from the repository root:

```bash
./gradlew test
./gradlew build
./gradlew runGameTestServer
```

The GameTest server exits after its dedicated-server test suite. See the [documentation index](docs/README.md), [implementation plan](docs/IMPLEMENTATION_PLAN.md), and [remaining roadmap](docs/ROADMAP_REMAINING.md) for decisions and status markers. `[~]` means implemented with specified real-world validation still pending.

Phase 7 research feedback and opt-in soak diagnostics are documented in the [Phase 7 technical-debt record](docs/PHASE_7_TECH_DEBT.md). Its standalone, persistent server rehearsal and three-hour run use `scripts/phase7-soak.sh`; neither replaces real-player validation.
