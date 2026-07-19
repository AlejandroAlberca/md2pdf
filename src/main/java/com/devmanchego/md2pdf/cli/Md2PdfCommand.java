package com.devmanchego.md2pdf.cli;

import com.devmanchego.md2pdf.core.HtmlToPdfRenderer;
import com.devmanchego.md2pdf.core.MarkdownToDocxRenderer;
import com.devmanchego.md2pdf.core.MarkdownToHtmlRenderer;
import com.devmanchego.md2pdf.core.Md2PdfConverter;
import com.devmanchego.md2pdf.mermaid.MermaidMode;
import com.devmanchego.md2pdf.mermaid.MermaidPreprocessor;
import com.devmanchego.md2pdf.mermaid.ProcessMermaidCliRunner;
import com.devmanchego.md2pdf.process.DefaultProcessExecutor;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * Command line entry point for md2pdf.
 *
 * <p>Example usage:
 * <pre>
 *   java -jar md2pdf.jar README.md
 *   java -jar md2pdf.jar -i README.md -o out/readme.pdf
 *   java -jar md2pdf.jar README.md --mmdc /usr/local/bin/mmdc --background transparent
 *   java -jar md2pdf.jar README.md --docx
 *   java -jar md2pdf.jar README.md --docx --docx-output out/readme.docx
 *   java -jar md2pdf.jar README.md --mermaid-mode text
 * </pre>
 */
@Command(
        name = "md2pdf",
        mixinStandardHelpOptions = true,
        version = "md2pdf 1.0.0",
        description = "Converts a Markdown file into a PDF, rendering Mermaid diagrams via the local mmdc CLI."
)
public class Md2PdfCommand implements Callable<Integer> {

    @Parameters(
            index = "0",
            arity = "0..1",
            paramLabel = "<input.md>",
            description = "Markdown file to convert. Alternative to --input."
    )
    private Path inputPositional;

    @Option(
            names = {"-i", "--input"},
            paramLabel = "<input.md>",
            description = "Markdown file to convert."
    )
    private Path inputOption;

    @Option(
            names = {"-o", "--output"},
            paramLabel = "<output.pdf>",
            description = "Destination PDF file. Defaults to the input name with a .pdf extension."
    )
    private Path output;

    @Option(
            names = {"--mmdc"},
            paramLabel = "<path>",
            defaultValue = ProcessMermaidCliRunner.DEFAULT_EXECUTABLE,
            description = "Path to the Mermaid CLI executable (default: ${DEFAULT-VALUE})."
    )
    private String mmdcExecutable;

    @Option(
            names = {"--background"},
            paramLabel = "<color>",
            defaultValue = ProcessMermaidCliRunner.DEFAULT_BACKGROUND,
            description = "Background color for Mermaid diagrams (default: ${DEFAULT-VALUE})."
    )
    private String background;

    @Option(
            names = {"--docx"},
            description = "Also produce a DOCX file alongside the PDF."
    )
    private boolean docx;

    @Option(
            names = {"--docx-output"},
            paramLabel = "<output.docx>",
            description = "Destination DOCX file (implies --docx). Defaults to the input name with a .docx extension."
    )
    private Path docxOutput;

    @Option(
            names = {"--mermaid-mode"},
            paramLabel = "<image|text>",
            defaultValue = "IMAGE",
            description = "How to handle Mermaid diagrams: 'image' renders each one to a picture via mmdc "
                    + "(default: ${DEFAULT-VALUE}); 'text' leaves the diagram source as a plain code block "
                    + "and does not require mmdc to be installed."
    )
    private MermaidMode mermaidMode;

    @Spec
    private CommandSpec spec;

    @Override
    public Integer call() {
        Path input = resolveInput();
        if (input == null) {
            spec.commandLine().usage(System.out);
            return 2;
        }

        Path pdfTarget = output != null ? output : defaultOutput(input, "pdf");
        boolean wantsDocx = docx || docxOutput != null;
        Path docxTarget = wantsDocx ? (docxOutput != null ? docxOutput : defaultOutput(input, "docx")) : null;

        Md2PdfConverter converter = new Md2PdfConverter(
                new MermaidPreprocessor(
                        new ProcessMermaidCliRunner(mmdcExecutable, background, new DefaultProcessExecutor()),
                        mermaidMode),
                new MarkdownToHtmlRenderer(),
                new HtmlToPdfRenderer(),
                wantsDocx ? new MarkdownToDocxRenderer() : null);

        try {
            converter.convert(input, pdfTarget, docxTarget);
            System.out.println("Created " + pdfTarget.toAbsolutePath());
            if (docxTarget != null) {
                System.out.println("Created " + docxTarget.toAbsolutePath());
            }
            return 0;
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
    }

    private Path resolveInput() {
        if (inputOption != null) {
            return inputOption;
        }
        return inputPositional;
    }

    static Path defaultOutput(Path input, String extension) {
        Path fileName = input.getFileName();
        String name = fileName == null ? "output" : fileName.toString();
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        Path parent = input.toAbsolutePath().getParent();
        String outputName = base + "." + extension;
        return parent != null ? parent.resolve(outputName) : Path.of(outputName);
    }
}
