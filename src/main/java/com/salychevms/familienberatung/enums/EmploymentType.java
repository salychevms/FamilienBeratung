package com.salychevms.familienberatung.enums;

import lombok.Getter;

@Getter
public enum EmploymentType {
    SOCIAL_INSURED("Sozialversicherungspflichtig beschäftigt"),
    MINI_JOB("Geringfügig beschäftigt (Minijob)"),
    SELF_EMPLOYED("Selbständig"),
    NONE("Keine Beschäftigung");

    private final String label;

    EmploymentType(String label) {
        this.label = label;
    }
}
