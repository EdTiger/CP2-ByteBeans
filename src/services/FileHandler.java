/*
 * FileHandler.java
 * Backward-compatible facade for CSV file operations.
 */
package services;

import models.EmployeeInformation;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Backward-compatible facade for CSV file operations.
 *
 * <p>This module delegates to the newer, modular services:
 * {@link CsvFileUtilities}, {@link EmployeeFileModule},
 * {@link AttendanceFileModule}, and {@link AtomicFileWriter}.
 * New code should call the specific modules directly.</p>
 */
public final class FileHandler {

    /** Private constructor prevents instantiation of this utility class. */
    private FileHandler() {
        // Utility class; prevent instantiation.
    }

    /**
     * Delegates to {@link EmployeeFileModule#loadEmployees()}.
     *
     * @return a list of validated employee records
     * @throws IOException if the file cannot be read
     */
    public static synchronized ArrayList<EmployeeInformation> loadEmployees() throws IOException {
        return EmployeeFileModule.loadEmployees();
    }

    /**
     * Delegates to {@link EmployeeFileModule#findEmployeeByNumber(int)}.
     *
     * @param employeeNumber the ID to search for
     * @return the employee, or null if not found
     * @throws IOException if the file cannot be loaded
     */
    public static synchronized EmployeeInformation findEmployeeByNumber(int employeeNumber) throws IOException {
        return EmployeeFileModule.findEmployeeByNumber(employeeNumber);
    }

    /**
     * Delegates to {@link EmployeeFileModule#employeeNumberExists(int)}.
     *
     * @param employeeNumber the ID to check
     * @return true if the employee exists
     * @throws IOException if the file cannot be loaded
     */
    public static synchronized boolean employeeNumberExists(int employeeNumber) throws IOException {
        return EmployeeFileModule.employeeNumberExists(employeeNumber);
    }

    /**
     * Delegates to {@link EmployeeFileModule#addEmployee(EmployeeInformation)}.
     *
     * @param employee the validated employee to add
     * @throws IOException if saving fails
     */
    public static synchronized void appendEmployeeRecord(EmployeeInformation employee) throws IOException {
        EmployeeFileModule.addEmployee(employee);
    }

    /**
     * Delegates to {@link EmployeeFileModule#addEmployee(EmployeeInformation)}.
     *
     * @param employee the validated employee to add
     * @throws IOException if saving fails
     */
    public static synchronized void addEmployee(EmployeeInformation employee) throws IOException {
        EmployeeFileModule.addEmployee(employee);
    }

    /**
     * Delegates to {@link EmployeeFileModule#updateEmployee(EmployeeInformation)}.
     *
     * @param employee the employee data to save
     * @throws IOException if saving fails
     */
    public static synchronized void updateEmployee(EmployeeInformation employee) throws IOException {
        EmployeeFileModule.updateEmployee(employee);
    }

    /**
     * Delegates to {@link EmployeeFileModule#updateEmployee(int, EmployeeInformation)}.
     *
     * @param originalEmployeeNumber the ID of the record to update
     * @param updatedEmployee the new employee data
     * @throws IOException if saving fails
     */
    public static synchronized void updateEmployee(int originalEmployeeNumber, EmployeeInformation updatedEmployee) throws IOException {
        EmployeeFileModule.updateEmployee(originalEmployeeNumber, updatedEmployee);
    }

    /**
     * Delegates to {@link EmployeeFileModule#deleteEmployee(int)}.
     *
     * @param employeeNumber the ID to delete
     * @return true if deleted, false if not found
     * @throws IOException if saving fails
     */
    public static synchronized boolean deleteEmployee(int employeeNumber) throws IOException {
        return EmployeeFileModule.deleteEmployee(employeeNumber);
    }

    /**
     * Delegates to {@link AttendanceFileModule#loadAttendance()}.
     *
     * @return a list of validated attendance records
     * @throws IOException if the file cannot be read
     */
    public static synchronized ArrayList<String[]> loadAttendance() throws IOException {
        return AttendanceFileModule.loadAttendance();
    }

    /**
     * Delegates to {@link EmployeeFileModule#savePayrollResults(int, double, double, double, double)}.
     *
     * @param employeeNumber the employee ID
     * @param hoursWorked hours worked
     * @param deductions total deductions
     * @param grossPay gross pay
     * @param netPay net pay
     * @throws IOException if saving fails
     */
    public static synchronized void savePayrollResults(
            int employeeNumber, double hoursWorked, double deductions, double grossPay, double netPay) throws IOException {
        EmployeeFileModule.savePayrollResults(employeeNumber, hoursWorked, deductions, grossPay, netPay);
    }

    /**
     * Delegates to {@link EmployeeFileModule#savePayrollResults(int[], double[], double[], double[], double[])}.
     *
     * @param employeeNumbers array of employee IDs
     * @param hoursWorked array of hours worked
     * @param deductions array of total deductions
     * @param grossPay array of gross pay
     * @param netPay array of net pay
     * @throws IOException if saving fails
     */
    public static synchronized void savePayrollResults(
            int[] employeeNumbers, double[] hoursWorked, double[] deductions, double[] grossPay, double[] netPay) throws IOException {
        EmployeeFileModule.savePayrollResults(employeeNumbers, hoursWorked, deductions, grossPay, netPay);
    }
}
