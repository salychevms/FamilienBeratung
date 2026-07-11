package com.salychevms.familienberatung.enums;

import lombok.Getter;

@Getter
public enum BarrierType {
    EMPLOYER_SENSITIZATION("Sensibilisierung der Arbeitgeber"),
    COACHING("Coaching der teilnehmenden Person"),
    REALISTIC_PERSPECTIVE("Entwicklung realistischer Perspektiven"),
    MOTIVATION("Motivation und Aktivierung"),
    OTHER("Sonstiges");

    private final String label;

    BarrierType(String label) {
        this.label = label;
    }
}
