/*
 * EmployeeFileModule.java
 * Employee CRUD operations extracted from FileHandler.
 */
package services;

import config.ApplicationConfig;
import models.EmployeeInformation;
import services.CsvFileUtilities.CsvTable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Manages CRUD operations for EmployeeInformation records.
 *
 * <p>This module handles reading, validating, adding, updating, and
 * deleting employees from the configured employee CSV file. It also
 * supports batch saving of payroll results into the CSV.</p>
 *
 * <p>All methods are {@code static} and {@code synchronized} to ensure
 * thread-safe file access. The class cannot be instantiated.</p>
 */
public final class EmployeeFileModule {

    /** Private constructor prevents instantiation of this utility class. */
    private EmployeeFileModule() {
        // Utility class; prevent instantiation.
    }

    // ---------------------------------------------------------------
    //  EmployeeColumns mapping
    // ---------------------------------------------------------------

    /**
     * Maps canonical column indices for employee records.
     */
    private static final class EmployeeColumns {
        final int employeeNumber;
        final int lastName;
        final int firstName;
        final int sssNumber;
        final int philHealthNumber;
        final int tinNumber;
        final int pagIbigNumber;
        final int basicSalary;
        final int hourlyRate;

        /**
         * Resolves column indices from CSV headers using normalised aliases.
         *
         * @param headers the parsed CSV header list
         * @return a mapped EmployeeColumns instance
         * @throws IllegalArgumentException if a required column is missing
         */
        static EmployeeColumns from(List<String> headers) {
            EmployeeColumns columns = new EmployeeColumns(
                    CsvFileUtilities.requiredColumn(headers, "Employee Number", "employee", "employeenumber", "employeeno", "employeeid"),
                    CsvFileUtilities.requiredColumn(headers, "Last Name", "lastname", "surname"),
                    CsvFileUtilities.requiredColumn(headers, "First Name", "firstname", "givenname"),
                    CsvFileUtilities.requiredColumn(headers, "SSS #", "sss", "sssnumber", "sssno"),
                    CsvFileUtilities.requiredColumn(headers, "PhilHealth #", "philhealth", "philhealthnumber", "philhealthno"),
                    CsvFileUtilities.requiredColumn(headers, "TIN #", "tin", "tinnumber", "tinno"),
                    CsvFileUtilities.requiredColumn(headers, "Pag-IBIG #", "pagibig", "pagibignumber", "pagibigno"),
                    CsvFileUtilities.requiredColumn(headers, "Basic Salary", "basicsalary", "salary"),
                    CsvFileUtilities.requiredColumn(headers, "Hourly Rate", "hourlyrate", "rate")
            );
            return columns;
        }

        private EmployeeColumns(
                int employeeNumber, int lastName, int firstName,
                int sssNumber, int philHealthNumber, int tinNumber,
                int pagIbigNumber, int basicSalary, int hourlyRate) {

            this.employeeNumber = employeeNumber;
            this.lastName = lastName;
            this.firstName = firstName;
            this.sssNumber = sssNumber;
            this.philHealthNumber = philHealthNumber;
            this.tinNumber = tinNumber;
            this.pagIbigNumber = pagIbigNumber;
            this.basicSalary = basicSalary;
            this.hourlyRate = hourlyRate;
        }
    }

    // ---------------------------------------------------------------
    //  Loading / Reading
    // ---------------------------------------------------------------

    /**
     * Loads all valid employees from the configured CSV file.
     *
     * @return a list of validated employee records
     * @throws IOException if the file cannot be read or contains duplicates/errors
     */
    public static synchronized ArrayList<EmployeeInformation> loadEmployees() throws IOException {
        ArrayList<EmployeeInformation> employees = new ArrayList<>();
        Path employeeFile = ApplicationConfig.getEmployeeFile();

        if (!Files.isRegularFile(employeeFile)) {
            throw new IOException("Employee data file was not found: " + employeeFile);
        }

        CsvTable table = CsvFileUtilities.readCsvTable(employeeFile);

        if (table.headers.isEmpty()) {
            return employees;
        }

        return parseEmployeeTable(table);
    }

    /**
     * Finds an employee by their employee number.
     *
     * @param employeeNumber the ID to search for
     * @return the employee, or null if not found
     * @throws IOException if the file cannot be loaded
     */
    public static synchronized EmployeeInformation findEmployeeByNumber(int employeeNumber) throws IOException {
        for (EmployeeInformation employee : loadEmployees()) {
            if (employee.getEmployeeNumber() == employeeNumber) {
                return employee;
            }
        }
        return null;
    }

