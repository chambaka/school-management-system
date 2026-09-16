package tz.co.chambaka.school.management.export;

import java.nio.charset.StandardCharsets;
import java.util.List;

public final class SimpleDocuments {

    private SimpleDocuments() {
    }

    public static byte[] csv(List<String> headers, List<List<String>> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append('\uFEFF');
        sb.append(String.join(",", headers.stream().map(SimpleDocuments::csvCell).toList()));
        sb.append('\n');
        for (List<String> row : rows) {
            sb.append(String.join(",", row.stream().map(SimpleDocuments::csvCell).toList()));
            sb.append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] pdf(String title, List<String> lines) {
        StringBuilder content = new StringBuilder();
        content.append("BT /F1 16 Tf 50 780 Td (").append(pdfEscape(title)).append(") Tj ET\n");
        int y = 750;
        for (String line : lines) {
            if (y < 60) {
                break;
            }
            content.append("BT /F1 11 Tf 50 ").append(y).append(" Td (").append(pdfEscape(line)).append(") Tj ET\n");
            y -= 16;
        }
        String stream = content.toString();
        StringBuilder pdf = new StringBuilder();
        pdf.append("%PDF-1.4\n");
        int[] offsets = new int[6];
        offsets[1] = pdf.length();
        pdf.append("1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n");
        offsets[2] = pdf.length();
        pdf.append("2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj\n");
        offsets[3] = pdf.length();
        pdf.append("3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >> endobj\n");
        offsets[4] = pdf.length();
        pdf.append("4 0 obj << /Length ").append(stream.length()).append(" >> stream\n").append(stream).append("endstream endobj\n");
        offsets[5] = pdf.length();
        pdf.append("5 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> endobj\n");
        int xref = pdf.length();
        pdf.append("xref\n0 6\n0000000000 65535 f \n");
        for (int i = 1; i <= 5; i++) {
            pdf.append(String.format("%010d 00000 n \n", offsets[i]));
        }
        pdf.append("trailer << /Size 6 /Root 1 0 R >>\nstartxref\n").append(xref).append("\n%%EOF");
        return pdf.toString().getBytes(StandardCharsets.ISO_8859_1);
    }

    private static String csvCell(String value) {
        String raw = value == null ? "" : value;
        if (raw.contains(",") || raw.contains("\"") || raw.contains("\n")) {
            return "\"" + raw.replace("\"", "\"\"") + "\"";
        }
        return raw;
    }

    private static String pdfEscape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)").replace("\n", " ");
    }
}
