package services;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Shared deterministic money-rounding utility functions.
 *
 * <p>All monetary arithmetic in the MotorPH payroll system passes through
 * this class to avoid floating-point drift. Every operation converts
 * {@code double} values to {@link BigDecimal}, performs the computation,
 * and rounds the result to two decimal places using
 * {@link RoundingMode#HALF_UP} before returning a {@code double}.</p>
 *
 * <p>This is a stateless utility class; all methods are {@code static} and
 * the constructor is private to prevent instantiation.</p>
 */
public final class MoneyMath {

    private MoneyMath() {
        // Static utility module; prevent instantiation.
    }

    /**
     * Rounds a monetary value to two decimal places (cents) using {@link RoundingMode#HALF_UP}.
     *
     * @param value the raw monetary amount
     * @return the value rounded to the nearest cent
     * @throws IllegalArgumentException if {@code value} is not finite
     */
    public static double roundToCent(double value) {
        return toCents(decimal(value));
    }

    /**
     * Multiplies two values and rounds the product to two decimal places.
     *
     * @param firstValue  the multiplicand
     * @param secondValue the multiplier
     * @return the product, rounded to the nearest cent
     * @throws IllegalArgumentException if either value is not finite
     */
    public static double multiplyAndRound(
            double firstValue,
            double secondValue) {

        return toCents(
                decimal(firstValue).multiply(decimal(secondValue))
        );
    }

    /**
     * Sums one or more monetary values and rounds the total to two decimal places.
     *
     * @param values the monetary amounts to add
     * @return the sum, rounded to the nearest cent
     * @throws IllegalArgumentException if {@code values} is null or any element is not finite
     */
    public static double sumAndRound(double... values) {
        if (values == null) {
            throw new IllegalArgumentException("Money values are required.");
        }

        BigDecimal total = BigDecimal.ZERO;

        // Accumulate each value via BigDecimal to avoid floating-point drift.
        for (double value : values) {
            total = total.add(decimal(value));
        }

        return toCents(total);
    }

    /**
     * Subtracts one or more values from a starting value and rounds the result.
     *
     * @param startingValue    the initial monetary amount
     * @param valuesToSubtract the amounts to subtract in order
     * @return the difference, rounded to two decimal places
     * @throws IllegalArgumentException if {@code valuesToSubtract} is null or any value is not finite
     */
    public static double subtractAndRound(
            double startingValue,
            double... valuesToSubtract) {

        if (valuesToSubtract == null) {
            throw new IllegalArgumentException("Money values are required.");
        }

        BigDecimal result = decimal(startingValue);

        // Subtract each value sequentially via BigDecimal.
        for (double value : valuesToSubtract) {
            result = result.subtract(decimal(value));
        }

        return toCents(result);
    }

    /**
     * Computes a progressive-bracket tax or contribution amount.
     *
     * <p>Formula: {@code baseAmount + (value − threshold) × rate},
     * rounded to two decimal places.</p>
     *
     * @param baseAmount the fixed base amount for the bracket
     * @param value      the income or salary being evaluated
     * @param threshold  the lower boundary of the bracket
     * @param rate       the marginal percentage applied to the excess
     * @return the computed amount, rounded to the nearest cent
     */
    public static double basePlusPercentageOfExcess(
            double baseAmount,
            double value,
            double threshold,
            double rate) {

        // Calculate excess over the bracket threshold.
        BigDecimal excess = decimal(value).subtract(decimal(threshold));

        // Add the marginal amount to the bracket's base tax.
        BigDecimal result = decimal(baseAmount).add(
                excess.multiply(decimal(rate))
        );

        return toCents(result);
    }

    /**
     * Converts a {@code double} to a {@link BigDecimal}, rejecting non-finite values.
     *
     * @param value the numeric value to convert
     * @return a {@link BigDecimal} representing the value
     * @throws IllegalArgumentException if {@code value} is NaN or infinite
     */
    private static BigDecimal decimal(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(
                    "Money value must be a finite number."
            );
        }

        return BigDecimal.valueOf(value);
    }

    /**
     * Rounds a {@link BigDecimal} to two decimal places using {@link RoundingMode#HALF_UP}
     * and converts it back to a {@code double}.
     *
     * @param value the BigDecimal to round
     * @return the rounded value as a primitive double
     */
    private static double toCents(BigDecimal value) {
        return value
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
