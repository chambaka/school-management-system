package tz.co.chambaka.school.management.export;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class XlsxDocuments {

    private XlsxDocuments() {
    }

    public static byte[] xlsx(String sheetName, List<String> headers, List<List<String>> rows) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(out)) {
                put(zip, "[Content_Types].xml", contentTypes());
                put(zip, "_rels/.rels", rels());
                put(zip, "xl/workbook.xml", workbook(sheetName));
                put(zip, "xl/_rels/workbook.xml.rels", workbookRels());
                put(zip, "xl/worksheets/sheet1.xml", sheet(headers, rows));
            }
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Could not build spreadsheet", ex);
        }
    }

    private static void put(ZipOutputStream zip, String name, String xml) throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(xml.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static String contentTypes() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                  <Default Extension="xml" ContentType="application/xml"/>
                  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                </Types>
                """;
    }

    private static String rels() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
                </Relationships>
                """;
    }

    private static String workbook(String sheetName) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
                          xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                  <sheets><sheet name="%s" sheetId="1" r:id="rId1"/></sheets>
                </workbook>
                """.formatted(escape(sheetName == null || sheetName.isBlank() ? "Sheet1" : sheetName));
    }

    private static String workbookRels() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
                </Relationships>
                """;
    }

    private static String sheet(List<String> headers, List<List<String>> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>");
        sb.append(rowXml(1, headers));
        int index = 2;
        for (List<String> row : rows) {
            sb.append(rowXml(index++, row));
        }
        sb.append("</sheetData></worksheet>");
        return sb.toString();
    }

    private static String rowXml(int rowIndex, List<String> cells) {
        StringBuilder sb = new StringBuilder();
        sb.append("<row r=\"").append(rowIndex).append("\">");
        for (int i = 0; i < cells.size(); i++) {
            String ref = colName(i) + rowIndex;
            sb.append("<c r=\"").append(ref).append("\" t=\"inlineStr\"><is><t>")
                    .append(escape(cells.get(i)))
                    .append("</t></is></c>");
        }
        sb.append("</row>");
        return sb.toString();
    }

    static String colName(int index) {
        StringBuilder sb = new StringBuilder();
        int n = index;
        do {
            sb.insert(0, (char) ('A' + (n % 26)));
            n = n / 26 - 1;
        } while (n >= 0);
        return sb.toString();
    }

    static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
