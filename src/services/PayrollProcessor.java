/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package services;

import models.EmployeeInformation;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static services.MoneyMath.multiplyAndRound;
import static services.MoneyMath.roundToCent;
import static services.MoneyMath.subtractAndRound;
import static services.MoneyMath.sumAndRound;

/**
 * Orchestrates end-to-end payroll computation.
 *
 * <p>Scans attendance history, calculates hours worked split by cutoffs,
 * computes gross pay, applies statutory deductions, and returns a detailed
 * {@link PayrollResult}.</p>
 */
public final class PayrollProcessor {

    private static final double STANDARD_MONTHLY_WORK_HOURS = 168;

    /** Array index for the employee count returned by {@link #generateSummary}. */
    public static final int SUMMARY_EMPLOYEE_COUNT = 0;

    /** Array index for total gross pay returned by {@link #generateSummary}. */
    public static final int SUMMARY_TOTAL_GROSS_PAY = 1;

    /** Array index for total deductions returned by {@link #generateSummary}. */
    public static final int SUMMARY_TOTAL_DEDUCTIONS = 2;

    /** Array index for average net pay returned by {@link #generateSummary}. */
    public static final int SUMMARY_AVERAGE_NET_PAY = 3;

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("M/d/uuuu")
                    .withResolverStyle(ResolverStyle.STRICT);

    private PayrollProcessor() {
        // Utility class; prevent instantiation.
    }

    /**
     * Processes payroll for a single employee based on their attendance history.
     *
     * <p>Filters attendance records by the specified month, calculates total hours
     * worked, and splits hours into first cutoff (days 1-15) and second cutoff (days 16+).</p>
     *
     * @param employee          the employee being processed
     * @param attendanceRecords all parsed attendance records
     * @param payrollMonth      the target month to process
     * @return the fully computed payroll result
     * @throws IllegalArgumentException if no attendance is found or parsing fails
     */
    public static PayrollResult processEmployeePayroll(
            EmployeeInformation employee,
            List<String[]> attendanceRecords,
            YearMonth payrollMonth) {

        Map<Integer, double[]> monthlyHours =
                calculateMonthlyHoursByEmployee(
                        attendanceRecords,
                        payrollMonth
                );

        return processEmployeePayroll(
                employee,
                monthlyHours,
                payrollMonth
        );
    }

    /**
     * Calculates first- and second-cutoff hours for every employee in one
     * attendance-file pass.
     *
     * <p>The map key is the employee number. Each value is a two-element
     * array containing first-cutoff hours at index {@code 0} and
     * second-cutoff hours at index {@code 1}. Batch payroll and summary
     * operations reuse this index, avoiding a complete attendance scan for
     * every employee.</p>
     *
     * @param attendanceRecords all parsed attendance records
     * @param payrollMonth      month to index
     * @return monthly cutoff hours keyed by employee number
     */
    public static Map<Integer, double[]>
            calculateMonthlyHoursByEmployee(
                    List<String[]> attendanceRecords,
                    YearMonth payrollMonth) {

        if (attendanceRecords == null) {
            throw new IllegalArgumentException(
                    "Attendance records are required."
            );
        }

        if (payrollMonth == null) {
            throw new IllegalArgumentException(
                    "Payroll month is required."
            );
        }

        Map<Integer, double[]> monthlyHours = new HashMap<>();

        for (int index = 0;
                index < attendanceRecords.size();
                index++) {

            String[] record = attendanceRecords.get(index);

            if (record == null || record.length < 6) {
                throw new IllegalArgumentException(
                        "Attendance row " + (index + 2)
                                + " is incomplete."
                );
            }

            int employeeNumber;

            try {
                String rawEmployeeNumber = record[0] == null
                        ? ""
                        : record[0].trim();
                employeeNumber = Integer.parseInt(rawEmployeeNumber);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException(
                        "Invalid employee number on attendance row "
                                + (index + 2) + ".",
                        exception
                );
            }

            LocalDate attendanceDate =
                    parseAttendanceDate(
                            record[3],
                            index + 2
                    );

            if (!YearMonth.from(attendanceDate)
                    .equals(payrollMonth)) {
                continue;
            }

            double workedHours;

            try {
                workedHours =
                        AttendanceComputation.calculateWorkedHours(
                                record[4],
                                record[5]
                        );
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException(
                        "Invalid attendance time on row "
                                + (index + 2) + ": "
                                + exception.getMessage(),
                        exception
                );
            }

            double[] cutoffHours = monthlyHours.computeIfAbsent(
                    employeeNumber,
                    ignored -> new double[2]
            );

            if (attendanceDate.getDayOfMonth() <= 15) {
                cutoffHours[0] += workedHours;
            } else {
                cutoffHours[1] += workedHours;
            }
        }

        return monthlyHours;
    }

