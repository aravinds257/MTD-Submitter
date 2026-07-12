package com.mtdsubmitter.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ObligationStatus {
    OPEN("Open"),
    FULFILLED("Submitted"),
    OVERDUE("Overdue");

    private final String displayLabel;

    public String getCssClass() {
        return switch (this) {
            case OPEN -> "success";
            case FULFILLED -> "warning";
            case OVERDUE -> "danger";
        };
    }
}
