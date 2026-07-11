package com.salychevms.familienberatung.enums;

import lombok.Getter;

@Getter
public enum ProjectTerminationReason {
    EARLY_SUCCESS("Vorzeitiger Projekterfolg (z.B. Arbeitsaufnahme)"),
    CHANGE_OF_CIRCUMSTANCES("Änderung der Lebensverhältnisse / Wegfall von Voraussetzungen"),
    LACK_OF_MOTIVATION("Mangelnde Motivation / Mitwirkungsbereitschaft"),
    NO_ANSWER("Keine Angabe");

    private final String label;

    ProjectTerminationReason(String label) {
        this.label = label;
    }
}
