package com.salychevms.familienberatung.enums;

import lombok.Getter;

@Getter
public enum SgbAfterExitType {
    ARBEITSLOSENGELD("Arbeitslosengeld (SGB III)"),
    SUPPLEMENTARY_SGB_II("Ergänzende Leistung (SGB II)"),
    NO("Nein"),
    NO_ANSWER("Keine Angabe");

    private final String label;

    SgbAfterExitType(String label) {
        this.label = label;
    }
}
