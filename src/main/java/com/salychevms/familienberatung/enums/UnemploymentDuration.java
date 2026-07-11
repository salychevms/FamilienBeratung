package com.salychevms.familienberatung.enums;

import lombok.Getter;

@Getter
public enum UnemploymentDuration {
    LESS_THAN_6_MONTHS("Weniger als 6 Monate"),
    BETWEEN_6_AND_12_MONTHS("6 bis unter 12 Monate"),
    MORE_THAN_12_MONTHS("12 Monate oder länger"),
    SPECIAL_LAST_12_MONTHS("Sonderfall innerhalb der letzten 12 Monate");

    private final String label;

    UnemploymentDuration(String label) {
        this.label = label;
    }
}
