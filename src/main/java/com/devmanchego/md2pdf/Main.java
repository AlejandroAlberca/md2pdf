package com.devmanchego.md2pdf;

import com.devmanchego.md2pdf.cli.Md2PdfCommand;
import picocli.CommandLine;

/**
 * Application entry point. Delegates argument parsing and execution to the
 * picocli-backed {@link Md2PdfCommand}.
 */
public final class Main {

    private Main() {
        // Utility class: no instances.
    }

    public static void main(String[] args) {
        CommandLine commandLine = new CommandLine(new Md2PdfCommand());
        commandLine.setCaseInsensitiveEnumValuesAllowed(true);
        int exitCode = commandLine.execute(args);
        System.exit(exitCode);
    }
}
