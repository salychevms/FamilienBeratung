package com.salychevms.familienberatung.ui.view;

import com.salychevms.familienberatung.model.*;
import com.salychevms.familienberatung.service.*;
import com.salychevms.familienberatung.ui.dialog.EditDialogFactory;
import com.salychevms.familienberatung.ui.dialog.EditField;
import com.salychevms.familienberatung.ui.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.VaadinRequest;
import jakarta.annotation.security.PermitAll;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Slf4j
@Route(value = "member/:id/:familyId", layout = MainLayout.class)
@PageTitle("Mitglied")
@RequiredArgsConstructor
@PermitAll
public class MemberDetailsView extends VerticalLayout implements BeforeEnterObserver {

    private final AuthService authService;
    private final EmployeeService employeeService;
    private final FamilyService familyService;
    private final FamilyMemberService familyMemberService;

    Employee currentEmployee;
    int lvl;
    Long familyId;
    Long memberId;
    Family currentFamily;
    FamilyMember currentFamilyMember;

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        removeAll();
        Employee employee = authService.getCurrentEmployee();
        if (employee == null || employee.isArchived() || !employee.isActive()) {
            event.forwardTo("login");
            return;
        }

        this.currentEmployee = employeeService.findByLogin(employee.getLogin());
        this.lvl = currentEmployee.getRole().getAccessLevel();

        Optional<Long> optMemberId = event.getRouteParameters().getLong("id");
        Optional<Long> optFamilyId = event.getRouteParameters().getLong("familyId");

        if (optMemberId.isEmpty() || optFamilyId.isEmpty()) {
            event.forwardTo("families");
            return;
        }

        this.memberId = optMemberId.get();
        this.familyId = optFamilyId.get();

        try {
            this.currentFamily = familyService.getFamilyById(familyId);
        } catch (Exception ex) {
            event.forwardTo("family/" + familyId);
            return;
        }

        if (currentFamily.getStatus().equals(RecordStatus.INVALID)) {
            event.forwardTo("families");
            return;
        }

        try {
            this.currentFamilyMember = familyMemberService.getMember(currentFamily, memberId, currentEmployee.getLogin());
        } catch (Exception e) {
            event.forwardTo("family/" + familyId);
            return;
        }

