package com.mtdsubmitter.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BusinessType {
    SELF_EMPLOYMENT("Self-Employment"),
    UK_PROPERTY("UK Property"),
    FOREIGN_PROPERTY("Foreign Property");

    private final String displayLabel;
}
