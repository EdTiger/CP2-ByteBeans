package view;

import models.EmployeeInformation;

import java.util.Locale;

/** Reusable employee display-formatting functions. */
public final class EmployeeDisplayModule {

    private EmployeeDisplayModule() {
        // Static presentation module; prevent instantiation.
    }

    /** Creates safe HTML for the employee-details dialog. */
    public static String createDetailsHtml(EmployeeInformation employee) {
        if (employee == null) {
            throw new IllegalArgumentException(
                    "Employee information is required."
            );
        }

        return "<html><div style='width:320px'>"
                + "<h2>Employee Information</h2>"
                + "<b>Employee Number:</b> "
                + employee.getEmployeeNumber()
                + "<br><br><b>Name:</b> "
                + escapeHtml(employee.getDisplayName())
                + "<br><br><b>SSS Number:</b> "
                + escapeHtml(employee.getSssNumber())
                + "<br><b>PhilHealth Number:</b> "
                + escapeHtml(employee.getPhilHealthNumber())
                + "<br><b>TIN:</b> "
                + escapeHtml(employee.getTinNumber())
                + "<br><b>Pag-IBIG Number:</b> "
                + escapeHtml(employee.getPagIbigNumber())
                + "<br><br><b>Basic Salary:</b> PHP "
                + String.format(Locale.US, "%,.2f", employee.getBasicSalary())
                + "<br><b>Hourly Rate:</b> PHP "
                + String.format(Locale.US, "%,.2f", employee.getHourlyRate())
                + "</div></html>";
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
