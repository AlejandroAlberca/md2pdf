package com.devmanchego.md2pdf.core;

import com.devmanchego.md2pdf.mermaid.MermaidPreprocessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Md2PdfConverterTest {

    @Mock
    private MermaidPreprocessor mermaidPreprocessor;
    @Mock
    private MarkdownToHtmlRenderer markdownToHtmlRenderer;
    @Mock
    private HtmlToPdfRenderer htmlToPdfRenderer;
    @Mock
    private MarkdownToDocxRenderer markdownToDocxRenderer;

    @TempDir
    Path tempDir;

    @Test
    void runsFullPipelineAndWritesPdf() throws Exception {
        Path input = tempDir.resolve("note.md");
        Files.writeString(input, "# Hi", StandardCharsets.UTF_8);
        Path output = tempDir.resolve("out/note.pdf");

        when(mermaidPreprocessor.process(eq("# Hi"), any(Path.class))).thenReturn("processed-md");
        when(markdownToHtmlRenderer.render(eq("processed-md"), any())).thenReturn("<html/>");
        doAnswer(invocation -> {
            OutputStream os = invocation.getArgument(2);
            os.write("%PDF-1.4 fake".getBytes(StandardCharsets.UTF_8));
            return null;
        }).when(htmlToPdfRenderer).render(eq("<html/>"), any(), any(OutputStream.class));

        Md2PdfConverter converter =
                new Md2PdfConverter(mermaidPreprocessor, markdownToHtmlRenderer, htmlToPdfRenderer);
        converter.convert(input, output);

        assertTrue(Files.exists(output), "PDF file should be created");
        assertTrue(Files.size(output) > 0, "PDF file should not be empty");

        verify(mermaidPreprocessor).process(eq("# Hi"), any(Path.class));
        verify(markdownToHtmlRenderer).render(eq("processed-md"), any());
        verify(htmlToPdfRenderer).render(eq("<html/>"), any(), any(OutputStream.class));
    }

    @Test
    void failsWhenInputDoesNotExist() {
        Path missing = tempDir.resolve("nope.md");
        Path output = tempDir.resolve("nope.pdf");

        Md2PdfConverter converter =
                new Md2PdfConverter(mermaidPreprocessor, markdownToHtmlRenderer, htmlToPdfRenderer);

        assertThrows(IOException.class, () -> converter.convert(missing, output));
    }

    @Test
    void alsoWritesDocxWhenRequestedAndRendererConfigured() throws Exception {
        Path input = tempDir.resolve("note.md");
        Files.writeString(input, "# Hi", StandardCharsets.UTF_8);
        Path pdfOutput = tempDir.resolve("note.pdf");
        Path docxOutput = tempDir.resolve("out/note.docx");

        when(mermaidPreprocessor.process(eq("# Hi"), any(Path.class))).thenReturn("processed-md");
        when(markdownToHtmlRenderer.render(eq("processed-md"), any())).thenReturn("<html/>");
        doAnswer(invocation -> {
            OutputStream os = invocation.getArgument(2);
            os.write("%PDF-1.4 fake".getBytes(StandardCharsets.UTF_8));
            return null;
        }).when(htmlToPdfRenderer).render(eq("<html/>"), any(), any(OutputStream.class));
        doAnswer(invocation -> {
            OutputStream os = invocation.getArgument(1);
            os.write("fake-docx-bytes".getBytes(StandardCharsets.UTF_8));
            return null;
        }).when(markdownToDocxRenderer).render(eq("processed-md"), any(OutputStream.class));

        Md2PdfConverter converter = new Md2PdfConverter(
                mermaidPreprocessor, markdownToHtmlRenderer, htmlToPdfRenderer, markdownToDocxRenderer);
        converter.convert(input, pdfOutput, docxOutput);

        assertTrue(Files.exists(pdfOutput), "PDF file should be created");
        assertTrue(Files.exists(docxOutput), "DOCX file should be created");
        verify(markdownToDocxRenderer).render(eq("processed-md"), any(OutputStream.class));
    }

    @Test
    void requestingDocxWithoutAConfiguredRendererFailsFast() throws Exception {
        Path input = tempDir.resolve("note.md");
        Files.writeString(input, "# Hi", StandardCharsets.UTF_8);
        Path pdfOutput = tempDir.resolve("note.pdf");
        Path docxOutput = tempDir.resolve("note.docx");

        Md2PdfConverter converter =
                new Md2PdfConverter(mermaidPreprocessor, markdownToHtmlRenderer, htmlToPdfRenderer);

        assertThrows(IllegalStateException.class, () -> converter.convert(input, pdfOutput, docxOutput));
    }
}
