package com.mtdsubmitter.util;

import java.time.LocalDate;
import java.time.Month;

/**
 * Utility class for UK tax year calculations.
 * UK tax year runs from 6 April to 5 April.
 */
public final class TaxYearUtils {

    private TaxYearUtils() {}

    /**
     * Get the tax year label for a given date (e.g., "2026-27").
     */
    public static String getTaxYearLabel(LocalDate date) {
        int startYear = getTaxYearStartYear(date);
        int endYear = startYear + 1;
        return startYear + "-" + String.valueOf(endYear).substring(2);
    }

    /**
     * Get the start year of the tax year containing the given date.
     * E.g., 2026-07-15 → 2026, 2027-03-01 → 2026.
     */
    public static int getTaxYearStartYear(LocalDate date) {
        if (date.getMonthValue() >= 4 && date.getDayOfMonth() >= 6 || date.getMonthValue() > 4) {
            return date.getYear();
        }
        // Jan 1 to Apr 5 belongs to previous year's tax year
        return date.getYear() - 1;
    }

    /**
     * Get the start date of the tax year (6 April).
     */
    public static LocalDate getTaxYearStart(int startYear) {
        return LocalDate.of(startYear, Month.APRIL, 6);
    }

    /**
     * Get the end date of the tax year (5 April next year).
     */
    public static LocalDate getTaxYearEnd(int startYear) {
        return LocalDate.of(startYear + 1, Month.APRIL, 5);
    }

    /**
     * Get the standard quarter dates for a tax year.
     * Returns array of [start, end] pairs for Q1-Q4.
     */
    public static LocalDate[][] getStandardQuarters(int taxYearStartYear) {
        return new LocalDate[][] {
            // Q1: 6 Apr – 5 Jul, due 7 Aug
            { LocalDate.of(taxYearStartYear, 4, 6), LocalDate.of(taxYearStartYear, 7, 5) },
            // Q2: 6 Jul – 5 Oct, due 7 Nov
            { LocalDate.of(taxYearStartYear, 7, 6), LocalDate.of(taxYearStartYear, 10, 5) },
            // Q3: 6 Oct – 5 Jan, due 7 Feb
            { LocalDate.of(taxYearStartYear, 10, 6), LocalDate.of(taxYearStartYear + 1, 1, 5) },
            // Q4: 6 Jan – 5 Apr, due 7 May
            { LocalDate.of(taxYearStartYear + 1, 1, 6), LocalDate.of(taxYearStartYear + 1, 4, 5) }
        };
    }

    /**
     * Get the deadline for a standard quarter submission.
     * Deadline is the 7th of the month after the quarter ends.
     */
    public static LocalDate getQuarterDeadline(int taxYearStartYear, int quarterNumber) {
        return switch (quarterNumber) {
            case 1 -> LocalDate.of(taxYearStartYear, 8, 7);      // Q1 due 7 Aug
            case 2 -> LocalDate.of(taxYearStartYear, 11, 7);     // Q2 due 7 Nov
            case 3 -> LocalDate.of(taxYearStartYear + 1, 2, 7);  // Q3 due 7 Feb
            case 4 -> LocalDate.of(taxYearStartYear + 1, 5, 7);  // Q4 due 7 May
            default -> throw new IllegalArgumentException("Quarter must be 1-4, got: " + quarterNumber);
        };
    }

    /**
     * Get the Final Declaration deadline (31 January following end of tax year).
     */
    public static LocalDate getFinalDeclarationDeadline(int taxYearStartYear) {
        return LocalDate.of(taxYearStartYear + 2, 1, 31);
    }

    /**
     * Determine which quarter a date falls in (1-4).
     */
    public static int getQuarterForDate(LocalDate date) {
        LocalDate[][] quarters = getStandardQuarters(getTaxYearStartYear(date));
        for (int i = 0; i < 4; i++) {
            if (!date.isBefore(quarters[i][0]) && !date.isAfter(quarters[i][1])) {
                return i + 1;
            }
        }
        throw new IllegalArgumentException("Date does not fall within any quarter: " + date);
    }
}
