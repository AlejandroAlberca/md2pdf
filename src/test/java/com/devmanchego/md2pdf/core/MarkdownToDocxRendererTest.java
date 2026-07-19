package com.devmanchego.md2pdf.core;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownToDocxRendererTest {

    private final MarkdownToDocxRenderer renderer = new MarkdownToDocxRenderer();

    @Test
    void rendersMarkdownToNonEmptyDocxPackage() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        renderer.render("# Title\n\nSome **bold** text.", output);

        byte[] bytes = output.toByteArray();
        assertTrue(bytes.length > 0, "DOCX output should not be empty");
        assertTrue(containsEntry(bytes, "word/document.xml"),
                "a valid .docx is a zip containing word/document.xml");
    }

    @Test
    void rendersTablesAndTaskLists() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        String markdown = "| A | B |\n|---|---|\n| 1 | 2 |\n\n- [x] done\n- [ ] todo\n";

        renderer.render(markdown, output);

        assertTrue(output.size() > 0);
        assertTrue(containsEntry(output.toByteArray(), "word/document.xml"));
    }

    @Test
    void rejectsNullMarkdown() {
        assertThrows(NullPointerException.class,
                () -> renderer.render(null, new ByteArrayOutputStream()));
    }

    @Test
    void rejectsNullOutputStream() {
        assertThrows(NullPointerException.class,
                () -> renderer.render("# Title", null));
    }

    private static boolean containsEntry(byte[] zipBytes, String entryName) throws Exception {
        try (ZipInputStream zip = new ZipInputStream(new java.io.ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.getName().equals(entryName)) {
                    return true;
                }
            }
        }
        return false;
    }
}
