package com.dnestr.base.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Minimal CSV reader for small test-data files (e.g. a lookup table of users or expected values
 * checked into test resources) — read once with {@link #readRows}, then look values up with
 * {@link #cell}. Not a general-purpose CSV parser: it handles comma separators, quoted cells
 * (which may contain commas and {@code ""}-escaped quotes) and a leading UTF-8 BOM, but not a
 * quoted cell spanning several lines, nor other separators.
 */
public final class CsvReaderUtils {

    private static final char BOM = '﻿';

    private CsvReaderUtils() {}

    /**
     * Every non-blank line of the UTF-8 {@code file} as its cells, the header row first. Cells are
     * returned as written (untrimmed); a leading byte-order mark is dropped.
     *
     * @throws UncheckedIOException if the file can't be read
     */
    public static List<List<String>> readRows(Path file) {
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            return reader.lines()
                    .map(line -> !line.isEmpty() && line.charAt(0) == BOM ? line.substring(1) : line)
                    .filter(line -> !line.isBlank())
                    .map(CsvReaderUtils::cells)
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * The trimmed {@code column} cell of the first data row whose trimmed {@code keyColumn} cell
     * equals {@code key}; empty when {@code rows} is empty or no row matches. Header names are
     * matched trimmed too. A matching row too short to reach {@code column} yields {@code ""}.
     *
     * @param rows rows as returned by {@link #readRows}, the header row first
     * @throws IllegalArgumentException if the header row lacks {@code keyColumn} or {@code column}
     */
    public static Optional<String> cell(List<List<String>> rows, String keyColumn, String key, String column) {
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        List<String> headers = rows.getFirst().stream().map(String::trim).toList();
        int keyIndex = headers.indexOf(keyColumn);
        int columnIndex = headers.indexOf(column);
        if (keyIndex < 0 || columnIndex < 0) {
            throw new IllegalArgumentException("Expected columns \"%s\" and \"%s\" in %s".formatted(keyColumn, column, headers));
        }
        return rows.stream().skip(1)
                .filter(row -> keyIndex < row.size() && key.equals(row.get(keyIndex).trim()))
                .findFirst()
                .map(row -> columnIndex < row.size() ? row.get(columnIndex).trim() : "");
    }

    /** One CSV line's cells; a quoted cell may hold commas and doubled quotes, never a line break here. */
    private static List<String> cells(String line) {
        List<String> cells = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quoted && c == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                cell.append('"');
                i++;
            } else if (c == '"') {
                quoted = !quoted;
            } else if (c == ',' && !quoted) {
                cells.add(cell.toString());
                cell.setLength(0);
            } else {
                cell.append(c);
            }
        }
        cells.add(cell.toString());
        return cells;
    }
}
