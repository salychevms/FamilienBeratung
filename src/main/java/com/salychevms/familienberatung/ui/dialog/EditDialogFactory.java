package com.salychevms.familienberatung.ui.dialog;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class EditDialogFactory {

    public static void openEditDialog(String title, List<EditField> fields, Consumer<Map<String, Object>> onSave) {
        Dialog dialog = new Dialog();
        dialog.setModal(true);
        dialog.setCloseOnOutsideClick(false);
        dialog.setWidth("600px");

        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(true);

        Map<String, Component> components = new HashMap<>();

        for (EditField field : fields) {
            Component c = buildComponent(field);
            components.put(field.getKey(), c);
            content.add(c);
        }

        Button cancel = new Button("Abbrechen", e -> {
            if (!isFormDirty(fields, components)) {
                dialog.close();
                return;
            }

            Dialog confirm = new Dialog();
            confirm.setModal(true);

            Span text = new Span("Änderungen gehen verloren. Fortfahren?");

            Button yes = new Button("Ja", ev -> {
                confirm.close();
                dialog.close();
            });

            Button no = new Button("Nein", ev -> confirm.close());

            HorizontalLayout buttons = new HorizontalLayout(yes, no);
            confirm.add(new VerticalLayout(text, buttons));
            confirm.open();
        });

        Button save = new Button("Speichern", e -> {
            if (!validateForm(fields, components)) return;

            Map<String, Object> changedValues = collectChangedValues(fields, components);

            if (changedValues.isEmpty()) {
                dialog.close();
                return;
            }

            onSave.accept(changedValues);
            dialog.close();
        });

        HorizontalLayout buttons = new HorizontalLayout(save, cancel);
        buttons.setWidthFull();
        buttons.setJustifyContentMode(HorizontalLayout.JustifyContentMode.END);

        dialog.add(new Span(title), content);
        dialog.getFooter().add(buttons);
        dialog.open();
    }

    public static boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) return true;
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }

    public static boolean isValidText(String text, int maxLength) {
        if (text == null || text.isBlank()) return true;
        String v = text.trim();
        if (v.length() > maxLength) return false;
        return v.chars().allMatch(c -> c >= 32 || c == 10 || c == 13);
    }

    public static boolean isValidFile(String originalName, long size, String type) {
        if (originalName == null) return false;
        if (originalName.contains("..") || originalName.contains("/") || originalName.contains("\\")) return false;

        if (size <= 0 || size > 10_000_000) return false;

        return type != null && type.matches("(?i)pdf|jpg|jpeg|png|docx");
    }

    public static boolean isValidPassword(String password, String oldPassword, String login, String email,
                                          String mobileNumber, String landNumber) {
        if (password == null || password.isBlank()) return false;

        if (password.length() < 12) return false;
        if (!password.matches(".*[A-Z].*")) return false;
        if (!password.matches(".*[a-z].*")) return false;
        if (!password.matches(".*\\d.*")) return false;
        if (!password.matches(".*[^A-Za-z0-9].*")) return false;

        if (!password.equals(password.trim())) return false;

        if (login != null && password.equalsIgnoreCase(login)) return false;
        if (email != null && password.equalsIgnoreCase(email)) return false;
        if (mobileNumber != null && password.equalsIgnoreCase(mobileNumber)) return false;
        if (landNumber != null && password.equalsIgnoreCase(landNumber)) return false;

        if (oldPassword != null && similarity(password, oldPassword) > 0.8) return false;
        return true;
    }

    private static double similarity(String a, String b) {
        int max = Math.max(a.length(), b.length());
        int same = 0;

        for (int i = 0; i < Math.min(a.length(), b.length()); i++) {
            if (a.charAt(i) == b.charAt(i)) same++;
        }
        return (double) same / (double) max;
    }

    private static Component buildComponent(EditField field) {
        if (field.getType() == EditField.Type.TEXT) {
            TextField tf = new TextField(field.getLabel());
            tf.setWidthFull();

            if (field.getInitialValue() != null) tf.setValue(field.getInitialValue().toString());
            if (field.getMaxLength() != null) tf.setMaxLength(field.getMaxLength());

            tf.setRequiredIndicatorVisible(field.isRequired());

            return tf;
        }

        if (field.getType() == EditField.Type.TEXTAREA) {
            TextArea ta = new TextArea(field.getLabel());
            ta.setWidthFull();

            if (field.getInitialValue() != null) ta.setValue(field.getInitialValue().toString());
            if (field.getMaxLength() != null) ta.setMaxLength(field.getMaxLength());

            ta.setRequiredIndicatorVisible(field.isRequired());

            return ta;
        }

        if (field.getType() == EditField.Type.SELECT) {
            ComboBox cb = new ComboBox(field.getLabel());
            cb.setWidthFull();
            cb.setItems(field.getOptions());
            cb.setValue(field.getInitialValue());

            cb.setRequiredIndicatorVisible(field.isRequired());

            return cb;
        }

        if (field.getType() == EditField.Type.DATE) {
            DatePicker dp = new DatePicker(field.getLabel());
            dp.setWidthFull();

            if (field.getInitialValue() != null)
                dp.setValue((LocalDate) field.getInitialValue());

            if(field.getMinDate()!=null)
                dp.setMin(field.getMinDate());

            if(field.getMaxDate()!=null)
                dp.setMax(field.getMaxDate());

            dp.setRequiredIndicatorVisible(field.isRequired());
            return dp;
        }

        throw new IllegalStateException("Unknown field type");
    }

    private static Object readValue(Component c) {
        if (c instanceof TextField tf) return tf.getValue();
        if (c instanceof TextArea ta) return ta.getValue();
        if (c instanceof ComboBox<?> cb) return cb.getValue();
        if (c instanceof DatePicker dp) return dp.getValue();
        return null;
    }

    private static boolean isValueEmpty(Object value) {
        if (value == null) return true;
        if (value instanceof String s) return s.isBlank();
        return false;
    }

    private static boolean validateField(EditField field, Component c) {
        if (c instanceof TextField tf) {
            tf.setInvalid(false);
            tf.setErrorMessage(null);
        }
        if (c instanceof ComboBox<?> cb) {
            cb.setInvalid(false);
            cb.setErrorMessage(null);
        }
        if (c instanceof TextArea ta) {
            ta.setInvalid(false);
            ta.setErrorMessage(null);
        }
        if (c instanceof DatePicker dp) {
            dp.setInvalid(false);
            dp.setErrorMessage(null);
        }

        Object value = readValue(c);
        if (field.isRequired() && isValueEmpty(value)) {
            setInvalid(c, "Pflichtfeld");
            return false;
        }
        if (field.getType() == EditField.Type.DATE) {
            if (!(value instanceof LocalDate)) {
                setInvalid(c, "Ungültiges Datum");
                return false;
            }
        }
        if (value instanceof String s && field.getMaxLength() != null && s.length() > field.getMaxLength()) {
            setInvalid(c, "Maximal " + field.getMaxLength() + " Zeichen");
            return false;
        }
        return true;
    }

    private static void setInvalid(Component c, String message) {
        if (c instanceof TextField tf) {
            tf.setInvalid(true);
            tf.setErrorMessage(message);
            return;
        }
        if (c instanceof ComboBox<?> cb) {
            cb.setInvalid(true);
            cb.setErrorMessage(message);
            return;
        }
        if (c instanceof TextArea ta) {
            ta.setInvalid(true);
            ta.setErrorMessage(message);
        }
    }

    private static boolean isFieldDirty(EditField field, Component c) {
        Object current = readValue(c);
        Object initial = field.getInitialValue();

        if (initial == null && current == null) return false;
        if (initial == null) return !isValueEmpty(current);
        if (current == null) return true;

        if (initial instanceof String s1 && current instanceof String s2) return !s1.equals(s2);
        return !initial.equals(current);
    }

    private static boolean isFormDirty(List<EditField> fields, Map<String, Component> components) {
        for (EditField field : fields) {
            Component c = components.get(field.getKey());
            if (c == null) continue;
            if (isFieldDirty(field, c)) return true;
        }
        return false;
    }

    private static boolean validateForm(List<EditField> fields, Map<String, Component> components) {
        boolean ok = true;
        for (EditField field : fields) {
            Component c = components.get(field.getKey());
            if (c == null) continue;

            boolean valid = validateField(field, c);
            if (!valid) ok = false;
        }
        return ok;
    }

    private static Map<String, Object> collectChangedValues(List<EditField> fields, Map<String, Component> components) {
        Map<String, Object> changed = new HashMap<>();

        for (EditField field : fields) {
            Component c = components.get(field.getKey());
            if (c == null) continue;
            if (isFieldDirty(field, c)) changed.put(field.getKey(), readValue(c));
        }
        return changed;
    }
}