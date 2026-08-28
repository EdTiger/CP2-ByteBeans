package services;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Computes daily worked hours using MotorPH time rules.
 *
 * <p>Business rules applied by this class:</p>
 * <ul>
 *   <li>Standard work hours: 08:00 – 17:00 (9 hours with 1-hour lunch)</li>
 *   <li>Grace period: arrivals at or before 08:10 are treated as 08:00</li>
 *   <li>Lunch break: 12:00 – 13:00 is automatically deducted</li>
 *   <li>Maximum payable minutes per day: 480 (8 hours)</li>
 *   <li>Early arrivals are normalised to 08:00</li>
 *   <li>Departures after 17:00 are capped at 17:00 (no overtime)</li>
 * </ul>
 *
 * <p>This is a stateless utility class; all methods are {@code static} and
 * the constructor is private to prevent instantiation.</p>
 */
public final class AttendanceComputation {

    /** Official start of the workday (08:00). */
    private static final LocalTime WORK_START = LocalTime.of(8, 0);

    /** End of the grace period; arrivals after this time are marked late (08:10). */
    private static final LocalTime GRACE_PERIOD_END = LocalTime.of(8, 10);

    /** Start of the mandatory lunch break (12:00). */
    private static final LocalTime LUNCH_START = LocalTime.of(12, 0);

    /** End of the mandatory lunch break (13:00). */
    private static final LocalTime LUNCH_END = LocalTime.of(13, 0);

    /** Official end of the workday; clock-outs after this are capped here (17:00). */
    private static final LocalTime WORK_END = LocalTime.of(17, 0);

    /** Formatter for parsing time strings in {@code H:mm} format (e.g. "8:00", "17:00"). */
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("H:mm");

    private AttendanceComputation() {
        // Utility class; prevent instantiation.
    }

    /**
     * Calculates the number of worked hours for a single day from string inputs.
     *
     * <p>Parses the provided time-in and time-out strings, then delegates to
     * {@link #calculateWorkedHours(LocalTime, LocalTime)}.</p>
     *
     * @param timeIn  the clock-in time as a string in {@code H:mm} format
     * @param timeOut the clock-out time as a string in {@code H:mm} format
     * @return the total worked hours (max 8.0), excluding the lunch break
     * @throws IllegalArgumentException if either string is null, blank, or unparseable
     */
    public static double calculateWorkedHours(
            String timeIn,
            String timeOut) {

        // Parse the raw time strings into LocalTime objects.
        LocalTime parsedTimeIn = parseTime(timeIn, "time in");
        LocalTime parsedTimeOut = parseTime(timeOut, "time out");

        return calculateWorkedHours(parsedTimeIn, parsedTimeOut);
    }

    /**
     * Calculates the number of worked hours for a single day from {@link LocalTime} inputs.
     *
     * <p>Applies MotorPH business rules: early arrivals snap to 08:00, departures
     * after 17:00 are capped, the 12:00–13:00 lunch break is deducted, and the
     * result is clamped to a maximum of 480 minutes (8 hours).</p>
     *
     * @param timeIn  the employee's clock-in time (must not be null)
     * @param timeOut the employee's clock-out time (must be after {@code timeIn})
     * @return the total worked hours as a {@code double} (e.g. 8.0 for a full day)
     * @throws IllegalArgumentException if either argument is null or timeOut is not after timeIn
     */
    public static double calculateWorkedHours(
            LocalTime timeIn,
            LocalTime timeOut) {

        // Guard: both time values are mandatory.
        if (timeIn == null || timeOut == null) {
            throw new IllegalArgumentException(
                    "Time in and time out are required."
            );
        }

        // Guard: clock-out must be strictly after clock-in.
        if (!timeOut.isAfter(timeIn)) {
            throw new IllegalArgumentException(
                    "Time out must be later than time in."
            );
        }

        // Apply grace-period and cap rules to get payable boundaries.
        LocalTime effectiveTimeIn = getEffectiveTimeIn(timeIn);
        LocalTime effectiveTimeOut = getEffectiveTimeOut(timeOut);

        // If effective times overlap or reverse (e.g. very late arrival), no hours are payable.
        if (!effectiveTimeOut.isAfter(effectiveTimeIn)) {
            return 0.0;
        }

        // Calculate raw worked minutes between effective boundaries.
        long workedMinutes = Duration.between(
                effectiveTimeIn,
                effectiveTimeOut
        ).toMinutes();

        // Subtract any overlap with the 12:00–13:00 lunch break.
        workedMinutes -= calculateLunchBreakMinutes(
                effectiveTimeIn,
                effectiveTimeOut
        );

        // Clamp to [0, 480] minutes (max 8 payable hours per day).
        workedMinutes = Math.max(0, Math.min(workedMinutes, 480));

        // Convert minutes to fractional hours.
        return workedMinutes / 60.0;
    }

