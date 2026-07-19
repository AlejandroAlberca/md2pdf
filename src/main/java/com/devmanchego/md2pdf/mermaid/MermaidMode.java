package com.devmanchego.md2pdf.mermaid;

/**
 * How {@link MermaidPreprocessor} should handle fenced Mermaid diagram blocks.
 */
public enum MermaidMode {

    /** Render each diagram to a PNG (via {@code mmdc}) and embed it as an image. */
    IMAGE,

    /** Leave the diagram source as a plain fenced code block; {@code mmdc} is never invoked. */
    TEXT
}
