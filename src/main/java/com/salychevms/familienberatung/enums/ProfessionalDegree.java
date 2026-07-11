package com.salychevms.familienberatung.enums;

import lombok.Getter;

@Getter
public enum ProfessionalDegree {
    VOCATIONAL_TRAINING("Berufsausbildung / Lehre"),
    BACHELOR_EQ("Bachelor / Meister / gleichwertig"),
    MASTER_EQ("Master / Diplom (Universität)"),
    PHD("Promotion (Doktortitel)"),
    NO_PROFESSIONAL_DEGREE("Keine abgeschlossene Berufsausbildung");

    private final String label;

    ProfessionalDegree(String label) {
        this.label = label;
    }
}
