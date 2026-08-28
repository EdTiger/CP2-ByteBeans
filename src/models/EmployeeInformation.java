package models;

/**
 * Data model representing a single MotorPH employee's information.
 *
 * <p>Stores personal identifiers (name, government IDs) and compensation
 * details (basic salary, hourly rate). All string fields are automatically
 * trimmed on assignment via the {@link #clean(String)} helper method.</p>
 */
public class EmployeeInformation {

    /** Unique numeric identifier for the employee. */
    private int employeeNumber;

    /** Employee's last (family) name. */
    private String lastName;

    /** Employee's first (given) name. */
    private String firstName;

    /** Social Security System (SSS) number. */
    private String sssNumber;

    /** Philippine Health Insurance Corporation (PhilHealth) number. */
    private String philHealthNumber;

    /** Tax Identification Number (TIN). */
    private String tinNumber;

    /** Home Development Mutual Fund (Pag-IBIG) number. */
    private String pagIbigNumber;

    /** The employee's monthly basic salary in Philippine Pesos. */
    private double basicSalary;

    /** The employee's computed hourly rate in Philippine Pesos. */
    private double hourlyRate;

    /**
     * Constructs a new {@code EmployeeInformation} instance with all fields.
     *
     * <p>All string parameters are trimmed automatically; {@code null} values
     * are replaced with empty strings.</p>
     *
     * @param employeeNumber   unique numeric identifier
     * @param lastName         the employee's last name
     * @param firstName        the employee's first name
     * @param sssNumber        SSS number (may include dashes or spaces)
     * @param philHealthNumber PhilHealth number
     * @param tinNumber        TIN number
     * @param pagIbigNumber    Pag-IBIG number
     * @param basicSalary      monthly basic salary in PHP
     * @param hourlyRate       hourly rate in PHP
     */
    public EmployeeInformation(
            int employeeNumber,
            String lastName,
            String firstName,
            String sssNumber,
            String philHealthNumber,
            String tinNumber,
            String pagIbigNumber,
            double basicSalary,
            double hourlyRate) {

        this.employeeNumber = employeeNumber;
        this.lastName = clean(lastName);
        this.firstName = clean(firstName);
        this.sssNumber = clean(sssNumber);
        this.philHealthNumber = clean(philHealthNumber);
        this.tinNumber = clean(tinNumber);
        this.pagIbigNumber = clean(pagIbigNumber);
        this.basicSalary = basicSalary;
        this.hourlyRate = hourlyRate;
    }

    /** Returns the unique numeric employee identifier. */
    public int getEmployeeNumber() {
        return employeeNumber;
    }

    /** Sets the unique numeric employee identifier. @param employeeNumber the new ID */
    public void setEmployeeNumber(int employeeNumber) {
        this.employeeNumber = employeeNumber;
    }

    /** Returns the employee's last (family) name. */
    public String getLastName() {
        return lastName;
    }

    /** Sets the employee's last name (trimmed automatically). @param lastName new last name */
    public void setLastName(String lastName) {
        this.lastName = clean(lastName);
    }

    /** Returns the employee's first (given) name. */
    public String getFirstName() {
        return firstName;
    }

    /** Sets the employee's first name (trimmed automatically). @param firstName new first name */
    public void setFirstName(String firstName) {
        this.firstName = clean(firstName);
    }

    /** Returns the employee's SSS number. */
    public String getSssNumber() {
        return sssNumber;
    }

    /** Sets the employee's SSS number (trimmed automatically). @param sssNumber new SSS number */
    public void setSssNumber(String sssNumber) {
        this.sssNumber = clean(sssNumber);
    }

    /** Returns the employee's PhilHealth number. */
    public String getPhilHealthNumber() {
        return philHealthNumber;
    }

    /** Sets the employee's PhilHealth number (trimmed automatically). @param philHealthNumber new PhilHealth number */
    public void setPhilHealthNumber(String philHealthNumber) {
        this.philHealthNumber = clean(philHealthNumber);
    }

    /** Returns the employee's TIN (Tax Identification Number). */
    public String getTinNumber() {
        return tinNumber;
    }

    /** Sets the employee's TIN (trimmed automatically). @param tinNumber new TIN */
    public void setTinNumber(String tinNumber) {
        this.tinNumber = clean(tinNumber);
    }

    /** Returns the employee's Pag-IBIG number. */
    public String getPagIbigNumber() {
        return pagIbigNumber;
    }

    /** Sets the employee's Pag-IBIG number (trimmed automatically). @param pagIbigNumber new Pag-IBIG number */
    public void setPagIbigNumber(String pagIbigNumber) {
        this.pagIbigNumber = clean(pagIbigNumber);
    }

    /** Returns the employee's hourly rate in Philippine Pesos. */
    public double getHourlyRate() {
        return hourlyRate;
    }

    /** Returns the employee's monthly basic salary in Philippine Pesos. */
    public double getBasicSalary() {
        return basicSalary;
    }

    /** Sets the employee's monthly basic salary. @param basicSalary new salary in PHP */
    public void setBasicSalary(double basicSalary) {
        this.basicSalary = basicSalary;
    }

    /** Sets the employee's hourly rate. @param hourlyRate new rate in PHP */
    public void setHourlyRate(double hourlyRate) {
        this.hourlyRate = hourlyRate;
    }

    /**
     * Returns the employee's full name in "First Last" format.
     *
     * @return the concatenated first and last name
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    /**
     * Returns the employee's display name in "Last, First" format.
     *
     * @return the formatted display name
     */
    public String getDisplayName() {
        return lastName + ", " + firstName;
    }

    /**
     * Trims whitespace from a string value, returning an empty string for {@code null}.
     *
     * @param value the raw string to clean
     * @return the trimmed value, or {@code ""} if null
     */
    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * Returns a human-readable string representation of the employee.
     *
     * <p>Format: {@code "<employeeNumber> - Last, First"}</p>
     *
     * @return the formatted employee string
     */
    @Override
    public String toString() {
        return employeeNumber + " - " + getDisplayName();
    }
}
