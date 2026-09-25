package tz.co.chambaka.school.management.export;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipInputStream;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;

class XlsxDocumentsTest {

    @Test
    void buildsSpreadsheetAndEscapesCells() throws Exception {
        byte[] bytes = XlsxDocuments.xlsx("Marks & more", List.of("A", "B"),
                List.of(List.of("1 < 2", "ok"), java.util.Arrays.asList(null, "x")));
        assertThat(bytes[0]).isEqualTo((byte) 'P');
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            assertThat(zip.getNextEntry().getName()).isEqualTo("[Content_Types].xml");
        }
        assertThat(XlsxDocuments.colName(0)).isEqualTo("A");
        assertThat(XlsxDocuments.colName(26)).isEqualTo("AA");
        assertThat(XlsxDocuments.escape("<a&\"b>")).contains("&lt;").contains("&amp;").contains("&quot;");
        assertThat(XlsxDocuments.escape(null)).isEmpty();
        byte[] unnamed = XlsxDocuments.xlsx("  ", List.of("C"), List.of());
        assertThat(workbookXml(unnamed)).contains("Sheet1");
    }

    private static String workbookXml(byte[] bytes) throws Exception {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if ("xl/workbook.xml".equals(entry.getName())) {
                    return new String(zip.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        }
        throw new AssertionError("workbook.xml missing");
    }
}
