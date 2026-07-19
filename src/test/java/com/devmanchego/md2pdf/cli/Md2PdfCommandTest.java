package com.devmanchego.md2pdf.cli;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Md2PdfCommandTest {

    @Test
    void defaultOutputReplacesExtensionWithPdf() {
        Path output = Md2PdfCommand.defaultOutput(Path.of("docs", "README.md"), "pdf");
        assertEquals("README.pdf", output.getFileName().toString());
    }

    @Test
    void defaultOutputHandlesNameWithoutExtension() {
        Path output = Md2PdfCommand.defaultOutput(Path.of("NOTES"), "pdf");
        assertEquals("NOTES.pdf", output.getFileName().toString());
    }

    @Test
    void defaultOutputUsesRequestedExtensionForDocx() {
        Path output = Md2PdfCommand.defaultOutput(Path.of("docs", "README.md"), "docx");
        assertEquals("README.docx", output.getFileName().toString());
    }

    @Test
    void docxFlagAlsoProducesDocxOutput(@org.junit.jupiter.api.io.TempDir Path tempDir) throws Exception {
        Path input = tempDir.resolve("note.md");
        java.nio.file.Files.writeString(input, "# Hi\n\nSome text.");

        int exitCode = new CommandLine(new Md2PdfCommand())
                .execute(input.toString(), "--docx");

        assertEquals(0, exitCode);
        assertTrue(java.nio.file.Files.exists(tempDir.resolve("note.pdf")), "PDF should be created");
        assertTrue(java.nio.file.Files.exists(tempDir.resolve("note.docx")), "DOCX should be created");
    }

    @Test
    void docxOutputOptionImpliesDocxGeneration(@org.junit.jupiter.api.io.TempDir Path tempDir) throws Exception {
        Path input = tempDir.resolve("note.md");
        java.nio.file.Files.writeString(input, "# Hi");
        Path docxTarget = tempDir.resolve("custom.docx");

        int exitCode = new CommandLine(new Md2PdfCommand())
                .execute(input.toString(), "--docx-output", docxTarget.toString());

        assertEquals(0, exitCode);
        assertTrue(java.nio.file.Files.exists(docxTarget), "DOCX should be created at the custom path");
    }

    @Test
    void missingInputReturnsUsageErrorCode() {
        // No input argument supplied: the command should report an input error (exit code 2).
        int exitCode = new CommandLine(new Md2PdfCommand()).execute();
        assertEquals(2, exitCode);
    }

    @Test
    void helpOptionIsRecognised() {
        int exitCode = new CommandLine(new Md2PdfCommand()).execute("--help");
        assertEquals(0, exitCode);
    }

    @Test
    void unknownInputFileProducesFailureExitCode() {
        int exitCode = new CommandLine(new Md2PdfCommand())
                .execute("definitely-does-not-exist-12345.md");
        assertTrue(exitCode != 0, "conversion of a missing file should fail");
    }
}