    /**
     * Checks if an employee number already exists.
     *
     * @param employeeNumber the ID to check
     * @return true if the employee exists
     * @throws IOException if the file cannot be loaded
     */
    public static synchronized boolean employeeNumberExists(int employeeNumber) throws IOException {
        return findEmployeeByNumber(employeeNumber) != null;
    }

    // ---------------------------------------------------------------
    //  Modifying (CRUD)
    // ---------------------------------------------------------------

    /**
     * Adds a new employee record to the CSV file atomically.
     *
     * @param employee the validated employee to add
     * @throws IOException if saving fails
     * @throws IllegalArgumentException if the ID is duplicated or in use
     */
    public static synchronized void addEmployee(EmployeeInformation employee) throws IOException {
        appendEmployeeRecord(employee);
    }

    /**
     * Appends a new employee record, verifying constraints first.
     */
    private static void appendEmployeeRecord(EmployeeInformation employee) throws IOException {
        validateEmployee(employee);

        CsvTable table = loadOrCreateEmployeeTable();
        EmployeeColumns columns = EmployeeColumns.from(table.headers);

        parseEmployeeTable(table);

        if (CsvFileUtilities.findRowByEmployeeNumber(table, columns.employeeNumber, employee.getEmployeeNumber()) >= 0) {
            throw new IllegalArgumentException("Employee number " + employee.getEmployeeNumber() + " already exists.");
        }

        if (AttendanceFileModule.attendanceContainsEmployeeNumber(employee.getEmployeeNumber())) {
            throw new IllegalArgumentException("Employee number " + employee.getEmployeeNumber()
                    + " is reserved by existing attendance history and cannot be reused.");
        }

        List<String> newRow = CsvFileUtilities.createEmptyRow(table.headers.size());
        setEmployeeValues(newRow, columns, employee);
        table.rows.add(newRow);

        AtomicFileWriter.writeCsvTableAtomically(ApplicationConfig.getEmployeeFile(), table);
    }

    /**
     * Updates an existing employee in-place.
     *
     * @param employee the employee data to save
     * @throws IOException if saving fails
     */
    public static synchronized void updateEmployee(EmployeeInformation employee) throws IOException {
        if (employee == null) {
            throw new IllegalArgumentException("Employee information is required.");
        }
        updateEmployee(employee.getEmployeeNumber(), employee);
    }

    /**
     * Updates an existing employee, guarding against ID changes.
     *
     * @param originalEmployeeNumber the ID of the record to update
     * @param updatedEmployee the new employee data
     * @throws IOException if saving fails
     * @throws IllegalArgumentException if ID is changed or record missing
     */
    public static synchronized void updateEmployee(int originalEmployeeNumber, EmployeeInformation updatedEmployee) throws IOException {
        validateEmployee(updatedEmployee);

        Path employeeFile = ApplicationConfig.getEmployeeFile();
        if (!Files.exists(employeeFile)) {
            throw new IOException("Employee data file was not found: " + employeeFile);
        }

        CsvTable table = CsvFileUtilities.readCsvTable(employeeFile);
        EmployeeColumns columns = EmployeeColumns.from(table.headers);

        parseEmployeeTable(table);

        int targetRow = CsvFileUtilities.findRowByEmployeeNumber(table, columns.employeeNumber, originalEmployeeNumber);

        if (targetRow < 0) {
            throw new IllegalArgumentException("Employee number " + originalEmployeeNumber + " does not exist.");
        }

        if (originalEmployeeNumber != updatedEmployee.getEmployeeNumber()) {
            throw new IllegalArgumentException("Employee numbers cannot be changed after a record "
                    + "is created because attendance history uses that number.");
        }

        List<String> existingRow = table.rows.get(targetRow);
        CsvFileUtilities.ensureRowSize(existingRow, table.headers.size());
        setEmployeeValues(existingRow, columns, updatedEmployee);

        AtomicFileWriter.writeCsvTableAtomically(employeeFile, table);
    }

