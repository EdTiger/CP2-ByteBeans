package config;

import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.Arrays;

/**
 * Loads application settings without placing file names or credentials in
 * source code. All methods are static so this remains a procedural module.
 */
public final class ApplicationConfig {

    private static final String CONFIG_PROPERTY = "motorph.config";
    private static final String CONFIG_ENVIRONMENT = "MOTORPH_CONFIG";
    private static final String DEFAULT_CONFIG_NAME = "motorph.properties";

    private static final String EMPLOYEE_FILE_KEY =
            "motorph.data.employee-file";
    private static final String ATTENDANCE_FILE_KEY =
            "motorph.data.attendance-file";
    private static final String EMPLOYEE_USERNAME_KEY =
            "motorph.auth.employee.username";
    private static final String EMPLOYEE_PASSWORD_KEY =
            "motorph.auth.employee.password";
    private static final String PAYROLL_USERNAME_KEY =
            "motorph.auth.payroll.username";
    private static final String PAYROLL_PASSWORD_KEY =
            "motorph.auth.payroll.password";

    private static final Path CONFIG_FILE = locateConfigurationFile();
    private static final Properties SETTINGS = loadSettings(CONFIG_FILE);

    private ApplicationConfig() {
        // Static utility module; prevent instantiation.
    }

    /** Verifies the required settings and source files before login. */
    public static void validate() {
        Path employeeFile = getEmployeeFile();
        Path attendanceFile = getAttendanceFile();

        String employeeUsername = getEmployeeUsername();
        String payrollUsername = getPayrollUsername();
        char[] employeePassword = getEmployeePassword();
        char[] payrollPassword = getPayrollPassword();

        Arrays.fill(employeePassword, '\0');
        Arrays.fill(payrollPassword, '\0');

        if (employeeUsername.equalsIgnoreCase(payrollUsername)) {
            throw new IllegalStateException(
                    "Employee and payroll accounts must use different "
                            + "usernames."
            );
        }

        if (employeeFile.equals(attendanceFile)) {
            throw new IllegalStateException(
                    "Employee and attendance data must use different files."
            );
        }

        requireReadableFile(employeeFile, "Employee data");
        requireReadableFile(attendanceFile, "Attendance data");

        if (!Files.isWritable(employeeFile)) {
            throw new IllegalStateException(
                    "Employee data is not writable: " + employeeFile
            );
        }
    }

    /**
     * Retrieves the configured file path for employee data.
     *
     * @return the resolved path to the employee CSV file
     */
    public static Path getEmployeeFile() {
        return getConfiguredPath(EMPLOYEE_FILE_KEY);
    }

    /**
     * Retrieves the configured file path for attendance data.
     *
     * @return the resolved path to the attendance CSV file
     */
    public static Path getAttendanceFile() {
        return getConfiguredPath(ATTENDANCE_FILE_KEY);
    }

    /**
     * Retrieves the configured username for the employee account.
     *
     * @return the employee username
     */
    public static String getEmployeeUsername() {
        return getRequiredSetting(EMPLOYEE_USERNAME_KEY);
    }

    /**
     * Retrieves the configured password for the employee account.
     *
     * @return the employee password as a character array
     */
    public static char[] getEmployeePassword() {
        return getRequiredSetting(EMPLOYEE_PASSWORD_KEY).toCharArray();
    }

    /**
     * Retrieves the configured username for the payroll staff account.
     *
     * @return the payroll staff username
     */
    public static String getPayrollUsername() {
        return getRequiredSetting(PAYROLL_USERNAME_KEY);
    }

    /**
     * Retrieves the configured password for the payroll staff account.
     *
     * @return the payroll staff password as a character array
     */
    public static char[] getPayrollPassword() {
        return getRequiredSetting(PAYROLL_PASSWORD_KEY).toCharArray();
    }

    /**
     * Returns the resolved path to the configuration file.
     *
     * @return the configuration file path
     */
    public static Path getConfigurationFile() {
        return CONFIG_FILE;
    }

    /**
     * Resolves a file path from settings, allowing for environment or property overrides.
     *
     * @param key the configuration key for the path
     * @return the resolved absolute normalized Path
     */
    private static Path getConfiguredPath(String key) {
        String override = getRuntimeOverride(key);
        Path configuredPath;

        if (override != null) {
            configuredPath = Paths.get(override);
            if (!configuredPath.isAbsolute()) {
                configuredPath = workingDirectory().resolve(configuredPath);
            }
        } else {
            configuredPath = Paths.get(getRequiredFileSetting(key));
            if (!configuredPath.isAbsolute()) {
                Path configDirectory = CONFIG_FILE.getParent();
                configuredPath = (configDirectory == null
                        ? workingDirectory()
                        : configDirectory).resolve(configuredPath);
            }
        }

        return configuredPath.toAbsolutePath().normalize();
    }