    /**
     * Processes one employee using a reusable monthly attendance index.
     *
     * @param employee     employee to process
     * @param monthlyHours cutoff hours produced by
     *                     {@link #calculateMonthlyHoursByEmployee}
     * @param payrollMonth month represented by the index
     * @return the fully computed payroll result
     */
    public static PayrollResult processEmployeePayroll(
            EmployeeInformation employee,
            Map<Integer, double[]> monthlyHours,
            YearMonth payrollMonth) {

        if (employee == null) {
            throw new IllegalArgumentException(
                    "Employee information is required."
            );
        }

        if (monthlyHours == null) {
            throw new IllegalArgumentException(
                    "Monthly attendance hours are required."
            );
        }

        if (payrollMonth == null) {
            throw new IllegalArgumentException(
                    "Payroll month is required."
            );
        }

        double[] cutoffHours = monthlyHours.get(
                employee.getEmployeeNumber()
        );

        if (cutoffHours == null || cutoffHours.length < 2) {
            throw new IllegalArgumentException(
                    "No attendance records were found for employee "
                            + employee.getEmployeeNumber()
                            + " in " + payrollMonth + "."
            );
        }

        return processPayroll(
                cutoffHours[0],
                cutoffHours[1],
                employee.getHourlyRate(),
                employee.getBasicSalary()
        );
    }

    /**
     * Generates aggregate payroll values for every loaded employee in a month.
     *
     * <p>Each employee is processed through {@link #processEmployeePayroll}, so
     * the summary uses the same gross-pay, deduction, and net-pay computations
     * as the individual payroll feature. The returned array contains employee
     * count, total gross pay, total deductions, and average net pay at the
     * indexes defined by the {@code SUMMARY_*} constants.</p>
     *
     * @param employees          all loaded employee records
     * @param attendanceRecords all loaded attendance records
     * @param payrollMonth      month to summarize
     * @return the four computed summary values
     * @throws IllegalArgumentException if source data or the month is missing
     */
    public static double[] generateSummary(
            List<EmployeeInformation> employees,
            List<String[]> attendanceRecords,
            YearMonth payrollMonth) {

        if (employees == null || employees.isEmpty()) {
            throw new IllegalArgumentException(
                    "No employee data is loaded."
            );
        }

        if (attendanceRecords == null
                || attendanceRecords.isEmpty()) {
            throw new IllegalArgumentException(
                    "No attendance data is loaded."
            );
        }

        if (payrollMonth == null) {
            throw new IllegalArgumentException(
                    "Payroll month is required."
            );
        }

        double totalGrossPay = 0;
        double totalDeductions = 0;
        double totalNetPay = 0;

        Map<Integer, double[]> monthlyHours =
                calculateMonthlyHoursByEmployee(
                        attendanceRecords,
                        payrollMonth
                );

        for (EmployeeInformation employee : employees) {
            PayrollResult result = processEmployeePayroll(
                    employee,
                    monthlyHours,
                    payrollMonth
            );

            totalGrossPay = sumAndRound(
                    totalGrossPay,
                    result.getGrossPay()
            );

            totalDeductions = sumAndRound(
                    totalDeductions,
                    result.getTotalDeductions()
            );

            totalNetPay = sumAndRound(
                    totalNetPay,
                    result.getNetPay()
            );
        }

        double averageNetPay = roundToCent(
                totalNetPay / employees.size()
        );

        return new double[]{
            employees.size(),
            totalGrossPay,
            totalDeductions,
            averageNetPay
        };
    }

