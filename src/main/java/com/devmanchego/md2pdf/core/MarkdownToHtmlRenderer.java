package com.devmanchego.md2pdf.core;

import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Entities;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

/**
 * Converts Markdown into a self-contained, well-formed XHTML document.
 *
 * <p>flexmark handles the Markdown-to-HTML conversion (GitHub-flavoured tables,
 * task lists, strike-through and autolinks are enabled). The resulting fragment
 * is wrapped in a styled HTML template and then normalised to strict XHTML with
 * jsoup, because the downstream PDF renderer requires well-formed XML.
 */
public class MarkdownToHtmlRenderer {

    private final Parser parser;
    private final HtmlRenderer renderer;

    public MarkdownToHtmlRenderer() {
        MutableDataSet options = new MutableDataSet();
        options.set(Parser.EXTENSIONS, List.of(
                TablesExtension.create(),
                StrikethroughExtension.create(),
                TaskListExtension.create(),
                AutolinkExtension.create()));
        // Keep any raw HTML (such as the <img> tags injected for Mermaid diagrams).
        options.set(HtmlRenderer.ESCAPE_HTML, false);
        options.set(HtmlRenderer.SUPPRESS_HTML, false);

        this.parser = Parser.builder(options).build();
        this.renderer = HtmlRenderer.builder(options).build();
    }

    /**
     * Renders Markdown to a complete XHTML document string.
     *
     * @param markdown the Markdown source (Mermaid blocks should already have been
     *                 replaced by {@code <img>} tags)
     * @param title    the document title used in the {@code <title>} element
     * @return a well-formed XHTML document
     */
    public String render(String markdown, String title) {
        Objects.requireNonNull(markdown, "markdown must not be null");
        String safeTitle = title == null ? "Document" : title;

        Node document = parser.parse(markdown);
        String body = renderer.render(document);

        String html = HtmlTemplate.wrap(safeTitle, body);
        return toXhtml(html);
    }

    private static String toXhtml(String html) {
        Document document = Jsoup.parse(html);
        document.outputSettings()
                .syntax(Document.OutputSettings.Syntax.xml)
                .escapeMode(Entities.EscapeMode.xhtml)
                .charset(StandardCharsets.UTF_8)
                .prettyPrint(false);
        return document.html();
    }
}
