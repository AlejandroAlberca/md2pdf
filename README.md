# md2pdf

Console application that converts a Markdown file into a **PDF** and, optionally,
a **DOCX**, rendering Mermaid diagrams (and standard Markdown: tables, task
lists, strike-through, code blocks, etc.) so the output looks like a Markdown
preview.

Mermaid is JavaScript, so by default diagrams are rasterised to PNG by the
local [`mmdc`](https://github.com/mermaid-js/mermaid-cli) CLI and embedded as
images in both output formats. This is configurable: diagrams can instead be
left as plain text code blocks, which does not require `mmdc` at all — see
[USAGE.md](USAGE.md).

## Requirements

- **JDK 21** (build and run).
- **Maven 3.9+** (build).
- **Node.js** with the Mermaid CLI installed, providing the `mmdc` command —
  only needed if you render Mermaid diagrams as images (the default):

  ```bash
  npm install -g @mermaid-js/mermaid-cli
  mmdc --version   # verify it is on your PATH
  ```

## Build

```bash
mvn clean package
```

This runs the unit tests and produces a self-contained fat jar (no other jars
needed on the classpath):

```
target/md2pdf.jar
```

## Quick start

```bash
# Simplest form: writes README.pdf next to the input
java -jar target/md2pdf.jar README.md

# Also produce a DOCX with the same content
java -jar target/md2pdf.jar README.md --docx

# Explicit input and output, help
java -jar target/md2pdf.jar --input docs/guide.md --output out/guide.pdf
java -jar target/md2pdf.jar --help
```

See [USAGE.md](USAGE.md) for the full command reference, every option, and
more examples (custom `mmdc` path, Mermaid text mode, DOCX output path, exit
codes, troubleshooting).

## How it works

1. **Read** the Markdown file.
2. **Preprocess** (`MermaidPreprocessor`): find fenced ` ```mermaid ` blocks
   and, depending on the configured Mermaid mode, either render each one to a
   PNG via `mmdc` and replace the block with a standard Markdown image
   reference (`IMAGE` mode, the default), or leave the block untouched so it
   renders as a plain code block (`TEXT` mode).
3. **Render Markdown to PDF**: flexmark converts the Markdown (GitHub tables,
   task lists, strike-through, autolinks) to HTML (`MarkdownToHtmlRenderer`),
   jsoup normalises it to well-formed XHTML with an embedded print
   stylesheet, and open-html-to-pdf / PDFBox renders it to PDF
   (`HtmlToPdfRenderer`).
4. **Render Markdown to DOCX** (only when `--docx`/`--docx-output` is given):
   flexmark parses the same processed Markdown and
   `flexmark-docx-converter` (backed by docx4j) renders it directly to a
   `.docx` package (`MarkdownToDocxRenderer`). Mermaid images generated in
   step 2 are embedded as pictures the same way any other Markdown image
   would be — Mermaid is only ever rendered once per conversion, regardless
   of how many output formats are requested.

## Project layout

```
com.devmanchego.md2pdf
├── Main                        entry point
├── cli.Md2PdfCommand           picocli command (argument parsing)
├── core
│   ├── Md2PdfConverter         pipeline orchestrator (PDF + optional DOCX)
│   ├── MarkdownToHtmlRenderer  flexmark + jsoup
│   ├── HtmlToPdfRenderer       open-html-to-pdf
│   ├── HtmlTemplate            HTML skeleton + CSS
│   └── MarkdownToDocxRenderer  flexmark-docx-converter (docx4j)
├── mermaid
│   ├── MermaidMode             IMAGE / TEXT
│   ├── MermaidPreprocessor     handles mermaid blocks per MermaidMode
│   ├── MermaidCliRunner        interface
│   ├── ProcessMermaidCliRunner invokes mmdc (with Windows PATH resolution)
│   └── MermaidRenderingException
├── process
│   ├── ProcessExecutor         interface (mockable)
│   ├── DefaultProcessExecutor  ProcessBuilder-based, with timeout
│   └── ProcessResult           exit code + captured output
└── util
    └── QuietFiles              best-effort temp file/directory cleanup
```

## Testing

```bash
mvn test
```

Tests use JUnit 5 and Mockito. External processes and the Mermaid renderer are
mocked, so the test suite runs without Node or `mmdc` installed.

A sample document is provided at [`sample/example.md`](sample/example.md):

```bash
java -jar target/md2pdf.jar sample/example.md --docx
```
