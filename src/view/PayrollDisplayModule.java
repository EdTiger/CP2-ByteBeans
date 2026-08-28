package view;

import models.EmployeeInformation;
import services.PayrollProcessor;
import services.PayrollProcessor.PayrollResult;

import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.Component;
import java.awt.Font;
import java.awt.Insets;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import static view.UiTheme.DETAILS_BACKGROUND;
import static view.UiTheme.TEXT_PRIMARY;

/**
 * Reusable payroll presentation functions for Swing views.
 *
 * <p>This module owns payroll text formatting and outcome dialogs so the
 * payroll frame can focus on user actions and state transitions.</p>
 */
public final class PayrollDisplayModule {

    /** User chose to save the successfully computed results. */
    public static final int SAVE_SUCCESSFUL = 0;

    /** User chose to keep the results visible without saving. */
    public static final int KEEP_RESULTS = 1;

    /** User chose to discard the computed results. */
    public static final int DISCARD_RESULTS = 2;

    private static final DateTimeFormatter MONTH_FORMAT =
            DateTimeFormatter.ofPattern("MMMM uuuu");

    private PayrollDisplayModule() {
        // Static presentation module; prevent instantiation.
    }

    /** Returns a complete, consistently formatted employee payslip. */
    public static String createPayslipText(
            EmployeeInformation employee,
            YearMonth payrollMonth,
            PayrollResult result) {

        if (employee == null || payrollMonth == null || result == null) {
            throw new IllegalArgumentException(
                    "Employee, payroll month, and payroll result are required."
            );
        }

        return "================ MOTORPH PAYSLIP ================\n"
                + "Payroll Month       : "
                + payrollMonth.format(MONTH_FORMAT)
                + "\n"
                + "Employee Number     : "
                + employee.getEmployeeNumber()
                + "\n"
                + "Employee Name       : "
                + employee.getDisplayName()
                + "\n"
                + "Hourly Rate         : PHP "
                + formatMoney(result.getHourlyRate())
                + "\n"
                + "Monthly Basic Salary: PHP "
                + formatMoney(result.getMonthlyBasicSalary())
                + "\n"
                + "-------------------------------------------------\n"
                + "First Cutoff Hours  : "
                + formatNumber(result.getFirstCutoffHours())
                + "\n"
                + "Second Cutoff Hours : "
                + formatNumber(result.getSecondCutoffHours())
                + "\n"
                + "Total Hours Worked  : "
                + formatNumber(result.getTotalHours())
                + "\n"
                + "First Cutoff Gross  : PHP "
                + formatMoney(result.getFirstCutoffGrossPay())
                + "\n"
                + "Second Cutoff Gross : PHP "
                + formatMoney(result.getSecondCutoffGrossPay())
                + "\n"
                + "Gross Pay           : PHP "
                + formatMoney(result.getGrossPay())
                + "\n"
                + "-------------------------------------------------\n"
                + "SSS                 : PHP "
                + formatMoney(result.getSss())
                + "\n"
                + "PhilHealth          : PHP "
                + formatMoney(result.getPhilHealth())
                + "\n"
                + "Employer PhilHealth : PHP "
                + formatMoney(result.getEmployerPhilHealth())
                + " (not deducted)\n"
                + "Pag-IBIG            : PHP "
                + formatMoney(result.getPagIbig())
                + "\n"
                + "Employer Pag-IBIG   : PHP "
                + formatMoney(result.getEmployerPagIbig())
                + " (not deducted)\n"
                + "Taxable Income      : PHP "
                + formatMoney(result.getTaxableIncome())
                + "\n"
                + "Withholding Tax     : PHP "
                + formatMoney(result.getWithholdingTax())
                + "\n"
                + "Total Deductions    : PHP "
                + formatMoney(result.getTotalDeductions())
                + "\n"
                + "=================================================\n"
                + "NET PAY             : PHP "
                + formatMoney(result.getNetPay())
                + "\n"
                + "=================================================\n";
    }