    /**
     * Deletes an employee by their employee number.
     *
     * @param employeeNumber the ID to delete
     * @return true if deleted, false if not found
     * @throws IOException if saving fails
     */
    public static synchronized boolean deleteEmployee(int employeeNumber) throws IOException {
        Path employeeFile = ApplicationConfig.getEmployeeFile();
        if (!Files.exists(employeeFile)) {
            return false;
        }

        CsvTable table = CsvFileUtilities.readCsvTable(employeeFile);
        if (table.headers.isEmpty()) {
            return false;
        }

        EmployeeColumns columns = EmployeeColumns.from(table.headers);
        parseEmployeeTable(table);

        int rowIndex = CsvFileUtilities.findRowByEmployeeNumber(table, columns.employeeNumber, employeeNumber);
        if (rowIndex < 0) {
            return false;
        }

        table.rows.remove(rowIndex);
        AtomicFileWriter.writeCsvTableAtomically(employeeFile, table);
        return true;
    }

    // ---------------------------------------------------------------
    //  Payroll Batch Saving
    // ---------------------------------------------------------------

    /**
     * Saves a batch of computed payroll results back to the employee CSV file.
     *
     * <p>Extends the CSV with new columns if they do not exist, and populates
     * the payroll results for the matching employees.</p>
     *
     * @param employeeNumbers array of employee IDs
     * @param hoursWorked array of hours worked
     * @param deductions array of total deductions
     * @param grossPay array of gross pay
     * @param netPay array of net pay
     * @throws IOException if saving fails or arrays mismatched
     */
    public static synchronized void savePayrollResults(
            int[] employeeNumbers,
            double[] hoursWorked,
            double[] deductions,
            double[] grossPay,
            double[] netPay) throws IOException {

        validatePayrollArrays(employeeNumbers, hoursWorked, deductions, grossPay, netPay);
        CsvTable table = loadOrCreateEmployeeTable();

        int idCol = CsvFileUtilities.ensureColumn(table, "Employee Number", "employee", "employeenumber", "employeeno");
        int hoursCol = CsvFileUtilities.ensureColumn(table, "Hours Worked", "hoursworked", "hours");
        int deductionsCol = CsvFileUtilities.ensureColumn(table, "Deductions", "deductions");
        int grossCol = CsvFileUtilities.ensureColumn(table, "Gross Pay", "grosspay", "gross");
        int netCol = CsvFileUtilities.ensureColumn(table, "Net Pay", "netpay", "net");

        for (int i = 0; i < employeeNumbers.length; i++) {
            int rowIndex = CsvFileUtilities.findRowByEmployeeNumber(table, idCol, employeeNumbers[i]);

            if (rowIndex >= 0) {
                List<String> row = table.rows.get(rowIndex);
                CsvFileUtilities.ensureRowSize(row, table.headers.size());

                row.set(hoursCol, CsvFileUtilities.formatDecimal(hoursWorked[i]));
                row.set(deductionsCol, CsvFileUtilities.formatDecimal(deductions[i]));
                row.set(grossCol, CsvFileUtilities.formatDecimal(grossPay[i]));
                row.set(netCol, CsvFileUtilities.formatDecimal(netPay[i]));
            }
        }

        AtomicFileWriter.writeCsvTableAtomically(ApplicationConfig.getEmployeeFile(), table);
    }

    /** Overload for a single employee save. */
    public static synchronized void savePayrollResults(
            int employeeNumber, double hoursWorked, double deductions, double grossPay, double netPay) throws IOException {
        savePayrollResults(
                new int[]{employeeNumber},
                new double[]{hoursWorked},
                new double[]{deductions},
                new double[]{grossPay},
                new double[]{netPay}
        );
    }

    // ---------------------------------------------------------------
    //  Internal Helpers
    // ---------------------------------------------------------------

    private static ArrayList<EmployeeInformation> parseEmployeeTable(CsvTable table) throws IOException {
        ArrayList<EmployeeInformation> employees = new ArrayList<>();
        EmployeeColumns columns;

        try {
            columns = EmployeeColumns.from(table.headers);
        } catch (IllegalArgumentException exception) {
            throw new IOException("Invalid employee CSV header: " + exception.getMessage(), exception);
        }

        Set<Integer> employeeNumbers = new HashSet<>();
        for (int index = 0; index < table.rows.size(); index++) {
            List<String> row = table.rows.get(index);
            try {
                EmployeeInformation employee = createEmployee(row, columns);
                validateEmployee(employee);

                if (!employeeNumbers.add(employee.getEmployeeNumber())) {
                    throw new IOException("Duplicate employee number " + employee.getEmployeeNumber() + " found on CSV row " + (index + 2) + ".");
                }
                employees.add(employee);
            } catch (IllegalArgumentException exception) {
                throw new IOException("Invalid employee data on CSV row " + (index + 2) + ": " + exception.getMessage(), exception);
            }
        }

        return employees;
    }

