package com.devmanchego.md2pdf.core;

import com.vladsch.flexmark.docx.converter.DocxRenderer;
import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Objects;

/**
 * Converts Markdown into a {@code .docx} document.
 *
 * <p>flexmark parses the Markdown into the same kind of AST used by
 * {@link MarkdownToHtmlRenderer} (same GitHub-flavoured extensions enabled), and
 * {@code flexmark-docx-converter} walks that tree to populate a docx4j
 * {@link WordprocessingMLPackage}, which is then written out as a {@code .docx}
 * file. Mermaid diagram images (already rendered to PNG and referenced with
 * standard Markdown image syntax by {@link com.devmanchego.md2pdf.mermaid.MermaidPreprocessor})
 * are embedded as pictures the same way any other Markdown image would be.
 */
public class MarkdownToDocxRenderer {

    private final Parser parser;
    private final MutableDataSet options;

    public MarkdownToDocxRenderer() {
        this.options = new MutableDataSet();
        options.set(Parser.EXTENSIONS, List.of(
                TablesExtension.create(),
                StrikethroughExtension.create(),
                TaskListExtension.create(),
                AutolinkExtension.create()));
        this.parser = Parser.builder(options).build();
    }

    /**
     * Renders Markdown to a {@code .docx} document and writes it to {@code output}.
     *
     * @param markdown the Markdown source (Mermaid blocks should already have been
     *                 replaced by image references, or left as-is for text mode)
     * @param output   the stream the DOCX bytes are written to (not closed by this method)
     * @throws IOException if parsing or DOCX generation fails
     */
    public void render(String markdown, OutputStream output) throws IOException {
        Objects.requireNonNull(markdown, "markdown must not be null");
        Objects.requireNonNull(output, "output must not be null");

        Node document = parser.parse(markdown);
        try {
            WordprocessingMLPackage template = DocxRenderer.getDefaultTemplate();
            DocxRenderer renderer = DocxRenderer.builder(options).build();
            renderer.render(document, template);
            template.save(output);
        } catch (Docx4JException e) {
            throw new IOException("Failed to render DOCX: " + e.getMessage(), e);
        }
    }
}
