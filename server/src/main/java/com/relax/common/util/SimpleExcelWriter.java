package com.relax.common.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Pure Java OpenXML (.xlsx) generator without external dependencies.
 * Generates valid Microsoft Excel / WeChat openDocument compatible spreadsheets.
 */
public final class SimpleExcelWriter {

    private SimpleExcelWriter() {}

    public static byte[] writeWorkbook(String sheetName, List<List<Object>> rows) throws IOException {
        String safeSheetName = (sheetName == null || sheetName.isBlank()) ? "Sheet1" : escapeXml(sheetName);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos, StandardCharsets.UTF_8)) {

            // 1. [Content_Types].xml
            addZipEntry(zos, "[Content_Types].xml",
                    """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                      <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                      <Default Extension="xml" ContentType="application/xml"/>
                      <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                      <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                    </Types>
                    """.stripIndent());

            // 2. _rels/.rels
            addZipEntry(zos, "_rels/.rels",
                    """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                      <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
                    </Relationships>
                    """.stripIndent());

            // 3. xl/_rels/workbook.xml.rels
            addZipEntry(zos, "xl/_rels/workbook.xml.rels",
                    """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                      <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
                    </Relationships>
                    """.stripIndent());

            // 4. xl/workbook.xml
            String workbookXml = String.format(
                    """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                      <sheets>
                        <sheet name="%s" sheetId="1" r:id="rId1"/>
                      </sheets>
                    </workbook>
                    """.stripIndent(), safeSheetName);
            addZipEntry(zos, "xl/workbook.xml", workbookXml);

            // 5. xl/worksheets/sheet1.xml
            StringBuilder sheetXml = new StringBuilder();
            sheetXml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
            sheetXml.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">\n");
            sheetXml.append("  <sheetData>\n");

            for (int rIdx = 0; rIdx < rows.size(); rIdx++) {
                int rowNum = rIdx + 1;
                List<Object> row = rows.get(rIdx);
                sheetXml.append("    <row r=\"").append(rowNum).append("\">\n");

                for (int cIdx = 0; cIdx < row.size(); cIdx++) {
                    Object val = row.get(cIdx);
                    if (val == null) continue;

                    String cellRef = getColLetter(cIdx) + rowNum;
                    if (val instanceof Number) {
                        sheetXml.append("      <c r=\"").append(cellRef).append("\"><v>")
                                .append(val).append("</v></c>\n");
                    } else {
                        sheetXml.append("      <c r=\"").append(cellRef).append("\" t=\"inlineStr\"><is><t>")
                                .append(escapeXml(val.toString())).append("</t></is></c>\n");
                    }
                }
                sheetXml.append("    </row>\n");
            }

            sheetXml.append("  </sheetData>\n");
            sheetXml.append("</worksheet>\n");

            addZipEntry(zos, "xl/worksheets/sheet1.xml", sheetXml.toString());
        }

        return baos.toByteArray();
    }

    private static void addZipEntry(ZipOutputStream zos, String entryName, String content) throws IOException {
        ZipEntry entry = new ZipEntry(entryName);
        zos.putNextEntry(entry);
        zos.write(content.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }

    public static String getColLetter(int col) {
        StringBuilder sb = new StringBuilder();
        int c = col + 1;
        while (c > 0) {
            int rem = (c - 1) % 26;
            sb.insert(0, (char) ('A' + rem));
            c = (c - 1) / 26;
        }
        return sb.toString();
    }

    public static String escapeXml(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            switch (ch) {
                case '&' -> sb.append("&amp;");
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '"' -> sb.append("&quot;");
                case '\'' -> sb.append("&apos;");
                default -> {
                    // Filter out invalid XML characters
                    if (ch == 0x9 || ch == 0xA || ch == 0xD || (ch >= 0x20 && ch <= 0xD7FF) || (ch >= 0xE000 && ch <= 0xFFFD)) {
                        sb.append(ch);
                    }
                }
            }
        }
        return sb.toString();
    }
}
