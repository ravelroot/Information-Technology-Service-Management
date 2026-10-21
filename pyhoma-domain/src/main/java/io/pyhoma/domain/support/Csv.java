package io.pyhoma.domain.support;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Csv {

    private Csv() {
    }

    public static String write(List<String> header, List<List<String>> rows, char separator) {
        StringBuilder sb = new StringBuilder();
        appendRow(sb, header, separator);
        for (List<String> r : rows) {
            appendRow(sb, r, separator);
        }
        return sb.toString();
    }

    private static void appendRow(StringBuilder sb, List<String> cells, char sep) {
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) {
                sb.append(sep);
            }
            String c = neutralize(cells.get(i) == null ? "" : cells.get(i));
            boolean quote = c.indexOf(sep) >= 0 || c.indexOf('"') >= 0 || c.indexOf('\n') >= 0 || c.indexOf('\r') >= 0;
            if (quote) {
                sb.append('"').append(c.replace("\"", "\"\"")).append('"');
            } else {
                sb.append(c);
            }
        }
        sb.append("\r\n");
    }

    static String neutralize(String cell) {
        if (cell.isEmpty()) {
            return cell;
        }
        char first = cell.charAt(0);
        boolean risky = first == '=' || first == '+' || first == '-' || first == '@' || first == '\t' || first == '\r'
                || first == '|' || first == '%';
        if (!risky || cell.matches("[-+]?\\d+([.,]\\d+)?")) {
            return cell;
        }
        return "'" + cell;
    }

    public static List<List<String>> parse(String text) {
        String t = text.startsWith("﻿") ? text.substring(1) : text;
        int firstBreak = t.indexOf('\n');
        String head = firstBreak < 0 ? t : t.substring(0, firstBreak);
        char sep = head.chars().filter(ch -> ch == ';').count() >= head.chars().filter(ch -> ch == ',').count() ? ';' : ',';
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < t.length(); i++) {
            char ch = t.charAt(i);
            if (quoted) {
                if (ch == '"') {
                    if (i + 1 < t.length() && t.charAt(i + 1) == '"') {
                        cell.append('"');
                        i++;
                    } else {
                        quoted = false;
                    }
                } else {
                    cell.append(ch);
                }
            } else if (ch == '"') {
                quoted = true;
            } else if (ch == sep) {
                row.add(cell.toString());
                cell.setLength(0);
            } else if (ch == '\n' || ch == '\r') {
                if (ch == '\r' && i + 1 < t.length() && t.charAt(i + 1) == '\n') {
                    i++;
                }
                row.add(cell.toString());
                cell.setLength(0);
                if (!(row.size() == 1 && row.get(0).isEmpty())) {
                    rows.add(row);
                }
                row = new ArrayList<>();
            } else {
                cell.append(ch);
            }
        }
        if (cell.length() > 0 || !row.isEmpty()) {
            row.add(cell.toString());
            rows.add(row);
        }
        return rows;
    }

    public static List<Map<String, String>> parseWithHeader(String text) {
        List<List<String>> rows = parse(text);
        List<Map<String, String>> result = new ArrayList<>();
        if (rows.isEmpty()) {
            return result;
        }
        List<String> header = rows.get(0).stream().map(String::trim).toList();
        for (int r = 1; r < rows.size(); r++) {
            Map<String, String> m = new LinkedHashMap<>();
            List<String> row = rows.get(r);
            for (int c = 0; c < header.size(); c++) {
                m.put(header.get(c), c < row.size() ? row.get(c) : "");
            }
            result.add(m);
        }
        return result;
    }

    public static Integer toInteger(String s) {
        String c = Texts.clean(s);
        if (c == null) {
            return null;
        }
        try {
            return Integer.valueOf(c);
        } catch (NumberFormatException e) {
            throw new BusinessException("validation.number", s);
        }
    }

    public static LocalDate toDate(String s) {
        String c = Texts.clean(s);
        if (c == null) {
            return null;
        }
        try {
            return c.contains("/") ? LocalDate.parse(c, DateTimeFormatter.ofPattern("dd/MM/yyyy")) : LocalDate.parse(c);
        } catch (DateTimeParseException e) {
            throw new BusinessException("validation.date", s);
        }
    }
}
