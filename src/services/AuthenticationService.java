package services;

import config.ApplicationConfig;

import java.util.Arrays;

/**
 * Procedural authentication functions used by the login view.
 *
 * <p>Compares user-supplied credentials against the values configured in
 * {@link config.ApplicationConfig} and returns a role constant indicating
 * the matched account type. Passwords are compared using
 * {@link java.util.Arrays#equals(char[], char[])} and zeroed immediately
 * after comparison to minimise time in memory.</p>
 *
 * <p>This is a stateless utility class; all methods are {@code static} and
 * the constructor is private to prevent instantiation.</p>
 */
public final class AuthenticationService {

    /** Role constant returned when the employee account credentials match. */
    public static final String EMPLOYEE_ROLE = "EMPLOYEE";

    /** Role constant returned when the payroll-staff account credentials match. */
    public static final String PAYROLL_ROLE = "PAYROLL";

    /** Sentinel returned when no credentials match any configured account. */
    public static final String NO_ROLE = "";

    private AuthenticationService() {
        // Static utility module; prevent instantiation.
    }

    /**
     * Authenticates a user against the configured MotorPH accounts.
     *
     * <p>The method checks the employee account first, then the payroll account.
     * If either set of credentials matches, the corresponding role constant
     * ({@link #EMPLOYEE_ROLE} or {@link #PAYROLL_ROLE}) is returned. Otherwise
     * {@link #NO_ROLE} is returned.</p>
     *
     * @param enteredUsername the username entered by the user
     * @param enteredPassword the password entered by the user (as a char array)
     * @return one of {@link #EMPLOYEE_ROLE}, {@link #PAYROLL_ROLE}, or {@link #NO_ROLE}
     */
    public static String authenticate(
            String enteredUsername,
            char[] enteredPassword) {

        // Null credentials cannot match any account.
        if (enteredUsername == null || enteredPassword == null) {
            return NO_ROLE;
        }

        // Check against the configured employee account first.
        if (credentialsMatch(
                enteredUsername,
                enteredPassword,
                ApplicationConfig.getEmployeeUsername(),
                ApplicationConfig.getEmployeePassword())) {

            return EMPLOYEE_ROLE;
        }

        // Fall through to the payroll-staff account.
        if (credentialsMatch(
                enteredUsername,
                enteredPassword,
                ApplicationConfig.getPayrollUsername(),
                ApplicationConfig.getPayrollPassword())) {

            return PAYROLL_ROLE;
        }

        // No matching account found.
        return NO_ROLE;
    }

    /**
     * Compares entered credentials against a configured account.
     *
     * <p>Username comparison is case-insensitive. The configured password
     * array is zeroed in a {@code finally} block regardless of outcome
     * to reduce the window during which plaintext credentials reside in memory.</p>
     *
     * @param enteredUsername     the username supplied by the user
     * @param enteredPassword     the password supplied by the user
     * @param configuredUsername  the expected username from configuration
     * @param configuredPassword  the expected password from configuration
     * @return {@code true} if both username and password match
     */
    private static boolean credentialsMatch(
            String enteredUsername,
            char[] enteredPassword,
            String configuredUsername,
            char[] configuredPassword) {

        try {
            // Case-insensitive username check + constant-time-ish password comparison.
            return configuredUsername.equalsIgnoreCase(
                    enteredUsername.trim()
            ) && Arrays.equals(configuredPassword, enteredPassword);
        } finally {
            // Zero out the configured password to limit plaintext exposure.
            Arrays.fill(configuredPassword, '\0');
        }
    }
}
