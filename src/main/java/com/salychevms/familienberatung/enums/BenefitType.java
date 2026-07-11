package com.salychevms.familienberatung.enums;

import lombok.Getter;

@Getter
public enum BenefitType {
    BEEG_BKGG("BEEG / BKGG (Elterngeld, Kindergeld usw.)"),
    UHVORSCHG("Unterhaltsvorschussgesetz (UhVorschG)"),
    MUTTER_UND_KIND("Bundesstiftung Mutter und Kind"),
    SGB_II_BASIC("SGB II Grundsicherung (Bürgergeld usw.)"),
    SGB_II_EDU("SGB II Bildung und Teilhabe"),
    LABOR_MARKET("SGB II / III Arbeitsmarktleistungen"),
    SGB_V_VII("SGB V / VII (Kranken-/Umfallversicherung)"),
    SGB_IX("SGB IX (Rehabilitation und Teilhabe)"),
    SGB_XII("SGB XII (Sozialhilfe)"),
    WOGG("Wohngeld (WoGG / WoBindG"),
    OTHER("Sonstige finanzielle Leistungen"),
    NONE("keine Leistung");

    private final String label;

    BenefitType(String label) {
        this.label = label;
    }
}
