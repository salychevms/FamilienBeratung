package com.salychevms.familienberatung.ui.dialog;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class EditField {
    public enum Type{
        TEXT,
        TEXTAREA,
        SELECT
    }

    private final String key;
    private final String label;
    private final Type type;
    private final Object initialValue;
    private final boolean required;
    private final Integer maxLength;
    private final List<?> options;

    public EditField(String key, String label, Type type, Object initialValue, boolean required,
                     Integer maxLength, List<?> options) {
        this.key = key;
        this.label = label;
        this.type = type;
        this.initialValue = initialValue;
        this.required = required;
        this.maxLength = maxLength;
        this.options = options;
    }
}