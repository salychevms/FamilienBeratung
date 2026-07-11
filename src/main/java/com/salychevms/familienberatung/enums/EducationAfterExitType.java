package com.salychevms.familienberatung.enums;

import lombok.Getter;

@Getter
public enum EducationAfterExitType {
    GENERAL_SCHOOL("Allgemeinbildende Schule"),
    VOCATIONAL_TRAINING("Berufliche Ausbildung"),
    CONTINUING_EDUCATION("Berufliche Weiterbildung"),
    STUDY("Studium");

    private final String label;

    EducationAfterExitType(String label) {
        this.label = label;
    }
}
