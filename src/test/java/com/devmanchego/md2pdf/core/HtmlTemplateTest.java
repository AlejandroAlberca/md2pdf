package com.devmanchego.md2pdf.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class HtmlTemplateTest {

    @Test
    void wrapsBodyWithDoctypeAndStylesheet() {
        String html = HtmlTemplate.wrap("My Doc", "<p>content</p>");

        assertTrue(html.startsWith("<!DOCTYPE html>"), "should start with a doctype");
        assertTrue(html.contains("<style>"), "print stylesheet should be embedded");
        assertTrue(html.contains("<p>content</p>"), "body fragment should be preserved");
    }

    @Test
    void escapesHtmlSpecialCharactersInTitle() {
        String html = HtmlTemplate.wrap("A <b> & C", "<p/>");

        assertTrue(html.contains("<title>A &lt;b&gt; &amp; C</title>"),
                "title should be HTML-escaped to avoid breaking the head element");
    }

    @Test
    void fallsBackGracefullyForEmptyTitle() {
        String html = HtmlTemplate.wrap("", "<p/>");

        assertTrue(html.contains("<title></title>"));
    }
}
