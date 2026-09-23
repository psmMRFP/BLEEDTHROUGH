package com.bleedthrough.meatscape.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class SoakTelemetryTest {
    @Test
    void headerIsStableAndRestartDoesNotDuplicateIt() throws Exception {
        Path path = Path.of("build", "test-soak-header", "meatscape-soak.csv");
        Files.createDirectories(path.getParent());
        Files.deleteIfExists(path);
        SoakTelemetry.ensureHeader(path);
        SoakTelemetry.ensureHeader(path);
        assertEquals(29, SoakTelemetry.HEADER.split(",").length);
        assertEquals(1, Files.readAllLines(path).size());
        Files.writeString(path, "sample\n", java.nio.file.StandardOpenOption.APPEND);
        SoakTelemetry.ensureHeader(path);
        assertEquals(2, Files.readAllLines(path).size());
        assertThrows(java.io.IOException.class, () -> SoakTelemetry.ensureHeader(path.resolve("bad")));
    }

    @Test
    void directorySizeSumsOnlyFiles() throws Exception {
        var root = Path.of("build", "test-soak-telemetry");
        try {
            Files.createDirectories(root);
            Files.writeString(root.resolve("a"), "abc");
            Files.createDirectory(root.resolve("nested"));
            Files.writeString(root.resolve("nested").resolve("b"), "hello");
            Files.writeString(root.resolve("meatscape-soak.csv"), "diagnostics");
            assertEquals(8L, SoakTelemetry.directorySize(root));
        } finally {
            try (var paths = Files.walk(root)) {
                paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
            }
        }
    }

    @Test void inaccessibleWorldSizeIsMissingNotZero() {
        assertEquals(-1L, SoakTelemetry.directorySize(Path.of("build", "absent-world-for-soak-test")));
    }

    @Test void oldCsvHeaderIsPreservedInsteadOfMixingSchemas() throws Exception {
        Path directory = Path.of("build", "test-soak-legacy");
        Files.createDirectories(directory);
        Path path = directory.resolve("meatscape-soak.csv");
        Files.writeString(path, "old_header\nold_sample\n");
        SoakTelemetry.ensureHeader(path);
        assertEquals(SoakTelemetry.HEADER, Files.readAllLines(path).get(0));
        try (var files = Files.list(directory)) {
            assertTrue(files.anyMatch(file -> file.getFileName().toString().startsWith("meatscape-soak-legacy-")
                    && file.getFileName().toString().endsWith(".csv")));
        }
    }

    @Test void rotationKeepsOldLogAndCreatesFreshHeader() throws Exception {
        Path path = Path.of("build", "test-soak-rotate", "meatscape-soak.csv");
        Files.createDirectories(path.getParent());
        Files.writeString(path, "x".repeat(8 * 1024 * 1024));
        Path rotated = SoakTelemetry.rotateIfNeeded(path, java.util.UUID.randomUUID());
        assertNotEquals(path, rotated);
        SoakTelemetry.ensureHeader(rotated);
        assertEquals(8L * 1024 * 1024, Files.size(path));
        assertEquals(1, Files.readAllLines(rotated).size());
    }
}
