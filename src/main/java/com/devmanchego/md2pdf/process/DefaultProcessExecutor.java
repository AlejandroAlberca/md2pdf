package com.devmanchego.md2pdf.process;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Default {@link ProcessExecutor} implementation backed by {@link ProcessBuilder}.
 *
 * <p>Standard output and standard error are drained on separate threads to avoid
 * dead-locks caused by a full pipe buffer when the child process produces a lot
 * of output. A timeout guards against a hung child process (for example, {@code
 * mmdc} waiting on a missing display or a network resource): if the process does
 * not finish in time, it is forcibly destroyed and an {@link IOException} is
 * thrown.
 */
public class DefaultProcessExecutor implements ProcessExecutor {

    /** Default upper bound on how long a child process may run. */
    public static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(2);

    private final Duration timeout;

    public DefaultProcessExecutor() {
        this(DEFAULT_TIMEOUT);
    }

    public DefaultProcessExecutor(Duration timeout) {
        this.timeout = Objects.requireNonNull(timeout, "timeout must not be null");
        if (timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
    }

    @Override
    public ProcessResult execute(List<String> command) throws IOException, InterruptedException {
        Objects.requireNonNull(command, "command must not be null");
        if (command.isEmpty()) {
            throw new IllegalArgumentException("command must not be empty");
        }

        ProcessBuilder builder = new ProcessBuilder(command);
        Process process = builder.start();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<String> stdout = pool.submit(() -> readStream(process.getInputStream()));
            Future<String> stderr = pool.submit(() -> readStream(process.getErrorStream()));

            boolean finished = process.waitFor(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IOException("Process timed out after " + timeout + ": " + String.join(" ", command));
            }

            return new ProcessResult(process.exitValue(), safeGet(stdout), safeGet(stderr));
        } finally {
            pool.shutdownNow();
        }
    }

    private static String safeGet(Future<String> future) throws IOException, InterruptedException {
        try {
            return future.get();
        } catch (java.util.concurrent.ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException io) {
                throw io;
            }
            throw new IOException("Failed to read process output", cause);
        }
    }

    private static String readStream(InputStream stream) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append(System.lineSeparator());
            }
        }
        return sb.toString();
    }
}
