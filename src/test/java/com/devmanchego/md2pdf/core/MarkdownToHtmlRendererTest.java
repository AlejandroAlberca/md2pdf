package com.devmanchego.md2pdf.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownToHtmlRendererTest {

    private final MarkdownToHtmlRenderer renderer = new MarkdownToHtmlRenderer();

    @Test
    void rendersHeadingsAndParagraphs() {
        String html = renderer.render("# Hello\n\nSome **bold** text.", "Doc");

        assertTrue(html.contains("<h1"), "heading should be rendered");
        assertTrue(html.contains("Hello"));
        assertTrue(html.contains("<strong>bold</strong>"), "bold text should be rendered");
    }

    @Test
    void rendersGithubFlavouredTables() {
        String markdown = "| A | B |\n|---|---|\n| 1 | 2 |";

        String html = renderer.render(markdown, "Doc");

        assertTrue(html.contains("<table"), "table should be rendered");
        assertTrue(html.contains("<th"), "table header cell expected");
        assertTrue(html.contains("<td"), "table data cell expected");
    }

    @Test
    void keepsInjectedMermaidImageTags() {
        String markdown = "<img class=\"mermaid-diagram\" src=\"file:/tmp/mermaid-1.png\" alt=\"Mermaid diagram 1\" />";

        String html = renderer.render(markdown, "Doc");

        assertTrue(html.contains("mermaid-diagram"), "img class should survive rendering");
        assertTrue(html.contains("mermaid-1.png"), "img source should survive rendering");
    }

    @Test
    void producesCompleteXhtmlDocument() {
        String html = renderer.render("# Title", "My Title");

        assertTrue(html.contains("<html"), "should contain root html element");
        assertTrue(html.contains("<title>My Title</title>"), "title should be embedded");
        assertTrue(html.contains("<style"), "print stylesheet should be embedded");
    }
}
