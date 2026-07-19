package com.devmanchego.md2pdf.process;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the real {@link DefaultProcessExecutor} by running the JVM that is
 * executing the tests. This keeps the test cross-platform and dependency-free
 * (the {@code java} launcher is always available from {@code java.home}).
 */
class DefaultProcessExecutorTest {

    private final DefaultProcessExecutor executor = new DefaultProcessExecutor();

    private static String javaExecutable() {
        String home = System.getProperty("java.home");
        Path bin = Paths.get(home, "bin",
                System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java");
        return bin.toString();
    }

    @Test
    void capturesExitCodeAndOutputOfSuccessfulProcess() throws Exception {
        // "java -version" exits 0 and writes the version banner to standard error.
        ProcessResult result = executor.execute(List.of(javaExecutable(), "-version"));

        assertEquals(0, result.exitCode());
        assertTrue(result.isSuccess());
        assertTrue(result.stderr().toLowerCase().contains("version"),
                "version banner expected on stderr");
    }

    @Test
    void reportsNonZeroExitCodeForFailingProcess() throws Exception {
        // An unknown option makes the launcher exit with a non-zero status.
        ProcessResult result =
                executor.execute(List.of(javaExecutable(), "--not-a-real-flag-xyz"));

        assertNotEquals(0, result.exitCode());
        assertTrue(result.isSuccess() == false);
    }

    @Test
    void rejectsEmptyCommand() {
        assertThrows(IllegalArgumentException.class, () -> executor.execute(List.of()));
    }

    @Test
    void throwsIoExceptionWhenExecutableMissing() throws IOException {
        Path missing = Files.createTempDirectory("md2pdf-test-").resolve("no-such-binary");
        assertThrows(IOException.class,
                () -> executor.execute(List.of(missing.toString())));
    }

    @Test
    void constructorRejectsNonPositiveTimeout() {
        assertThrows(IllegalArgumentException.class, () -> new DefaultProcessExecutor(Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> new DefaultProcessExecutor(Duration.ofSeconds(-1)));
    }

    @Test
    void killsProcessAndThrowsIoExceptionWhenItExceedsTheTimeout(@org.junit.jupiter.api.io.TempDir Path tempDir)
            throws IOException {
        // A tiny single-file-source Java program that sleeps far longer than the
        // configured timeout, simulating a hung mmdc process.
        Path source = tempDir.resolve("Sleep.java");
        Files.writeString(source, """
                class Sleep {
                    public static void main(String[] args) throws InterruptedException {
                        Thread.sleep(60_000);
                    }
                }
                """);

        DefaultProcessExecutor shortTimeoutExecutor = new DefaultProcessExecutor(Duration.ofMillis(500));

        IOException ex = assertThrows(IOException.class,
                () -> shortTimeoutExecutor.execute(List.of(javaExecutable(), source.toString())));
        assertTrue(ex.getMessage().toLowerCase().contains("timed out"));
    }
}