        buildUI();
    }

    private void buildUI() {
        setPadding(true);
        setSpacing(true);
        setSizeFull();

        buildBreadCrumbs();
        buildHeader();
        buildPersonalData();
        buildContacts();
        buildWorkAndEducation();
        buildNotes();
        buildButtonArea();
        buildAudit();
    }

    private void buildBreadCrumbs() {
        HorizontalLayout bcrumbs = new HorizontalLayout();
        bcrumbs.setSpacing(true);
        bcrumbs.setPadding(true);
        bcrumbs.setAlignItems(FlexComponent.Alignment.CENTER);

        RouterLink l1 = new RouterLink("Übersicht", OverviewView.class);
        RouterLink l2 = new RouterLink("Familien", FamiliesView.class);
        RouterLink l3 = new RouterLink("Familie: " + currentFamily.getFamilyName(), FamilyDetailsView.class,
                new RouteParameters("id", String.valueOf(currentFamily.getId())));

        Span sep1 = new Span(" >> ");
        Span sep2 = new Span(" >> ");
        Span sep3 = new Span(" >> ");

        Span member = new Span("Mitglied: " + currentFamilyMember.getFirstName() +
                " " + currentFamilyMember.getLastName());

        member.getStyle().set("font-weight", "bold");

        bcrumbs.add(l1, sep1, l2, sep2, l3, sep3, member);

        add(bcrumbs);
    }

    private void buildHeader() {
        VerticalLayout box = new VerticalLayout();
        box.setSpacing(false);
        box.setPadding(false);
        box.setWidthFull();

        HorizontalLayout header = new HorizontalLayout();
        header.setSpacing(false);

        if (lvl != 10 && currentFamily.getStatus().equals(RecordStatus.ACTIVE) && !currentFamily.isCaseClosed()) {
            Button editButton = new Button(VaadinIcon.EDIT.create());
            editButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            editButton.addClassName("edit-btn");
            header.add(editButton);
        }

        String fullName = currentFamilyMember.getFirstName() + " " + currentFamilyMember.getLastName();
        H2 title = new H2("Familienmitglied: " + fullName);
        header.add(title);

        Span fTitle = new Span("Familie: ");
        Span fName = new Span(currentFamily.getFamilyName());
        fName.getStyle().set("font-weight", "bold");
        Span fStatusTitle = new Span("Status: ");
        Span fStatusIs = new Span(currentFamily.getStatus().toString());
        fStatusIs.getStyle().set("font-weight", "bold");

        Span mTitle = new Span("Mitglied ID: ");
        Span mId = new Span(String.valueOf(currentFamilyMember.getId()));
        mId.getStyle().set("font-weight", "bold");
        Span mStatusTitle = new Span("Status: ");
        Span mStatusIs = new Span();
        mStatusIs.getStyle().set("font-weight", "bold");
        if (!currentFamilyMember.isInvalid()) {
            if (currentFamily.getStatus().equals(RecordStatus.ARCHIVED)) {
                fStatusIs.getStyle().set("color", "#b58900");
                mStatusIs.setText(RecordStatus.BLOCKED.toString());
                mStatusIs.getStyle().set("color", "red");
            } else if (currentFamily.getStatus().equals(RecordStatus.BLOCKED)) {
                fStatusIs.getStyle().set("color", "red");
                mStatusIs.setText(RecordStatus.BLOCKED.toString());
                mStatusIs.getStyle().set("color", "red");
            } else if (currentFamily.getStatus().equals(RecordStatus.ACTIVE)) {
                fStatusIs.getStyle().set("color", "green");
                mStatusIs.setText(RecordStatus.ACTIVE.toString());
                mStatusIs.getStyle().set("color", "green");
            }
        }
        box.add(header, new HorizontalLayout(fTitle, fName, fStatusTitle, fStatusIs),
                new HorizontalLayout(mTitle, mId, mStatusTitle, mStatusIs));
        add(box);
    }

    private void buildButtonArea() {
        if (lvl != 10 && currentFamily.getStatus().equals(RecordStatus.ACTIVE) && !currentFamily.isCaseClosed()) {
            HorizontalLayout buttons = new HorizontalLayout();
            buttons.setSpacing(true);
            buttons.setAlignItems(FlexComponent.Alignment.CENTER);
            buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
            buttons.setWidthFull();

            Button button = new Button("Mitglied löschen");
            button.getStyle().set("color", "red");

            button.addClickListener(event -> {
                Dialog okDialog = new Dialog("Mitglied löschen");
                okDialog.setWidth("300px");

                Span text = new Span("Wollen Sie Mitglied löschen?");
                Span name = new Span(currentFamilyMember.getFirstName() + " " + currentFamilyMember.getLastName());
                name.getStyle().set("font-weight", "bold");
                okDialog.add(new VerticalLayout(text, name));

                Button ok = new Button("Ja", e -> {
                    VaadinRequest req = VaadinRequest.getCurrent();
                    familyMemberService.invalidateMember(currentFamily, currentFamilyMember, currentEmployee.getLogin(),
                            req != null ? req.getRemoteAddr() : "UNKNOWN",
                            req != null ? req.getHeader("User-Agent") : "UNKNOWN");
                    getUI().ifPresent(ui -> ui.navigate(FamilyDetailsView.class,
                            new RouteParameters("id", currentFamily.getId().toString())));
                    okDialog.close();
                });

                Button cancel = new Button("Nein", e -> okDialog.close());

                HorizontalLayout btns = new HorizontalLayout();
                btns.setJustifyContentMode(JustifyContentMode.END);
                btns.setWidthFull();
                btns.add(cancel, ok);

                okDialog.getFooter().add(btns);
                okDialog.open();
            });

            buttons.add(button);
            add(buttons);
        }
    }

    private void buildPersonalData() {
        VerticalLayout block = new VerticalLayout();
        block.setSpacing(false);
        block.setPadding(false);
        block.setWidthFull();

        block.getStyle().set("border", "1px solid #ddd").set("border-radius", "6px").set("padding", "10px")
                .set("margin-top", "0px");

        HorizontalLayout header = new HorizontalLayout();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setSpacing(false);

        if (lvl != 10 && currentFamily.getStatus().equals(RecordStatus.ACTIVE) && !currentFamily.isCaseClosed()) {
            Button editButton = new Button(VaadinIcon.EDIT.create());
            editButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            editButton.addClassName("edit-btn");

            editButton.addClickListener(e -> EditDialogFactory.openEditDialog(
                    "Persönliche Daten ändern", List.of(
                            new EditField("gender", "Gender", EditField.Type.SELECT,
                                    currentFamilyMember.getGender(), true, null,
                                    List.of(FamilyMemberGender.values()), null, null),
                            new EditField("birthDate", "Geburtsdatum", EditField.Type.DATE,
                                    currentFamilyMember.getBirthDate(), true, null, null,
                                    LocalDate.of(1935, 1, 1), LocalDate.now()),
                            new EditField("birthCity", "Geburtsstadt", EditField.Type.TEXT,
                                    currentFamilyMember.getBirthCity(), true, 255, null,
                                    null, null),
                            new EditField("birthCountry", "Geburtsland", EditField.Type.TEXT,
                                    currentFamilyMember.getBirthCountry(), true, 255, null,
                                    null, null),
                            new EditField("nationality", "Nationalität", EditField.Type.TEXT,
                                    currentFamilyMember.getNationality(), false, 255, null,
                                    null, null),
                            new EditField("languages", "Sprachen", EditField.Type.TEXT,
                                    currentFamilyMember.getLanguages(), false, 255, null,
                                    null, null),
                            new EditField("livesWithFamily", "Lebt mit der Familie", EditField.Type.SELECT,
                                    currentFamilyMember.isLivesWithFamily(), true, null,
                                    List.of("Ja", "Nein"), null, null)),
                    values -> {
                        FamilyMember updated = familyMemberService.getMember(currentFamily, currentFamilyMember.getId(),
                                currentEmployee.getLogin());

                        if (values.get("gender") != null)
                            updated.setGender((FamilyMemberGender) values.get("gender"));
                        if (values.get("birthDate") != null)
                            updated.setBirthDate((LocalDate) (values.get("birthDate")));
                        if (values.get("birthCity") != null)
                            updated.setBirthCity((String) values.get("birthCity"));
                        if (values.get("birthCountry") != null)
                            updated.setBirthCountry((String) values.get("birthCountry"));
                        if (values.get("nationality") != null)
                            updated.setNationality((String) values.get("nationality"));
                        if (values.get("languages") != null)
                            updated.setLanguages((String) values.get("languages"));
                        if (values.get("livesWithFamily") != null)
                            updated.setLivesWithFamily("Ja".equals(values.get("livesWithFamily")));

                        VaadinRequest req = VaadinRequest.getCurrent();

                        familyMemberService.updateMember(currentFamily, currentFamilyMember, updated,
                                currentEmployee.getLogin(), req != null ? req.getRemoteAddr() : "UNKNOWN",
                                req != null ? req.getHeader("User-Agent") : "UNKNOWN");
                        getUI().ifPresent(ui -> ui.getPage().reload());
                    }));
            header.add(editButton);
        }

        Span title = new Span("Persönliche Daten");
        title.getStyle().set("font-weight", "bold");
        header.add(title);

        String genderText;
        if (currentFamilyMember.getGender().equals(FamilyMemberGender.MAENNLICH))
            genderText = "männlich";
        else if (currentFamilyMember.getGender().equals(FamilyMemberGender.DIVERS))
            genderText = "divers";
        else
            genderText = "weiblich";

        String birthDateText = currentFamilyMember.getBirthDate() != null
                ? currentFamilyMember.getBirthDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                : "nicht angegeben";
        int age = (currentFamilyMember.getBirthDate() != null)
                ? Period.between(currentFamilyMember.getBirthDate(), LocalDate.now()).getYears() : -1;
        block.add(header, getHLWithSpans("Gender: ", genderText),
                getHLWithSpans("Alter: ", String.valueOf(age)),
                getHLWithSpans("Geburtsdatum: ", birthDateText),
                getHLWithSpans("Geburtsstadt: ", empty(currentFamilyMember.getBirthCity())),
                getHLWithSpans("Geburtsland: ", empty(currentFamilyMember.getBirthCountry())),
                getHLWithSpans("Nationalität/Staatsbürgerschaft: ", empty(currentFamilyMember.getNationality())),
                getHLWithSpans("Sprachen: ", empty(currentFamilyMember.getLanguages())),
                getHLWithSpans("lebt mit der Familie: ", (currentFamilyMember.isLivesWithFamily() ? "Ja" : "Nein")));
        add(block);
    }

    private void buildContacts() {
        VerticalLayout block = new VerticalLayout();
        block.setSpacing(false);
        block.setPadding(false);
        block.setWidthFull();

        block.getStyle().set("border", "1px solid #ddd").set("border-radius", "6px").set("padding", "10px");

        HorizontalLayout header = new HorizontalLayout();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setSpacing(false);

        if (lvl != 10 && currentFamily.getStatus().equals(RecordStatus.ACTIVE) && !currentFamily.isCaseClosed()) {
            Button editButton = new Button(VaadinIcon.EDIT.create());
            editButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            editButton.addClassName("edit-btn");

            editButton.addClickListener(e -> EditDialogFactory.openEditDialog("Kontaktdaten ändern",
                            List.of(
                                    new EditField("phone", "Telefon", EditField.Type.TEXT,
                                            currentFamilyMember.getPhone(), false, 255, null,
                                            null, null),
                                    new EditField("email", "E-Mail", EditField.Type.TEXT,
                                            currentFamilyMember.getEmail(), false, 255, null,
                                            null, null)
                            ), values -> {
                                FamilyMember updated = familyMemberService.getMember(currentFamily,
                                        currentFamilyMember.getId(), currentEmployee.getLogin());

                                if (values.get("phone") != null)
                                    updated.setPhone((String) values.get("phone"));
                                if (values.get("email") != null)
                                    updated.setEmail((String) values.get("email"));

                                VaadinRequest req = VaadinRequest.getCurrent();

                                familyMemberService.updateMember(currentFamily, currentFamilyMember, updated,
                                        currentEmployee.getLogin(), req != null ? req.getRemoteAddr() : "UNKNOWN",
                                        req != null ? req.getHeader("User-Agent") : "UNKNOWN");

                                getUI().ifPresent(ui -> ui.getPage().reload());
                            }
                    )
            );
            header.add(editButton);
        }

        Span title = new Span("Kontaktdaten");
        title.getStyle().set("font-weight", "bold");
        header.add(title);

        block.add(header, getHLWithSpans("Telefon: ", empty(currentFamilyMember.getPhone())),
                getHLWithSpans("E-Mail: ", empty(currentFamilyMember.getEmail())));
        add(block);
    }

    private void buildWorkAndEducation() {
        VerticalLayout block = new VerticalLayout();
        block.setSpacing(false);
        block.setPadding(false);
        block.setWidthFull();

        block.getStyle().set("border", "1px solid #ddd").set("border-radius", "6px").set("padding", "10px").
                set("padding-bottom", "0");

        HorizontalLayout header = new HorizontalLayout();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setSpacing(false);

        if (lvl != 10 && currentFamily.getStatus().equals(RecordStatus.ACTIVE) && !currentFamily.isCaseClosed()) {
            Button editButton = new Button(VaadinIcon.EDIT.create());
            editButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            editButton.addClassName("edit-btn");

            editButton.addClickListener(e ->
                    EditDialogFactory.openEditDialog("Beruf und Bildung ändern", List.of(
                                    new EditField("income", "Einkommen", EditField.Type.TEXT,
                                            currentFamilyMember.getIncome(), false, 255, null,
                                            null, null),
                                    new EditField("workInfo", "Berufliche Tätigkeit / Erfahrung",
                                            EditField.Type.TEXTAREA, currentFamilyMember.getWorkInfo(),
                                            false, 4000, null, null, null),
                                    new EditField("educationDegree", "Bildungsabschluss",
                                            EditField.Type.TEXT, currentFamilyMember.getEducationDegree(),
                                            false, 255, null, null, null),
                                    new EditField("educationInfo", "Ausbildung / Studium",
                                            EditField.Type.TEXTAREA, currentFamilyMember.getEducationInfo(),
                                            false, 4000, null, null, null)
                            ), values -> {
                                FamilyMember updated = familyMemberService.getMember(currentFamily,
                                        currentFamilyMember.getId(), currentEmployee.getLogin());

                                if (values.get("income") != null)
                                    updated.setIncome((String) values.get("income"));
                                if (values.get("workInfo") != null)
                                    updated.setWorkInfo((String) values.get("workInfo"));
                                if (values.get("educationDegree") != null)
                                    updated.setEducationDegree((String) values.get("educationDegree"));
                                if (values.get("educationInfo") != null)
                                    updated.setEducationInfo((String) values.get("educationInfo"));

                                VaadinRequest req = VaadinRequest.getCurrent();

                                familyMemberService.updateMember(currentFamily, currentFamilyMember, updated,
                                        currentEmployee.getLogin(), req != null ? req.getRemoteAddr() : "UNKNOWN",
                                        req != null ? req.getHeader("User-Agent") : "UNKNOWN");

                                getUI().ifPresent(ui -> ui.getPage().reload());
                            }
                    )
            );
            header.add(editButton);
        }

        Span title = new Span("Beruf und Bildung");
        title.getStyle().set("font-weight", "bold");
        header.add(title);

        VerticalLayout workLayout = new VerticalLayout();
        workLayout.setSpacing(false);
        workLayout.setPadding(false);
        workLayout.setWidthFull();

        TextArea workText = new TextArea("Berufliche Tätigkeit / Erfahrung: ");
        workText.setValue(empty(currentFamilyMember.getWorkInfo()));
        workText.setReadOnly(true);
        workText.setMaxLength(4000);
        workText.setWidthFull();
        workText.setHeight("200px");
        workText.getStyle().set("white-space", "pre-wrap");

        workLayout.add(getHLWithSpans("Einkommen: ", empty(currentFamilyMember.getIncome())), workText);

        VerticalLayout eduLayout = new VerticalLayout();
        eduLayout.setSpacing(false);
        eduLayout.setPadding(false);
        eduLayout.setWidthFull();

        TextArea educationText = new TextArea("Ausbildung / Studium: ");
        educationText.setValue(empty(currentFamilyMember.getEducationInfo()));
        educationText.setReadOnly(true);
        educationText.setMaxLength(4000);
        educationText.setWidthFull();
        educationText.setHeight("200px");
        educationText.getStyle().set("white-space", "pre-wrap");

        eduLayout.add(getHLWithSpans("Bildungsabschluss: ",
                empty(currentFamilyMember.getEducationDegree())), educationText);

        block.add(header, workLayout, eduLayout);
        add(block);
    }

    private void buildNotes() {
        VerticalLayout block = new VerticalLayout();
        block.setSpacing(false);
        block.setPadding(false);
        block.setWidthFull();

        block.getStyle().set("border", "1px solid #ddd").set("border-radius", "6px").set("padding", "10px").
                set("padding-bottom", "0");

        HorizontalLayout header = new HorizontalLayout();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setSpacing(false);

        if (lvl != 10 && currentFamily.getStatus().equals(RecordStatus.ACTIVE) && !currentFamily.isCaseClosed()) {
            Button editButton = new Button(VaadinIcon.EDIT.create());
            editButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            editButton.addClassName("edit-btn");

            editButton.addClickListener(e -> EditDialogFactory.openEditDialog("Notizen",
                            List.of(
                                    new EditField("notes", "Notizen", EditField.Type.TEXTAREA,
                                            currentFamilyMember.getNotes(), false, 1000, null,
                                            null, null)
                            ), values -> {
                                FamilyMember updated = familyMemberService.getMember(currentFamily,
                                        currentFamilyMember.getId(), currentEmployee.getLogin());

                                if (values.get("notes") != null)
                                    updated.setNotes((String) values.get("notes"));


                                VaadinRequest req = VaadinRequest.getCurrent();

                                familyMemberService.updateMember(currentFamily, currentFamilyMember, updated,
                                        currentEmployee.getLogin(), req != null ? req.getRemoteAddr() : "UNKNOWN",
                                        req != null ? req.getHeader("User-Agent") : "UNKNOWN");

                                getUI().ifPresent(ui -> ui.getPage().reload());
                            }
                    )
            );
            header.add(editButton);
        }

        Span title = new Span("Notizen");
        title.getStyle().set("font-weight", "bold");
        header.add(title);

        TextArea notes = new TextArea();
        notes.setReadOnly(true);
        notes.setMaxLength(1000);
        notes.setValue(empty(currentFamilyMember.getNotes()));
        notes.setHeight("200px");
        notes.setWidthFull();
        notes.getStyle().set("white-space", "pre-wrap").set("margin-bottom", "0");

        block.add(header, notes);
        add(block);
    }

    private void buildAudit() {
        VerticalLayout block = new VerticalLayout();
        block.setSpacing(false);
        block.setPadding(false);
        block.setWidthFull();

        block.getStyle().set("border", "1px solid #ddd").set("border-radius", "6px").set("padding", "10px");

        Span title = new Span("Verlauf");
        title.getStyle().set("font-weight", "bold");
        block.add(title);

        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

        if (currentFamilyMember.getCreatedAt() != null) {
            block.add(makeAuditLine("Erstellt am " + currentFamilyMember.getCreatedAt().format(df) + " von " +
                    empty(currentFamilyMember.getCreatedBy())));
        }
        if (currentFamilyMember.getUpdatedAt() != null) {
            block.add(makeAuditLine("Geändert am " + currentFamilyMember.getUpdatedAt().format(df) + " von " +
                    empty(currentFamilyMember.getUpdatedBy())));
        }
        if (currentFamilyMember.getInvalidAt() != null) {
            block.add(makeAuditLine("Gelöscht am " + currentFamilyMember.getInvalidAt().format(df) + " von " +
                    empty(currentFamilyMember.getInvalidBy())));
        }
        if (currentFamilyMember.getRestoredAt() != null) {
            block.add(makeAuditLine("Wiederherstellt am " + currentFamilyMember.getRestoredAt().format(df) +
                    " von " + empty(currentFamilyMember.getRestoredBy())));
            if (currentFamilyMember.getRestoredReason() != null) {
                block.add(makeAuditLine("Grund: " + currentFamilyMember.getRestoredReason()));
            }
        }
        add(block);
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

    private HorizontalLayout getHLWithSpans(String title, String data) {
        HorizontalLayout header = new HorizontalLayout();
        header.setSpacing(true);
        header.setPadding(false);
        header.setWidthFull();

        Span titleSpan = new Span(title);
        Span dataSpan = new Span(data);
        dataSpan.getStyle().set("font-weight", "bold");
        header.add(titleSpan, dataSpan);
        return header;
    }

    private HorizontalLayout getHL(Span titleSpan, Span dataSpan) {
        HorizontalLayout header = new HorizontalLayout();
        header.setSpacing(true);
        header.setPadding(false);
        header.setWidthFull();
        dataSpan.getStyle().set("font-weight", "bold");
        header.add(titleSpan, dataSpan);
        return header;
    }
}