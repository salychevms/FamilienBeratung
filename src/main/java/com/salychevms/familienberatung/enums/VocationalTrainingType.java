package com.salychevms.familienberatung.enums;

import lombok.Getter;

@Getter
public enum VocationalTrainingType {
    COMPANY_BASED("Betriebliche Berufsausbildung"),
    SCHOOL_BASED("Schulische Berufausbildung"),
    EXTRA_COMPANY("Außerbetriebliche Berufsausbildung");

    private final String label;

    VocationalTrainingType(String label) {
        this.label = label;
    }
}
