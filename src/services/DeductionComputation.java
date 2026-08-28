/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package services;

import static services.MoneyMath.basePlusPercentageOfExcess;
import static services.MoneyMath.multiplyAndRound;
import static services.MoneyMath.roundToCent;
import static services.MoneyMath.subtractAndRound;
import static services.MoneyMath.sumAndRound;

/**
 * Calculates MotorPH statutory deductions (SSS, PhilHealth, Pag-IBIG, and Tax).
 *
 * <p>All methods are stateless and rely on {@link MoneyMath} for correct rounding.</p>
 */
public final class DeductionComputation {

    private static final double SSS_MINIMUM_CONTRIBUTION = 135.00;
    private static final double SSS_BRACKET_INCREMENT = 22.50;
    private static final double SSS_MAXIMUM_CONTRIBUTION = 1125.00;

    private static final double PAG_IBIG_MINIMUM_SALARY = 1000;
    private static final double PAG_IBIG_FIRST_BRACKET_MAXIMUM = 1500;
    private static final double PAG_IBIG_EMPLOYEE_FIRST_RATE = 0.01;
    private static final double PAG_IBIG_EMPLOYEE_SECOND_RATE = 0.02;
    private static final double PAG_IBIG_EMPLOYER_RATE = 0.02;
    private static final double PAG_IBIG_MAXIMUM_SHARE = 100;

    // MotorPH monthly withholding-tax table boundaries and base amounts.
    private static final double NO_WITHHOLDING_TAX_MAXIMUM = 20832;
    private static final double FIRST_EXCESS_BASE = 20833;
    private static final double SECOND_EXCESS_BASE = 33333;
    private static final double THIRD_EXCESS_BASE = 66667;
    private static final double FOURTH_EXCESS_BASE = 166667;
    private static final double FIFTH_EXCESS_BASE = 666667;

    private static final double SECOND_BRACKET_BASE_TAX = 2500;
    private static final double THIRD_BRACKET_BASE_TAX = 10833;
    private static final double FOURTH_BRACKET_BASE_TAX = 40833.33;
    private static final double FIFTH_BRACKET_BASE_TAX = 200833.33;

    private DeductionComputation() {
        // Utility class; prevent instantiation.
    }

    /**
     * Computes the monthly SSS contribution based on the MotorPH table.
     *
     * <p>Formula: minimum 135, max 1125, increment 22.50 per 500 peso bracket.</p>
     *
     * @param monthlyCompensation the basic salary
     * @return the SSS contribution amount
     */
    public static double computeSSS(double monthlyCompensation) {
        validateAmount(monthlyCompensation, "Monthly compensation");

        if (monthlyCompensation == 0) {
            return 0;
        }

        if (monthlyCompensation < 3250) {
            return SSS_MINIMUM_CONTRIBUTION;
        }

        if (monthlyCompensation >= 24750) {
            return SSS_MAXIMUM_CONTRIBUTION;
        }

        int bracket =
                (int) ((monthlyCompensation - 3250) / 500) + 1;

        return roundToCent(
                SSS_MINIMUM_CONTRIBUTION
                        + (bracket * SSS_BRACKET_INCREMENT)
        );
    }

    /**
     * Computes the employee share of PhilHealth (50% of the total premium).
     *
     * @param monthlyBasicSalary the basic salary
     * @return the employee PhilHealth share
     */
    public static double computePhilHealth(double monthlyBasicSalary) {
        return multiplyAndRound(
                computePhilHealthPremium(monthlyBasicSalary),
                0.50
        );
    }

    /**
     * Computes the employer share of PhilHealth (50% of the total premium).
     *
     * @param monthlyBasicSalary the basic salary
     * @return the employer PhilHealth share
     */
    public static double computeEmployerPhilHealth(
            double monthlyBasicSalary) {

        return multiplyAndRound(
                computePhilHealthPremium(monthlyBasicSalary),
                0.50
        );
    }

    /**
     * Computes the total PhilHealth premium (3% of salary, floor 300, ceiling 1800).
     *
     * @param monthlyBasicSalary the basic salary
     * @return the total PhilHealth premium
     */
    public static double computePhilHealthPremium(
            double monthlyBasicSalary) {

        validateAmount(monthlyBasicSalary, "Monthly basic salary");

        if (monthlyBasicSalary == 0) {
            return 0;
        }

        if (monthlyBasicSalary <= 10000) {
            return 300;
        } else if (monthlyBasicSalary >= 60000) {
            return 1800;
        }

        return multiplyAndRound(monthlyBasicSalary, 0.03);
    }

    /**
     * Computes the employee Pag-IBIG contribution based on rate tiers, max 100.
     *
     * @param monthlyBasicSalary the basic salary
     * @return the employee Pag-IBIG contribution
     */
    public static double computePagIBIG(double monthlyBasicSalary) {
        validateAmount(monthlyBasicSalary, "Monthly basic salary");

        if (monthlyBasicSalary < PAG_IBIG_MINIMUM_SALARY) {
            return 0;
        }

        double contributionRate =
                monthlyBasicSalary <= PAG_IBIG_FIRST_BRACKET_MAXIMUM
                        ? PAG_IBIG_EMPLOYEE_FIRST_RATE
                        : PAG_IBIG_EMPLOYEE_SECOND_RATE;

        return Math.min(
                multiplyAndRound(
                        monthlyBasicSalary,
                        contributionRate
                ),
                PAG_IBIG_MAXIMUM_SHARE
        );
    }

