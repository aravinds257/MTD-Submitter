package com.mtdsubmitter.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum IncomeCategory {
    TURNOVER("turnover", "Turnover / Sales"),
    OTHER_INCOME("otherIncome", "Other business income"),
    RENTAL_INCOME("totalRentsReceived", "Rental income"),
    PREMIUM_INCOME("premiumsOfLeaseGrant", "Lease premiums"),
    OTHER_PROPERTY_INCOME("otherPropertyIncome", "Other property income");

    private final String hmrcFieldName;
    private final String displayLabel;

    public static IncomeCategory fromHmrcFieldName(String hmrcFieldName) {
        return Arrays.stream(values())
                .filter(c -> c.hmrcFieldName.equals(hmrcFieldName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown HMRC field name: " + hmrcFieldName));
    }
}
