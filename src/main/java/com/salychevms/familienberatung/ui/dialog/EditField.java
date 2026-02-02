package com.salychevms.familienberatung.ui.dialog;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Setter
@Getter
public class EditField {
    public enum Type{
        TEXT,
        TEXTAREA,
        SELECT,
        DATE
    }

    private final String key;
    private final String label;
    private final Type type;
    private final Object initialValue;
    private final boolean required;
    private final Integer maxLength;
    private final List<?> options;
    private final LocalDate minDate;
    private final LocalDate maxDate;

    public EditField(String key, String label, Type type, Object initialValue, boolean required,
                     Integer maxLength, List<?> options, LocalDate minDate, LocalDate maxDate) {
        this.key = key;
        this.label = label;
        this.type = type;
        this.initialValue = initialValue;
        this.required = required;
        this.maxLength = maxLength;
        this.options = options;
        this.minDate = minDate;
        this.maxDate = maxDate;
    }
}