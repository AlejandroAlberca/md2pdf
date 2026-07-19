package com.devmanchego.md2pdf.mermaid;

import com.devmanchego.md2pdf.process.ProcessExecutor;
import com.devmanchego.md2pdf.process.ProcessResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessMermaidCliRunnerTest {

    @Mock
    private ProcessExecutor processExecutor;

    @TempDir
    Path tempDir;

    @Test
    void buildsExpectedCommandAndSucceedsWhenOutputProduced() throws Exception {
        Path output = tempDir.resolve("mermaid-1.png");
        // Simulate mmdc creating the output file on success.
        when(processExecutor.execute(anyList())).thenAnswer(invocation -> {
            Files.writeString(output, "fake-png-bytes");
            return new ProcessResult(0, "", "");
        });

        ProcessMermaidCliRunner runner =
                new ProcessMermaidCliRunner("mmdc", "white", processExecutor);
        runner.render("graph TD; A-->B;", output);

        ArgumentCaptor<List<String>> commandCaptor = ArgumentCaptor.forClass(List.class);
        verify(processExecutor).execute(commandCaptor.capture());
        List<String> command = commandCaptor.getValue();

        // Executable resolution against PATH is exercised in isolation, with a
        // controlled PATH, in ProcessMermaidCliRunnerExecutableResolutionTest. Here we
        // let the runner resolve against this machine's real PATH, so on a Windows box
        // that actually has mmdc installed the resolved value is an absolute path to
        // mmdc.cmd/.exe/.bat rather than the bare name -- accept either.
        assertTrue(command.get(0).equals("mmdc") || command.get(0).matches("(?i).*mmdc\\.(cmd|exe|bat)$"),
                "expected 'mmdc' or a resolved path ending in mmdc.cmd/.exe/.bat, got: " + command.get(0));
        assertTrue(command.contains("--input"));
        assertTrue(command.contains("--output"));
        assertTrue(command.contains(output.toString()));
        assertTrue(command.contains("--backgroundColor"));
        assertTrue(command.contains("white"));
    }

    @Test
    void doesNotWrapExecutableThatAlreadyHasAnExtension() throws Exception {
        Path output = tempDir.resolve("mermaid-1.png");
        when(processExecutor.execute(anyList())).thenAnswer(invocation -> {
            Files.writeString(output, "fake-png-bytes");
            return new ProcessResult(0, "", "");
        });

        ProcessMermaidCliRunner runner =
                new ProcessMermaidCliRunner("C:\\tools\\mmdc.cmd", "white", processExecutor);
        runner.render("graph TD; A-->B;", output);

        ArgumentCaptor<List<String>> commandCaptor = ArgumentCaptor.forClass(List.class);
        verify(processExecutor).execute(commandCaptor.capture());
        assertEquals("C:\\tools\\mmdc.cmd", commandCaptor.getValue().get(0));
    }

    @Test
    void throwsWhenExitCodeIsNonZero() throws Exception {
        Path output = tempDir.resolve("mermaid-1.png");
        when(processExecutor.execute(anyList()))
                .thenReturn(new ProcessResult(1, "", "syntax error near line 2"));

        ProcessMermaidCliRunner runner =
                new ProcessMermaidCliRunner("mmdc", "white", processExecutor);

        MermaidRenderingException ex = assertThrows(MermaidRenderingException.class,
                () -> runner.render("bad diagram", output));
        assertTrue(ex.getMessage().contains("syntax error near line 2"));
    }

    @Test
    void throwsWhenOutputFileMissingDespiteSuccess() throws Exception {
        Path output = tempDir.resolve("mermaid-1.png");
        when(processExecutor.execute(anyList()))
                .thenReturn(new ProcessResult(0, "", ""));

        ProcessMermaidCliRunner runner =
                new ProcessMermaidCliRunner("mmdc", "white", processExecutor);

        MermaidRenderingException ex = assertThrows(MermaidRenderingException.class,
                () -> runner.render("graph TD; A-->B;", output));
        assertTrue(ex.getMessage().contains("no output file"));
    }

    @Test
    void wrapsIoExceptionWithHelpfulMessage() throws Exception {
        Path output = tempDir.resolve("mermaid-1.png");
        when(processExecutor.execute(anyList()))
                .thenThrow(new IOException("Cannot run program \"mmdc\""));

        ProcessMermaidCliRunner runner =
                new ProcessMermaidCliRunner("mmdc", "white", processExecutor);

        MermaidRenderingException ex = assertThrows(MermaidRenderingException.class,
                () -> runner.render("graph TD; A-->B;", output));
        assertTrue(ex.getMessage().contains("PATH"));
        assertTrue(ex.getCause() instanceof IOException);
    }
}
