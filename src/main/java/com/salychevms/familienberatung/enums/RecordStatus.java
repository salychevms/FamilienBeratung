package com.salychevms.familienberatung.enums;

import lombok.Getter;

@Getter
public enum RecordStatus {
    ACTIVE("AKTIF"),
    INVALID("GELÖSCHT"),
    ARCHIVED("ARKHIV"),
    BLOCKED("BLOCKIERT");

    private final String label;

    RecordStatus(String label) {
        this.label = label;
    }
}
