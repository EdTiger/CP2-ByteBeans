/*
 * CsvFileUtilities.java
 * Pure CSV file operations extracted from FileHandler.
 */
package services;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Generic CSV file reading, writing, parsing, and column-lookup utilities.
 *
 * <p>This module contains no domain-specific knowledge. It handles
 * RFC-style CSV parsing (quoted fields, escaped double-quotes),
 * column matching by normalised aliases, and safe value extraction.
 * All methods are {@code static}; the class cannot be instantiated.</p>
 */
public final class CsvFileUtilities {

    /** Private constructor prevents instantiation of this utility class. */
    private CsvFileUtilities() {
        // Utility class; prevent instantiation.
    }

    // ---------------------------------------------------------------
    //  CsvTable — in-memory representation of a parsed CSV file
    // ---------------------------------------------------------------

    /**
     * Immutable in-memory representation of a CSV file as a header row
     * and a list of data rows. Both are mutable lists so that callers
     * can add columns or rows before writing the table back to disk.
     */
    public static final class CsvTable {

        /** Column header names from the first non-blank CSV line. */
        final List<String> headers;

        /** Data rows. Each inner list has the same length as {@code headers}. */
        final List<List<String>> rows;

        /**
         * Constructs a CsvTable with the given headers and rows.
         *
         * @param headers the column header names
         * @param rows    the data rows (each list aligned with headers)
         */
        public CsvTable(List<String> headers, List<List<String>> rows) {
            this.headers = headers;
            this.rows = rows;
        }
    }

    // ---------------------------------------------------------------
    //  Reading
    // ---------------------------------------------------------------

    /**
     * Reads a CSV file into a {@link CsvTable}. The first non-blank
     * line becomes the header row; subsequent lines become data rows.
     * A byte-order mark on the first header value is stripped.
     *
     * @param filePath absolute path to the CSV file
     * @return a parsed CsvTable
     * @throws IOException if the file cannot be read or contains
     *                     malformed CSV (unclosed quotes, excess columns)
     */
    static CsvTable readCsvTable(Path filePath) throws IOException {
        List<String> headers = new ArrayList<>();
        List<List<String>> rows = new ArrayList<>();

        // Open the file with UTF-8 encoding for consistent character handling.
        try (BufferedReader reader = Files.newBufferedReader(
                filePath, StandardCharsets.UTF_8)) {

            String line;
            int lineNumber = 0;

            // Read the file line-by-line, skipping blank lines.
            while ((line = reader.readLine()) != null) {
                lineNumber++;

                // Skip blank lines anywhere in the file.
                if (line.trim().isEmpty()) {
                    continue;
                }

                List<String> values;

                // Parse the line using RFC-style CSV rules.
                try {
                    values = parseCsvLine(line);
                } catch (IllegalArgumentException exception) {
                    throw new IOException(
                            "Invalid CSV format on line "
                                    + lineNumber + ".",
                            exception
                    );
                }

                if (headers.isEmpty()) {
                    // First non-blank line becomes the header row.
                    if (!values.isEmpty()) {
                        // Strip a UTF-8 BOM if present on the first value.
                        values.set(0, removeByteOrderMark(values.get(0)));
                    }
                    headers.addAll(values);
                } else {
                    // Data rows must not exceed the header column count.
                    if (values.size() > headers.size()) {
                        throw new IOException(
                                "CSV line " + lineNumber
                                        + " contains more values than the "
                                        + "header. No data was changed."
                        );
                    }

                    // Pad the row to match the header length.
                    ensureRowSize(values, headers.size());
                    rows.add(values);
                }
            }
        }

        return new CsvTable(headers, rows);
    }

    // ---------------------------------------------------------------
    //  Parsing
    // ---------------------------------------------------------------

    /**
     * Parses a single CSV line into a list of trimmed field values.
     * Supports quoted fields and escaped double-quotes ({@code ""}).
     *
     * @param line the raw CSV line (must not be {@code null})
     * @return the list of parsed field values
     * @throws IllegalArgumentException if quotes are not properly closed
     */
    static List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder currentValue = new StringBuilder();
        boolean insideQuotes = false;

        // Walk through every character in the line.
        for (int index = 0; index < line.length(); index++) {
            char currentCharacter = line.charAt(index);

            if (currentCharacter == '"') {
                // Check for an escaped double-quote ("") inside a quoted field.
                if (insideQuotes
                        && index + 1 < line.length()
                        && line.charAt(index + 1) == '"') {

                    currentValue.append('"');
                    index++; // Skip the second quote.
                } else {
                    // Toggle the quoted-field state.
                    insideQuotes = !insideQuotes;
                }
            } else if (currentCharacter == ',' && !insideQuotes) {
                // Unquoted comma is a field separator.
                values.add(currentValue.toString().trim());
                currentValue.setLength(0);
            } else {
                // Regular character; accumulate into the current field.
                currentValue.append(currentCharacter);
            }
        }

