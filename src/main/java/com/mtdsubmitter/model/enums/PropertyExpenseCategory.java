package com.mtdsubmitter.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum PropertyExpenseCategory {
    PREMISES_RUNNING_COSTS("premisesRunningCosts", "Property running costs"),
    REPAIRS_AND_MAINTENANCE("repairsAndMaintenance", "Repairs & maintenance"),
    FINANCE_COSTS("financeCosts", "Finance costs"),
    RESIDENTIAL_FINANCE_COST("residentialFinanceCost", "Mortgage interest (residential)"),
    PROFESSIONAL_FEES("professionalFees", "Letting agent / legal fees"),
    COST_OF_SERVICES("costOfServices", "Service charges / staff"),
    OTHER("other", "Other property expenses"),
    CONSOLIDATED_EXPENSES("consolidatedExpenses", "Total expenses (simplified)");

    private final String hmrcFieldName;
    private final String displayLabel;

    public static PropertyExpenseCategory fromHmrcFieldName(String hmrcFieldName) {
        return Arrays.stream(values())
                .filter(c -> c.hmrcFieldName.equals(hmrcFieldName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown HMRC field name: " + hmrcFieldName));
    }
}
