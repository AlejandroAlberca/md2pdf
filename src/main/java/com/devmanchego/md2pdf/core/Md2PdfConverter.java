package com.devmanchego.md2pdf.core;

import com.devmanchego.md2pdf.mermaid.MermaidPreprocessor;
import com.devmanchego.md2pdf.util.QuietFiles;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Orchestrates the full Markdown-to-document pipeline:
 * <ol>
 *     <li>read the Markdown file,</li>
 *     <li>process Mermaid blocks (render to images, or leave as text, depending
 *         on the configured {@link com.devmanchego.md2pdf.mermaid.MermaidMode}),</li>
 *     <li>convert the Markdown to XHTML and render it to a PDF file,</li>
 *     <li>optionally, also convert the same processed Markdown to a DOCX file.</li>
 * </ol>
 *
 * <p>Collaborators are injected through the constructor, which keeps this class
 * easy to unit test with mocks. The DOCX renderer is optional: when {@code null},
 * requesting DOCX output fails fast with a clear error instead of silently doing
 * nothing.
 */
public class Md2PdfConverter {

    private final MermaidPreprocessor mermaidPreprocessor;
    private final MarkdownToHtmlRenderer markdownToHtmlRenderer;
    private final HtmlToPdfRenderer htmlToPdfRenderer;
    private final MarkdownToDocxRenderer markdownToDocxRenderer;

    public Md2PdfConverter(MermaidPreprocessor mermaidPreprocessor,
                           MarkdownToHtmlRenderer markdownToHtmlRenderer,
                           HtmlToPdfRenderer htmlToPdfRenderer) {
        this(mermaidPreprocessor, markdownToHtmlRenderer, htmlToPdfRenderer, null);
    }

    public Md2PdfConverter(MermaidPreprocessor mermaidPreprocessor,
                           MarkdownToHtmlRenderer markdownToHtmlRenderer,
                           HtmlToPdfRenderer htmlToPdfRenderer,
                           MarkdownToDocxRenderer markdownToDocxRenderer) {
        this.mermaidPreprocessor = Objects.requireNonNull(mermaidPreprocessor);
        this.markdownToHtmlRenderer = Objects.requireNonNull(markdownToHtmlRenderer);
        this.htmlToPdfRenderer = Objects.requireNonNull(htmlToPdfRenderer);
        this.markdownToDocxRenderer = markdownToDocxRenderer;
    }

    /**
     * Converts a Markdown file into a PDF file.
     *
     * @param inputMarkdown the source {@code .md} file
     * @param outputPdf     the destination {@code .pdf} file
     * @throws IOException if reading, rendering, or writing fails
     */
    public void convert(Path inputMarkdown, Path outputPdf) throws IOException {
        convert(inputMarkdown, outputPdf, null);
    }

    /**
     * Converts a Markdown file into a PDF file and, optionally, a DOCX file from
     * the same preprocessed Markdown (so Mermaid diagrams are only rendered once).
     *
     * @param inputMarkdown the source {@code .md} file
     * @param outputPdf     the destination {@code .pdf} file
     * @param outputDocx    the destination {@code .docx} file, or {@code null} to skip DOCX output
     * @throws IOException           if reading, rendering, or writing fails
     * @throws IllegalStateException if {@code outputDocx} is given but no {@link MarkdownToDocxRenderer}
     *                                was configured
     */
    public void convert(Path inputMarkdown, Path outputPdf, Path outputDocx) throws IOException {
        Objects.requireNonNull(inputMarkdown, "inputMarkdown must not be null");
        Objects.requireNonNull(outputPdf, "outputPdf must not be null");
        if (outputDocx != null && markdownToDocxRenderer == null) {
            throw new IllegalStateException(
                    "DOCX output was requested but no MarkdownToDocxRenderer was configured");
        }

        if (!Files.isRegularFile(inputMarkdown)) {
            throw new IOException("Input file does not exist: " + inputMarkdown);
        }

        String markdown = Files.readString(inputMarkdown, StandardCharsets.UTF_8);
        Path imageDirectory = Files.createTempDirectory("md2pdf-mermaid-");
        try {
            String processedMarkdown = mermaidPreprocessor.process(markdown, imageDirectory);
            String title = documentTitle(inputMarkdown);

            writePdf(processedMarkdown, title, imageDirectory, outputPdf);
            if (outputDocx != null) {
                writeDocx(processedMarkdown, outputDocx);
            }
        } finally {
            QuietFiles.deleteRecursivelyQuietly(imageDirectory);
        }
    }

    private void writePdf(String processedMarkdown, String title, Path imageDirectory, Path outputPdf)
            throws IOException {
        String xhtml = markdownToHtmlRenderer.render(processedMarkdown, title);
        createParentDirectories(outputPdf);
        try (OutputStream output = Files.newOutputStream(outputPdf)) {
            htmlToPdfRenderer.render(xhtml, imageDirectory.toUri().toString(), output);
        }
    }

    private void writeDocx(String processedMarkdown, Path outputDocx) throws IOException {
        createParentDirectories(outputDocx);
        try (OutputStream output = Files.newOutputStream(outputDocx)) {
            markdownToDocxRenderer.render(processedMarkdown, output);
        }
    }

    private static void createParentDirectories(Path file) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    private static String documentTitle(Path inputMarkdown) {
        String name = inputMarkdown.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }
}
