package com.salychevms.familienberatung.enums;

import lombok.Getter;

@Getter
public enum SupportType {
    SGB_II_SUPPORT("SGB Ii Beratungs-/Betreuungsangebote (§16a)"),
    SGB_III_SUPPORT("SGB II Bildungs-/Vermittlungsangebote"),
    HEALTH_PREVENTION("Gesundheitsprävetion (SGB V / VII)"),
    YOUTH_WELFARE("Kinder- und Jugendhilfe (SGB VIII)"),
    SGB_IX_SUPPORT("Beratung Teilhabe (SGB IX)"),
    OTHER("Sonstige nicht-finanzielle Hilfeangebote"),
    NONE("Keine Hilfeangebote");

    private final String label;

    SupportType(String label) {
        this.label = label;
    }
}
