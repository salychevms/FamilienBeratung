package com.salychevms.familienberatung.ui.view;

import com.salychevms.familienberatung.model.*;
import com.salychevms.familienberatung.service.*;
import com.salychevms.familienberatung.ui.dialog.EditDialogFactory;
import com.salychevms.familienberatung.ui.dialog.EditField;
import com.salychevms.familienberatung.ui.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
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

        HorizontalLayout familyHeader = new HorizontalLayout();
        familyHeader.setWidthFull();
        familyHeader.setAlignItems(Alignment.CENTER);
        familyHeader.setJustifyContentMode(JustifyContentMode.BETWEEN);

        HorizontalLayout h2Title = new HorizontalLayout();
        h2Title.setAlignItems(FlexComponent.Alignment.CENTER);
        h2Title.setSpacing(true);

        if (lvl != 10 && currentFamily.getStatus().equals(RecordStatus.ACTIVE) && !currentFamily.isCaseClosed()) {
            Button h2EditBtn = new Button(VaadinIcon.EDIT.create());
            h2EditBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            h2EditBtn.setClassName("edit-btn");

            h2EditBtn.addClickListener(e -> {
                EditDialogFactory.openEditDialog("Familienname ändern", List.of(new EditField(
                        "familyName", "Familienname", EditField.Type.TEXT, currentFamily.getFamilyName(),
                        true, 255, null)), values -> {
                    String newName = (String) values.get("familyName");
                    if (!EditDialogFactory.isValidText(newName, 255)) {
                        showOkDialog("Fehler", "Familienname ist leer oder zu lang");
                        return;
                    }

                    showConfirmDialog("Speichern", "Änderunge speichern?", () -> {
                        Family f = new Family(currentFamily);
                        f.setFamilyName(newName);

                        VaadinRequest req = VaadinRequest.getCurrent();
                        familyService.updateFamily(f, currentEmployee.getLogin(), req != null ? req.getRemoteAddr() : "UNKNOWN",
                                req != null ? req.getHeader("User-Agent") : "UNKNOWN");
                        getUI().ifPresent(ui -> ui.getPage().reload());
                    }, () -> {
                    });
                });
            });
            h2Title.add(h2EditBtn);
        }

        H2 title = new H2("Familie: " + currentFamily.getFamilyName());
        h2Title.add(title);

        familyHeader.add(h2Title);

        String status = "";
        if (currentFamily.getStatus().equals(RecordStatus.ACTIVE)) status = "AKTIV";
        else if (currentFamily.getStatus().equals(RecordStatus.BLOCKED)) status = "BLOCKIERT";
        else if (currentFamily.getStatus().equals(RecordStatus.ARCHIVED)) status = "ARCHIV";
        else if (currentFamily.getStatus().equals(RecordStatus.INVALID)) status = "GELÖSCHT";

        String caseState = currentFamily.isCaseClosed() ? "geschlossen" : "öffen";

        String line = "ID: " + currentFamily.getId() +
                " | Zeus ID: " + kein(currentFamily.getZeusId()) +
                " | Status: " + status +
                " | Ablauf: " + caseState;

        Span info = new Span(line);
        VerticalLayout content = new VerticalLayout();
        content.setSpacing(false);
        content.setPadding(true);
        content.setWidthFull();

        String emp = currentFamily.getAssignedEmployee().getFirstName() +
                " " + currentFamily.getAssignedEmployee().getLastName();
        Span employee = new Span("Berater*in: " + emp);

        String cnt = "Beratungen: " + getConsultationsCount();
        Span consCount = new Span(cnt);

        List<Consultation> consultations = consultationService.getConsultationsByFamily(currentFamily);
        int mnts = 0;
        for (Consultation c : consultations) {
            mnts += c.getDurationMinutes();
        }
        Span hours = new Span("Beratungsstundenanzahl: " + (mnts / 60) + ":" + String.format("%02d", mnts % 60) + " St.");
        content.add(familyHeader, info, employee, consCount, hours);

        box.add(content, buildButtonsBlock(), buildMembers());

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

    private VerticalLayout buildMembers() {
        VerticalLayout block = new VerticalLayout();
        block.setSpacing(false);
        block.setPadding(true);
        block.setWidthFull();
        block.getStyle().set("border", "1px solid #ddd").set("border-radius", "6px").set("padding", "10px");

        HorizontalLayout header = new HorizontalLayout();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setSpacing(true);

        if (lvl != 10 && currentFamily.getStatus().equals(RecordStatus.ACTIVE) && !currentFamily.isCaseClosed()) {
            Button member = new Button(VaadinIcon.PLUS.create(), e -> buildCreateMemberDialog());
            member.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            member.addClassName("plus-btn");
            header.add(member);
        }

        Span title = new Span("Familienmitglieder");
        title.getStyle().set("font-weight", "bold");
        header.add(title);
        block.add(header);

        if (members == null || members.isEmpty()) {
            Span none = new Span("Noch keine Mitglieder...");
            none.getStyle().set("color", "#666");
            block.add(none);
        } else {
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
        }
        return block;
    }

    private void buildContact() {
        VerticalLayout block = new VerticalLayout();
        block.setSpacing(false);
        block.setPadding(true);
        block.setWidthFull();
        block.getStyle().set("padding", "10px").set("border-radius", "6px").set("border", "1px solid #ddd");

        HorizontalLayout titleLayout = new HorizontalLayout();
        titleLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        titleLayout.setSpacing(true);
        if (lvl != 10 && currentFamily.getStatus().equals(RecordStatus.ACTIVE) && !currentFamily.isCaseClosed()) {
            Button contactEditBtn = new Button(VaadinIcon.EDIT.create());
            contactEditBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            contactEditBtn.addClassName("edit-btn");
            contactEditBtn.addClickListener(ev -> EditDialogFactory.openEditDialog(
                    "Kontakt ändern", List.of(new EditField("phone", "Telefon", EditField.Type.TEXT,
                            currentFamily.getPhone(), false, 255, null), new EditField(
                            "email", "E-Mail", EditField.Type.TEXT, currentFamily.getEmail(),
                            false, 255, null
                    )), values -> {
                        String phone = (String) values.get("phone");
                        String email = (String) values.get("email");

                        if (!EditDialogFactory.isValidText(phone, 255)) {
                            showOkDialog("Fehler", "Telefon ist ungültigt");
                            return;
                        }
                        if (!EditDialogFactory.isValidText(email, 255)) {
                            showOkDialog("Fehler", "E-Mail ist ungültigt, zu lang oder falsch strukturiert");
                            return;
                        }

                        showConfirmDialog("Speichern", "Änderungen speichern?", () -> {
                            Family f = new Family(currentFamily);
                            f.setPhone((String) values.get("phone"));
                            f.setEmail((String) values.get("email"));

                            VaadinRequest req = VaadinRequest.getCurrent();
                            familyService.updateFamily(f, currentEmployee.getLogin(), req != null ? req.getRemoteAddr() : "UNKNOWN",
                                    req != null ? req.getHeader("User-Agent") : "UNKNOWN");
                            getUI().ifPresent(ui -> ui.getPage().reload());
                        }, () -> {
                        });
                    }
            ));
            titleLayout.add(contactEditBtn);
        }

        Span title = new Span("Kontaktdaten");
        title.getStyle().set("font-weight", "bold");
        titleLayout.add(title);
        block.add(titleLayout);

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

        HorizontalLayout titleLayout = new HorizontalLayout();
        titleLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        titleLayout.setSpacing(true);

        if (lvl != 10 && currentFamily.getStatus().equals(RecordStatus.ACTIVE) && !currentFamily.isCaseClosed()) {
            Button addressEditBtn = new Button(VaadinIcon.EDIT.create());
            addressEditBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            addressEditBtn.setClassName("edit-btn");
            addressEditBtn.addClickListener(ev -> EditDialogFactory.openEditDialog("Adresse ändern",
                    List.of(new EditField("street", "Straße", EditField.Type.TEXT, currentFamily.getStreet(),
                                    false, 255, null),
                            new EditField("houseNumber", "Hausnummer", EditField.Type.TEXT,
                                    currentFamily.getHouseNumber(), false, 255, null),
                            new EditField("zip", "PLZ", EditField.Type.TEXT, currentFamily.getZip(),
                                    false, 255, null),
                            new EditField("city", "Stadt", EditField.Type.TEXT, currentFamily.getCity(),
                                    false, 255, null)
                    ), values -> {
                        String street = (String) values.get("street");
                        String houseNumber = (String) values.get("houseNumber");
                        String zip = (String) values.get("zip");
                        String city = (String) values.get("city");
                        if (!EditDialogFactory.isValidText(street, 255) ||
                                !EditDialogFactory.isValidText(houseNumber, 255) ||
                                !EditDialogFactory.isValidText(zip, 255) ||
                                !EditDialogFactory.isValidText(city, 255)) {
                            showOkDialog("Fehler", "Adresse ist ungültigt");
                            return;
                        }
                        showConfirmDialog("Speichern", "Änderungen speichern?", () -> {
                            Family f = new Family(currentFamily);
                            f.setStreet(street);
                            f.setHouseNumber(houseNumber);
                            f.setZip(zip);
                            f.setCity(city);

                            VaadinRequest req = VaadinRequest.getCurrent();
                            familyService.updateFamily(f, currentEmployee.getLogin(),
                                    req != null ? req.getRemoteAddr() : "UNKNOWN",
                                    req != null ? req.getHeader("User-Agent") : "UNKNOWN");
                            getUI().ifPresent(ui -> ui.getPage().reload());
                        }, () -> {
                        });
                    }));
            titleLayout.add(addressEditBtn);
        }
        Span title = new Span("Adresse");
        title.getStyle().set("font-weight", "bold");
        titleLayout.add(title);
        block.add(titleLayout);

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
        VerticalLayout personal = new VerticalLayout();
        personal.setSpacing(false);
        personal.setPadding(true);
        personal.setWidthFull();
        personal.getStyle().set("border", "1px solid #ddd").set("border-radius", "6px").set("padding", "10px");

        HorizontalLayout additionalLayout = new HorizontalLayout();
        additionalLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        additionalLayout.setSpacing(true);
        if (lvl != 10 && currentFamily.getStatus().equals(RecordStatus.ACTIVE) && !currentFamily.isCaseClosed()) {
            Button additionalEditBtn = new Button(VaadinIcon.EDIT.create());
            additionalEditBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            additionalEditBtn.addClassName("edit-btn");
            additionalEditBtn.addClickListener(e -> EditDialogFactory.openEditDialog(
                    "Weitere Angaben ändern", List.of(
                            new EditField("citizenship", "Staatsangehörigkeit", EditField.Type.TEXT,
                                    currentFamily.getCitizenship(), false, 255, null),
                            new EditField("languages", "Sprachen", EditField.Type.TEXT,
                                    currentFamily.getLanguages(), false, 255, null)),
                    values -> {
                        String citizenship = (String) values.get("citizenship");
                        String languages = (String) values.get("languages");

                        if (!EditDialogFactory.isValidText(citizenship, 255)) {
                            showOkDialog("Fehler", "Staatsangehörigkeit ist ungültigt");
                            return;
                        }
                        if (!EditDialogFactory.isValidText(languages, 255)) {
                            showOkDialog("Fehler", "Sprachen sind ungültigt");
                            return;
                        }
                        showConfirmDialog("Speichern", "Änderungen speichern?", () -> {
                            Family f = new Family(currentFamily);
                            f.setCitizenship(citizenship);
                            f.setLanguages(languages);
                            VaadinRequest req = VaadinRequest.getCurrent();
                            familyService.updateFamily(f, currentEmployee.getLogin(),
                                    req != null ? req.getRemoteAddr() : "UNKNOWN",
                                    req != null ? req.getHeader("User-Agent") : "UNKNOWN");
                            getUI().ifPresent(ui -> ui.getPage().reload());
                        }, () -> {
                        });
                    }
            ));
            additionalLayout.add(additionalEditBtn);
        }

        Span title = new Span("Weitere Angaben");
        title.getStyle().set("font-weight", "bold");
        additionalLayout.add(title);

        Span citizenship = new Span("Staatsangehörigkeit: " + empty(currentFamily.getCitizenship()));
        Span languages = new Span("Sprachen: " + empty(currentFamily.getLanguages()));
        personal.add(additionalLayout, citizenship, languages);

        VerticalLayout reasonBlock = new VerticalLayout();
        reasonBlock.setSpacing(false);
        reasonBlock.setPadding(false);
        reasonBlock.setWidthFull();
        reasonBlock.getStyle().set("border", "1px solid #ddd").set("border-radius", "6px")
                .set("padding", "10px").set("padding-bottom", "0");

        HorizontalLayout reasonLayout = new HorizontalLayout();
        reasonLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        reasonLayout.setSpacing(true);

        if (lvl != 10 && currentFamily.getStatus().equals(RecordStatus.ACTIVE) && !currentFamily.isCaseClosed()) {
            Button additionalEditBtn = new Button(VaadinIcon.EDIT.create());
            additionalEditBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            additionalEditBtn.addClassName("edit-btn");
            additionalEditBtn.addClickListener(e -> EditDialogFactory.
                    openEditDialog("Grund der Beratung ändern", List.of(
                            new EditField("reason", "Grund der Beratung", EditField.Type.TEXTAREA,
                                    currentFamily.getReasonDescription(), false, 4000, null)
                    ), values -> {
                        String reason = (String) values.get("reason");
                        if (!EditDialogFactory.isValidText(reason, 4000)) {
                            showOkDialog("Fehler", "Grund ist ungültigt oder zu lang");
                            return;
                        }
                        showConfirmDialog("Speichern", "Änderungen speichern?", () -> {
                            Family f = new Family(currentFamily);
                            f.setReasonDescription(reason);
                            VaadinRequest req = VaadinRequest.getCurrent();
                            familyService.updateFamily(f, currentEmployee.getLogin(),
                                    req != null ? req.getRemoteAddr() : "UNKNOWN",
                                    req != null ? req.getHeader("User-Agent") : "UNKNOWN");
                            getUI().ifPresent(ui -> ui.getPage().reload());
                        }, () -> {
                        });
                    }));
            reasonLayout.add(additionalEditBtn);
        }
        Span r1 = new Span("Grund der Beratung");
        r1.getStyle().set("font-weight", "bold");
        reasonLayout.add(r1);

        TextArea reasonArea = new TextArea();
        reasonArea.setWidthFull();
        reasonArea.setReadOnly(true);
        reasonArea.setValue(empty(currentFamily.getReasonDescription()));
        reasonArea.setMaxLength(4000);
        reasonArea.setHeight("200px");
        reasonArea.getStyle().set("white-space", "pre-wrap");

        reasonBlock.add(reasonLayout, reasonArea);

        VerticalLayout noteBlock = new VerticalLayout();
        noteBlock.setSpacing(false);
        noteBlock.setPadding(false);
        noteBlock.getStyle().set("border", "1px solid #ddd").set("border-radius", "6px")
                .set("padding", "10px").set("padding-bottom", "0");

        HorizontalLayout noteLayout = new HorizontalLayout();
        noteLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        noteLayout.setSpacing(true);
        if (lvl != 10 && currentFamily.getStatus().equals(RecordStatus.ACTIVE) && !currentFamily.isCaseClosed()) {
            Button additionalEditBtn = new Button(VaadinIcon.EDIT.create());
            additionalEditBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            additionalEditBtn.addClassName("edit-btn");
            additionalEditBtn.addClickListener(e -> EditDialogFactory.
                    openEditDialog("Notizen ändern", List.of(new EditField("notes", "Notizen",
                                    EditField.Type.TEXTAREA, currentFamily.getNotes(), false, 255, null)),
                            values -> {
                                String notes = (String) values.get("notes");
                                if (!EditDialogFactory.isValidText(notes, 255)) {
                                    showOkDialog("Fehler", "Notizen sind ungültigt oder zu lang");
                                    return;
                                }
                                showConfirmDialog("Speichern", "Änderungen speichern?", () -> {
                                    Family f = new Family(currentFamily);
                                    f.setNotes(notes);
                                    VaadinRequest req = VaadinRequest.getCurrent();
                                    familyService.updateFamily(f, currentEmployee.getLogin(),
                                            req != null ? req.getRemoteAddr() : "UNKNOWN",
                                            req != null ? req.getHeader("User-Agent") : "UNKNOWN");
                                    getUI().ifPresent(ui -> ui.getPage().reload());
                                }, () -> {
                                });
                            }));
            noteLayout.add(additionalEditBtn);
        }
        Span n1 = new Span("Interne Notizen");
        n1.getStyle().set("font-weight", "bold");
        noteLayout.add(n1);

        TextArea noteArea = new TextArea();
        noteArea.setWidthFull();
        noteArea.setReadOnly(true);
        noteArea.setValue(empty(currentFamily.getNotes()));
        noteArea.setMaxLength(255);
        noteArea.setHeight("200px");
        noteArea.getStyle().set("white-space", "pre-wrap");

        noteBlock.add(noteLayout, noteArea);

        add(personal, reasonBlock, noteBlock);
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

    private HorizontalLayout buildButtonsBlock() {
        HorizontalLayout bar = new HorizontalLayout();
        bar.setWidthFull();
        bar.setAlignItems(Alignment.CENTER);
        bar.setSpacing(true);

        Button viewBtn = new Button("Beratungen", e -> getUI().ifPresent(ui -> ui.navigate(
                ConsultationsView.class, new RouteParameters("familyId", currentFamily.getId().toString()))));
        bar.add(viewBtn);

        if (lvl != 10 && currentFamily.getStatus().equals(RecordStatus.ACTIVE) && !currentFamily.isCaseClosed()) {
            Button newBtn = new Button("Beratung", VaadinIcon.PLUS.create(),
                    e -> buildCreateConsultationDialog());
            bar.add(newBtn);
        }

        if ((lvl == 80 || lvl == 100) && currentFamily.getStatus().equals(RecordStatus.ACTIVE) && !currentFamily.isCaseClosed()) {
            String zeusId = currentFamily.getZeusId();
            Button zeusIdBtn;
            String zsId = empty(currentFamily.getZeusId());
            if (zeusId == null || zeusId.isEmpty()) {
                zeusIdBtn = new Button("Zeus ID", VaadinIcon.PLUS.create());
                zeusIdBtn.getStyle().set("color", "red");
            } else {
                zeusIdBtn = new Button("Zeus ID", VaadinIcon.EDIT.create());
            }
            zeusIdBtn.addClickListener(e -> EditDialogFactory.openEditDialog(
                    "Zeus ID ändern", List.of(new EditField("zeusId", "Zeus ID", EditField.Type.TEXT, zsId,
                            false, 255, null)), values -> {
                        String zeusID = (String) values.get("zeusId");
                        if (!EditDialogFactory.isValidText(zeusID, 255)) {
                            showOkDialog("Fehler", "Zeus ID ist ungültigt oder zu lang");
                            return;
                        }
                        showConfirmDialog("Speichern", "Änderungen speichern?", () -> {
                            VaadinRequest req = VaadinRequest.getCurrent();
                            Family f = new Family(currentFamily);
                            f.setZeusId(zeusID);
                            familyService.updateFamily(f, currentEmployee.getLogin(),
                                    req != null ? req.getRemoteAddr() : "UNKNOWN",
                                    req != null ? req.getHeader("User-Agent") : "UNKNOWN");
                            getUI().ifPresent(ui -> ui.getPage().reload());
                        }, () -> {
                        });
                    }));
            bar.add(zeusIdBtn);
        }

        if ((lvl == 80 || lvl == 100)
                && currentFamily.getStatus().equals(RecordStatus.ACTIVE) && !currentFamily.isCaseClosed()) {
            Button employeeBtn = new Button("Berater*in", VaadinIcon.EDIT.create());
            employeeBtn.addClickListener(e -> openChangeAssigneeDialog());
            bar.add(employeeBtn);
        }

        if ((lvl == 80 || lvl == 100) ||
                (lvl == 50 && (currentFamily.getStatus().equals(RecordStatus.ACTIVE) ||
                        currentFamily.getStatus().equals(RecordStatus.ARCHIVED)))) {
            Button statusBtn = new Button("Status");

            statusBtn.addClickListener(e -> openFamilyStatusDialog());

            bar.add(statusBtn);
        }

        if (lvl != 10 && currentFamily.getStatus().equals(RecordStatus.ACTIVE)) {
            boolean caseClosed = currentFamily.isCaseClosed();
            Button caseClosedBtn;
            if (caseClosed) {
                caseClosedBtn = new Button("Ablauf wiederherstellen", VaadinIcon.UNLOCK.create());
                caseClosedBtn.getStyle().set("color", "green");
            } else {
                caseClosedBtn = new Button("Ablauf schließen", VaadinIcon.LOCK.create());
                caseClosedBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
            }
            caseClosedBtn.addClickListener(e -> {
                if (!caseClosed) {
                    showConfirmDialog("Schluss des Ablaufs der Familie",
                            "Ist die Arbeit mit der Familie \"" + currentFamily.getFamilyName() + "\" beendet?" +
                                    " Wollen Sie die Dateien sperren?",
                            () -> {
                                VaadinRequest req = VaadinRequest.getCurrent();
                                familyService.closeCase(currentFamily.getId(), currentEmployee.getLogin(),
                                        req != null ? req.getRemoteAddr() : "UNKNOWN",
                                        req != null ? req.getHeader("User-Agent") : "UNKNOWN");
                                getUI().ifPresent(ui -> ui.getPage().reload());
                            }, () -> {
                            });
                } else {
                    showConfirmDialog("Wiederaufnahmeverfahren", "Wollen Sie die Familie \"" +
                                    currentFamily.getFamilyName() +
                                    "\" entsperren und die Dateien bearbeiten?",
                            () -> {
                                VaadinRequest req = VaadinRequest.getCurrent();
                                familyService.openCaseBack(currentFamily.getId(), currentEmployee.getLogin(),
                                        req != null ? req.getRemoteAddr() : "UNKNOWN",
                                        req != null ? req.getHeader("User-Agent") : "UNKNOWN");
                                getUI().ifPresent(ui -> ui.getPage().reload());
                            }, () -> {
                            });
                }
            });
            bar.add(caseClosedBtn);
        }

        return bar;
    }

    private void openFamilyStatusDialog() {
        Dialog dialog = new Dialog();
        dialog.setModal(true);
        dialog.setCloseOnOutsideClick(false);
        dialog.setWidth("600px");

        RecordStatus status = currentFamily.getStatus();

        VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);

        ComboBox<String> action = new ComboBox<>("Aktion");
        action.setWidthFull();

        if (status.equals(RecordStatus.ACTIVE)) {
        }
        if (lvl == 80 || lvl == 100)
            if (!currentFamily.isCaseClosed()) action.setItems("BLOCKIEREN", "ARCHIVIEREN", "LÖSCHEN");
            else action.setItems("BLOCKIEREN", "ARCHIVIEREN");
        else if (lvl == 50)
            if (!currentFamily.isCaseClosed()) action.setItems("ARCHIVIEREN", "LÖSCHEN");
            else action.setItems("ARCHIVIEREN");

        if (status.equals(RecordStatus.ARCHIVED) && (lvl == 80 || lvl == 100 || lvl == 50))
            action.setItems("WIEDERHERSTELLEN");

        if (status.equals(RecordStatus.BLOCKED) && (lvl == 80 || lvl == 100)) action.setItems("ENTSPERREN");

        TextArea reason = new TextArea();
        reason.setWidthFull();
        reason.setVisible(false);
        reason.setMinLength(5);

        Span hint = new Span();
        hint.getStyle().set("color", "#666");

        action.addValueChangeListener(ev -> {
            String v = ev.getValue();
            if (v == null) return;

            reason.setVisible(false);
            hint.setText("");

            switch (v) {
                case "BLOCKIEREN" -> {
                    reason.setVisible(true);
                    reason.setLabel("Begründung der Blockierung");
                    hint.setText("Familie wird gesperrt und ist nicht mehr editierbar. " +
                            "Geben Sie bitte eine Begründung an. Mind. 5 Zeichen.");
                }
                case "LÖSCHEN" -> hint.setText(lvl == 50
                        ? "Die Familie " + currentFamily.getFamilyName() + " wird gelöscht. " +
                        "14 Tage Wiederherstellung möglich."
                        : "Die Familie " + currentFamily.getFamilyName() + " wird gelöscht.");
                case "ARCHIVIEREN" -> {
                    if (!currentFamily.isCaseClosed())
                        hint.setText("Archivierung nur möglich, wenn Ablauf geschlossen ist.");
                    else hint.setText("Die Familie " + currentFamily.getFamilyName() + " wird archiviert.");
                }
                case "WIEDERHERSTELLEN" -> {
                    reason.setVisible(true);
                    reason.setLabel("Begründung der Wiederherstellung");
                    hint.setText("Die Familie " + currentFamily.getFamilyName() + " wird wiederherstellt. " +
                            "Geben Sie bitte eine Begründung an. Mind. 5 Zeichen.");
                }
                case "ENTSPERREN" -> {
                    reason.setVisible(true);
                    reason.setLabel("Begründung der Entsperrung.");
                    hint.setText("Die Familie " + currentFamily.getFamilyName() + " wird entsperrt. " +
                            "Geben Sie bitte eine Begründung an. Mind. 5 Zeichen.");
                }
            }
        });

        Button cancel = new Button("Abbrechen", e -> dialog.close());

        Button save = new Button("Speichern", e -> {
            String v = action.getValue();
            if (v == null) return;

            if (reason.isVisible() && (reason.getValue() == null || reason.getValue().length() < 5)) {
                showOkDialog("Achtung!", "Begründung muss mindestens 5 Zeichen haben.");
                return;
            }

            if ("ARCHIVIEREN".equals(v) && !currentFamily.isCaseClosed()) {
                showOkDialog("Achtung!",
                        "Archivierung nich möglich! Die Famile kann archiviert werden, wenn der Ablauf " +
                                "geschlossen ist. Schließen Sie bitte den Dialogfenster und schließen bitte den Ablauf.");
                return;
            }

            showConfirmDialog("Status ändern", "Aktion wirklich durchführen?", () -> {
                VaadinRequest req = VaadinRequest.getCurrent();
                String ip = req != null ? req.getRemoteAddr() : "UNKNOWN";
                String browser = req != null ? req.getHeader("User-Agent") : "UNKNOWN";

                switch (v) {
                    case "BLOCKIEREN" -> {
                        familyService.blockFamily(familyId, reason.getValue(), currentEmployee.getLogin(), ip, browser);
                        getUI().ifPresent(ui -> ui.getPage().reload());
                    }
                    case "ENTSPERREN" -> {
                        familyService.unblockFamily(familyId, reason.getValue(), currentEmployee.getLogin(), ip, browser);
                        getUI().ifPresent(ui -> ui.getPage().reload());
                    }
                    case "ARCHIVIEREN" -> {
                        familyService.archiveFamily(familyId, currentEmployee.getLogin(), ip, browser);
                        getUI().ifPresent(ui -> ui.getPage().reload());
                    }
                    case "WIEDERHERSTELLEN" -> {
                        familyService.unarchiveFamily(familyId, reason.getValue(), currentEmployee.getLogin(), ip, browser);
                        getUI().ifPresent(ui -> ui.getPage().reload());
                    }
                    case "LÖSCHEN" -> {
                        familyService.invalidateFamily(familyId, currentEmployee.getLogin(), ip, browser);
                        getUI().ifPresent(ui -> ui.navigate(FamiliesView.class));
                    }
                }
                dialog.close();
            }, () -> {
            });
        });

        HorizontalLayout buttons = new HorizontalLayout(cancel, save);
        buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        content.add(action, reason, hint);
        dialog.add(content);
        dialog.getFooter().add(buttons);
        dialog.open();
    }

    private void openChangeAssigneeDialog() {
        Dialog dialog = new Dialog();
        dialog.setModal(true);
        dialog.setCloseOnOutsideClick(false);
        dialog.setWidth("600px");

        ComboBox<Employee> combo = new ComboBox<>("Berater*in");
        combo.setWidthFull();


        List<Employee> items = employeeService.findAll().stream().filter(
                employee -> !employee.isArchived()).toList();
        combo.setItems(items);
        combo.setItemLabelGenerator(emp -> emp.getFirstName() + " " + emp.getLastName());
        combo.setRenderer(new ComponentRenderer<>(emp -> {
            HorizontalLayout row = new HorizontalLayout();
            row.setAlignItems(FlexComponent.Alignment.CENTER);
            row.setSpacing(true);

            if (!emp.isActive()) {
                Icon icon = VaadinIcon.LOCK.create();
                icon.getStyle().set("color", "red");
                row.add(icon);
            }

            Span text = new Span(emp.getFirstName() + " " + emp.getLastName() + " : " + emp.getRole().getLabel());
            if (!emp.isActive()) text.getStyle().set("color", "#888");

            row.add(text);
            return row;
        }));

        combo.setValue(currentFamily.getAssignedEmployee());

        Icon icon = VaadinIcon.LOCK.create();
        Span error = new Span();

        error.add(icon, new Span(" Blockierte Berater können nicht zugewiesen werden"));
        error.getStyle().set("color", "red");
        error.setVisible(false);

        Button cancel = new Button("Abbrechen", e -> dialog.close());

        Button save = new Button("Speichern");
        save.setEnabled(true);

        combo.addValueChangeListener(ev -> {
            Employee selected = ev.getValue();
            if (selected == null) {
                error.setVisible(false);
                save.setEnabled(false);
                return;
            }
            if (!selected.isActive()) {
                error.setVisible(true);
                save.setEnabled(false);
                return;
            }
            if (selected.equals(currentFamily.getAssignedEmployee())) {
                error.setVisible(false);
                save.setEnabled(false);
                return;
            }

            error.setVisible(false);
            save.setEnabled(true);
        });

        save.addClickListener(e -> {
            Employee selected = combo.getValue();
            if (selected == null) {
                return;
            }
            if (selected.equals(currentFamily.getAssignedEmployee())) {
                dialog.close();
                return;
            }
            showConfirmDialog("Berater*in wechseln", "Möchten Soe den Fall wirklich übertragen?",
                    () -> {
                        VaadinRequest req = VaadinRequest.getCurrent();
                        familyService.updateAssignedEmployee(currentFamily.getId(), selected.getId(), currentEmployee.getLogin(),
                                req != null ? req.getRemoteAddr() : "UNKNOWN",
                                req != null ? req.getHeader("User-Agent") : "UNKNOWN");
                        getUI().ifPresent(ui -> ui.getPage().reload());
                    }, () -> {
                    });
        });

        HorizontalLayout buttons = new HorizontalLayout(cancel, save);
        buttons.setWidthFull();
        buttons.setJustifyContentMode(HorizontalLayout.JustifyContentMode.END);

        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(true);

        content.add(new Span("Berater*in zuweisen"), combo, error);

        dialog.add(content);
        dialog.getFooter().add(buttons);
        dialog.open();
    }

    private void buildCreateConsultationDialog() {
        Dialog dialog = new Dialog();
        dialog.setModal(true);
        dialog.setResizable(false);
        dialog.setDraggable(false);
        dialog.setCloseOnOutsideClick(false);
        dialog.setWidth("800px");

        H2 title = new H2("Beratung für die Familie " + currentFamily.getFamilyName());
        dialog.add(title);

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

        Span detailsTitle = new Span("Details der Beratung");
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

        HorizontalLayout buttons = new HorizontalLayout(save, cancel);
        buttons.setWidthFull();
        buttons.setJustifyContentMode(JustifyContentMode.END);

        dialog.getFooter().add(buttons);
        dialog.open();
    }

    private void buildCreateMemberDialog() {
        Dialog dialog = new Dialog();
        dialog.setModal(true);
        dialog.setResizable(false);
        dialog.setDraggable(false);
        dialog.setCloseOnOutsideClick(false);
        dialog.setWidth("800px");

        H2 title = new H2("Neues Mitglied der Familie " + currentFamily.getFamilyName());
        dialog.add(title);

        TextField firstName = new TextField("Vorname (*)");
        TextField lastName = new TextField("Nachname (*)");
        ComboBox<FamilyMemberGender> gender = new ComboBox<>("Gender (*)");
        gender.setItems(FamilyMemberGender.values());
        gender.setPlaceholder("Bitte wählen...");

        DatePicker birthDate = new DatePicker("Geburtsdatum (*)");
        TextField birthCity = new TextField("Geburtsstadt (*)");
        TextField birthCountry = new TextField("Geburtsland (*)");

        Checkbox livesWithFamily = new Checkbox("Lebt mit der Familie");
        livesWithFamily.setValue(true);

        firstName.setWidthFull();
        lastName.setWidthFull();
        gender.setWidthFull();
        birthDate.setWidthFull();
        birthCity.setWidthFull();
        birthCountry.setWidthFull();

        Span mainTitle = new Span("Persönliche Daten");
        mainTitle.getStyle().set("font-weight", "bold");

        VerticalLayout mainBlock = new VerticalLayout();
        mainBlock.setSpacing(false);
        mainBlock.setPadding(true);
        mainBlock.getStyle().set("border", "1px solid #ddd").set("padding", "10px").set("border-radius", "6px");

        mainBlock.add(mainTitle, firstName, lastName, gender, birthDate, birthCity, birthCountry, livesWithFamily);

        TextField nationality = new TextField("Staatsangehörigkeit");
        TextField languages = new TextField("Sprachen");
        languages.setValue(currentFamily.getLanguages());
        TextField income = new TextField("Einkommen");
        TextArea workInfo = new TextArea("Berufliche Tätigkeit");
        TextField educationDegree = new TextField("Bildungsabschluss");
        TextArea educationInfo = new TextArea("Ausbildung / Studium");

        nationality.setWidthFull();
        languages.setWidthFull();
        income.setWidthFull();
        workInfo.setWidthFull();
        educationDegree.setWidthFull();
        educationInfo.setWidthFull();
        workInfo.setHeight("130px");
        educationInfo.setHeight("130px");

        Span extraTitle = new Span("Weitere Angaben");
        extraTitle.getStyle().set("font-weight", "bold");

        VerticalLayout extraBlock = new VerticalLayout();
        extraBlock.setSpacing(false);
        extraBlock.setPadding(true);
        extraBlock.getStyle().set("border", "1px solid #ddd").set("padding", "10px").set("border-radius", "6px");

        extraBlock.add(extraTitle, nationality, languages, income, workInfo, educationDegree, educationInfo);

        TextField phone = new TextField("Telefon");
        TextField email = new TextField("E-Mail");
        TextArea notes = new TextArea("Notizen");

        phone.setWidthFull();
        email.setWidthFull();
        notes.setWidthFull();
        notes.setHeight("130px");

        Span contactTitle = new Span("Kontakt / Notizen");
        contactTitle.getStyle().set("font-weight", "bold");

        VerticalLayout contactBlock = new VerticalLayout();
        contactBlock.setSpacing(false);
        contactBlock.setPadding(true);
        contactBlock.getStyle().set("border", "1px solid #ddd").set("padding", "10px").set("border-radius", "6px");

        contactBlock.add(contactTitle, phone, email, notes);

        VerticalLayout content = new VerticalLayout();
        content.setWidthFull();
        content.add(mainBlock, extraBlock, contactBlock);

        dialog.add(content);

        Button cancel = new Button("Abbrechen");
        Button save = new Button("Speichern");

        String oLanguages = currentFamily.getLanguages();

        cancel.addClickListener(event -> {
            if (!isMemberFormDirty(oLanguages, firstName.getValue(), lastName.getValue(), gender.getValue(),
                    birthDate.getValue(), birthCity.getValue(), birthCountry.getValue(), nationality.getValue(),
                    languages.getValue(), income.getValue(), workInfo.getValue(), educationDegree.getValue(),
                    educationInfo.getValue(), notes.getValue(), phone.getValue(), email.getValue())) {
                dialog.close();
                return;
            }
            showConfirmDialog("Änderungen verwerfen", "Ihre Eingaben gehen verloren. Fortfahren?",
                    dialog::close, () -> {
                    });
        });

        save.addClickListener(event -> handleMemberSave(dialog, firstName, lastName,
                gender, birthDate, birthCity, birthCountry, nationality, languages, livesWithFamily, income, workInfo,
                educationDegree, educationInfo, notes, phone, email));

        HorizontalLayout buttons = new HorizontalLayout(save, cancel);
        buttons.setWidthFull();
        buttons.setJustifyContentMode(JustifyContentMode.END);

        dialog.getFooter().add(buttons);
        dialog.open();
    }

    private void handleMemberSave(Dialog dialog, TextField firstName, TextField lastName,
                                  ComboBox<FamilyMemberGender> gender, DatePicker birthDate, TextField birthCity,
                                  TextField birthCountry, TextField nationality, TextField languages,
                                  Checkbox livesWithFamily, TextField income, TextArea workInfo, TextField education,
                                  TextArea educationInfo, TextArea notes, TextField phone, TextField email) {
        List<String> errors = validateMemberForm(firstName.getValue(), lastName.getValue(), gender.getValue(),
                birthDate.getValue(), birthCity.getValue(), birthCountry.getValue());

        firstName.getStyle().remove("border");
        lastName.getStyle().remove("border");
        gender.getStyle().remove("border");
        birthDate.getStyle().remove("border");
        birthCity.getStyle().remove("border");
        birthCountry.getStyle().remove("border");

        if (!errors.isEmpty()) {
            if (firstName.getValue().isEmpty()) firstName.getStyle().set("border", "1px solid red");
            if (lastName.getValue().isEmpty()) lastName.getStyle().set("border", "1px solid red");
            if (gender.getValue() == null) gender.getStyle().set("border", "1px solid red");
            if (birthDate.getValue() == null) birthDate.getStyle().set("border", "1px solid red");
            if (birthCity.getValue().isEmpty()) birthCity.getStyle().set("border", "1px solid red");
            if (birthCountry.getValue().isEmpty()) birthCountry.getStyle().set("border", "1px solid " +
                    "red");

            showOkDialog("Fehler", String.join("\n", errors));
            return;
        }

        try {
            VaadinRequest req = VaadinRequest.getCurrent();
            String ip = req != null ? req.getRemoteAddr() : "UNKNOWN";
            String browser = req != null ? req.getHeader("User-Agent") : "UNKNOWN";

            familyMemberService.createMember(currentFamily, firstName.getValue(), lastName.getValue(), gender.getValue(),
                    birthDate.getValue(), birthCity.getValue(), birthCountry.getValue(), nationality.getValue(),
                    languages.getValue(), livesWithFamily.getValue(), income.getValue(), workInfo.getValue(),
                    education.getValue(), educationInfo.getValue(), notes.getValue(), phone.getValue(), email.getValue(),
                    currentEmployee.getLogin(), ip, browser);

            dialog.close();
            getUI().ifPresent(ui -> ui.getPage().reload());
        } catch (Exception e) {
            showOkDialog("Fehler", String.join("\n", e.getMessage()));
        }
    }

    private boolean isMemberFormDirty(String oLanguages, String currentFirstName, String currentLastName,
                                      FamilyMemberGender currentGender, LocalDate currentBirthDate,
                                      String currentBirthCity, String currentBirthCountry, String currentNationality,
                                      String currentLanguages, String currentIncome, String currentWorkInfo,
                                      String currentEducationDegree, String currentEducationInfo, String currentNotes,
                                      String currentPhone, String currentEmail) {
        if (!currentFirstName.isEmpty()) return true;
        if (!currentLastName.isEmpty()) return true;
        if (currentGender != null) return true;
        if (currentBirthDate != null) return true;
        if (!currentBirthCity.isEmpty()) return true;
        if (!currentBirthCountry.isEmpty()) return true;
        if (!currentNationality.isEmpty()) return true;
        if (!currentLanguages.equals(oLanguages)) return true;
        if (!currentIncome.isEmpty()) return true;
        if (!currentWorkInfo.isEmpty()) return true;
        if (!currentEducationDegree.isEmpty()) return true;
        if (!currentEducationInfo.isEmpty()) return true;
        if (!currentNotes.isEmpty()) return true;
        if (!currentPhone.isEmpty()) return true;
        if (!currentEmail.isEmpty()) return true;
        return false;
    }

    private List<String> validateMemberForm(String firstName, String lastName, FamilyMemberGender gender,
                                            LocalDate birthDate, String birthCity, String birthCountry) {
        List<String> errors = new ArrayList<>();
        if (firstName == null || firstName.isEmpty()) errors.add("Vorname ist erforderlich");
        if (lastName == null || lastName.isEmpty()) errors.add("Nachname ist erforderlich");
        if (gender == null) errors.add("Gender ist erforderlich");
        if (birthDate == null) errors.add("Geburtsdatum ist erforderlich");
        if (birthCity == null || birthCity.isEmpty()) errors.add("Geburtsstadt ist erforderlich");
        if (birthCountry == null || birthCountry.isEmpty()) errors.add("Geburtsland ist erforderlich");

        return errors;
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
            askBackdatedConfirmation(() -> handleOptionalFinalConsultationSave(dialog, dateTime, duration, topic, description,
                    result, followUp));
            return;
        }

        handleOptionalFinalConsultationSave(dialog, dateTime, duration, topic, description, result, followUp);
    }

    private void handleOptionalFinalConsultationSave(Dialog dialog, LocalDateTime dateTime, Integer duration, String topic,
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

    private int getConsultationsCount() {
        List<Consultation> consultations = new ArrayList<>();
        if (!currentFamily.getStatus().equals(RecordStatus.INVALID))
            consultations.addAll(consultationService.getConsultationsByFamily(currentFamily));
        return consultations.size();
    }
}