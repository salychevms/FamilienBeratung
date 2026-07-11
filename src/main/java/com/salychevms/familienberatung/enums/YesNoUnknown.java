package com.salychevms.familienberatung.enums;

import lombok.Getter;

@Getter
public enum YesNoUnknown {
    YES("Ja"),
    NO("Nein"),
    NO_ANSWER("Keine Angabe");
    private final String label;

    YesNoUnknown(String label) {
        this.label = label;
    }
}
