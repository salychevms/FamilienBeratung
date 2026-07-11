package com.salychevms.familienberatung.enums;

import lombok.Getter;

@Getter
public enum QualificationProofType {
    CERTIFICATE("Bescheinigung über berufliche Qualifizierung"),
    HIGHER_ED_LEVEL("Erreichung eines höheren Bildungsstand (ISCED/DQR)"),
    FORMAL_LEARNING_RESULT("Formale Feststellung eines Lernergebnisses");

    private final String label;

    QualificationProofType(String label) {
        this.label = label;
    }
}
