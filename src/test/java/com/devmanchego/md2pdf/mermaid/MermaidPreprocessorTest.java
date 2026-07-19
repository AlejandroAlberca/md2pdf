package com.devmanchego.md2pdf.mermaid;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class MermaidPreprocessorTest {

    @Mock
    private MermaidCliRunner runner;

    private final Path imageDir = Path.of("target", "images");

    @Test
    void replacesSingleMermaidBlockWithImageTag() {
        MermaidPreprocessor preprocessor = new MermaidPreprocessor(runner);
        String markdown = "# Title\n\n```mermaid\ngraph TD; A-->B;\n```\n\nAfter.";

        String result = preprocessor.process(markdown, imageDir);

        assertFalse(result.contains("```mermaid"), "fence should be removed");
        assertTrue(result.contains("!["), "a Markdown image reference should be inserted");
        assertTrue(result.contains("mermaid-1.png"), "image name should reference the first diagram");
        assertTrue(result.contains("# Title"), "surrounding text should be preserved");
        assertTrue(result.contains("After."), "trailing text should be preserved");
    }

    @Test
    void passesDiagramSourceWithoutFencesToRunner() {
        MermaidPreprocessor preprocessor = new MermaidPreprocessor(runner);
        String markdown = "```mermaid\ngraph TD;\n  A-->B;\n```";

        preprocessor.process(markdown, imageDir);

        ArgumentCaptor<String> source = ArgumentCaptor.forClass(String.class);
        verify(runner).render(source.capture(), org.mockito.ArgumentMatchers.any(Path.class));
        assertEquals("graph TD;\n  A-->B;", source.getValue());
    }

    @Test
    void rendersEveryBlockInDocument() {
        MermaidPreprocessor preprocessor = new MermaidPreprocessor(runner);
        String markdown = """
                ```mermaid
                graph TD; A-->B;
                ```

                text

                ```mermaid
                sequenceDiagram
                  A->>B: hi
                ```
                """;

        String result = preprocessor.process(markdown, imageDir);

        verify(runner, times(2)).render(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(Path.class));
        assertTrue(result.contains("mermaid-1.png"), "first diagram image expected");
        assertTrue(result.contains("mermaid-2.png"), "second diagram image expected");
    }

    @Test
    void leavesDocumentUntouchedWhenNoMermaidPresent() {
        MermaidPreprocessor preprocessor = new MermaidPreprocessor(runner);
        String markdown = "# Just Markdown\n\n```java\nint x = 1;\n```\n";

        String result = preprocessor.process(markdown, imageDir);

        assertEquals(markdown, result);
        verifyNoInteractions(runner);
    }

    @Test
    void supportsTildeFences() {
        MermaidPreprocessor preprocessor = new MermaidPreprocessor(runner);
        String markdown = "~~~mermaid\ngraph LR; X-->Y;\n~~~";

        String result = preprocessor.process(markdown, imageDir);

        assertTrue(result.contains("!["));
        verify(runner, times(1)).render(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(Path.class));
    }

    @Test
    void textModeLeavesMermaidBlockUnchangedAndNeverInvokesRunner() {
        MermaidPreprocessor preprocessor = new MermaidPreprocessor(runner, MermaidMode.TEXT);
        String markdown = "# Title\n\n```mermaid\ngraph TD; A-->B;\n```\n\nAfter.";

        String result = preprocessor.process(markdown, imageDir);

        assertEquals(markdown, result, "the fenced block should be passed through untouched");
        verifyNoInteractions(runner);
    }

    @Test
    void textModeHandlesMultipleBlocksAndTildeFences() {
        MermaidPreprocessor preprocessor = new MermaidPreprocessor(runner, MermaidMode.TEXT);
        String markdown = "~~~mermaid\ngraph LR; X-->Y;\n~~~\n\ntext\n\n```mermaid\nsequenceDiagram\n```";

        String result = preprocessor.process(markdown, imageDir);

        assertEquals(markdown, result);
        verifyNoInteractions(runner);
    }
}
