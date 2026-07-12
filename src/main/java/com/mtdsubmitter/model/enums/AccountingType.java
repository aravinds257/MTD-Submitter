package com.mtdsubmitter.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AccountingType {
    CASH("Cash basis"),
    ACCRUALS("Traditional accounting");

    private final String displayLabel;
}
