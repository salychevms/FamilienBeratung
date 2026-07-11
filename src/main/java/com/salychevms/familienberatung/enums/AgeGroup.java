package com.salychevms.familienberatung.enums;

import lombok.Getter;

@Getter
public enum AgeGroup {
    UNDER_18("17 Jahre oder jünger"),
    AGE_18_29("18-29 Jahre"),
    AGE_30_54("30-54 Jahre"),
    AGE_55_PLUS("55 Jahre oder älter");

    private final String label;

    AgeGroup(String label) {
        this.label = label;
    }
}