    /**
     * Computes the employer Pag-IBIG contribution (always 2%, max 100).
     *
     * @param monthlyBasicSalary the basic salary
     * @return the employer Pag-IBIG contribution
     */
    public static double computeEmployerPagIBIG(
            double monthlyBasicSalary) {

        validateAmount(monthlyBasicSalary, "Monthly basic salary");

        if (monthlyBasicSalary < PAG_IBIG_MINIMUM_SALARY) {
            return 0;
        }

        return Math.min(
                multiplyAndRound(
                        monthlyBasicSalary,
                        PAG_IBIG_EMPLOYER_RATE
                ),
                PAG_IBIG_MAXIMUM_SHARE
        );
    }

    /**
     * Computes the total Pag-IBIG contribution (employee + employer).
     *
     * @param monthlyBasicSalary the basic salary
     * @return the total Pag-IBIG contribution
     */
    public static double computeTotalPagIBIGContribution(
            double monthlyBasicSalary) {

        return sumAndRound(
                computePagIBIG(monthlyBasicSalary),
                computeEmployerPagIBIG(monthlyBasicSalary)
        );
    }

    // Compatibility method for code using the older spelling.
    /**
     * Computes the employee Pag-IBIG contribution (legacy alias).
     *
     * @param monthlyBasicSalary the basic salary
     * @return the employee Pag-IBIG contribution
     */
    public static double computePagibig(double monthlyBasicSalary) {
        return computePagIBIG(monthlyBasicSalary);
    }

    /**
     * Computes the monthly withholding tax using the 6-bracket table.
     *
     * @param taxableIncome gross pay minus pre-tax deductions
     * @return the withholding tax amount
     */
    public static double computeWithholdingTax(
            double taxableIncome) {

        validateAmount(taxableIncome, "Taxable income");

        double tax;

        if (taxableIncome <= NO_WITHHOLDING_TAX_MAXIMUM) {
            tax = 0;
        } else if (taxableIncome < SECOND_EXCESS_BASE) {
            tax = basePlusPercentageOfExcess(
                    0, taxableIncome, FIRST_EXCESS_BASE, 0.20
            );
        } else if (taxableIncome < THIRD_EXCESS_BASE) {
            tax = basePlusPercentageOfExcess(
                    SECOND_BRACKET_BASE_TAX,
                    taxableIncome,
                    SECOND_EXCESS_BASE,
                    0.25
            );
        } else if (taxableIncome < FOURTH_EXCESS_BASE) {
            tax = basePlusPercentageOfExcess(
                    THIRD_BRACKET_BASE_TAX,
                    taxableIncome,
                    THIRD_EXCESS_BASE,
                    0.30
            );
        } else if (taxableIncome < FIFTH_EXCESS_BASE) {
            tax = basePlusPercentageOfExcess(
                    FOURTH_BRACKET_BASE_TAX,
                    taxableIncome,
                    FOURTH_EXCESS_BASE,
                    0.32
            );
        } else {
            tax = basePlusPercentageOfExcess(
                    FIFTH_BRACKET_BASE_TAX,
                    taxableIncome,
                    FIFTH_EXCESS_BASE,
                    0.35
            );
        }

        // The source table jumps from 20,832 to 20,833. This prevents
        // cent-valued income inside that gap from producing a negative tax.
        return roundToCent(Math.max(0, tax));
    }

    // Compatibility method for code using the older method name.
    /**
     * Computes the withholding tax (legacy alias).
     *
     * @param taxableIncome taxable income
     * @return the withholding tax amount
     */
    public static double computeTax(double taxableIncome) {
        return computeWithholdingTax(taxableIncome);
    }

    /**
     * Computes all deductions assuming gross pay equals basic salary.
     *
     * @param grossPay the total earnings
     * @return the sum of SSS, PhilHealth, Pag-IBIG, and Tax
     */
    public static double computeDeductions(double grossPay) {
        return computeDeductions(grossPay, grossPay);
    }

    /**
     * Computes all deductions using the provided gross pay and basic salary.
     *
     * @param grossPay           the total earnings
     * @param monthlyBasicSalary the basic salary driving statutory rates
     * @return the sum of SSS, PhilHealth, Pag-IBIG, and Tax
     */
    public static double computeDeductions(
            double grossPay,
            double monthlyBasicSalary) {

        validateAmount(grossPay, "Gross pay");
        validateAmount(monthlyBasicSalary, "Monthly basic salary");

        double sss = computeSSS(monthlyBasicSalary);
        double philHealth = computePhilHealth(monthlyBasicSalary);
        double pagIbig = computePagIBIG(monthlyBasicSalary);

        double taxableIncome = Math.max(
                0,
                subtractAndRound(
                        grossPay,
                        sss,
                        philHealth,
                        pagIbig
                )
        );

        double withholdingTax =
                computeWithholdingTax(taxableIncome);

        return sumAndRound(
                sss,
                philHealth,
                pagIbig,
                withholdingTax
        );
    }

    /**
     * Validates that an amount is finite and non-negative.
     *
     * @param amount    the amount to check
     * @param fieldName the name used in error messages
     */
    private static void validateAmount(
            double amount,
            String fieldName) {

        if (!Double.isFinite(amount) || amount < 0) {
            throw new IllegalArgumentException(
                    fieldName
                            + " must be a valid non-negative number."
            );
        }
    }

}
