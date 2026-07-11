package com.salychevms.familienberatung.enums;

import lombok.Getter;

@Getter
public enum MemberGender {
    MAENNLICH("Männlich"),
    WEIBLICH("Weiblich"),
    DIVERS("Divers");
    private final String label;

    MemberGender(String label) {
        this.label = label;
    }
}