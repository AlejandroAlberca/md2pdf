package com.devmanchego.md2pdf.core;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

/**
 * Renders a well-formed XHTML document to PDF using the open-html-to-pdf engine
 * (backed by Apache PDFBox).
 */
public class HtmlToPdfRenderer {

    /**
     * Renders the given XHTML to PDF and writes it to {@code output}.
     *
     * @param xhtml   a well-formed XHTML document
     * @param baseUri base URI used to resolve relative resources; may be {@code null}
     *                when the document only references absolute {@code file:} URIs
     * @param output  the stream the PDF bytes are written to (not closed by this method)
     * @throws IOException if PDF generation or writing fails
     */
    public void render(String xhtml, String baseUri, OutputStream output) throws IOException {
        Objects.requireNonNull(xhtml, "xhtml must not be null");
        Objects.requireNonNull(output, "output must not be null");

        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.useFastMode();
        builder.withHtmlContent(xhtml, baseUri);
        builder.toStream(output);
        try {
            builder.run();
        } catch (Exception e) {
            throw new IOException("Failed to render PDF: " + e.getMessage(), e);
        }
    }
}
