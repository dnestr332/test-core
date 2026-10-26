package com.dnestr.base.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CsvReaderUtilsTest {

    @TempDir
    Path dir;

    private Path csv(String content) throws IOException {
        return Files.writeString(dir.resolve("data.csv"), content);
    }

    @Test
    void readRows_splitsLinesIntoCells_headerFirst() throws IOException {
        List<List<String>> rows = CsvReaderUtils.readRows(csv("id,name\n1,Alice\n2,Bob\n"));

        assertThat(rows).containsExactly(List.of("id", "name"), List.of("1", "Alice"), List.of("2", "Bob"));
    }

    @Test
    void readRows_skipsEmptyAndBlankLines() throws IOException {
        List<List<String>> rows = CsvReaderUtils.readRows(csv("id,name\n\n1,Alice\n   \n"));

        assertThat(rows).containsExactly(List.of("id", "name"), List.of("1", "Alice"));
    }

    @Test
    void readRows_handlesCrlfLineEndings() throws IOException {
        List<List<String>> rows = CsvReaderUtils.readRows(csv("id,name\r\n1,Alice\r\n"));

        assertThat(rows).containsExactly(List.of("id", "name"), List.of("1", "Alice"));
    }

    @Test
    void readRows_stripsLeadingByteOrderMark() throws IOException {
        List<List<String>> rows = CsvReaderUtils.readRows(csv("﻿id,name\n1,Alice\n"));

        assertThat(rows.getFirst()).containsExactly("id", "name");
    }

    @Test
    void readRows_quotedCell_keepsCommasAndUnescapesDoubledQuotes() throws IOException {
        List<List<String>> rows = CsvReaderUtils.readRows(csv("id,note\n1,\"a, \"\"quoted\"\" b\"\n"));

        assertThat(rows.get(1)).containsExactly("1", "a, \"quoted\" b");
    }

    @Test
    void readRows_keepsEmptyCells_includingTrailing() throws IOException {
        List<List<String>> rows = CsvReaderUtils.readRows(csv("a,b,c\n,x,\n"));

        assertThat(rows.get(1)).containsExactly("", "x", "");
    }

    @Test
    void readRows_missingFile_throwsUncheckedIOException() {
        assertThatThrownBy(() -> CsvReaderUtils.readRows(dir.resolve("missing.csv")))
                .isInstanceOf(UncheckedIOException.class);
    }

    @Test
    void cell_returnsColumnOfFirstMatchingRow() {
        List<List<String>> rows = List.of(
                List.of("id", "name"),
                List.of("1", "Alice"),
                List.of("1", "Duplicate"));

        assertThat(CsvReaderUtils.cell(rows, "id", "1", "name")).contains("Alice");
    }

    @Test
    void cell_trimsHeadersKeysAndValue() {
        List<List<String>> rows = List.of(
                List.of(" id ", " name "),
                List.of(" 1 ", " Alice "));

        assertThat(CsvReaderUtils.cell(rows, "id", "1", "name")).contains("Alice");
    }

    @Test
    void cell_noMatchingRow_returnsEmpty() {
        List<List<String>> rows = List.of(List.of("id", "name"), List.of("1", "Alice"));

        assertThat(CsvReaderUtils.cell(rows, "id", "2", "name")).isEmpty();
    }

    @Test
    void cell_emptyRows_returnsEmpty() {
        assertThat(CsvReaderUtils.cell(List.of(), "id", "1", "name")).isEmpty();
    }

    @Test
    void cell_rowTooShortForColumn_returnsEmptyString() {
        List<List<String>> rows = List.of(List.of("id", "name"), List.of("1"));

        assertThat(CsvReaderUtils.cell(rows, "id", "1", "name")).contains("");
    }

    @Test
    void cell_rowTooShortForKeyColumn_isSkipped() {
        List<List<String>> rows = List.of(
                List.of("name", "id"),
                List.of("Short"),
                List.of("Alice", "1"));

        assertThat(CsvReaderUtils.cell(rows, "id", "1", "name")).contains("Alice");
    }

    @Test
    void cell_unknownColumn_throwsNamingColumnsAndHeaders() {
        List<List<String>> rows = List.of(List.of("id", "name"));

        assertThatThrownBy(() -> CsvReaderUtils.cell(rows, "id", "1", "email"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("\"email\"")
                .hasMessageContaining("[id, name]");
    }

    @Test
    void readRowsThenCell_endToEnd() throws IOException {
        List<List<String>> rows = CsvReaderUtils.readRows(csv("﻿login,password\nadmin,\"p,ss\"\n"));

        assertThat(CsvReaderUtils.cell(rows, "login", "admin", "password")).contains("p,ss");
    }
}
