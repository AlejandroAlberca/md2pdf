package com.devmanchego.md2pdf.mermaid;

import java.nio.file.Path;

/**
 * Renders a single Mermaid diagram definition into an image file.
 */
public interface MermaidCliRunner {

    /**
     * Renders the given Mermaid source into the supplied output file.
     *
     * @param mermaidSource the raw Mermaid diagram definition (without the fences)
     * @param outputFile    the image file to create
     * @throws MermaidRenderingException if rendering fails for any reason
     */
    void render(String mermaidSource, Path outputFile);
}
