package com.salychevms.familienberatung.enums;

import lombok.Getter;

@Getter
public enum WorkingHoursType {
    FULL_TIME("Vollzeit (35+ Std./Woche)"),
    PART_TIME("Teilzeit (bis 34 Std./Woche");

    private final String label;

    WorkingHoursType(String label) {
        this.label = label;
    }
}
