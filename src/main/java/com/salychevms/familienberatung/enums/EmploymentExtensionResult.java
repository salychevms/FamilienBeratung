package com.salychevms.familienberatung.enums;

import lombok.Getter;

@Getter
public enum EmploymentExtensionResult {
    PARTICIPANT("Ja, die die teilnehmende Person"),
    PARTNER("Ja, der Partner / die partnerin"),
    NO("Nein"),
    NO_ANSWER("Keine Angabe");

    private final String label;

    EmploymentExtensionResult(String label) {
        this.label = label;
    }
}
