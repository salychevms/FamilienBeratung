package com.salychevms.familienberatung.enums;

import lombok.Getter;

@Getter
public enum ChildAgeGroup {
    AGE_0_3("0-3 Jahre"),
    AGE_4_6("4-6 Jahre"),
    AGE_7_10("7-10 Jahre"),
    AGE_11_15("11-15 Jahre"),
    AGE_16_18("16-18 Jahre"),
    AGE_19_25("19-25 Jahre"),
    AGE_OVER_25("Über 25 Jahre");

    private final String label;

    ChildAgeGroup(String label) {
        this.label = label;
    }
}