    /**
     * Processes payroll using a single lump sum of hours and assuming gross equals basic salary.
     *
     * @param hoursWorked the total hours worked
     * @param hourlyRate  the employee's hourly rate
     * @return the computed payroll result
     */
    public static PayrollResult processPayroll(
            double hoursWorked,
            double hourlyRate) {

        return processPayroll(
                hoursWorked,
                0,
                hourlyRate
        );
    }

    /**
     * Processes payroll with split cutoff hours, deriving basic salary from the hourly rate.
     *
     * @param firstCutoffHours  hours worked from day 1 to 15
     * @param secondCutoffHours hours worked from day 16 onwards
     * @param hourlyRate        the employee's hourly rate
     * @return the computed payroll result
     */
    public static PayrollResult processPayroll(
            double firstCutoffHours,
            double secondCutoffHours,
            double hourlyRate) {

        return processPayroll(
                firstCutoffHours,
                secondCutoffHours,
                hourlyRate,
                computeMonthlyBasicSalary(hourlyRate)
        );
    }

    /**
     * Fully processes payroll using explicitly provided hours and basic salary.
     *
     * @param firstCutoffHours   hours worked from day 1 to 15
     * @param secondCutoffHours  hours worked from day 16 onwards
     * @param hourlyRate         the employee's hourly rate
     * @param monthlyBasicSalary the fixed basic salary used for deductions
     * @return the computed payroll result
     */
    public static PayrollResult processPayroll(
            double firstCutoffHours,
            double secondCutoffHours,
            double hourlyRate,
            double monthlyBasicSalary) {

        validateNonNegative(
                firstCutoffHours,
                "First cutoff hours"
        );

        validateNonNegative(
                secondCutoffHours,
                "Second cutoff hours"
        );

        validateNonNegative(hourlyRate, "Hourly rate");
        validateNonNegative(
                monthlyBasicSalary,
                "Monthly basic salary"
        );

        double payableFirstCutoffHours =
                roundToCent(firstCutoffHours);

        double payableSecondCutoffHours =
                roundToCent(secondCutoffHours);

        double firstCutoffGrossPay =
                computeGrossPay(
                        payableFirstCutoffHours,
                        hourlyRate
                );

        double secondCutoffGrossPay =
                computeGrossPay(
                        payableSecondCutoffHours,
                        hourlyRate
                );

        double totalHours = roundToCent(
                payableFirstCutoffHours
                        + payableSecondCutoffHours
        );

        double grossPay = sumAndRound(
                firstCutoffGrossPay,
                secondCutoffGrossPay
        );

        double sss =
                DeductionComputation.computeSSS(monthlyBasicSalary);

        double philHealth =
                DeductionComputation.computePhilHealth(
                        monthlyBasicSalary
                );

        double employerPhilHealth =
                DeductionComputation.computeEmployerPhilHealth(
                        monthlyBasicSalary
                );

        double pagIbig =
                DeductionComputation.computePagIBIG(
                        monthlyBasicSalary
                );

        double employerPagIbig =
                DeductionComputation.computeEmployerPagIBIG(
                        monthlyBasicSalary
                );

        double taxableIncome = computeTaxableIncome(
                grossPay,
                sss,
                philHealth,
                pagIbig
        );

        double withholdingTax =
                DeductionComputation.computeWithholdingTax(
                        taxableIncome
                );

        double totalDeductions = computeTotalDeductions(
                sss,
                philHealth,
                pagIbig,
                withholdingTax
        );

        double netPay = computeNetPay(
                grossPay,
                totalDeductions
        );

        return new PayrollResult(
                payableFirstCutoffHours,
                payableSecondCutoffHours,
                totalHours,
                hourlyRate,
                monthlyBasicSalary,
                firstCutoffGrossPay,
                secondCutoffGrossPay,
                grossPay,
                sss,
                philHealth,
                employerPhilHealth,
                pagIbig,
                employerPagIbig,
                taxableIncome,
                withholdingTax,
                totalDeductions,
                netPay
        );
    }

