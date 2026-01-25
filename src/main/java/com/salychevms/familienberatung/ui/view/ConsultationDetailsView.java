package com.salychevms.familienberatung.ui.view;

import com.salychevms.familienberatung.model.*;
import com.salychevms.familienberatung.service.*;
import com.salychevms.familienberatung.ui.dialog.EditDialogFactory;
import com.salychevms.familienberatung.ui.dialog.EditField;
import com.salychevms.familienberatung.ui.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.VaadinRequest;
import jakarta.annotation.security.PermitAll;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Slf4j
@Route(value = "consultation/:id", layout = MainLayout.class)
@PageTitle("Beratung")
@RequiredArgsConstructor
@PermitAll
public class ConsultationDetailsView extends VerticalLayout implements BeforeEnterObserver {
    private final AuthService authService;
    private final EmployeeService employeeService;
    private final FamilyService familyService;
    private final ConsultationService consultationService;
    private final DelegationService delegationService;

    private Employee currentEmployee;
    private Family currentFamily;
    private Consultation currentConsultation;
    private int lvl;

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Employee e = authService.getCurrentEmployee();
        if (e == null || e.isArchived() || !e.isActive()) {
            event.forwardTo("login");
            return;
        }

        this.currentEmployee = employeeService.findByLogin(e.getLogin());
        this.lvl = currentEmployee.getRole().getAccessLevel();

        Optional<Long> optId = event.getRouteParameters().getLong("id");
        if (optId.isEmpty()) {
            event.forwardTo("consultations");
            return;
        }

        this.currentConsultation = consultationService.getConsultationById(optId.get(), currentEmployee);
        if (currentConsultation == null) {
            event.forwardTo("consultations");
            return;
        }

        this.currentFamily = familyService.getFamilyById(currentConsultation.getFamily().getId());
        if (currentFamily == null || currentFamily.getStatus().equals(RecordStatus.INVALID)) {
            event.forwardTo("consultations");
            return;
        }

