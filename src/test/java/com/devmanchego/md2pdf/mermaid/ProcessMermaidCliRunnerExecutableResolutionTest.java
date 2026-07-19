package com.devmanchego.md2pdf.mermaid;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Exercises {@link ProcessMermaidCliRunner#resolveExecutable} in isolation. It is
 * parameterised on the platform flag and PATH string precisely so these cases can
 * run deterministically on any OS, independent of the actual test machine.
 */
class ProcessMermaidCliRunnerExecutableResolutionTest {

    @TempDir
    Path tempDir;

    @Test
    void resolvesToCmdWrapperFoundOnPath() throws Exception {
        Path npmDir = Files.createDirectory(tempDir.resolve("npm"));
        Path mmdcCmd = Files.createFile(npmDir.resolve("mmdc.cmd"));

        String resolved = ProcessMermaidCliRunner.resolveExecutable("mmdc", npmDir.toString(), true);

        assertEquals(mmdcCmd.toString(), resolved);
    }

    @Test
    void searchesEachPathEntryInOrder() throws Exception {
        Path empty = Files.createDirectory(tempDir.resolve("empty"));
        Path npmDir = Files.createDirectory(tempDir.resolve("npm"));
        Path mmdcCmd = Files.createFile(npmDir.resolve("mmdc.cmd"));
        String path = empty + File.pathSeparator + npmDir;

        String resolved = ProcessMermaidCliRunner.resolveExecutable("mmdc", path, true);

        assertEquals(mmdcCmd.toString(), resolved);
    }

    @Test
    void leavesNameUnchangedWhenNotFoundOnPath() {
        String resolved = ProcessMermaidCliRunner.resolveExecutable("mmdc", tempDir.toString(), true);

        assertEquals("mmdc", resolved);
    }

    @Test
    void leavesNameUnchangedOnNonWindowsPlatforms() throws Exception {
        Path npmDir = Files.createDirectory(tempDir.resolve("npm"));
        Files.createFile(npmDir.resolve("mmdc.cmd"));

        String resolved = ProcessMermaidCliRunner.resolveExecutable("mmdc", npmDir.toString(), false);

        assertEquals("mmdc", resolved);
    }

    @Test
    void leavesNameUnchangedWhenAlreadyHasAnExtension() throws Exception {
        Path npmDir = Files.createDirectory(tempDir.resolve("npm"));
        Files.createFile(npmDir.resolve("mmdc.cmd"));

        String resolved = ProcessMermaidCliRunner.resolveExecutable("mmdc.cmd", npmDir.toString(), true);

        assertEquals("mmdc.cmd", resolved);
    }

    @Test
    void leavesExplicitPathUnchanged() {
        String explicitPath = tempDir.resolve("custom").resolve("mmdc").toString();

        String resolved = ProcessMermaidCliRunner.resolveExecutable(explicitPath, tempDir.toString(), true);

        assertEquals(explicitPath, resolved);
    }

    @Test
    void handlesNullOrBlankPath() {
        assertEquals("mmdc", ProcessMermaidCliRunner.resolveExecutable("mmdc", null, true));
        assertEquals("mmdc", ProcessMermaidCliRunner.resolveExecutable("mmdc", "  ", true));
    }
}