        // An unclosed quote is a format error.
        if (insideQuotes) {
            throw new IllegalArgumentException("Unclosed quotation mark.");
        }

        // Add the final field value.
        values.add(currentValue.toString().trim());

        return values;
    }

    // ---------------------------------------------------------------
    //  Writing
    // ---------------------------------------------------------------

    /**
     * Writes one CSV row (a list of values) to a buffered writer.
     * Values are separated by commas and terminated by a newline.
     *
     * @param writer the destination writer
     * @param values the field values for this row
     * @throws IOException if writing fails
     */
    static void writeCsvRow(
            BufferedWriter writer,
            List<String> values) throws IOException {

        // Write each value, preceded by a comma after the first.
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                writer.write(',');
            }
            writer.write(escapeCsv(values.get(index)));
        }

        writer.newLine();
    }

    /**
     * Escapes a CSV field value by wrapping it in double-quotes if it
     * contains commas, quotes, or newlines. Internal quotes are doubled.
     *
     * @param value the raw field value (may be {@code null})
     * @return the safely escaped value
     */
    static String escapeCsv(String value) {
        String safeValue = value == null ? "" : value;

        // Check if the value contains characters that require quoting.
        boolean requiresQuotes =
                safeValue.contains(",")
                        || safeValue.contains("\"")
                        || safeValue.contains("\n")
                        || safeValue.contains("\r");

        if (!requiresQuotes) {
            return safeValue;
        }

        // Wrap in quotes and double any internal quotes.
        return "\""
                + safeValue.replace("\"", "\"\"")
                + "\"";
    }

    // ---------------------------------------------------------------
    //  Column lookup
    // ---------------------------------------------------------------

    /**
     * Normalises a header string for fuzzy comparison: removes BOM,
     * converts to lowercase, and strips all non-alphanumeric characters.
     *
     * @param header the raw header text
     * @return the normalised header string
     */
    static String normalizeHeader(String header) {
        return removeByteOrderMark(header)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
    }

    /**
     * Strips a UTF-8 byte-order mark ({@code \uFEFF}) from the start
     * of a string, if present.
     *
     * @param value the raw string (may be {@code null})
     * @return the string without a leading BOM, or empty if null
     */
    static String removeByteOrderMark(String value) {
        if (value != null
                && !value.isEmpty()
                && value.charAt(0) == '\uFEFF') {

            return value.substring(1);
        }

        return value == null ? "" : value;
    }

    /**
     * Searches for a column whose normalised header matches any of the
     * provided aliases.
     *
     * @param headers the CSV header row
     * @param aliases one or more normalised alias strings to match
     * @return the zero-based column index, or {@code -1} if not found
     */
    static int findColumn(List<String> headers, String... aliases) {
        // Build a set of normalised aliases for fast lookup.
        Set<String> normalizedAliases = new HashSet<>();
        for (String alias : aliases) {
            normalizedAliases.add(normalizeHeader(alias));
        }

        // Scan headers for a match.
        for (int index = 0; index < headers.size(); index++) {
            String normalizedHeader = normalizeHeader(headers.get(index));
            if (normalizedAliases.contains(normalizedHeader)) {
                return index;
            }
        }

        return -1;
    }

    /**
     * Finds a required column by alias, throwing if it is missing.
     *
     * @param headers     the CSV header row
     * @param displayName human-readable column name for error messages
     * @param aliases     one or more normalised aliases to match
     * @return the zero-based column index
     * @throws IllegalArgumentException if no matching column is found
     */
    static int requiredColumn(
            List<String> headers,
            String displayName,
            String... aliases) {

        int column = findColumn(headers, aliases);

        // A missing required column is a structural CSV error.
        if (column < 0) {
            throw new IllegalArgumentException(
                    displayName + " column is missing from the CSV file."
            );
        }

        return column;
    }

    /**
     * Finds an existing column by alias, or creates a new column with
     * the given name if none is found. When a new column is created,
     * all existing rows are padded with an empty value.
     *
     * @param table      the CsvTable to search and possibly extend
     * @param columnName the display name for a newly created column
     * @param aliases    one or more normalised aliases to match
     * @return the zero-based column index (existing or newly created)
     */
    static int ensureColumn(
            CsvTable table,
            String columnName,
            String... aliases) {

        int existingIndex = findColumn(table.headers, aliases);

        // Return the existing column if found.
        if (existingIndex >= 0) {
            return existingIndex;
        }

        // Create a new column: add header and pad all rows.
        table.headers.add(columnName);
        for (List<String> row : table.rows) {
            row.add("");
        }

        return table.headers.size() - 1;
    }

    /**
     * Searches data rows for the first row whose employee-number column
     * matches the given employee number.
     *
     * @param table                the CsvTable to search
     * @param employeeNumberColumn the column index containing employee numbers
     * @param employeeNumber       the employee number to find
     * @return the zero-based row index, or {@code -1} if not found
     */
    static int findRowByEmployeeNumber(
            CsvTable table,
            int employeeNumberColumn,
            int employeeNumber) {

        // Scan each row for a matching employee number.
        for (int index = 0; index < table.rows.size(); index++) {
            String value = getValue(
                    table.rows.get(index),
                    employeeNumberColumn
            );

            try {
                if (parseEmployeeNumber(value) == employeeNumber) {
                    return index;
                }
            } catch (IllegalArgumentException ignored) {
                // Invalid rows are reported when records are loaded.
            }
        }

        return -1;
    }

    // ---------------------------------------------------------------
    //  Value access and parsing
    // ---------------------------------------------------------------

    /**
     * Safely retrieves a trimmed value from a row at the given column
     * index. Returns an empty string for out-of-range indices or null values.
     *
     * @param row    the data row
     * @param column the zero-based column index
     * @return the trimmed value, or empty string if unavailable
     */
    static String getValue(List<String> row, int column) {
        if (column < 0 || column >= row.size()) {
            return "";
        }

        String value = row.get(column);
        return value == null ? "" : value.trim();
    }

    /**
     * Retrieves a required non-blank value from a row, throwing if
     * the value is missing or empty.
     *
     * @param row       the data row
     * @param column    the zero-based column index
     * @param fieldName human-readable field name for error messages
     * @return the trimmed non-blank value
     * @throws IllegalArgumentException if the value is blank or missing
     */
    static String getRequiredValue(
            List<String> row,
            int column,
            String fieldName) {

        return requireText(getValue(row, column), fieldName);
    }

    /**
     * Validates that a string value is non-null and non-blank.
     *
     * @param value     the string to validate
     * @param fieldName human-readable field name for error messages
     * @return the trimmed value
     * @throws IllegalArgumentException if the value is null or blank
     */
    static String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    fieldName + " is required."
            );
        }

        return value.trim();
    }

    /**
     * Parses and validates a positive integer employee number from a string.
     *
     * @param value the string to parse
     * @return the positive employee number
     * @throws IllegalArgumentException if the value is not a positive integer
     */
    static int parseEmployeeNumber(String value) {
        try {
            int employeeNumber = Integer.parseInt(value.trim());

            // Employee numbers must be positive.
            if (employeeNumber <= 0) {
                throw new NumberFormatException();
            }

            return employeeNumber;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Employee number must be a positive whole number.",
                    exception
            );
        }
    }

    /**
     * Parses a monetary value from a string, stripping currency symbols
     * ({@code ₱}, {@code PHP}), commas, and whitespace.
     *
     * @param value     the raw monetary string
     * @param fieldName human-readable field name for error messages
     * @return the parsed non-negative double value
     * @throws IllegalArgumentException if the value is not a valid
     *                                  non-negative number
     */
    static double parseMoney(String value, String fieldName) {
        // Strip currency symbols, commas, and whitespace.
        String cleanedValue = value
                .replace(",", "")
                .replace("₱", "")
                .replaceAll("(?i)PHP", "")
                .trim();

        try {
            double result = Double.parseDouble(cleanedValue);

            // Monetary values must be non-negative.
            validateNonNegative(result, fieldName);
            return result;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    fieldName + " must contain a valid number.",
                    exception
            );
        }
    }

    /**
     * Formats a double value to exactly two decimal places using
     * US locale (period as decimal separator).
     *
     * @param value the numeric value to format
     * @return the formatted string (e.g. "1234.56")
     */
    static String formatDecimal(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    // ---------------------------------------------------------------
    //  Row manipulation
    // ---------------------------------------------------------------

    /**
     * Adjusts a row's size to exactly match the required number of
     * columns. Pads with empty strings if too short; removes trailing
     * values if too long.
     *
     * @param row          the data row to adjust
     * @param requiredSize the target column count
     */
    static void ensureRowSize(List<String> row, int requiredSize) {
        // Pad with empty strings if the row is too short.
        while (row.size() < requiredSize) {
            row.add("");
        }

        // Remove trailing values if the row is too long.
        while (row.size() > requiredSize) {
            row.remove(row.size() - 1);
        }
    }

    /**
     * Creates a new row filled with empty strings.
     *
     * @param size the number of columns
     * @return a new list of empty strings with the given size
     */
    static List<String> createEmptyRow(int size) {
        List<String> row = new ArrayList<>();

        // Fill every column with an empty string.
        for (int index = 0; index < size; index++) {
            row.add("");
        }

        return row;
    }

    /**
     * Validates that a numeric value is finite and non-negative.
     *
     * @param value     the value to validate
     * @param fieldName human-readable field name for error messages
     * @throws IllegalArgumentException if the value is NaN, infinite,
     *                                  or negative
     */
    static void validateNonNegative(double value, String fieldName) {
        if (!Double.isFinite(value) || value < 0) {
            throw new IllegalArgumentException(
                    fieldName
                            + " must be a valid non-negative number."
            );
        }
    }
}