        if (lvl == 50) {
            boolean isOwn = currentFamily.getAssignedEmployee().equals(currentEmployee);
            boolean isActiveDelegation = delegationService.getDelegationByToEmployeeAndFamily(
                    currentEmployee.getLogin(), currentFamily) != null;

            if (!isOwn && !isActiveDelegation) {
                event.forwardTo("consultations");
                return;
            }
        }
        removeAll();
        buildUI();
    }

    private void buildUI() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        buildBreadCrumbs();
        buildHeader();
        buildTopLayout();
        buildDetailsBlock();
        buildAuditBlock();
    }

    private void buildBreadCrumbs() {
        HorizontalLayout bcrumbs = new HorizontalLayout();
        bcrumbs.setSpacing(true);
        bcrumbs.setPadding(true);
        bcrumbs.setAlignItems(FlexComponent.Alignment.CENTER);

        RouterLink l1 = new RouterLink("Übersicht", OverviewView.class);
        RouterLink l2 = new RouterLink("Beratungen", ConsultationsView.class);
        RouterLink l3 = new RouterLink("Familie: " + currentFamily.getFamilyName(),
                FamilyDetailsView.class, new RouteParameters("id", String.valueOf(currentFamily.getId())));

        Span sep1 = new Span(" >> ");
        sep1.getStyle().set("font-size", "var(--lumo-font-size-s)").set("color", "var(--lumo-secondary-text-color)");
        Span sep2 = new Span(" >> ");
        sep2.getStyle().set("font-size", "var(--lumo-font-size-s)").set("color", "var(--lumo-secondary-text-color)");
        Span sep3 = new Span(" >> ");
        sep3.getStyle().set("font-size", "var(--lumo-font-size-s)").set("color", "var(--lumo-secondary-text-color)");

        Span current = new Span("Beratung");
        current.getStyle().set("font-size", "var(--lumo-font-size-s)").set("font-weight", "bold")
                .set("color", "var(--lumo-body-text-color)");

        bcrumbs.add(l1, sep1, l2, sep2, l3, sep3, current);
        add(bcrumbs);
    }

    private void buildHeader() {
        VerticalLayout box = new VerticalLayout();
        box.setSpacing(false);
        box.setPadding(false);
        box.setWidthFull();

        HorizontalLayout header = new HorizontalLayout();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setSpacing(true);

        if (lvl != 10 && !currentConsultation.isInvalid() && currentFamily.getStatus().equals(RecordStatus.ACTIVE)
                && !currentFamily.isCaseClosed()) {
            Button editButton = new Button(VaadinIcon.EDIT.create());
            editButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            editButton.addClassName("edit-btn");

            header.add(editButton);
        }

        String date = currentConsultation.getDateTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
        String duration = formatDuration(currentConsultation.getDurationMinutes());

        H2 title = new H2("Beratung ID: " + currentConsultation.getId());
        Span family = new Span("Familie: " + currentFamily.getFamilyName());
        Span backdated = new Span("Nachträglich gespeichert: " +
                (currentConsultation.isBackdated() ? "Ja" : "Nein"));
        header.add(title);
        box.add(header, family, backdated);

        Span assignedE = new Span("Berater*in: " + currentConsultation.getFamily().getAssignedEmployee().getFirstName()
                + " " + currentConsultation.getFamily().getAssignedEmployee().getLastName());
        box.add(assignedE);
        Span managed = new Span("Durchgeführt von: " + currentConsultation.getEmployee().getFirstName()
                + " " + currentConsultation.getEmployee().getLastName());
        box.add(managed);
        if (!currentConsultation.getFamily().getAssignedEmployee().equals(currentConsultation.getEmployee())) {
            Delegation d = delegationService.getDelegationByToEmployeeAndFamily(
                    currentConsultation.getEmployee().getLogin(), currentFamily);
            if (d != null) {
                String startDate = d.getStartDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
                String endDate = d.getEndDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
                Span delegationTime = new Span("Wurde delegiert von " + startDate + " bis " + endDate);
                Span delegationStatus = new Span("Status: " + (!d.isExpired() ? "noch aktiv" : "abgelaufen"));
                Span reason = new Span("Grund: " + (d.getReason() != null ? d.getReason() : "kA"));
                box.add(delegationTime, delegationStatus, reason);
            }
        }
        add(box);
    }

    private void buildTopLayout() {
        VerticalLayout box = new VerticalLayout();
        box.setSpacing(false);
        box.setPadding(false);
        box.setWidthFull();

        HorizontalLayout btns = buildActionButtons();
        if (btns != null)
            box.add(btns);
        box.add(buildGeneralBlock());
        add(box);
    }

    private HorizontalLayout buildActionButtons() {
        if (lvl != 10 && currentFamily.getStatus().equals(RecordStatus.ACTIVE) && !currentFamily.isCaseClosed()) {
            HorizontalLayout buttons = new HorizontalLayout();
            buttons.setSpacing(false);
            buttons.setPadding(false);
            buttons.setWidthFull();

            Button delete = new Button("Beratung löschen");
            delete.getStyle().set("color", "red");

            delete.addClickListener(e -> {
                Dialog dialog = new Dialog();
                dialog.setHeaderTitle("Beratung löschen");

                Span text = new Span("Möchten Sie diese Beratung wirklich löschen?");
                dialog.add(text);

                Button yes = new Button("Ja", ev -> {
                    VaadinRequest req = VaadinRequest.getCurrent();
                    consultationService.invalidateConsultation(currentConsultation, currentFamily, currentEmployee,
                            req != null ? req.getRemoteAddr() : "UNKNOWN",
                            req != null ? req.getHeader("User-Agent") : "UNKNOWN");
                    dialog.close();
                    getUI().ifPresent(ui -> ui.navigate(ConsultationsView.class));
                });

                Button no = new Button("Nein", ev -> dialog.close());

                HorizontalLayout btns = new HorizontalLayout(no, yes);
                btns.setJustifyContentMode(JustifyContentMode.END);
                btns.setWidthFull();

                dialog.getFooter().add(btns);
                dialog.open();
            });
            buttons.add(delete);
            return buttons;
        }
        return null;
    }

    private VerticalLayout buildGeneralBlock() {
        VerticalLayout block = new VerticalLayout();
        block.setSpacing(false);
        block.setPadding(false);
        block.setWidthFull();
        block.getStyle().set("border", "1px solid #ddd").set("border-radius", "6px").set("padding", "10px");

        HorizontalLayout titleLayout = new HorizontalLayout();
        titleLayout.setSpacing(true);
        titleLayout.setAlignItems(FlexComponent.Alignment.CENTER);

        if (lvl != 10 && currentFamily.getStatus().equals(RecordStatus.ACTIVE) && !currentFamily.isCaseClosed()) {
            Button edit = new Button(VaadinIcon.EDIT.create());
            edit.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            edit.addClassName("edit-btn");
            edit.addClickListener(e -> {
                Dialog dialog = new Dialog();
                dialog.setModal(true);
                dialog.setCloseOnOutsideClick(false);
                dialog.setWidth("800px");

                H2 dlgTitle = new H2("Allgemeine Angaben ändern");
                dialog.add(dlgTitle);

                DateTimePicker dateTime = new DateTimePicker("Datum und Uhrzeit (*)");
                dateTime.setWidthFull();
                dateTime.setRequiredIndicatorVisible(true);
                dateTime.setValue(currentConsultation.getDateTime());

                IntegerField duration = new IntegerField("Dauer (Minuten, 1-480 (*)");
                duration.setWidthFull();
                duration.setRequiredIndicatorVisible(true);
                duration.setValue(currentConsultation.getDurationMinutes());
                duration.setMax(480);
                duration.setMin(1);

                DateTimePicker followUp = new DateTimePicker("Folgetermin");
                followUp.setWidthFull();
                followUp.setRequiredIndicatorVisible(true);
                followUp.setValue(currentConsultation.getFollowUp());

                VerticalLayout blck = new VerticalLayout();
                blck.setSpacing(false);
                blck.setPadding(true);
                blck.getStyle().set("border", "1px solid #ddd").set("padding", "10px").set("border-radius", "6px");

                blck.add(dateTime, duration, followUp);
                dialog.add(blck);

                Button save = new Button("Speichern");
                Button cancel = new Button("Abbrechen", ev -> dialog.close());

                save.addClickListener(ev -> {
                    LocalDateTime dt = dateTime.getValue();
                    Integer dur = duration.getValue();
                    LocalDateTime fUp = followUp.getValue();

                    Dialog err = new Dialog();
                    if (dt == null || dur == null || dur <= 0 || dur > 480) {
                        err.setHeaderTitle("Ungültige Eingaben");
                        err.add(new Span("Bitte Datum/Uhryeit und Dauer korrekt angeben."));
                        err.add(new Button("OK", x -> err.close()));
                        err.open();
                        return;
                    }

                    if (dt.toLocalDate().isAfter(LocalDate.now())) {
                        err.setHeaderTitle("Ungültiges Datum");
                        err.add(new Span("Datum darf nicht in der Zukunft liegen."));
                        err.add(new Button("OK", x -> err.close()));
                        err.open();
                        return;
                    }

                    if (dt.isBefore(currentConsultation.getCreatedAt())) {
                        err.setHeaderTitle("Ungültiges Datum");
                        err.add(new Span("Datum liegt vor Erstellung der Fanilie"));
                        err.add(new Button("OK", x -> err.close()));
                        err.open();
                        return;
                    }

                    if (fUp != null && !fUp.isAfter(dt)) {
                        err.setHeaderTitle("Ungültiger Folgetermin");
                        err.add(new Span("Der Folgetermin muss nach der Bertung liegen"));
                        err.add(new Button("OK", x -> err.close()));
                        err.open();
                        return;
                    }

                    try {
                        VaadinRequest req = VaadinRequest.getCurrent();
                        Consultation updated = consultationService.getConsultationById(
                                currentConsultation.getId(), currentEmployee);
                        updated.setDateTime(dt);
                        updated.setDurationMinutes(dur);
                        updated.setFollowUp(fUp);
                        consultationService.updateConsultation(currentConsultation, updated, currentFamily,
                                currentEmployee, req != null ? req.getRemoteAddr() : "UNKNOWN",
                                req != null ? req.getHeader("User-Agent") : "UNKNOWN");
                        dialog.close();
                        getUI().ifPresent(ui -> ui.getPage().reload());
                    } catch (Exception ex) {
                        Dialog error = new Dialog("Fehler");
                        error.add(new Span("ex.getMessage()"));
                        error.add(new Button("OK", x -> error.close()));
                        error.open();
                    }
                });

                HorizontalLayout buttons = new HorizontalLayout(cancel, save);
                buttons.setWidthFull();
                buttons.setJustifyContentMode(JustifyContentMode.END);

                dialog.getFooter().add(buttons);
                dialog.open();
            });
            titleLayout.add(edit);
        }

        Span title = new Span("Allgemeine Angaben");
        title.getStyle().set("font-weight", "bold");
        titleLayout.add(title);

        String dateTime = currentConsultation.getDateTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
        String duration = formatDuration(currentConsultation.getDurationMinutes());

        Span dateRow = new Span("Datum und Uhrzeit: " + dateTime);
        Span durationRow = new Span("Dauer: " + duration + " St.");

        HorizontalLayout fuLayout = new HorizontalLayout();
        fuLayout.setSpacing(false);
        fuLayout.setPadding(false);
        fuLayout.setWidthFull();
        fuLayout.setAlignItems(FlexComponent.Alignment.CENTER);

        LocalDateTime fu = currentConsultation.getFollowUp();
        Span followUpRow = new Span("Folgetermin: " +
                (fu != null ? fu.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) : "-"));
        fuLayout.add(followUpRow);

        if (currentConsultation.getFollowUp() != null) {
            Button fUpDelete = new Button(VaadinIcon.CLOSE_CIRCLE.create());
            fUpDelete.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            fUpDelete.addClassName("edit-btn");
            fUpDelete.addClickListener(ev -> {
                Dialog fUpDialog = new Dialog();
                fUpDialog.setHeaderTitle("Folgetermin löschen");

                Button yes = new Button("Ja", event -> {
                    fUpDialog.close();
                    VaadinRequest req = VaadinRequest.getCurrent();
                    String ip = req != null ? req.getRemoteAddr() : "UNKNOWN";
                    String browser = req != null ? req.getHeader("User-Agent") : "UNKNOWN";

                    consultationService.deleteFollowUp(currentFamily, currentConsultation, currentEmployee,
                            ip, browser);
                    getUI().ifPresent(ui -> ui.getPage().reload());
                });
                Button no = new Button("Nein", event -> fUpDialog.close());

                fUpDialog.add(new Span("Wollen Sie den Folgetermin löschen?"));

                fUpDialog.getFooter().add(no, yes);
                fUpDialog.open();
            });
            fuLayout.add(fUpDelete);
        }

        block.add(titleLayout, dateRow, durationRow, fuLayout);
        return block;
    }

    private void buildDetailsBlock() {
        VerticalLayout block = new VerticalLayout();
        block.setSpacing(false);
        block.setPadding(false);
        block.setWidthFull();
        block.getStyle().set("border", "1px solid #ddd").set("border-radius", "6px").set("padding", "10px").
                set("padding-bottom", "0");

        HorizontalLayout titleLayout = new HorizontalLayout();
        titleLayout.setSpacing(false);
        titleLayout.setAlignItems(FlexComponent.Alignment.CENTER);

        if (lvl != 10 && !currentFamily.getStatus().equals(RecordStatus.ARCHIVED) && !currentFamily.isCaseClosed()
                && !currentFamily.getStatus().equals(RecordStatus.BLOCKED)) {
            Button edit = new Button(VaadinIcon.EDIT.create());
            edit.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            edit.addClassName("edit-btn");
            edit.addClickListener(ev ->
                    EditDialogFactory.openEditDialog("Details der Beratung ändern", List.of(
                                    new EditField("topic", "Thema", EditField.Type.TEXT,
                                            currentConsultation.getTopic(), false, 255, null),
                                    new EditField("description", "Beschreibung", EditField.Type.TEXTAREA,
                                            currentConsultation.getDescription(), false, 4000, null),
                                    new EditField("result", "Ergebnis", EditField.Type.TEXTAREA,
                                            currentConsultation.getResult(), false, 4000, null)),
                            values -> {
                                try {
                                    Consultation updated = consultationService.getConsultationById(
                                            currentConsultation.getId(), currentEmployee);
                                    if (values.get("topic") != null)
                                        updated.setTopic(values.get("topic").toString());
                                    if (values.get("description") != null)
                                        updated.setDescription(values.get("description").toString());
                                    if (values.get("result") != null)
                                        updated.setResult(values.get("result").toString());

                                    VaadinRequest req = VaadinRequest.getCurrent();
                                    consultationService.updateConsultation(currentConsultation, updated, currentFamily,
                                            currentEmployee, req != null ? req.getRemoteAddr() : "UNKNOWN",
                                            req != null ? req.getHeader("User-Agent") : "UNKNOWN");
                                    getUI().ifPresent(ui -> ui.getPage().reload());
                                } catch (Exception e) {
                                    Dialog err = new Dialog("Fehler");
                                    err.add(new Span(e.getMessage()));
                                    err.add(new Button("OK", x -> err.close()));
                                    err.open();
                                }
                            }
                    ));
            titleLayout.add(edit);
        }

        Span title = new Span("Details der Beratung");
        title.getStyle().set("font-weight", "bold");
        titleLayout.add(title);

        Span topic = new Span("Thema: " + empty(currentConsultation.getTopic()));

        TextArea description = new TextArea("Beschreibung");
        description.setReadOnly(true);
        description.setWidthFull();
        description.setHeight("200px");
        description.setValue(empty(currentConsultation.getDescription()));
        description.getStyle().set("white-space", "pre-wrap");

        TextArea result = new TextArea("Ergebnis");
        result.setReadOnly(true);
        result.setWidthFull();
        result.setHeight("200px");
        result.setValue(empty(currentConsultation.getResult()));
        result.getStyle().set("white-space", "pre-wrap").set("margin-bottom", "0");

        block.add(titleLayout, topic, description, result);
        add(block);
    }

    private void buildAuditBlock() {
        VerticalLayout block = new VerticalLayout();
        block.setSpacing(false);
        block.setPadding(false);
        block.setWidthFull();

        block.getStyle().set("border", "1px solid #ddd").set("border-radius", "6px").set("padding", "10px");

        Span title = new Span("Verlauf");
        title.getStyle().set("font-weight", "bold");
        block.add(title);

        if (currentConsultation.getCreatedAt() != null)
            block.add(makeAuditLine("Erstellt am " + formatDate(currentConsultation.getCreatedAt()) + " um " +
                    formatTime(currentConsultation.getCreatedAt()) + " von " +
                    empty(currentConsultation.getCreatedBy())));
        if (currentConsultation.getUpdatedAt() != null)
            block.add(makeAuditLine("Geändert am " + formatDate(currentConsultation.getUpdatedAt()) + " um " +
                    formatTime(currentConsultation.getUpdatedAt()) + " von " +
                    empty(currentConsultation.getUpdatedBy())));
        if (currentConsultation.isInvalid() && currentConsultation.getInvalidAt() != null)
            block.add(makeAuditLine("Gelöscht am " + formatDate(currentConsultation.getInvalidAt()) + " um " +
                    formatTime(currentConsultation.getInvalidAt()) + " von " +
                    empty(currentConsultation.getInvalidBy())));
        if (!currentConsultation.isInvalid() && currentConsultation.getRestoredAt() != null) {
            block.add(makeAuditLine("Wiederherstellt am " + formatDate(currentConsultation.getRestoredAt()) +
                    " um " + formatTime(currentConsultation.getRestoredAt()) + " von " +
                    empty(currentConsultation.getRestoredBy())));
            if (currentConsultation.getRestoredReason() != null)
                block.add(makeAuditReason("Grund: " + currentConsultation.getRestoredReason()));
        }
        add(block);
    }

    private String formatDuration(int durationMinutes) {
        if (durationMinutes < 0) return "00:00";
        int hours = durationMinutes / 60;
        int minutes = durationMinutes % 60;
        return String.format("%02d:%02d", hours, minutes);
    }

    private String empty(String v) {
        return (v == null || v.isBlank())
                ? "nicht angegeben"
                : v.trim();
    }

    private Span makeAuditLine(String text) {
        Span s = new Span(text);
        s.getStyle().set("font-size", "12px").set("color", "#555");
        return s;
    }

    private Span makeAuditReason(String text) {
        Span s = new Span(text);
        s.getStyle().set("font-size", "12px").set("color", "#777").set("margin-top", "12px");
        return s;
    }

    private String formatDate(LocalDateTime dt) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        return dt.format(dateFormatter);
    }

    private String formatTime(LocalDateTime dt) {
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        return dt.format(timeFormatter);
    }
}