    /**
     * Retrieves a required string setting. Throws if missing.
     *
     * @param key the setting key
     * @return the string value
     */
    private static String getRequiredSetting(String key) {
        String override = getRuntimeOverride(key);
        String value = override == null
                ? SETTINGS.getProperty(key)
                : override;

        if (value == null || value.trim().isEmpty()) {
            throw missingSetting(key);
        }

        return value.trim();
    }

    /**
     * Retrieves a required string setting that represents a file name (bypasses overrides).
     *
     * @param key the setting key
     * @return the string value
     */
    private static String getRequiredFileSetting(String key) {
        String value = SETTINGS.getProperty(key);

        if (value == null || value.trim().isEmpty()) {
            throw missingSetting(key);
        }

        return value.trim();
    }

    /**
     * Checks system properties and environment variables for an override.
     *
     * @param key the configuration key
     * @return the override value, or null if none exists
     */
    private static String getRuntimeOverride(String key) {
        String systemValue = System.getProperty(key);
        if (systemValue != null && !systemValue.trim().isEmpty()) {
            return systemValue.trim();
        }

        String environmentKey = key
                .toUpperCase(Locale.ROOT)
                .replace('.', '_')
                .replace('-', '_');
        String environmentValue = System.getenv(environmentKey);

        return environmentValue == null
                || environmentValue.trim().isEmpty()
                        ? null
                        : environmentValue.trim();
    }

    /**
     * Creates an exception for a missing setting.
     *
     * @param key the missing configuration key
     * @return the generated IllegalStateException
     */
    private static IllegalStateException missingSetting(String key) {
        return new IllegalStateException(
                "Required setting '" + key + "' is missing from "
                        + CONFIG_FILE + "."
        );
    }

    /**
     * Ensures a file exists and is readable.
     *
     * @param file        the file path
     * @param description a human-readable description for error messages
     */
    private static void requireReadableFile(
            Path file,
            String description) {

        if (!Files.isRegularFile(file) || !Files.isReadable(file)) {
            throw new IllegalStateException(
                    description + " file is missing or unreadable: " + file
            );
        }
    }

    /**
     * Loads properties from a specific configuration file.
     *
     * @param configFile the file to read
     * @return the loaded Properties object
     */
    private static Properties loadSettings(Path configFile) {
        Properties properties = new Properties();
        if (!Files.isRegularFile(configFile)) {
            return properties;
        }

        try (Reader reader = Files.newBufferedReader(
                configFile,
                StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Unable to read configuration file: " + configFile,
                    exception
            );
        }

        return properties;
    }

    /**
     * Searches standard locations to find the motorph.properties file.
     *
     * @return the resolved Path to the configuration file
     */
    private static Path locateConfigurationFile() {
        String explicitPath = System.getProperty(CONFIG_PROPERTY);
        if (explicitPath == null || explicitPath.trim().isEmpty()) {
            explicitPath = System.getenv(CONFIG_ENVIRONMENT);
        }

        if (explicitPath != null && !explicitPath.trim().isEmpty()) {
            return Paths.get(explicitPath.trim())
                    .toAbsolutePath().normalize();
        }

        Set<Path> candidates = new LinkedHashSet<>();
        Path workingDirectory = workingDirectory();
        candidates.add(workingDirectory.resolve(DEFAULT_CONFIG_NAME));

        Path current = findCodeLocation();
        for (int level = 0; current != null && level < 5; level++) {
            candidates.add(current.resolve(DEFAULT_CONFIG_NAME));
            current = current.getParent();
        }

        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate)) {
                return candidate.toAbsolutePath().normalize();
            }
        }

        return workingDirectory.resolve(DEFAULT_CONFIG_NAME);
    }

    /**
     * Determines the location of the executing jar or class folder.
     *
     * @return the directory containing the code
     */
    private static Path findCodeLocation() {
        try {
            URI location = ApplicationConfig.class
                    .getProtectionDomain().getCodeSource()
                    .getLocation().toURI();
            Path path = Paths.get(location).toAbsolutePath().normalize();
            return Files.isDirectory(path) ? path : path.getParent();
        } catch (Exception exception) {
            return null;
        }
    }

    /**
     * Returns the current working directory.
     *
     * @return the working directory path
     */
    private static Path workingDirectory() {
        return Paths.get(System.getProperty("user.dir", "."))
                .toAbsolutePath().normalize();
    }
}
