package com.devmanchego.md2pdf.process;

/**
 * Immutable result of running an external process.
 *
 * @param exitCode the process exit code (0 means success by convention)
 * @param stdout   the captured standard output
 * @param stderr   the captured standard error
 */
public record ProcessResult(int exitCode, String stdout, String stderr) {

    public boolean isSuccess() {
        return exitCode == 0;
    }
}
