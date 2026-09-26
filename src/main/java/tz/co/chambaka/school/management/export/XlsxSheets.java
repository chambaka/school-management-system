package tz.co.chambaka.school.management.export;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class XlsxSheets {

    private static final int MAX_ENTRIES = 40;
    private static final int MAX_BYTES = 8_000_000;
    private static final String REL_NS = "http://schemas.openxmlformats.org/officeDocument/2006/relationships";

    private XlsxSheets() {
    }

    public record SheetRow(int number, List<String> cells) {
    }

    public static List<SheetRow> rows(byte[] bytes) {
        if (bytes == null || bytes.length < 4 || bytes[0] != 'P' || bytes[1] != 'K') {
            throw new IllegalArgumentException("This file is not an Excel workbook. Download the template from Grades.");
        }
        Map<String, byte[]> parts = unzip(bytes);
        List<String> shared = sharedStrings(parts.get("xl/sharedStrings.xml"));
        byte[] sheet = parts.get(sheetPath(parts));
        if (sheet == null) {
            throw new IllegalArgumentException("This file is not an Excel workbook. Download the template from Grades.");
        }
        return parseSheet(sheet, shared);
    }

    private static Map<String, byte[]> unzip(byte[] bytes) {
        Map<String, byte[]> parts = new TreeMap<>();
        int total = 0;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            int count = 0;
            while ((entry = zip.getNextEntry()) != null) {
                if (++count > MAX_ENTRIES) {
                    throw new IllegalArgumentException("This Excel file is too large to import.");
                }
                String name = entry.getName().replace('\\', '/');
                if (name.contains("..")) {
                    throw new IllegalArgumentException("This file is not an Excel workbook. Download the template from Grades.");
                }
                byte[] body = zip.readAllBytes();
                total += body.length;
                if (total > MAX_BYTES) {
                    throw new IllegalArgumentException("This Excel file is too large to import.");
                }
                parts.put(name, body);
            }
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("This file is not an Excel workbook. Download the template from Grades.");
        }
        return parts;
    }

    private static String sheetPath(Map<String, byte[]> parts) {
        byte[] workbook = parts.get("xl/workbook.xml");
        byte[] rels = parts.get("xl/_rels/workbook.xml.rels");
        if (workbook == null || rels == null) {
            return "xl/worksheets/sheet1.xml";
        }
        Element firstSheet = first(parse(workbook), "sheet");
        if (firstSheet == null) {
            return "xl/worksheets/sheet1.xml";
        }
        String id = firstSheet.getAttributeNS(REL_NS, "id");
        if (id == null || id.isBlank()) {
            id = firstSheet.getAttribute("r:id");
        }
        Element match = null;
        for (Element relationship : elements(parse(rels), "Relationship")) {
            if (id.equals(relationship.getAttribute("Id"))) {
                match = relationship;
                break;
            }
        }
        if (match == null) {
            return "xl/worksheets/sheet1.xml";
        }
        String target = match.getAttribute("Target").replace('\\', '/');
        if (target.startsWith("/")) {
            target = target.substring(1);
        }
        if (!target.startsWith("xl/")) {
            target = "xl/" + target;
        }
        return target;
    }

    private static List<String> sharedStrings(byte[] xml) {
        List<String> values = new ArrayList<>();
        if (xml == null) {
            return values;
        }
        for (Element item : elements(parse(xml), "si")) {
            StringBuilder text = new StringBuilder();
            for (Element part : elements(item, "t")) {
                text.append(part.getTextContent());
            }
            values.add(text.toString());
        }
        return values;
    }

    private static List<SheetRow> parseSheet(byte[] xml, List<String> shared) {
        List<SheetRow> rows = new ArrayList<>();
        int fallback = 1;
        for (Element row : elements(parse(xml), "row")) {
            int number = numberAttr(row.getAttribute("r"), fallback);
            fallback = number + 1;
            TreeMap<Integer, String> cells = new TreeMap<>();
            int next = 0;
            for (Element cell : elements(row, "c")) {
                int column = columnIndex(cell.getAttribute("r"), next);
                next = column + 1;
                cells.put(column, cellText(cell, shared));
            }
            if (cells.isEmpty()) {
                continue;
            }
            int last = cells.lastKey();
            List<String> values = new ArrayList<>();
            for (int i = 0; i <= last; i++) {
                values.add(cells.getOrDefault(i, ""));
            }
            if (values.stream().allMatch(String::isBlank)) {
                continue;
            }
            rows.add(new SheetRow(number, values));
        }
        return rows;
    }

    private static String cellText(Element cell, List<String> shared) {
        String type = cell.getAttribute("t");
        if ("inlineStr".equals(type)) {
            return textOf(cell, "t");
        }
        String raw = textOf(cell, "v");
        if ("s".equals(type)) {
            try {
                int index = Integer.parseInt(raw.trim());
                return index >= 0 && index < shared.size() ? shared.get(index) : "";
            } catch (NumberFormatException ex) {
                return "";
            }
        }
        if ("b".equals(type)) {
            return "1".equals(raw.trim()) ? "TRUE" : "FALSE";
        }
        if (raw.isBlank()) {
            return "";
        }
        if (type == null || type.isBlank() || "n".equals(type)) {
            return plainNumber(raw.trim());
        }
        return raw.trim();
    }

    private static String plainNumber(String raw) {
        try {
            return new BigDecimal(raw).stripTrailingZeros().toPlainString();
        } catch (NumberFormatException ex) {
            return raw;
        }
    }

    private static String textOf(Element parent, String name) {
        StringBuilder text = new StringBuilder();
        for (Element child : elements(parent, name)) {
            text.append(child.getTextContent());
        }
        return text.toString();
    }

    static int columnIndex(String ref, int fallback) {
        if (ref == null || ref.isBlank()) {
            return fallback;
        }
        int column = 0;
        int i = 0;
        while (i < ref.length() && Character.isLetter(ref.charAt(i))) {
            column = column * 26 + (Character.toUpperCase(ref.charAt(i)) - 'A' + 1);
            i++;
        }
        return column == 0 ? fallback : column - 1;
    }

    private static int numberAttr(String raw, int fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private static Element first(Element root, String name) {
        List<Element> found = elements(root, name);
        return found.isEmpty() ? null : found.getFirst();
    }

    private static List<Element> elements(Element root, String name) {
        List<Element> found = new ArrayList<>();
        collect(root, name.toLowerCase(Locale.ROOT), found);
        return found;
    }

    private static void collect(Node node, String name, List<Element> found) {
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element element) {
                if (element.getLocalName() != null && element.getLocalName().equalsIgnoreCase(name)) {
                    found.add(element);
                }
                collect(element, name, found);
            }
        }
    }

    private static Element parse(byte[] xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setExpandEntityReferences(false);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            return factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml)).getDocumentElement();
        } catch (Exception ex) {
            throw new IllegalArgumentException("This file is not an Excel workbook. Download the template from Grades.");
        }
    }
}
