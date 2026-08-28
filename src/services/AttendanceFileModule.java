/*
 * AttendanceFileModule.java
 * Attendance CSV loading operations extracted from FileHandler.
 */
package services;

import config.ApplicationConfig;
import services.CsvFileUtilities.CsvTable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads and validates attendance records from the configured CSV file.
 *
 * <p>This module reads the attendance CSV using column-order-independent
 * parsing (columns are matched by normalised header aliases, not by
 * position). Each record is validated for a valid employee number, names,
 * date, and time range before being included in the result.</p>
 *
 * <p>All methods are {@code static} and {@code synchronized} to prevent
 * concurrent file-access issues. The class cannot be instantiated.</p>
 */
public final class AttendanceFileModule {

    /** Private constructor prevents instantiation of this utility class. */
    private AttendanceFileModule() {
        // Utility class; prevent instantiation.
    }

    /**
     * Loads all attendance records from the configured attendance CSV file.
     *
     * <p>Each returned array element is a six-element {@code String[]}:</p>
     * <ol start="0">
     *   <li>Employee Number</li>
     *   <li>Last Name</li>
     *   <li>First Name</li>
     *   <li>Date (M/d/yyyy)</li>
     *   <li>Time In (H:mm)</li>
     *   <li>Time Out (H:mm)</li>
     * </ol>
     *
     * <p>Column order in the CSV file does not matter; columns are
     * matched by normalised header aliases.</p>
     *
     * @return a list of validated attendance records
     * @throws IOException if the file cannot be read, is missing, or
     *                     contains invalid data
     */
    public static synchronized ArrayList<String[]>
            loadAttendance() throws IOException {

        ArrayList<String[]> attendanceRecords = new ArrayList<>();

        Path attendanceFile = ApplicationConfig.getAttendanceFile();

        // Verify the attendance file exists before attempting to read it.
        if (!Files.isRegularFile(attendanceFile)) {
            throw new IOException(
                    "Attendance data file was not found: "
                            + attendanceFile
            );
        }

        CsvTable table = CsvFileUtilities.readCsvTable(attendanceFile);

        // Locate each required column by normalised header aliases.
        int employeeNumberColumn = CsvFileUtilities.requiredColumn(
                table.headers,
                "Employee Number",
                "employee",
                "employeenumber",
                "employeeno",
                "employeeid"
        );
        int lastNameColumn = CsvFileUtilities.requiredColumn(
                table.headers,
                "Last Name",
                "lastname",
                "surname"
        );
        int firstNameColumn = CsvFileUtilities.requiredColumn(
                table.headers,
                "First Name",
                "firstname",
                "givenname"
        );
        int dateColumn = CsvFileUtilities.requiredColumn(
                table.headers,
                "Date",
                "date",
                "attendancedate"
        );
        int timeInColumn = CsvFileUtilities.requiredColumn(
                table.headers,
                "Log In",
                "login",
                "timein"
        );
        int timeOutColumn = CsvFileUtilities.requiredColumn(
                table.headers,
                "Log Out",
                "logout",
                "timeout"
        );

        // Parse and validate each data row.
        for (int index = 0; index < table.rows.size(); index++) {
            List<String> row = table.rows.get(index);

            // Extract fields in the canonical order regardless of CSV column order.
            String[] record = {
                CsvFileUtilities.getValue(row, employeeNumberColumn),
                CsvFileUtilities.getValue(row, lastNameColumn),
                CsvFileUtilities.getValue(row, firstNameColumn),
                CsvFileUtilities.getValue(row, dateColumn),
                CsvFileUtilities.getValue(row, timeInColumn),
                CsvFileUtilities.getValue(row, timeOutColumn)
            };

            // Validate all required fields before adding the record.
            if (!InputValidator.isValidEmployeeNumber(record[0])
                    || InputValidator.isBlank(record[1])
                    || InputValidator.isBlank(record[2])
                    || !InputValidator.isValidDate(record[3])
                    || !InputValidator.isValidTimeRange(
                            record[4],
                            record[5]
                    )) {

                throw new IOException(
                        "Invalid attendance data on CSV row "
                                + (index + 2) + "."
                );
            }

            attendanceRecords.add(record);
        }

        return attendanceRecords;
    }

    /**
     * Checks whether any attendance record contains the given employee
     * number. Used to prevent reuse of employee numbers that have
     * historical attendance data.
     *
     * @param employeeNumber the employee number to search for
     * @return {@code true} if the employee number appears in at least
     *         one attendance record, {@code false} otherwise
     * @throws IOException if the attendance file cannot be loaded
     */
    public static synchronized boolean attendanceContainsEmployeeNumber(
            int employeeNumber) throws IOException {

        // Scan all attendance records for the given employee number.
        for (String[] record : loadAttendance()) {
            if (InputValidator.parseEmployeeNumber(record[0])
                    == employeeNumber) {

                return true;
            }
        }

        return false;
    }
}
