package com.salychevms.familienberatung.ui.view;

import com.salychevms.familienberatung.model.*;
import com.salychevms.familienberatung.service.*;
import com.salychevms.familienberatung.ui.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.VaadinRequest;
import jakarta.annotation.security.PermitAll;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.*;


@Slf4j
@Route(value = "family/:id", layout = MainLayout.class)
@PageTitle("Familie")
@RequiredArgsConstructor
@PermitAll
public class FamilyDetailsView extends VerticalLayout implements BeforeEnterObserver {
    private final AuthService authService;
    private final FamilyService familyService;
    private final DelegationService delegationService;
    private final ConsultationService consultationService;
    private final EmployeeService employeeService;
    private final FamilyMemberService familyMemberService;

    private Employee currentEmployee;
    private int lvl;

    private Long familyId;
    private Family currentFamily;
    private List<FamilyMember> members;
    private List<Consultation> consultations;
    private Delegation activeDelegation;

    private boolean initialized = false;

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Employee e = authService.getCurrentEmployee();
        if (e == null || !e.isActive() || e.isArchived()) {
            event.forwardTo("login");
            return;
        }

        this.currentEmployee = employeeService.findByLogin(e.getLogin());
        this.lvl = currentEmployee.getRole().getAccessLevel();

        Optional<Long> optId = event.getRouteParameters().getLong("id");
        if (optId.isEmpty()) {
            event.forwardTo("families");
            return;
        }

        this.familyId = optId.get();
        this.currentFamily = familyService.getFamilyById(currentEmployee.getLogin(), familyId);

        consultations = new ArrayList<>();
        List<Consultation> consultationList = consultationService.getConsultationsByFamily(currentFamily);
        for (Consultation c : consultationList) {
            if (!c.isInvalid()) this.consultations.add(c);
        }

        List<Delegation> delegationList = delegationService.getDelegations();
        for (Delegation d : delegationList) {
            if (d.getFamily().equals(currentFamily) && (d.getToEmployee().getLogin().equals(currentEmployee.getLogin())
                    || d.getFromEmployee().getLogin().equals(currentEmployee.getLogin())))
                this.activeDelegation = d;
        }

        members = new ArrayList<>();
        members = familyMemberService.getMembers(currentFamily, currentEmployee.getLogin());

