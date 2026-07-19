package com.devmanchego.md2pdf.process;

import java.io.IOException;
import java.util.List;

/**
 * Abstraction over running an external operating-system process.
 *
 * <p>Extracting this behind an interface keeps the classes that need to invoke
 * external tools (such as {@code mmdc}) unit-testable: tests can supply a mock
 * implementation instead of spawning real processes.
 */
public interface ProcessExecutor {

    /**
     * Runs the given command and blocks until it terminates.
     *
     * @param command the command and its arguments (never empty)
     * @return the exit code together with captured output
     * @throws IOException          if the process cannot be started or its I/O fails
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    ProcessResult execute(List<String> command) throws IOException, InterruptedException;
}