    /** Returns the read-only text shown by the payroll summary dialog. */
    public static String createSummaryText(
            YearMonth payrollMonth,
            double[] summary) {

        if (payrollMonth == null
                || summary == null
                || summary.length <= PayrollProcessor.SUMMARY_AVERAGE_NET_PAY) {
            throw new IllegalArgumentException(
                    "Payroll month and complete summary values are required."
            );
        }

        return "============== PAYROLL SUMMARY ==============\n"
                + "Payroll Month     : "
                + payrollMonth.format(MONTH_FORMAT)
                + "\n"
                + "Total Employees   : "
                + (int) summary[PayrollProcessor.SUMMARY_EMPLOYEE_COUNT]
                + "\n"
                + "Total Gross Pay   : PHP "
                + formatMoney(summary[PayrollProcessor.SUMMARY_TOTAL_GROSS_PAY])
                + "\n"
                + "Total Deductions  : PHP "
                + formatMoney(summary[PayrollProcessor.SUMMARY_TOTAL_DEDUCTIONS])
                + "\n"
                + "Average Net Pay   : PHP "
                + formatMoney(summary[PayrollProcessor.SUMMARY_AVERAGE_NET_PAY])
                + "\n"
                + "=============================================\n";
    }

    /** Displays the payroll summary in a read-only text area. */
    public static void showSummaryDialog(
            Component parent,
            YearMonth payrollMonth,
            double[] summary) {

        JTextArea summaryArea = createReadOnlyArea(
                createSummaryText(payrollMonth, summary),
                8,
                42,
                false
        );
        summaryArea.getAccessibleContext()
                .setAccessibleName("Payroll summary");

        JOptionPane.showMessageDialog(
                parent,
                summaryArea,
                "Payroll Summary",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    /**
     * Shows partial-computation details and returns the user's safe next step.
     * A batch with zero successful rows never offers a save action.
     */
    public static int showComputationOutcomeDialog(
            Component parent,
            int successfulCount,
            List<String> errors) {

        if (errors == null || errors.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one computation error is required."
            );
        }

        StringBuilder message = new StringBuilder();
        message.append(successfulCount)
                .append(" payroll record(s) were computed successfully.\n\n")
                .append(errors.size())
                .append(" record(s) could not be processed:\n\n");

        for (String error : errors) {
            message.append("- ").append(error).append('\n');
        }

        message.append("\nCorrect the listed source data and compute again.");

        JTextArea errorArea = createReadOnlyArea(
                message.toString(),
                12,
                58,
                true
        );
        errorArea.getAccessibleContext()
                .setAccessibleName("Payroll computation errors");
        JScrollPane errorScrollPane = new JScrollPane(errorArea);
        errorScrollPane.setBorder(BorderFactory.createEmptyBorder());

        if (successfulCount <= 0) {
            JOptionPane.showMessageDialog(
                    parent,
                    errorScrollPane,
                    "Payroll Computation Failed",
                    JOptionPane.ERROR_MESSAGE
            );
            return DISCARD_RESULTS;
        }

        Object[] options = {
            "Save Successful (" + successfulCount + ")",
            "Keep Results",
            "Discard Results"
        };

        int choice = JOptionPane.showOptionDialog(
                parent,
                errorScrollPane,
                "Computation Completed with Warnings",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[1]
        );

        if (choice == 0) {
            return SAVE_SUCCESSFUL;
        }
        if (choice == 2) {
            return DISCARD_RESULTS;
        }
        return KEEP_RESULTS;
    }

    /** Formats a currency amount with grouping and two decimal places. */
    public static String formatMoney(double value) {
        return String.format(Locale.US, "%,.2f", value);
    }

    /** Formats a numeric amount with two decimal places. */
    public static String formatNumber(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    private static JTextArea createReadOnlyArea(
            String text,
            int rows,
            int columns,
            boolean wrap) {

        JTextArea area = new JTextArea(rows, columns);
        area.setEditable(false);
        area.setFont(new Font("Monospaced", Font.PLAIN, 13));
        area.setBackground(DETAILS_BACKGROUND);
        area.setForeground(TEXT_PRIMARY);
        area.setMargin(new Insets(10, 12, 10, 12));
        area.setLineWrap(wrap);
        area.setWrapStyleWord(wrap);
        area.setText(text);
        area.setCaretPosition(0);
        return area;
    }
}
