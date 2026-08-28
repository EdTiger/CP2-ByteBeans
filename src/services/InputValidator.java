/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package services;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

public final class InputValidator {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("M/d/uuuu")
                    .withResolverStyle(ResolverStyle.STRICT);

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("H:mm")
                    .withResolverStyle(ResolverStyle.STRICT);

    /**
     * Private constructor prevents instantiation of this utility class.
     */
    private InputValidator() {
        // Utility class; prevent instantiation.
    }

    /**
     * Checks if a string is null, empty, or consists only of whitespace.
     *
     * @param value the string to check
     * @return true if the string is blank
     */
    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * Checks if all provided strings contain non-whitespace text.
     *
     * @param values the strings to check
     * @return true if all strings are non-blank, false otherwise
     */
    public static boolean areAllFieldsFilled(String... values) {
        if (values == null || values.length == 0) {
            return false;
        }

        // Loop through all values to ensure none are blank.
        for (String value : values) {
            if (isBlank(value)) {
                return false;
            }
        }

        return true;
    }

    // Preserves compatibility with the teammate's existing code.
    /**
     * Checks if a string can be parsed as an integer.
     *
     * @param value the string to check
     * @return true if the string is a valid integer
     */
    public static boolean isNumber(String value) {
        if (isBlank(value)) {
            return false;
        }

        try {
            Integer.parseInt(value.trim());
            return true;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    /**
     * Checks if a string represents a strictly positive integer (> 0).
     *
     * @param value the string to check
     * @return true if the string is a positive integer
     */
    public static boolean isPositiveInteger(String value) {
        if (!isNumber(value)) {
            return false;
        }

        return Integer.parseInt(value.trim()) > 0;
    }

    /**
     * Checks if a string is a valid employee number (positive integer).
     *
     * @param value the string to check
     * @return true if valid
     */
    public static boolean isValidEmployeeNumber(String value) {
        return isPositiveInteger(value);
    }

    /**
     * Checks if a string can be parsed as a finite decimal number.
     *
     * @param value the string to check
     * @return true if the string is a valid finite decimal
     */
    public static boolean isValidDecimal(String value) {
        if (isBlank(value)) {
            return false;
        }

        try {
            double number = Double.parseDouble(
                    cleanNumericValue(value)
            );

            return Double.isFinite(number);
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    /**
     * Checks if a string represents a non-negative decimal (>= 0).
     *
     * @param value the string to check
     * @return true if the string is a valid non-negative decimal
     */
    public static boolean isNonNegativeDecimal(String value) {
        if (!isValidDecimal(value)) {
            return false;
        }

        return Double.parseDouble(
                cleanNumericValue(value)
        ) >= 0;
    }

    /**
     * Checks if a string represents a strictly positive decimal (> 0).
     *
     * @param value the string to check
     * @return true if the string is a positive decimal
     */
    public static boolean isPositiveDecimal(String value) {
        if (!isValidDecimal(value)) {
            return false;
        }

        return Double.parseDouble(
                cleanNumericValue(value)
        ) > 0;
    }

    /**
     * Checks if a string is a valid hourly rate (positive decimal).
     *
     * @param value the string to check
     * @return true if valid
     */
    public static boolean isValidHourlyRate(String value) {
        return isPositiveDecimal(value);
    }

    /**
     * Checks if a string is a valid name containing only letters, spaces, dots, apostrophes, and hyphens.
     *
     * @param value the string to check
     * @return true if the string is a valid name
     */
    public static boolean isValidName(String value) {
        if (isBlank(value)) {
            return false;
        }

        String trimmedValue = value.trim();

        return trimmedValue.length() <= 100
                && trimmedValue.matches(
                        "[\\p{L} .'-]+"
                );
    }

    /**
     * Checks if a string is a valid SSS number (10 digits).
     *
     * @param value the string to check
     * @return true if valid
     */
    public static boolean isValidSSSNumber(String value) {
        return hasValidGovernmentIdFormat(value, 10, 10);
    }

    /**
     * Checks if a string is a valid PhilHealth number (12 digits).
     *
     * @param value the string to check
     * @return true if valid
     */
    public static boolean isValidPhilHealthNumber(String value) {
        return hasValidGovernmentIdFormat(value, 12, 12);
    }

    /**
     * Checks if a string is a valid TIN number (9 to 12 digits).
     *
     * @param value the string to check
     * @return true if valid
     */
    public static boolean isValidTinNumber(String value) {
        return hasValidGovernmentIdFormat(value, 9, 12);
    }

    /**
     * Checks if a string is a valid Pag-IBIG number (12 digits).
     *
     * @param value the string to check
     * @return true if valid
     */
    public static boolean isValidPagIbigNumber(String value) {
        return hasValidGovernmentIdFormat(value, 12, 12);
    }

    /**
     * Checks if a string is a valid date in M/d/yyyy format.
     *
     * @param value the string to check
     * @return true if valid
     */
    public static boolean isValidDate(String value) {
        if (isBlank(value)) {
            return false;
        }

        try {
            LocalDate.parse(value.trim(), DATE_FORMAT);
            return true;
        } catch (DateTimeParseException exception) {
            return false;
        }
    }

    /**
     * Checks if a string is a valid time in H:mm format.
     *
     * @param value the string to check
     * @return true if valid
     */
    public static boolean isValidTime(String value) {
        if (isBlank(value)) {
            return false;
        }

        try {
            LocalTime.parse(value.trim(), TIME_FORMAT);
            return true;
        } catch (DateTimeParseException exception) {
            return false;
        }
    }

    /**
     * Checks if two time strings represent a valid chronological time range.
     *
     * @param timeIn  the start time
     * @param timeOut the end time
     * @return true if both are valid and timeOut is after timeIn
     */
    public static boolean isValidTimeRange(
            String timeIn,
            String timeOut) {

        if (!isValidTime(timeIn) || !isValidTime(timeOut)) {
            return false;
        }

        LocalTime parsedTimeIn =
                LocalTime.parse(timeIn.trim(), TIME_FORMAT);

        LocalTime parsedTimeOut =
                LocalTime.parse(timeOut.trim(), TIME_FORMAT);

        return parsedTimeOut.isAfter(parsedTimeIn);
    }

    /**
     * Validates all employee inputs and returns an error message if any is invalid.
     *
     * @param employeeNumber   employee number
     * @param lastName         last name
     * @param firstName        first name
     * @param sssNumber        SSS number
     * @param philHealthNumber PhilHealth number
     * @param tinNumber        TIN number
     * @param pagIbigNumber    Pag-IBIG number
     * @param hourlyRate       hourly rate
     * @return an error message, or null if all fields are valid
     */
    public static String validateEmployeeInput(
            String employeeNumber,
            String lastName,
            String firstName,
            String sssNumber,
            String philHealthNumber,
            String tinNumber,
            String pagIbigNumber,
            String hourlyRate) {

        int invalidField = findFirstInvalidEmployeeField(
                employeeNumber,
                lastName,
                firstName,
                sssNumber,
                philHealthNumber,
                tinNumber,
                pagIbigNumber,
                hourlyRate
        );

        if (invalidField < 0) {
            return null;
        }

        String[] values = {
            employeeNumber,
            lastName,
            firstName,
            sssNumber,
            philHealthNumber,
            tinNumber,
            pagIbigNumber,
            hourlyRate
        };

        if (isBlank(values[invalidField])) {
            return "All employee fields are required.";
        }

        switch (invalidField) {
            case 0:
                return "Employee number must be a positive whole number.";
            case 1:
                return "Last name contains invalid characters.";
            case 2:
                return "First name contains invalid characters.";
            case 3:
                return "SSS number must contain exactly 10 digits.";
            case 4:
                return "PhilHealth number must contain exactly 12 digits.";
            case 5:
                return "TIN must contain between 9 and 12 digits.";
            case 6:
                return "Pag-IBIG number must contain exactly 12 digits.";
            case 7:
                return "Hourly rate must be a number greater than zero.";
            default:
                return "Employee information is invalid.";
        }
    }

    /** 
     * Returns the first invalid field index, or -1 when all fields are valid. 
     * 
     * @param employeeNumber   employee number
     * @param lastName         last name
     * @param firstName        first name
     * @param sssNumber        SSS number
     * @param philHealthNumber PhilHealth number
     * @param tinNumber        TIN number
     * @param pagIbigNumber    Pag-IBIG number
     * @param hourlyRate       hourly rate
     * @return the zero-based index of the first invalid field, or -1 if all are valid
     */
    public static int findFirstInvalidEmployeeField(
            String employeeNumber,
            String lastName,
            String firstName,
            String sssNumber,
            String philHealthNumber,
            String tinNumber,
            String pagIbigNumber,
            String hourlyRate) {

        String[] values = {
            employeeNumber,
            lastName,
            firstName,
            sssNumber,
            philHealthNumber,
            tinNumber,
            pagIbigNumber,
            hourlyRate
        };

        for (int index = 0; index < values.length; index++) {
            if (isBlank(values[index])) {
                return index;
            }
        }

        if (!isValidEmployeeNumber(employeeNumber)) {
            return 0;
        }
        if (!isValidName(lastName)) {
            return 1;
        }
        if (!isValidName(firstName)) {
            return 2;
        }
        if (!isValidSSSNumber(sssNumber)) {
            return 3;
        }
        if (!isValidPhilHealthNumber(philHealthNumber)) {
            return 4;
        }
        if (!isValidTinNumber(tinNumber)) {
            return 5;
        }
        if (!isValidPagIbigNumber(pagIbigNumber)) {
            return 6;
        }
        if (!isValidHourlyRate(hourlyRate)) {
            return 7;
        }

        return -1;
    }

    /**
     * Parses an employee number from a string.
     *
     * @param value the string to parse
     * @return the parsed positive integer
     * @throws IllegalArgumentException if invalid
     */
    public static int parseEmployeeNumber(String value) {
        if (!isValidEmployeeNumber(value)) {
            throw new IllegalArgumentException(
                    "Employee number must be a positive whole number."
            );
        }

        return Integer.parseInt(value.trim());
    }

    /**
     * Parses a decimal number from a string, stripping formatting.
     *
     * @param value the string to parse
     * @return the parsed double value
     * @throws IllegalArgumentException if invalid
     */
    public static double parseDecimal(String value) {
        if (!isValidDecimal(value)) {
            throw new IllegalArgumentException(
                    "The value must be a valid number."
            );
        }

        return Double.parseDouble(cleanNumericValue(value));
    }

    /**
     * Checks if a string has a valid government ID format (digits and optional hyphens).
     *
     * @param value         the string to check
     * @param minimumDigits the minimum number of digits
     * @param maximumDigits the maximum number of digits
     * @return true if valid
     */
    private static boolean hasValidGovernmentIdFormat(
            String value,
            int minimumDigits,
            int maximumDigits) {

        if (isBlank(value)) {
            return false;
        }

        String trimmedValue = value.trim();

        if (!trimmedValue.matches("[0-9 -]+")) {
            return false;
        }

        String digitsOnly =
                trimmedValue.replaceAll("[^0-9]", "");

        return digitsOnly.length() >= minimumDigits
                && digitsOnly.length() <= maximumDigits;
    }

    /**
     * Cleans a numeric string by removing commas, currency symbols, and whitespace.
     *
     * @param value the string to clean
     * @return the cleaned string ready for parsing
     */
    private static String cleanNumericValue(String value) {
        return value.trim()
                .replace(",", "")
                .replace("₱", "")
                .replaceAll("(?i)PHP", "")
                .trim();
    }
}
