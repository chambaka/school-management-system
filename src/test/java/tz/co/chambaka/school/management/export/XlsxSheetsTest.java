package tz.co.chambaka.school.management.export;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class XlsxSheetsTest {

    @Test
    void readsInlineStringsWrittenByThisApp() {
        byte[] bytes = XlsxDocuments.xlsx("Marks", List.of("Admission number", "Marks"),
                List.of(List.of("ADM-001", "78")));
        List<XlsxSheets.SheetRow> rows = XlsxSheets.rows(bytes);
        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).cells()).containsExactly("Admission number", "Marks");
        assertThat(rows.get(1).number()).isEqualTo(2);
        assertThat(rows.get(1).cells()).containsExactly("ADM-001", "78");
    }

    @Test
    void readsSharedStringsAndNumericCells() throws Exception {
        byte[] bytes = excelWithSharedStrings();
        List<XlsxSheets.SheetRow> rows = XlsxSheets.rows(bytes);
        assertThat(rows.get(0).cells()).containsExactly("Admission number", "Student name", "Marks");
        assertThat(rows.get(1).cells()).containsExactly("ADM-001", "Amina", "80");
        assertThat(XlsxSheets.columnIndex("C2", 0)).isEqualTo(2);
        assertThat(XlsxSheets.columnIndex("AA10", 0)).isEqualTo(26);
    }

    @Test
    void rejectsAFileThatIsNotAWorkbook() {
        assertThatThrownBy(() -> XlsxSheets.rows(new byte[] {1, 2, 3, 4}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Excel workbook");
    }

    private static byte[] excelWithSharedStrings() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            put(zip, "[Content_Types].xml", """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                      <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                      <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                      <Override PartName="/xl/sharedStrings.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml"/>
                    </Types>
                    """);
            put(zip, "xl/workbook.xml", """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
                              xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                      <sheets><sheet name="Marks" sheetId="1" r:id="rId1"/></sheets>
                    </workbook>
                    """);
            put(zip, "xl/_rels/workbook.xml.rels", """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                      <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
                    </Relationships>
                    """);
            put(zip, "xl/sharedStrings.xml", """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                      <si><t>Admission number</t></si>
                      <si><t>Student name</t></si>
                      <si><t>Marks</t></si>
                      <si><t>ADM-001</t></si>
                      <si><t>Amina</t></si>
                    </sst>
                    """);
            put(zip, "xl/worksheets/sheet1.xml", """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                      <sheetData>
                        <row r="1">
                          <c r="A1" t="s"><v>0</v></c>
                          <c r="B1" t="s"><v>1</v></c>
                          <c r="C1" t="s"><v>2</v></c>
                        </row>
                        <row r="2">
                          <c r="A2" t="s"><v>3</v></c>
                          <c r="B2" t="s"><v>4</v></c>
                          <c r="C2"><v>80.0</v></c>
                        </row>
                      </sheetData>
                    </worksheet>
                    """);
        }
        return out.toByteArray();
    }

    private static void put(ZipOutputStream zip, String name, String xml) throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(xml.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }
}
