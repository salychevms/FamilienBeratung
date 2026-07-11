package com.salychevms.familienberatung.enums;

import lombok.Getter;

@Getter
public enum ContractType {
    LIMITED("Befristeter Arbeitsvertrag"),
    TEMP_WORK("Zeitarbeit/Leiharbeit"),
    PARENTAL_OR_CARE_LEAVE("Elternzeit oder Pflegezeit");

    private final String label;

    ContractType(String label) {
        this.label = label;
    }
}
