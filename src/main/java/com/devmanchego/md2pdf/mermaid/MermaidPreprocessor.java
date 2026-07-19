package com.devmanchego.md2pdf.mermaid;

import java.nio.file.Path;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Scans Markdown source for fenced Mermaid code blocks and, depending on the
 * configured {@link MermaidMode}, either renders each one to an image using a
 * {@link MermaidCliRunner} and replaces the block with a standard Markdown image
 * reference ({@link MermaidMode#IMAGE}), or leaves the fenced block untouched so
 * it renders as a plain code block ({@link MermaidMode#TEXT}, which does not
 * require {@code mmdc} to be installed at all).
 *
 * <p>The image reference uses ordinary Markdown syntax ({@code ![alt](uri)})
 * rather than raw HTML so it round-trips correctly through every downstream
 * renderer (HTML/PDF and DOCX alike).
 *
 * <p>A fenced Mermaid block looks like:
 * <pre>
 * ```mermaid
 * graph TD; A--&gt;B;
 * ```
 * </pre>
 *
 * Both back-tick ({@code ```}) and tilde ({@code ~~~}) fences are supported.
 * Everything that is not a Mermaid block is passed through untouched so it can be
 * handled by the normal Markdown renderer.
 */
public class MermaidPreprocessor {

    /** Matches an opening fence such as "```mermaid" or "~~~ mermaid". */
    private static final Pattern FENCE_START =
            Pattern.compile("^(`{3,}|~{3,})\\s*mermaid\\b.*$");

    private final MermaidCliRunner runner;
    private final MermaidMode mode;

    public MermaidPreprocessor(MermaidCliRunner runner) {
        this(runner, MermaidMode.IMAGE);
    }

    public MermaidPreprocessor(MermaidCliRunner runner, MermaidMode mode) {
        this.runner = Objects.requireNonNull(runner, "runner must not be null");
        this.mode = Objects.requireNonNull(mode, "mode must not be null");
    }

    /**
     * Processes every Mermaid block in {@code markdown} according to the
     * configured {@link MermaidMode}.
     *
     * @param markdown       the original Markdown text
     * @param imageDirectory the directory in which generated images are written
     *                       (unused, but still required, in {@link MermaidMode#TEXT})
     * @return the transformed Markdown text
     */
    public String process(String markdown, Path imageDirectory) {
        Objects.requireNonNull(markdown, "markdown must not be null");
        Objects.requireNonNull(imageDirectory, "imageDirectory must not be null");

        String[] lines = markdown.split("\n", -1);
        StringBuilder output = new StringBuilder();
        int diagramIndex = 0;
        int i = 0;

        while (i < lines.length) {
            String line = lines[i];
            String stripped = line.strip();
            String fence = openingFenceMarker(stripped);

            if (fence != null) {
                String openingLine = line;
                StringBuilder body = new StringBuilder();
                i++;
                while (i < lines.length && !isClosingFence(lines[i].strip(), fence)) {
                    if (body.length() > 0) {
                        body.append('\n');
                    }
                    body.append(lines[i]);
                    i++;
                }
                String closingLine = null;
                if (i < lines.length) {
                    closingLine = lines[i];
                    i++;
                }

                if (mode == MermaidMode.TEXT) {
                    output.append(openingLine);
                    if (body.length() > 0) {
                        output.append('\n').append(body);
                    }
                    if (closingLine != null) {
                        output.append('\n').append(closingLine);
                    }
                } else {
                    diagramIndex++;
                    Path imageFile = imageDirectory.resolve("mermaid-" + diagramIndex + ".png");
                    runner.render(body.toString(), imageFile);
                    output.append(imageMarkdown(imageFile, diagramIndex));
                }
                appendNewlineIfNotLast(output, i, lines.length);
            } else {
                output.append(line);
                appendNewlineIfNotLast(output, i + 1, lines.length);
                i++;
            }
        }

        return output.toString();
    }

    private static String openingFenceMarker(String strippedLine) {
        if (!FENCE_START.matcher(strippedLine).matches()) {
            return null;
        }
        char fenceChar = strippedLine.charAt(0);
        int length = 0;
        while (length < strippedLine.length() && strippedLine.charAt(length) == fenceChar) {
            length++;
        }
        return strippedLine.substring(0, length);
    }

    private static boolean isClosingFence(String strippedLine, String openingFence) {
        if (strippedLine.length() < openingFence.length()) {
            return false;
        }
        char fenceChar = openingFence.charAt(0);
        // A closing fence is a run of the same fence character, at least as long as
        // the opening one, with no trailing content.
        for (int i = 0; i < strippedLine.length(); i++) {
            if (strippedLine.charAt(i) != fenceChar) {
                return false;
            }
        }
        return true;
    }

    private static String imageMarkdown(Path imageFile, int diagramIndex) {
        String src = imageFile.toAbsolutePath().toUri().toString();
        return "![Mermaid diagram " + diagramIndex + "](" + src + ")";
    }

    private static void appendNewlineIfNotLast(StringBuilder output, int nextIndex, int total) {
        if (nextIndex < total) {
            output.append('\n');
        }
    }
}
