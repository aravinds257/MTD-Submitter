package com.mtdsubmitter.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum SelfEmploymentExpenseCategory {
    COST_OF_GOODS("costOfGoods", "Cost of goods"),
    STAFF_COSTS("staffCosts", "Staff costs"),
    TRAVEL_COSTS("travelCosts", "Travel & vehicle"),
    PREMISES_RUNNING_COSTS("premisesRunningCosts", "Premises costs"),
    MAINTENANCE_COSTS("maintenanceCosts", "Repairs & maintenance"),
    ADMIN_COSTS("adminCosts", "Office & admin"),
    ADVERTISING_COSTS("advertisingCosts", "Advertising"),
    BUSINESS_ENTERTAINMENT_COSTS("businessEntertainmentCosts", "Entertainment"),
    INTEREST("interest", "Loan interest"),
    FINANCIAL_COSTS("financialCosts", "Bank charges"),
    BAD_DEBT("badDebt", "Bad debts"),
    PROFESSIONAL_FEES("professionalFees", "Professional fees"),
    DEPRECIATION("depreciation", "Depreciation"),
    OTHER("other", "Other expenses"),
    CONSOLIDATED_EXPENSES("consolidatedExpenses", "Total expenses (simplified)");

    private final String hmrcFieldName;
    private final String displayLabel;

    public static SelfEmploymentExpenseCategory fromHmrcFieldName(String hmrcFieldName) {
        return Arrays.stream(values())
                .filter(c -> c.hmrcFieldName.equals(hmrcFieldName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown HMRC field name: " + hmrcFieldName));
    }
}