        if (!initialized) {
            initialized = true;
            buildUI();
        }
    }

    private void buildUI() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        buildBreadcrumbs();
        buildHeader();
        buildDelegation();
        buildMembers();
        buildContact();
        buildAddress();
        buildAdditionalInfo();
        buildAudit();
    }

    private void buildBreadcrumbs() {
        HorizontalLayout bcrumbs = new HorizontalLayout();
        bcrumbs.setSpacing(true);
        bcrumbs.setPadding(true);
        bcrumbs.setAlignItems(Alignment.CENTER);

        RouterLink l1 = new RouterLink("Übersicht", OverviewView.class);
        RouterLink l2 = new RouterLink("Familien", FamiliesView.class);

        Span sep1 = new Span(" >> ");
        sep1.getStyle().set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)");
        Span sep2 = new Span(" >> ");
        sep2.getStyle().set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)");
        Span l3 = new Span("Familie: " + currentFamily.getFamilyName());
        l3.getStyle().set("font-size", "var(--lumo-font-size-s)")
                .set("font-weight", "bold")
                .set("color", "var(--lumo-body-text-color)");

        bcrumbs.add(l1, sep1, l2, sep2, l3);

        add(bcrumbs);
    }

    private void buildHeader() {
        VerticalLayout box = new VerticalLayout();
        box.setSpacing(false);
        box.setPadding(false);

        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setAlignItems(Alignment.CENTER);
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);

        H2 title = new H2("Familie: " + currentFamily.getFamilyName());
        header.add(title, buildConsultationsButtons());

        String status = currentFamily.getStatus().name();
        String caseState = currentFamily.isCaseClosed() ? "geschlossen" : "öffen";

        String line = "ID: " + currentFamily.getId() +
                " | Zeus ID: " + kein(currentFamily.getZeusId()) +
                " | Status: " + status +
                " | Ablauf: " + caseState;

        Span info = new Span(line);

        box.add(header, info);

        if (activeDelegation != null) {
            buildDelegation();
        }

        add(box);
    }

    private void buildDelegation() {
        if (activeDelegation == null) return;

        VerticalLayout block = new VerticalLayout();
        block.setSpacing(true);
        block.setPadding(false);
        block.getStyle().set("border", "1px solid #ddd").set("background-color", "#fafafa").
                set("border-radius", "6px").set("padding", "10px");

        Span title = new Span("Delegation");
        title.getStyle().set("font-weight", "bold");
        block.add(title);

        Employee from = activeDelegation.getFromEmployee();
        Employee to = activeDelegation.getToEmployee();

        boolean isTarget = to.getId().equals(currentEmployee.getId());

        String line1 = isTarget ? "Delegiert an Sie" : "Delegiert an: " + to.getFirstName() + " " + to.getLastName();
        String line2 = isTarget ? "Von: " + from.getFirstName() + " " + from.getLastName() : "";
        String period = activeDelegation.getStartDate() + " - " + activeDelegation.getEndDate();

        Span s1 = new Span(line1);
        Span s2 = new Span(line2);
        Span s3 = new Span(period);

        s1.getStyle().set("font-size", "13px");
        s2.getStyle().set("font-size", "13px");
        s3.getStyle().set("font-size", "13px");
        block.add(s1);
        if (line2.isEmpty()) block.add(s2);
        block.add(s3);

        Span reason = new Span("Grund: " + activeDelegation.getReason());
        reason.getStyle().set("font-size", "12px").set("font-size", "12px").set("color", "#555");
        block.add(reason);

        add(block);
    }

    private void buildMembers() {
        VerticalLayout block = new VerticalLayout();
        block.setSpacing(false);
        block.setPadding(true);
        block.setWidthFull();
        block.getStyle().set("border", "1px solid #ddd").set("border-radius", "6px").set("padding", "10px");

        Span title = new Span("Familienmitglieder");
        title.getStyle().set("font-weight", "bold");
        block.add(title);

        if (members == null || members.isEmpty()) {
            Span none = new Span("Noch keine Mitglieder...");
            none.getStyle().set("color", "#666");
            block.add(none);
            add(block);
            return;
        }

        for (FamilyMember m : members) {
            Div row = new Div();
            row.setWidthFull();
            row.getStyle().set("padding", "6px 10px").set("border-bottom", "1px solid #e0e0e0").set("cursor", "pointer");
            row.getElement().addEventListener("mouseenter", e ->
                    row.getStyle().set("background-color", "#f5f5f5"));
            row.getElement().addEventListener("mouseleave", e ->
                    row.getStyle().remove("background-color"));

            int age = (m.getBirthDate() != null) ? Period.between(m.getBirthDate(), LocalDate.now()).getYears() : -1;

            String gender = "kA";
            if (m.getGender().equals(FamilyMemberGender.DIVERS)) {
                gender = "divers";
            } else if (m.getGender().equals(FamilyMemberGender.MAENNLICH)) {
                gender = "männlich";
            } else if (m.getGender().equals(FamilyMemberGender.WEIBLICH)) {
                gender = "weiblich";
            }

            String text = "ID: " + m.getId() + "  |  " + m.getFirstName() + " " + m.getLastName() + "  |  "
                    + gender + "  |  " + "Alter: " + age + "  |  " +
                    "Lebt mit der Familie: " + (m.isLivesWithFamily() ? "Ja" : "Nein");

            Span s = new Span(text);
            row.add(s);

            row.addClickListener(ev ->
            {
                getUI().ifPresent(ui ->
                        ui.navigate(MemberDetailsView.class,
                                new RouteParameters(Map.of("id", String.valueOf(m.getId()),
                                        "familyId", String.valueOf(currentFamily.getId())))));
            });
            block.add(row);
        }
        add(block);
    }

    private void buildContact() {
        VerticalLayout block = new VerticalLayout();
        block.setSpacing(false);
        block.setPadding(true);
        block.setWidthFull();
        block.getStyle().set("padding", "10px").set("border-radius", "6px").set("border", "1px solid #ddd");

        Span title = new Span("Kontaktdaten");
        title.getStyle().set("font-weight", "bold");
        block.add(title);

        String phone = (currentFamily.getPhone() == null ||
                currentFamily.getPhone().isBlank() ? "nicht angegeben" : currentFamily.getPhone());

        String email = (currentFamily.getEmail() == null ||
                currentFamily.getEmail().isBlank() ? "nicht angegeben" : currentFamily.getEmail());

        Span phoneRow = new Span("Telefon: " + phone);
        Span emailRow = new Span("E-Mail: " + email);

        block.add(phoneRow, emailRow);
        add(block);
    }

    private void buildAddress() {
        VerticalLayout block = new VerticalLayout();
        block.setSpacing(false);
        block.setPadding(true);
        block.setWidthFull();
        block.getStyle().set("border", "1px solid #ddd").set("border-radius", "6px").set("padding", "10px");
        Span title = new Span("Adresse");
        title.getStyle().set("font-weight", "bold");
        block.add(title);

        String street = currentFamily.getStreet();
        String houseNumber = currentFamily.getHouseNumber();
        String zip = currentFamily.getZip();
        String city = currentFamily.getCity();

        String line1;
        if (street == null || street.isBlank() && (houseNumber == null || houseNumber.isBlank())) {
            line1 = "nicht angegeben";
        } else {
            String s = street.isBlank() ? "" : street;
            String h = (houseNumber == null || houseNumber.isBlank()) ? "" : " " + houseNumber;
            line1 = s + h;
        }

        String line2;
        if ((zip == null || zip.isBlank()) && (city == null || city.isBlank())) {
            line2 = ", nicht angegeben";
        } else {
            String z = (zip == null || zip.isBlank()) ? "" : ", " + zip;
            String c = (city == null || city.isBlank()) ? "" : " " + city;
            line2 = z + c;
        }

        block.add(new Span(line1 + line2));
        add(block);
    }

    private void buildAdditionalInfo() {
        VerticalLayout block = new VerticalLayout();
        block.setSpacing(false);
        block.setPadding(true);
        block.setWidthFull();
        block.getStyle().set("border", "1px solid #ddd").set("border-radius", "6px").set("padding", "10px");

        Span title = new Span("Weitere Angaben");
        title.getStyle().set("font-weight", "bold");
        block.add(title);

        VerticalLayout personal = new VerticalLayout();
        personal.setSpacing(false);
        personal.setPadding(true);
        personal.setWidthFull();
        personal.getStyle().set("border", "1px solid #ddd").set("border-radius", "6px").set("padding", "10px");

        Span p1 = new Span("Staatsangehörigkeit");
        p1.getStyle().set("font-weight", "bold");
        Span p1v = new Span(empty(currentFamily.getCitizenship()));
        personal.add(p1, p1v);

        Span p2 = new Span("Sprachen");
        p2.getStyle().set("font-weight", "bold");
        Span p2v = new Span(empty(currentFamily.getLanguages()));
        personal.add(p2, p2v);
        block.add(personal);

        VerticalLayout reasonBlock = new VerticalLayout();
        reasonBlock.setSpacing(false);
        reasonBlock.setPadding(true);
        reasonBlock.setWidthFull();
        reasonBlock.getStyle().set("border", "1px solid #ddd").set("border-radius", "6px").set("padding", "10px");

        Span r1 = new Span("Grund der Beratung");
        r1.getStyle().set("font-weight", "bold");
        TextArea reasonArea = new TextArea();
        reasonArea.setWidthFull();
        reasonArea.setReadOnly(true);
        reasonArea.setValue(empty(currentFamily.getReasonDescription()));
        reasonArea.setMaxLength(4000);
        reasonArea.setHeight("200px");
        reasonArea.getStyle().set("white-space", "pre-wrap");

        reasonBlock.add(r1, reasonArea);
        block.add(reasonBlock);

        VerticalLayout noteBlock = new VerticalLayout();
        noteBlock.setSpacing(false);
        noteBlock.setPadding(true);
        noteBlock.getStyle().set("border", "1px solid #ddd").set("border-radius", "6px").set("padding", "10px");

        Span n1 = new Span("Interne Notizen");
        n1.getStyle().set("font-weight", "bold");
        TextArea noteArea = new TextArea();
        noteArea.setWidthFull();
        noteArea.setReadOnly(true);
        noteArea.setValue(empty(currentFamily.getNotes()));
        noteArea.setMaxLength(255);
        noteArea.setHeight("200px");
        noteArea.getStyle().set("white-space", "pre-wrap");

        noteBlock.add(n1, noteArea);
        block.add(noteBlock);

        add(block);
    }

    private void buildAudit() {
        VerticalLayout block = new VerticalLayout();
        block.setSpacing(false);
        block.setPadding(true);
        block.setWidthFull();
        block.getStyle().set("border", "1px solid #ddd").set("border-radius", "6px").set("padding", "10px");

        Span title = new Span("Verlauf");
        title.getStyle().set("font-weight", "bold");
        block.add(title);

        Family family = currentFamily;

        if (family.getCreatedAt() != null) block.add(makeAuditLine("Erstellt am " + zDate(family.getCreatedAt()) +
                " um " + zTime(family.getCreatedAt()) + " von " + empty(family.getCreatedBy())));

        if (family.getUpdatedAt() != null) block.add(makeAuditLine("Geändert am " + zDate(family.getUpdatedAt()) +
                " um " + zTime(family.getUpdatedAt()) + " von " + empty(family.getUpdatedBy())));

        if (family.getInvalidAt() != null && family.getStatus().equals(RecordStatus.INVALID))
            block.add(makeAuditLine("Gelöscht am " + zDate(family.getInvalidAt()) +
                    " um " + zTime(family.getInvalidAt()) + " von " + empty(family.getInvalidBy())));

        if (family.getRestoredAt() != null && family.getStatus().equals(RecordStatus.ACTIVE)) {
            block.add(makeAuditLine("Wiederherstellt am " + zDate(family.getRestoredAt()) +
                    " um " + zTime(family.getRestoredAt()) + " von " + empty(family.getRestoredBy())));
            block.add(makeAuditReason("Grund: " + empty(family.getRestoredReason())));
        }

        if (family.getBlockedAt() != null && family.getStatus().equals(RecordStatus.BLOCKED)) {
            block.add(makeAuditLine("Blockiert am " + zDate(family.getBlockedAt()) +
                    " um " + zTime(family.getBlockedAt()) + " von " + empty(family.getBlockedBy())));
            block.add(makeAuditReason("Grund: " + empty(family.getBlockedReason())));
        }

        if (family.getArchivedAt() != null && family.getStatus().equals(RecordStatus.ARCHIVED))
            block.add(makeAuditLine("Archiviert am " + zDate(family.getArchivedAt()) +
                    " um " + zTime(family.getArchivedAt()) + " von " + empty(family.getArchivedBy())));

        add(block);
    }

    private HorizontalLayout buildConsultationsButtons() {
        HorizontalLayout bar = new HorizontalLayout();
        bar.setWidthFull();
        bar.setJustifyContentMode(JustifyContentMode.END);
        bar.setAlignItems(Alignment.CENTER);
        bar.setSpacing(true);

        Button newBtn = new Button();
        if (lvl != 10 || currentFamily.getStatus().equals(RecordStatus.ACTIVE))
            newBtn = new Button("Neue Beratung", e -> buildCreateConsultationDialog());
        Button viewBtn = new Button("Beratungen", e -> getUI().ifPresent(ui -> ui.navigate(
                ConsultationsView.class, new RouteParameters("familyId", currentFamily.getId().toString()))));
        bar.add(viewBtn, newBtn);

        return bar;
    }

    private void buildCreateConsultationDialog() {
        Dialog dialog = new Dialog();
        dialog.setModal(true);
        dialog.setResizable(false);
        dialog.setDraggable(false);
        dialog.setCloseOnOutsideClick(false);

        DateTimePicker dateTime = new DateTimePicker("Datum und Uhrzeit (*)");
        dateTime.setWidthFull();
        dateTime.setRequiredIndicatorVisible(true);

        dateTime.setValue(roundToNextQuarterHour(LocalDateTime.now()));

        IntegerField duration = new IntegerField("Dauer (Minuten, 1-480) (*)");
        duration.setRequiredIndicatorVisible(true);
        duration.setWidthFull();
        duration.setStep(15);
        duration.setMin(15);
        duration.setMax(480);
        duration.setValue(60);

        TextField topic = new TextField("Thema");
        topic.setWidthFull();

        TextArea description = new TextArea("Beschreibung");
        description.setWidthFull();
        description.setHeight("130px");

        TextArea result = new TextArea("Ergebnis");
        result.setWidthFull();
        result.setHeight("130px");

        DateTimePicker followUp = new DateTimePicker("Folgetermin");
        followUp.setWidthFull();

        Span mainTitle = new Span("Allgemeine Anganem");
        mainTitle.getStyle().set("font-weight", "bold");

        VerticalLayout mainBlock = new VerticalLayout();
        mainBlock.setSpacing(false);
        mainBlock.setPadding(true);
        mainBlock.getStyle().set("border", "1px solid #ddd").set("padding", "10px").set("border-radius", "6px");

        mainBlock.add(mainTitle, dateTime, duration);

        Span detailsTitle = new Span("Details yur Beratung");
        detailsTitle.getStyle().set("font-weight", "bold");

        VerticalLayout detailsBlock = new VerticalLayout();
        detailsBlock.setSpacing(false);
        detailsBlock.setPadding(true);
        detailsBlock.getStyle().set("border", "1px solid #ddd").set("padding", "10px").set("border-radius", "6px");

        detailsBlock.add(detailsTitle, topic, description, result, followUp);

        VerticalLayout content = new VerticalLayout();
        content.setWidthFull();
        content.add(mainBlock, detailsBlock);

        dialog.add(content);

        Button save = new Button("Speichern");
        Button cancel = new Button("Abbrechen");

        cancel.addClickListener(e -> handleConsultationCancel(dialog, topic.getValue(),
                description.getValue(), result.getValue(), followUp.getValue()));

        save.addClickListener(e -> handleConsultationSave(dialog, dateTime, duration, topic,
                description, result, followUp));

        HorizontalLayout buttons = new HorizontalLayout(cancel, save);
        buttons.setWidthFull();
        buttons.setJustifyContentMode(JustifyContentMode.END);

        dialog.getFooter().add(buttons);
        dialog.open();
    }

    private void handleConsultationCancel(Dialog dialog, String topic, String desc,
                                          String result, LocalDateTime followUp) {

        if ((topic == null || topic.isBlank()) && (desc == null || desc.isBlank()) &&
                (result == null || result.isBlank()) && (followUp == null)) {
            dialog.close();
            return;
        }
        showConfirmDialog("Änderungen verwerfen", "Ihre Eingaben gehen verloren. Fortfahren?",
                dialog::close, () -> {
                });
    }

    private void handleConsultationSave(Dialog dialog, DateTimePicker dateTimeField, IntegerField durationField,
                                        TextField topicField, TextArea descriptionField, TextArea resultField,
                                        DateTimePicker followUpField) {
        LocalDateTime dateTime = dateTimeField.getValue();
        Integer duration = durationField.getValue();
        String topic = topicField.getValue();
        String description = descriptionField.getValue();
        String result = resultField.getValue();
        LocalDateTime followUp = followUpField.getValue();

        List<String> errors = validateConsultationForm(dateTime, duration);
        if (!errors.isEmpty()) {
            showOkDialog("Fehler", String.join("\n", errors));
            return;
        }

        String state = validateConsultationDate(dateTime, currentFamily.getCreatedAt());

        if ("FUTURE".equals(state)) {
            showOkDialog("Ungültiges Datum", "Datum darf nicht in der Zukunft liegen");
            return;
        }

        if ("BEFORE_FAMILY".equals(state)) {
            showOkDialog("Ungültiges Datum", "Datum liegt vor Erstellung der Familie");
            return;
        }

        if ("BACKDATED".equals(state)) {
            askBackdatedConfirmation(() -> handleOptionalFinalSave(dialog, dateTime, duration, topic, description,
                    result, followUp));
            return;
        }

        handleOptionalFinalSave(dialog, dateTime, duration, topic, description, result, followUp);
    }

    private void handleOptionalFinalSave(Dialog dialog, LocalDateTime dateTime, Integer duration, String topic,
                                         String description, String result, LocalDateTime followUp) {
        List<String> empty = new ArrayList<>();
        if (topic == null || topic.isBlank()) empty.add("Thema");
        if (description == null || description.isBlank()) empty.add("Beschreibung");
        if (result == null || result.isBlank()) empty.add("Ergebnis");
        if (followUp == null) empty.add("Folgetermin");

        if (!empty.isEmpty()) {
            askOptionalFieldsMissing(empty, () -> {
                finalSave(dialog, dateTime, duration, topic, description, result, followUp);
            });
            return;
        }

        finalSave(dialog, dateTime, duration, topic, description, result, followUp);
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

    private String zDate(LocalDateTime dt) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        return dt.format(dateFormatter);
    }

    private String zTime(LocalDateTime dt) {
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        return dt.format(timeFormatter);
    }

    private String kein(String v) {
        return v == null ? "kein" : v;
    }

    private String empty(String v) {
        return (v == null || v.isBlank()) ? "nicht angegeben" : v.trim();
    }

    private LocalDateTime roundToNextQuarterHour(LocalDateTime dt) {
        int minute = dt.getMinute();
        int mod = minute % 15;
        if (mod == 0) return dt.withSecond(0).withNano(0);

        int add = 15 - mod;
        dt = dt.plusMinutes(add);
        return dt.withSecond(0).withNano(0);
    }

    private boolean changed(Object original, Object current) {
        if (original == null && current == null) return false;
        if (original == null) return true;
        return !original.equals(current);
    }

    private void showOkDialog(String title, String message) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(title);

        Span msg = new Span(message);
        dialog.add(msg);

        Button ok = new Button("OK", e -> dialog.close());

        HorizontalLayout btns = new HorizontalLayout(ok);
        btns.setWidthFull();
        btns.setJustifyContentMode(JustifyContentMode.END);

        dialog.getFooter().add(btns);
        dialog.open();
    }

    private void showConfirmDialog(String title, String message, Runnable yesAction, Runnable noAction) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(title);

        Span msg = new Span(message);
        dialog.add(msg);

        Button yes = new Button("Ja", e -> {
            dialog.close();
            if (yesAction != null) yesAction.run();
        });

        Button no = new Button("Nein", e -> {
            dialog.close();
            if (noAction != null) noAction.run();
        });

        HorizontalLayout btns = new HorizontalLayout(yes, no);
        btns.setWidthFull();
        btns.setJustifyContentMode(JustifyContentMode.END);

        dialog.getFooter().add(btns);
        dialog.open();
    }

    private List<String> validateConsultationForm(LocalDateTime dateTime, Integer durationMinutes) {
        List<String> errors = new ArrayList<>();

        if (dateTime == null) errors.add("Datum und Uhryeit müssen angegeben werden.");
        if (durationMinutes == null || durationMinutes <= 0) errors.add("Dauer muss größer als 0 sein");
        if (durationMinutes != null && durationMinutes > 480)
            errors.add("Dauer muss nicht mehr als 480 Min bzw. 8 St. sein");

        return errors;
    }

    private String validateConsultationDate(LocalDateTime dateTime, LocalDateTime familyCreatedAt) {
        LocalDate today = LocalDate.now();

        if (dateTime.toLocalDate().isAfter(today)) return "FUTURE";
        if (dateTime.isBefore(familyCreatedAt)) return "BEFORE_FAMILY";
        if (dateTime.toLocalDate().isBefore(today)) return "BACKDATED";

        return "OK";
    }

    private void askBackdatedConfirmation(Runnable onConfirm) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Vergangene Beratung");

        Span msg = new Span("Sie erstellen eine Beratung in der Vergangenheit. Möchten Sie fortfahren?");
        dialog.add(msg);
        Button yes = new Button("Ja", e -> {
            dialog.close();
            onConfirm.run();
        });

        Button no = new Button("Nein", e -> dialog.close());

        HorizontalLayout btns = new HorizontalLayout(yes, no);
        btns.setJustifyContentMode(JustifyContentMode.END);
        btns.setWidthFull();

        dialog.getFooter().add(btns);
        dialog.open();
    }

    private void askOptionalFieldsMissing(List<String> emptyFields, Runnable onConfirm) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Leere Felder");

        String text = "Folgende Felder sind leer: " + String.join(", ", emptyFields) + ". Trotzdem speichern?";

        Span msg = new Span(text);
        dialog.add(msg);

        Button yes = new Button("Ja", e -> {
            dialog.close();
            onConfirm.run();
        });

        Button no = new Button("Nein", e -> dialog.close());

        HorizontalLayout btns = new HorizontalLayout(yes, no);
        btns.setJustifyContentMode(JustifyContentMode.END);
        btns.setWidthFull();

        dialog.getFooter().add(btns);
        dialog.open();
    }

    private void finalSave(Dialog parent, LocalDateTime dateTime, int duration, String topic, String description,
                           String result, LocalDateTime followUp) {
        try {
            VaadinRequest req = VaadinRequest.getCurrent();
            String ip = req != null ? req.getRemoteAddr() : "UNKNOWN";
            String browser = req != null ? req.getHeader("User-Agent") : "UNKNOWN";

            consultationService.createConsultation(currentFamily, currentEmployee, dateTime, duration, topic,
                    description, result, followUp, ip, browser);

            parent.close();

            showSuccessConsultationDialog();

            getUI().ifPresent(ui -> ui.getPage().reload());
        } catch (Exception ex) {
            showErrorConsultationDialog(ex.getMessage());
        }
    }

    private void showSuccessConsultationDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Gespeichert");

        dialog.add(new Span("Beratung wurde erstellt"));

        Button ok = new Button("OK", e -> dialog.close());
        dialog.getFooter().add(ok);
        dialog.open();
    }

    private void showErrorConsultationDialog(String message) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Fehler");

        dialog.add(new Span(message));

        Button ok = new Button("OK", e -> dialog.close());
        dialog.getFooter().add(ok);
        dialog.open();
    }
}