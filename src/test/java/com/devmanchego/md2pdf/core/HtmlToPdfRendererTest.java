package com.devmanchego.md2pdf.core;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HtmlToPdfRendererTest {

    private final HtmlToPdfRenderer renderer = new HtmlToPdfRenderer();

    @Test
    void rendersWellFormedXhtmlToNonEmptyPdf() throws Exception {
        String xhtml = "<html><head><title>t</title></head><body><p>Hello</p></body></html>";
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        renderer.render(xhtml, null, output);

        byte[] pdfBytes = output.toByteArray();
        assertTrue(pdfBytes.length > 0, "PDF output should not be empty");
        String header = new String(pdfBytes, 0, 5, java.nio.charset.StandardCharsets.US_ASCII);
        assertTrue(header.startsWith("%PDF-"), "output should start with a PDF header");
    }

    @Test
    void wrapsMalformedHtmlFailureAsIoExceptionWithContext() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        IOException ex = assertThrows(IOException.class,
                () -> renderer.render("<html><body><p>unclosed", null, output));
        assertTrue(ex.getMessage().contains("Failed to render PDF"),
                "message should give context about the failure stage");
    }

    @Test
    void rejectsNullXhtml() {
        assertThrows(NullPointerException.class,
                () -> renderer.render(null, null, new ByteArrayOutputStream()));
    }

    @Test
    void rejectsNullOutputStream() {
        assertThrows(NullPointerException.class,
                () -> renderer.render("<html><body/></html>", null, null));
    }
}
