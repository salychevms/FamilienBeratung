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
@Route(value = "member/:id/:memberId", layout = MainLayout.class)
@PageTitle("ESF-Teilnehmer*in")
@RequiredArgsConstructor
@PermitAll
public class MemberEsfView extends VerticalLayout implements BeforeEnterObserver {

    private final AuthService authService;
    private final EmployeeService employeeService;
    private final MemberService memberService;
    private final MemberEsfService memberEsfService;

    Employee currentEmployee;
    int lvl;
    Long familyId;
    Long memberEsfId;
    Member currentMember;
    MemberEsf currentMemberEsf;

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

        Optional<Long> optMemberEsfId = event.getRouteParameters().getLong("id");
        Optional<Long> optFamilyId = event.getRouteParameters().getLong("familyId");

        if (optMemberEsfId.isEmpty() || optFamilyId.isEmpty()) {
            event.forwardTo("families");
            return;
        }

        this.memberEsfId = optMemberEsfId.get();
        this.familyId = optFamilyId.get();

        try {
            this.currentMember = memberService.getMemberById(familyId);
        } catch (Exception ex) {
            event.forwardTo("family/" + familyId);
            return;
        }

        if (currentMember.getStatus().equals(RecordStatus.INVALID)) {
            event.forwardTo("families");
            return;
        }