    /**
     * Computes gross pay from hours worked and hourly rate.
     *
     * @param hoursWorked hours worked
     * @param hourlyRate  hourly rate
     * @return the computed gross pay
     */
    public static double computeGrossPay(
            double hoursWorked,
            double hourlyRate) {

        validateNonNegative(hoursWorked, "Hours worked");
        validateNonNegative(hourlyRate, "Hourly rate");

        return multiplyAndRound(hoursWorked, hourlyRate);
    }

    /**
     * Computes a theoretical monthly basic salary from an hourly rate using standard hours (168).
     *
     * @param hourlyRate the hourly rate
     * @return the computed monthly basic salary
     */
    public static double computeMonthlyBasicSalary(double hourlyRate) {
        validateNonNegative(hourlyRate, "Hourly rate");

        return multiplyAndRound(
                hourlyRate,
                STANDARD_MONTHLY_WORK_HOURS
        );
    }

    // Compatibility with the teammate's original method name.
    /**
     * Computes gross salary (legacy alias).
     *
     * @param hoursWorked hours worked
     * @param hourlyRate  hourly rate
     * @return the computed gross salary
     */
    public static double computeGrossSalary(
            double hoursWorked,
            double hourlyRate) {

        return computeGrossPay(hoursWorked, hourlyRate);
    }

    /**
     * Computes taxable income (Gross Pay minus SSS, PhilHealth, Pag-IBIG).
     *
     * @param grossPay   the gross pay
     * @param sss        the SSS deduction
     * @param philHealth the PhilHealth deduction
     * @param pagIbig    the Pag-IBIG deduction
     * @return the taxable income (floored at 0)
     */
    public static double computeTaxableIncome(
            double grossPay,
            double sss,
            double philHealth,
            double pagIbig) {

        validateNonNegative(grossPay, "Gross pay");
        validateNonNegative(sss, "SSS deduction");
        validateNonNegative(
                philHealth,
                "PhilHealth deduction"
        );
        validateNonNegative(
                pagIbig,
                "Pag-IBIG deduction"
        );

        return Math.max(
                0,
                subtractAndRound(
                        grossPay,
                        sss,
                        philHealth,
                        pagIbig
                )
        );
    }

    /**
     * Sums all standard deductions.
     *
     * @param sss            the SSS deduction
     * @param philHealth     the PhilHealth deduction
     * @param pagIbig        the Pag-IBIG deduction
     * @param withholdingTax the withholding tax
     * @return the total deductions
     */
    public static double computeTotalDeductions(
            double sss,
            double philHealth,
            double pagIbig,
            double withholdingTax) {

        validateNonNegative(sss, "SSS deduction");
        validateNonNegative(
                philHealth,
                "PhilHealth deduction"
        );
        validateNonNegative(
                pagIbig,
                "Pag-IBIG deduction"
        );
        validateNonNegative(
                withholdingTax,
                "Withholding tax"
        );

        return sumAndRound(
                sss,
                philHealth,
                pagIbig,
                withholdingTax
        );
    }

    /**
     * Computes final net pay (Gross Pay minus Total Deductions).
     *
     * @param grossPay        the gross pay
     * @param totalDeductions the sum of all deductions
     * @return the final net pay
     * @throws IllegalArgumentException if deductions exceed gross pay
     */
    public static double computeNetPay(
            double grossPay,
            double totalDeductions) {

        validateNonNegative(grossPay, "Gross pay");
        validateNonNegative(
                totalDeductions,
                "Total deductions"
        );

        if (totalDeductions > grossPay) {
            throw new IllegalArgumentException(
                    "Total deductions cannot exceed gross pay."
            );
        }

        return subtractAndRound(grossPay, totalDeductions);
    }

    // Computes net salary directly from gross pay.
    /**
     * Computes net salary directly from gross pay using standard deductions.
     *
     * @param grossPay the gross pay
     * @return the final net pay
     */
    public static double computeNetSalary(double grossPay) {
        validateNonNegative(grossPay, "Gross pay");

        double totalDeductions =
                DeductionComputation.computeDeductions(
                        grossPay
                );

        return computeNetPay(
                grossPay,
                totalDeductions
        );
    }