    /**
     * Calculates how many minutes late an employee arrived.
     *
     * <p>If the employee clocked in at or before the grace period (08:10),
     * they are considered on time and zero is returned. Otherwise, lateness
     * is measured from the official start time of 08:00.</p>
     *
     * @param timeIn the clock-in time as a string in {@code H:mm} format
     * @return the number of minutes late, or {@code 0} if on time
     * @throws IllegalArgumentException if {@code timeIn} is null, blank, or unparseable
     */
    public static long calculateLateMinutes(String timeIn) {
        LocalTime parsedTimeIn = parseTime(timeIn, "time in");

        // Arrival within the grace period (≤08:10) is not considered late.
        if (!parsedTimeIn.isAfter(GRACE_PERIOD_END)) {
            return 0;
        }

        // Late minutes are counted from the official work start (08:00).
        return Duration.between(
                WORK_START,
                parsedTimeIn
        ).toMinutes();
    }

    /**
     * Returns the effective (payable) clock-in time.
     *
     * <p>If the employee arrived at or before the grace period end (08:10),
     * their time-in is normalised to the official start time (08:00).
     * Otherwise, the actual clock-in time is used.</p>
     *
     * @param timeIn the raw clock-in time
     * @return {@code 08:00} if within grace period, otherwise {@code timeIn} unchanged
     */
    private static LocalTime getEffectiveTimeIn(LocalTime timeIn) {
        // Arrival within the grace window snaps to the official start.
        if (!timeIn.isAfter(GRACE_PERIOD_END)) {
            return WORK_START;
        }

        return timeIn;
    }

    /**
     * Returns the effective (payable) clock-out time.
     *
     * <p>Clock-outs after the official end time (17:00) are capped at 17:00
     * because overtime is not recognised in this payroll model.</p>
     *
     * @param timeOut the raw clock-out time
     * @return {@code 17:00} if past the end of day, otherwise {@code timeOut} unchanged
     */
    private static LocalTime getEffectiveTimeOut(LocalTime timeOut) {
        // Cap departure at the official end of the workday.
        if (timeOut.isAfter(WORK_END)) {
            return WORK_END;
        }

        return timeOut;
    }

    /**
     * Calculates the number of minutes that overlap with the lunch break.
     *
     * <p>The lunch break is defined as 12:00 – 13:00. Only the portion of
     * the employee's work span that falls within this window is deducted.
     * If the employee's work span does not overlap lunch at all, zero is returned.</p>
     *
     * @param timeIn  the effective clock-in time
     * @param timeOut the effective clock-out time
     * @return the number of lunch-overlap minutes to deduct (0–60)
     */
    public static long calculateLunchBreakMinutes(
            LocalTime timeIn,
            LocalTime timeOut) {

        // Determine the start of the overlap: the later of timeIn or 12:00.
        LocalTime overlapStart =
                timeIn.isAfter(LUNCH_START) ? timeIn : LUNCH_START;

        // Determine the end of the overlap: the earlier of timeOut or 13:00.
        LocalTime overlapEnd =
                timeOut.isBefore(LUNCH_END) ? timeOut : LUNCH_END;

        // No overlap exists if the computed end is not after the computed start.
        if (!overlapEnd.isAfter(overlapStart)) {
            return 0;
        }

        return Duration.between(
                overlapStart,
                overlapEnd
        ).toMinutes();
    }

    /**
     * Parses a time string into a {@link LocalTime} using the {@code H:mm} format.
     *
     * @param time      the time string to parse (e.g. "8:00", "17:30")
     * @param fieldName a human-readable label used in error messages (e.g. "time in")
     * @return the parsed {@link LocalTime}
     * @throws IllegalArgumentException if {@code time} is null/blank or cannot be parsed
     */
    private static LocalTime parseTime(
            String time,
            String fieldName) {

        // Reject null or blank time values.
        if (time == null || time.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "The " + fieldName + " value is required."
            );
        }

        // Attempt to parse; wrap DateTimeParseException in IllegalArgumentException.
        try {
            return LocalTime.parse(time.trim(), TIME_FORMAT);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(
                    "Invalid " + fieldName
                    + ". Use the HH:mm format.",
                    exception
            );
        }
    }
}