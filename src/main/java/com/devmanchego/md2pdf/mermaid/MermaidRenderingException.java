package com.devmanchego.md2pdf.mermaid;

/**
 * Thrown when a Mermaid diagram cannot be rendered to an image, for example when
 * the {@code mmdc} executable is missing, fails, or produces no output.
 */
public class MermaidRenderingException extends RuntimeException {

    public MermaidRenderingException(String message) {
        super(message);
    }

    public MermaidRenderingException(String message, Throwable cause) {
        super(message, cause);
    }
}