    /**
     * Safely parses an attendance date, converting format errors to validation messages.
     *
     * @param date      the raw date string
     * @param rowNumber the CSV row number for context
     * @return the parsed LocalDate
     */
    private static LocalDate parseAttendanceDate(
            String date,
            int rowNumber) {

        if (date == null || date.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Attendance date is missing on row "
                            + rowNumber + "."
            );
        }

        try {
            return LocalDate.parse(
                    date.trim(),
                    DATE_FORMAT
            );
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(
                    "Invalid attendance date on row "
                            + rowNumber
                            + ". Use MM/dd/yyyy format.",
                    exception
            );
        }
    }

    /**
     * Validates that an amount is finite and non-negative.
     *
     * @param value     the amount to check
     * @param fieldName the name used in error messages
     */
    private static void validateNonNegative(
            double value,
            String fieldName) {

        if (!Double.isFinite(value) || value < 0) {
            throw new IllegalArgumentException(
                    fieldName
                            + " must be a valid non-negative number."
            );
        }
    }

    /**
     * An immutable record representing a fully computed payroll result.
     */
    public static final class PayrollResult {

        private final double firstCutoffHours;
        private final double secondCutoffHours;
        private final double totalHours;
        private final double hourlyRate;
        private final double monthlyBasicSalary;
        private final double firstCutoffGrossPay;
        private final double secondCutoffGrossPay;
        private final double grossPay;
        private final double sss;
        private final double philHealth;
        private final double employerPhilHealth;
        private final double pagIbig;
        private final double employerPagIbig;
        private final double taxableIncome;
        private final double withholdingTax;
        private final double totalDeductions;
        private final double netPay;

        private PayrollResult(
                double firstCutoffHours,
                double secondCutoffHours,
                double totalHours,
                double hourlyRate,
                double monthlyBasicSalary,
                double firstCutoffGrossPay,
                double secondCutoffGrossPay,
                double grossPay,
                double sss,
                double philHealth,
                double employerPhilHealth,
                double pagIbig,
                double employerPagIbig,
                double taxableIncome,
                double withholdingTax,
                double totalDeductions,
                double netPay) {

            this.firstCutoffHours =
                    roundToCent(firstCutoffHours);

            this.secondCutoffHours =
                    roundToCent(secondCutoffHours);

            this.totalHours = totalHours;
            this.hourlyRate = hourlyRate;
            this.monthlyBasicSalary = monthlyBasicSalary;

            this.firstCutoffGrossPay =
                    firstCutoffGrossPay;

            this.secondCutoffGrossPay =
                    secondCutoffGrossPay;

            this.grossPay = grossPay;
            this.sss = sss;
            this.philHealth = philHealth;
            this.employerPhilHealth = employerPhilHealth;
            this.pagIbig = pagIbig;
            this.employerPagIbig = employerPagIbig;
            this.taxableIncome = taxableIncome;
            this.withholdingTax = withholdingTax;
            this.totalDeductions = totalDeductions;
            this.netPay = netPay;
        }

        public double getFirstCutoffHours() {
            return firstCutoffHours;
        }

        public double getSecondCutoffHours() {
            return secondCutoffHours;
        }

        public double getTotalHours() {
            return totalHours;
        }

        public double getHourlyRate() {
            return hourlyRate;
        }

        public double getMonthlyBasicSalary() {
            return monthlyBasicSalary;
        }

        public double getFirstCutoffGrossPay() {
            return firstCutoffGrossPay;
        }

        public double getSecondCutoffGrossPay() {
            return secondCutoffGrossPay;
        }

        public double getGrossPay() {
            return grossPay;
        }

        public double getSss() {
            return sss;
        }

        public double getPhilHealth() {
            return philHealth;
        }

        public double getEmployerPhilHealth() {
            return employerPhilHealth;
        }

        public double getPagIbig() {
            return pagIbig;
        }

        public double getEmployerPagIbig() {
            return employerPagIbig;
        }

        public double getTaxableIncome() {
            return taxableIncome;
        }

        public double getWithholdingTax() {
            return withholdingTax;
        }

        public double getTotalDeductions() {
            return totalDeductions;
        }

        public double getNetPay() {
            return netPay;
        }
    }
}