        try {
            this.currentMemberEsf = memberEsfService.getMember(currentMember, memberEsfId, currentEmployee.getLogin());
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
        RouterLink l2 = new RouterLink("Alle Teilnehmer*innen", MembersView.class);
        RouterLink l3 = new RouterLink("Teilnehmer*in: " + currentMember.getLastName(), MemberView.class,
                new RouteParameters("id", String.valueOf(currentMember.getId())));

        Span sep1 = new Span(" >> ");
        Span sep2 = new Span(" >> ");
        Span sep3 = new Span(" >> ");

        Span member = new Span("ESF-Teilnehmer*in: " + currentMemberEsf.getId());

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

        if (lvl != 10 && currentMember.getStatus().equals(RecordStatus.ACTIVE) && !currentMember.isCaseClosed()) {
            Button editButton = new Button(VaadinIcon.EDIT.create());
            editButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            editButton.addClassName("edit-btn");
            header.add(editButton);
        }

        String fullName = currentMemberEsf.getId().toString();
        H2 title = new H2("ESF-Teilnehmer*in: " + fullName);
        header.add(title);

        Span fTitle = new Span("Teilnehmer*in: ");
        Span fName = new Span(currentMember.getFirstName() + " " + currentMember.getLastName());
        fName.getStyle().set("font-weight", "bold");
        Span fStatusTitle = new Span("Status: ");
        Span fStatusIs = new Span(currentMember.getStatus().toString());
        fStatusIs.getStyle().set("font-weight", "bold");

        Span mTitle = new Span("Mitglied ID: ");
        Span mId = new Span(String.valueOf(currentMemberEsf.getId()));
        mId.getStyle().set("font-weight", "bold");
        Span mStatusTitle = new Span("Status: ");
        Span mStatusIs = new Span();
        mStatusIs.getStyle().set("font-weight", "bold");
        if (!currentMemberEsf.isInvalid()) {
            if (currentMember.getStatus().equals(RecordStatus.ARCHIVED)) {
                fStatusIs.getStyle().set("color", "#b58900");
                mStatusIs.setText(RecordStatus.BLOCKED.toString());
                mStatusIs.getStyle().set("color", "red");
            } else if (currentMember.getStatus().equals(RecordStatus.BLOCKED)) {
                fStatusIs.getStyle().set("color", "red");
                mStatusIs.setText(RecordStatus.BLOCKED.toString());
                mStatusIs.getStyle().set("color", "red");
            } else if (currentMember.getStatus().equals(RecordStatus.ACTIVE)) {
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
        if (lvl != 10 && currentMember.getStatus().equals(RecordStatus.ACTIVE) && !currentMember.isCaseClosed()) {
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
                Span name = new Span(currentMemberEsf.getId().toString());
                name.getStyle().set("font-weight", "bold");
                okDialog.add(new VerticalLayout(text, name));

                Button ok = new Button("Ja", e -> {
                    VaadinRequest req = VaadinRequest.getCurrent();
                    memberEsfService.invalidateMember(currentMember, currentMemberEsf, currentEmployee.getLogin(),
                            req != null ? req.getRemoteAddr() : "UNKNOWN",
                            req != null ? req.getHeader("User-Agent") : "UNKNOWN");
                    getUI().ifPresent(ui -> ui.navigate(MemberView.class,
                            new RouteParameters("id", currentMember.getId().toString())));
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

        if (lvl != 10 && currentMember.getStatus().equals(RecordStatus.ACTIVE) && !currentMember.isCaseClosed()) {
            Button editButton = new Button(VaadinIcon.EDIT.create());
            editButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            editButton.addClassName("edit-btn");

            editButton.addClickListener(e -> EditDialogFactory.openEditDialog(
                    "Persönliche Daten ändern", List.of(
                            new EditField("gender", "Gender", EditField.Type.SELECT,
                                    currentMemberEsf.getGender(), true, null,
                                    List.of(MemberGender.values()), null, null),
                            new EditField("birthDate", "Geburtsdatum", EditField.Type.DATE,
                                    currentMemberEsf.getBirthDate(), true, null, null,
                                    LocalDate.of(1935, 1, 1), LocalDate.now()),
                            new EditField("birthCity", "Geburtsstadt", EditField.Type.TEXT,
                                    currentMemberEsf.getBirthCity(), true, 255, null,
                                    null, null),
                            new EditField("birthCountry", "Geburtsland", EditField.Type.TEXT,
                                    currentMemberEsf.getBirthCountry(), true, 255, null,
                                    null, null),
                            new EditField("nationality", "Nationalität", EditField.Type.TEXT,
                                    currentMemberEsf.getNationality(), false, 255, null,
                                    null, null),
                            new EditField("languages", "Sprachen", EditField.Type.TEXT,
                                    currentMemberEsf.getLanguages(), false, 255, null,
                                    null, null),
                            new EditField("livesWithFamily", "Lebt mit der Familie", EditField.Type.SELECT,
                                    currentMemberEsf.isLivesWithFamily(), true, null,
                                    List.of("Ja", "Nein"), null, null)),
                    values -> {
                        MemberEsf updated = memberEsfService.getMember(currentMember, currentMemberEsf.getId(),
                                currentEmployee.getLogin());

                        if (values.get("gender") != null)
                            updated.setGender((MemberGender) values.get("gender"));
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

                        memberEsfService.updateMember(currentMember, currentMemberEsf, updated,
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
        if (currentMemberEsf.getGender().equals(MemberGender.MAENNLICH))
            genderText = "männlich";
        else if (currentMemberEsf.getGender().equals(MemberGender.DIVERS))
            genderText = "divers";
        else
            genderText = "weiblich";

        String birthDateText = currentMemberEsf.getBirthDate() != null
                ? currentMemberEsf.getBirthDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                : "nicht angegeben";
        int age = (currentMemberEsf.getBirthDate() != null)
                ? Period.between(currentMemberEsf.getBirthDate(), LocalDate.now()).getYears() : -1;
        block.add(header, getHLWithSpans("Gender: ", genderText),
                getHLWithSpans("Alter: ", String.valueOf(age)),
                getHLWithSpans("Geburtsdatum: ", birthDateText),
                getHLWithSpans("Geburtsstadt: ", empty(currentMemberEsf.getBirthCity())),
                getHLWithSpans("Geburtsland: ", empty(currentMemberEsf.getBirthCountry())),
                getHLWithSpans("Nationalität/Staatsbürgerschaft: ", empty(currentMemberEsf.getNationality())),
                getHLWithSpans("Sprachen: ", empty(currentMemberEsf.getLanguages())),
                getHLWithSpans("lebt mit der Familie: ", (currentMemberEsf.isLivesWithFamily() ? "Ja" : "Nein")));
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

        if (lvl != 10 && currentMember.getStatus().equals(RecordStatus.ACTIVE) && !currentMember.isCaseClosed()) {
            Button editButton = new Button(VaadinIcon.EDIT.create());
            editButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            editButton.addClassName("edit-btn");

            editButton.addClickListener(e -> EditDialogFactory.openEditDialog("Kontaktdaten ändern",
                            List.of(
                                    new EditField("phone", "Telefon", EditField.Type.TEXT,
                                            currentMemberEsf.getPhone(), false, 255, null,
                                            null, null),
                                    new EditField("email", "E-Mail", EditField.Type.TEXT,
                                            currentMemberEsf.getEmail(), false, 255, null,
                                            null, null)
                            ), values -> {
                                MemberEsf updated = memberEsfService.getMember(currentMember,
                                        currentMemberEsf.getId(), currentEmployee.getLogin());

                                if (values.get("phone") != null)
                                    updated.setPhone((String) values.get("phone"));
                                if (values.get("email") != null)
                                    updated.setEmail((String) values.get("email"));

                                VaadinRequest req = VaadinRequest.getCurrent();

                                memberEsfService.updateMember(currentMember, currentMemberEsf, updated,
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

        block.add(header, getHLWithSpans("Telefon: ", empty(currentMemberEsf.getPhone())),
                getHLWithSpans("E-Mail: ", empty(currentMemberEsf.getEmail())));
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

        if (lvl != 10 && currentMember.getStatus().equals(RecordStatus.ACTIVE) && !currentMember.isCaseClosed()) {
            Button editButton = new Button(VaadinIcon.EDIT.create());
            editButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            editButton.addClassName("edit-btn");

            editButton.addClickListener(e ->
                    EditDialogFactory.openEditDialog("Beruf und Bildung ändern", List.of(
                                    new EditField("income", "Einkommen", EditField.Type.TEXT,
                                            currentMemberEsf.getIncome(), false, 255, null,
                                            null, null),
                                    new EditField("workInfo", "Berufliche Tätigkeit / Erfahrung",
                                            EditField.Type.TEXTAREA, currentMemberEsf.getWorkInfo(),
                                            false, 4000, null, null, null),
                                    new EditField("educationDegree", "Bildungsabschluss",
                                            EditField.Type.TEXT, currentMemberEsf.getEducationDegree(),
                                            false, 255, null, null, null),
                                    new EditField("educationInfo", "Ausbildung / Studium",
                                            EditField.Type.TEXTAREA, currentMemberEsf.getEducationInfo(),
                                            false, 4000, null, null, null)
                            ), values -> {
                                MemberEsf updated = memberEsfService.getMember(currentMember,
                                        currentMemberEsf.getId(), currentEmployee.getLogin());

                                if (values.get("income") != null)
                                    updated.setIncome((String) values.get("income"));
                                if (values.get("workInfo") != null)
                                    updated.setWorkInfo((String) values.get("workInfo"));
                                if (values.get("educationDegree") != null)
                                    updated.setEducationDegree((String) values.get("educationDegree"));
                                if (values.get("educationInfo") != null)
                                    updated.setEducationInfo((String) values.get("educationInfo"));

                                VaadinRequest req = VaadinRequest.getCurrent();

                                memberEsfService.updateMember(currentMember, currentMemberEsf, updated,
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
        workText.setValue(empty(currentMemberEsf.getWorkInfo()));
        workText.setReadOnly(true);
        workText.setMaxLength(4000);
        workText.setWidthFull();
        workText.setHeight("200px");
        workText.getStyle().set("white-space", "pre-wrap");

        workLayout.add(getHLWithSpans("Einkommen: ", empty(currentMemberEsf.getIncome())), workText);

        VerticalLayout eduLayout = new VerticalLayout();
        eduLayout.setSpacing(false);
        eduLayout.setPadding(false);
        eduLayout.setWidthFull();

        TextArea educationText = new TextArea("Ausbildung / Studium: ");
        educationText.setValue(empty(currentMemberEsf.getEducationInfo()));
        educationText.setReadOnly(true);
        educationText.setMaxLength(4000);
        educationText.setWidthFull();
        educationText.setHeight("200px");
        educationText.getStyle().set("white-space", "pre-wrap");

        eduLayout.add(getHLWithSpans("Bildungsabschluss: ",
                empty(currentMemberEsf.getEducationDegree())), educationText);

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

        if (lvl != 10 && currentMember.getStatus().equals(RecordStatus.ACTIVE) && !currentMember.isCaseClosed()) {
            Button editButton = new Button(VaadinIcon.EDIT.create());
            editButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            editButton.addClassName("edit-btn");

            editButton.addClickListener(e -> EditDialogFactory.openEditDialog("Notizen",
                            List.of(
                                    new EditField("notes", "Notizen", EditField.Type.TEXTAREA,
                                            currentMemberEsf.getNotes(), false, 1000, null,
                                            null, null)
                            ), values -> {
                                MemberEsf updated = memberEsfService.getMember(currentMember,
                                        currentMemberEsf.getId(), currentEmployee.getLogin());

                                if (values.get("notes") != null)
                                    updated.setNotes((String) values.get("notes"));


                                VaadinRequest req = VaadinRequest.getCurrent();

                                memberEsfService.updateMember(currentMember, currentMemberEsf, updated,
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
        notes.setValue(empty(currentMemberEsf.getNotes()));
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

        if (currentMemberEsf.getCreatedAt() != null) {
            block.add(makeAuditLine("Erstellt am " + currentMemberEsf.getCreatedAt().format(df) + " von " +
                    empty(currentMemberEsf.getCreatedBy())));
        }
        if (currentMemberEsf.getUpdatedAt() != null) {
            block.add(makeAuditLine("Geändert am " + currentMemberEsf.getUpdatedAt().format(df) + " von " +
                    empty(currentMemberEsf.getUpdatedBy())));
        }
        if (currentMemberEsf.getInvalidAt() != null) {
            block.add(makeAuditLine("Gelöscht am " + currentMemberEsf.getInvalidAt().format(df) + " von " +
                    empty(currentMemberEsf.getInvalidBy())));
        }
        if (currentMemberEsf.getRestoredAt() != null) {
            block.add(makeAuditLine("Wiederherstellt am " + currentMemberEsf.getRestoredAt().format(df) +
                    " von " + empty(currentMemberEsf.getRestoredBy())));
            if (currentMemberEsf.getRestoredReason() != null) {
                block.add(makeAuditLine("Grund: " + currentMemberEsf.getRestoredReason()));
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