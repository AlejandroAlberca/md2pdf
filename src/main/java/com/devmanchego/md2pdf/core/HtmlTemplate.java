package com.devmanchego.md2pdf.core;

/**
 * Provides the HTML skeleton and print stylesheet used to wrap rendered Markdown
 * before it is converted to PDF. The styling deliberately mimics a clean,
 * GitHub-like Markdown preview.
 */
final class HtmlTemplate {

    private HtmlTemplate() {
        // Utility class: no instances.
    }

    private static final String CSS = """
            @page {
                size: A4;
                margin: 2cm;
            }
            body {
                font-family: 'Helvetica', 'Arial', sans-serif;
                font-size: 11pt;
                line-height: 1.5;
                color: #24292f;
            }
            h1, h2, h3, h4, h5, h6 {
                font-weight: 600;
                line-height: 1.25;
                margin-top: 1.2em;
                margin-bottom: 0.6em;
            }
            h1 { font-size: 2em; border-bottom: 1px solid #d0d7de; padding-bottom: 0.3em; }
            h2 { font-size: 1.5em; border-bottom: 1px solid #d0d7de; padding-bottom: 0.3em; }
            h3 { font-size: 1.25em; }
            p { margin: 0.6em 0; }
            a { color: #0969da; text-decoration: none; }
            code {
                font-family: 'Courier New', monospace;
                background-color: #f6f8fa;
                padding: 0.15em 0.35em;
                border-radius: 4px;
                font-size: 0.9em;
            }
            pre {
                background-color: #f6f8fa;
                padding: 12px;
                border-radius: 6px;
                overflow: auto;
            }
            pre code {
                background-color: transparent;
                padding: 0;
            }
            blockquote {
                margin: 0.6em 0;
                padding: 0 1em;
                color: #57606a;
                border-left: 4px solid #d0d7de;
            }
            table {
                border-collapse: collapse;
                margin: 0.8em 0;
                width: auto;
            }
            th, td {
                border: 1px solid #d0d7de;
                padding: 6px 12px;
            }
            th { background-color: #f6f8fa; font-weight: 600; }
            img { max-width: 100%; }
            img[alt^="Mermaid diagram"] {
                display: block;
                margin: 1em auto;
            }
            hr {
                border: 0;
                border-top: 1px solid #d0d7de;
                margin: 1.5em 0;
            }
            """;

    /**
     * Wraps an HTML body fragment in a complete document with the print stylesheet.
     *
     * @param title    the document title
     * @param bodyHtml the HTML body content
     * @return a complete HTML document string
     */
    static String wrap(String title, String bodyHtml) {
        return "<!DOCTYPE html>\n"
                + "<html lang=\"en\">\n"
                + "<head>\n"
                + "<meta charset=\"UTF-8\"/>\n"
                + "<title>" + escape(title) + "</title>\n"
                + "<style>\n" + CSS + "</style>\n"
                + "</head>\n"
                + "<body>\n"
                + bodyHtml + "\n"
                + "</body>\n"
                + "</html>\n";
    }

    private static String escape(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
