package com.devmanchego.md2pdf.mermaid;

import com.devmanchego.md2pdf.process.ProcessExecutor;
import com.devmanchego.md2pdf.process.ProcessResult;
import com.devmanchego.md2pdf.util.QuietFiles;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * {@link MermaidCliRunner} that shells out to the locally installed Mermaid CLI
 * ({@code mmdc}, provided by the {@code @mermaid-js/mermaid-cli} npm package).
 *
 * <p>Each diagram is written to a temporary {@code .mmd} file which is then fed to
 * {@code mmdc -i <input> -o <output>}. The runner does not care about the output
 * image format: the extension of {@code outputFile} tells {@code mmdc} what to
 * produce (this application uses PNG).
 */
public class ProcessMermaidCliRunner implements MermaidCliRunner {

    /** Default executable name; resolved against the {@code PATH}. */
    public static final String DEFAULT_EXECUTABLE = "mmdc";

    /** Default diagram background. {@code "white"} avoids transparent PNGs in the PDF. */
    public static final String DEFAULT_BACKGROUND = "white";

    private final String executable;
    private final String backgroundColor;
    private final ProcessExecutor processExecutor;

    public ProcessMermaidCliRunner(ProcessExecutor processExecutor) {
        this(DEFAULT_EXECUTABLE, DEFAULT_BACKGROUND, processExecutor);
    }

    public ProcessMermaidCliRunner(String executable, String backgroundColor, ProcessExecutor processExecutor) {
        this.executable = Objects.requireNonNull(executable, "executable must not be null");
        this.backgroundColor = Objects.requireNonNull(backgroundColor, "backgroundColor must not be null");
        this.processExecutor = Objects.requireNonNull(processExecutor, "processExecutor must not be null");
    }

    @Override
    public void render(String mermaidSource, Path outputFile) {
        Objects.requireNonNull(mermaidSource, "mermaidSource must not be null");
        Objects.requireNonNull(outputFile, "outputFile must not be null");

        Path inputFile = null;
        List<String> command = null;
        try {
            inputFile = writeTemporarySource(mermaidSource, outputFile);
            command = buildCommand(inputFile, outputFile);

            ProcessResult result = processExecutor.execute(command);
            if (!result.isSuccess()) {
                throw new MermaidRenderingException(
                        "mmdc exited with code " + result.exitCode()
                                + ". Standard error:\n" + result.stderr().strip());
            }
            if (!Files.exists(outputFile)) {
                throw new MermaidRenderingException(
                        "mmdc reported success but produced no output file at " + outputFile);
            }
        } catch (IOException e) {
            String resolvedExecutable = command != null && !command.isEmpty() ? command.get(0) : executable;
            throw new MermaidRenderingException(
                    "Failed to run mmdc. Tried to launch '" + resolvedExecutable
                            + "' (configured as '" + executable + "'). Is it installed and on the PATH? "
                            + "Underlying error: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MermaidRenderingException("Interrupted while waiting for mmdc to finish", e);
        } finally {
            QuietFiles.deleteQuietly(inputFile);
        }
    }

    private Path writeTemporarySource(String mermaidSource, Path outputFile) throws IOException {
        Path directory = outputFile.toAbsolutePath().getParent();
        if (directory == null) {
            directory = Path.of(".").toAbsolutePath();
        }
        Files.createDirectories(directory);
        String baseName = outputFile.getFileName().toString();
        Path inputFile = directory.resolve(baseName + ".mmd");
        Files.writeString(inputFile, mermaidSource, StandardCharsets.UTF_8);
        return inputFile;
    }

    private List<String> buildCommand(Path inputFile, Path outputFile) {
        List<String> command = new ArrayList<>();
        command.add(resolveExecutable(executable, System.getenv("PATH"), isWindows()));
        command.add("--input");
        command.add(inputFile.toString());
        command.add("--output");
        command.add(outputFile.toString());
        command.add("--backgroundColor");
        command.add(backgroundColor);
        return command;
    }

    /**
     * On Windows, {@code ProcessBuilder} calls {@code CreateProcess} directly and,
     * unlike {@code cmd.exe}, does not consult {@code PATHEXT} to resolve an
     * extension-less name (e.g. {@code mmdc}) to the {@code .cmd}/{@code .bat}
     * wrapper script that npm installs global CLIs as. This searches the {@code
     * PATH} directories ourselves for a matching {@code .cmd}/{@code .bat}/{@code
     * .exe} file and, if found, returns its absolute path so {@code
     * ProcessBuilder} can launch it directly (the JDK auto-wraps {@code .cmd}/
     * {@code .bat} targets with {@code cmd.exe} internally). If nothing matches,
     * the original name is returned unchanged so behaviour on non-Windows
     * platforms, or for names that are already resolvable, is unaffected.
     *
     * <p>Package-private and parameterised on the platform/PATH so it can be unit
     * tested without depending on the real OS or environment.
     */
    static String resolveExecutable(String executable, String pathEnv, boolean windows) {
        if (!windows || hasExecutableExtension(executable) || executable.contains("\\")
                || executable.contains("/")) {
            return executable;
        }
        if (pathEnv == null || pathEnv.isBlank()) {
            return executable;
        }

        for (String directory : pathEnv.split(java.util.regex.Pattern.quote(java.io.File.pathSeparator))) {
            if (directory.isBlank()) {
                continue;
            }
            for (String extension : new String[] {".cmd", ".exe", ".bat"}) {
                Path candidate = Path.of(directory, executable + extension);
                if (Files.isRegularFile(candidate)) {
                    return candidate.toString();
                }
            }
        }
        return executable;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    private static boolean hasExecutableExtension(String executable) {
        String lower = executable.toLowerCase(Locale.ROOT);
        return lower.endsWith(".exe") || lower.endsWith(".cmd") || lower.endsWith(".bat");
    }
}