    private static CsvTable loadOrCreateEmployeeTable() throws IOException {
        Path employeeFile = ApplicationConfig.getEmployeeFile();
        if (Files.exists(employeeFile)) {
            CsvTable table = CsvFileUtilities.readCsvTable(employeeFile);
            if (!table.headers.isEmpty()) {
                return table;
            }
        }

        List<String> defaultHeaders = new ArrayList<>(List.of(
                "Employee Number", "Last Name", "First Name", "SSS #", "PhilHealth #",
                "TIN #", "Pag-IBIG #", "Basic Salary", "Hourly Rate"
        ));
        return new CsvTable(defaultHeaders, new ArrayList<>());
    }

    private static EmployeeInformation createEmployee(List<String> row, EmployeeColumns columns) {
        return new EmployeeInformation(
                CsvFileUtilities.parseEmployeeNumber(CsvFileUtilities.getValue(row, columns.employeeNumber)),
                CsvFileUtilities.getRequiredValue(row, columns.lastName, "Last name"),
                CsvFileUtilities.getRequiredValue(row, columns.firstName, "First name"),
                CsvFileUtilities.getRequiredValue(row, columns.sssNumber, "SSS number"),
                CsvFileUtilities.getRequiredValue(row, columns.philHealthNumber, "PhilHealth number"),
                CsvFileUtilities.getRequiredValue(row, columns.tinNumber, "TIN"),
                CsvFileUtilities.getRequiredValue(row, columns.pagIbigNumber, "Pag-IBIG number"),
                CsvFileUtilities.parseMoney(CsvFileUtilities.getValue(row, columns.basicSalary), "Basic salary"),
                CsvFileUtilities.parseMoney(CsvFileUtilities.getValue(row, columns.hourlyRate), "Hourly rate")
        );
    }

    private static void setEmployeeValues(List<String> row, EmployeeColumns columns, EmployeeInformation employee) {
        row.set(columns.employeeNumber, String.valueOf(employee.getEmployeeNumber()));
        row.set(columns.lastName, employee.getLastName());
        row.set(columns.firstName, employee.getFirstName());
        row.set(columns.sssNumber, employee.getSssNumber());
        row.set(columns.philHealthNumber, employee.getPhilHealthNumber());
        row.set(columns.tinNumber, employee.getTinNumber());
        row.set(columns.pagIbigNumber, employee.getPagIbigNumber());
        row.set(columns.basicSalary, CsvFileUtilities.formatDecimal(employee.getBasicSalary()));
        row.set(columns.hourlyRate, CsvFileUtilities.formatDecimal(employee.getHourlyRate()));
    }

    private static void validateEmployee(EmployeeInformation employee) {
        if (employee == null) {
            throw new IllegalArgumentException("Employee information is required.");
        }
        if (employee.getEmployeeNumber() <= 0) {
            throw new IllegalArgumentException("Employee number must be positive.");
        }
        CsvFileUtilities.requireText(employee.getLastName(), "Last name");
        CsvFileUtilities.requireText(employee.getFirstName(), "First name");
        CsvFileUtilities.requireText(employee.getSssNumber(), "SSS number");
        CsvFileUtilities.requireText(employee.getPhilHealthNumber(), "PhilHealth number");
        CsvFileUtilities.requireText(employee.getTinNumber(), "TIN");
        CsvFileUtilities.requireText(employee.getPagIbigNumber(), "Pag-IBIG number");
        CsvFileUtilities.validateNonNegative(employee.getBasicSalary(), "Basic salary");
        CsvFileUtilities.validateNonNegative(employee.getHourlyRate(), "Hourly rate");
    }

    private static void validatePayrollArrays(int[] employeeNumbers, double[] hoursWorked, double[] deductions, double[] grossPay, double[] netPay) {
        if (employeeNumbers == null || hoursWorked == null || deductions == null || grossPay == null || netPay == null) {
            throw new IllegalArgumentException("Payroll arrays cannot be null.");
        }
        int length = employeeNumbers.length;
        if (hoursWorked.length != length || deductions.length != length || grossPay.length != length || netPay.length != length) {
            throw new IllegalArgumentException("All payroll arrays must have the same length.");
        }
    }
}
