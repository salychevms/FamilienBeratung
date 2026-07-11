package com.salychevms.familienberatung.enums;

import lombok.Getter;

@Getter
public enum SchoolDegree {
    SPECIAL_SCHOOL("Förderschulabschluss"),
    HAUPTSCHULE("Hauptschulabschluss"),
    REALSCHULE("Mittlerer SChulabschluss (Realschule)"),
    VOC_PREPARATION_YEAR("Berufsvorbereitungsjaht o. ä."),
    VOC_BASIC_YEAR("Berufsgrundbildungsjahr"),
    ABITUR_FIRST_PATH("Abitur/Fachhochschulreife (1. Bildungsweg)"),
    ABITUR_SECOND_PATH("Abitur/Fachhochschulreife (2. Bildungsweg)"),
    NO_DEGREE_4_PLUS_YEARS("Kein Abschluss (mind. 4 Jahre Schule)"),
    NO_DEGREE_UNDER_4_YEARS("Kein Abschluss (unter 4 Jahre Schule)");

    private final String label;

    SchoolDegree(String label) {
        this.label = label;
    }
}
