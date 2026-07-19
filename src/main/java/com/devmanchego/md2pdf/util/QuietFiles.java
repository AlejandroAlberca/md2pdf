package com.devmanchego.md2pdf.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Best-effort filesystem cleanup helpers. Failures are swallowed because the
 * callers use these only to remove temporary working files/directories, and a
 * failed cleanup must never fail the overall conversion.
 */
public final class QuietFiles {

    private QuietFiles() {
        // Utility class: no instances.
    }

    /**
     * Deletes a single file if it exists, ignoring any failure.
     */
    public static void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Best-effort cleanup.
        }
    }

    /**
     * Recursively deletes a directory (or file) if it exists, ignoring any failure.
     */
    public static void deleteRecursivelyQuietly(Path directory) {
        if (directory == null || !Files.exists(directory)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(QuietFiles::deleteQuietly);
        } catch (IOException ignored) {
            // Best-effort cleanup.
        }
    }
}
