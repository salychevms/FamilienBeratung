package com.salychevms.familienberatung.ui.view;

import com.salychevms.familienberatung.model.*;
import com.salychevms.familienberatung.service.*;
import com.salychevms.familienberatung.ui.dialog.EditDialogFactory;
import com.salychevms.familienberatung.ui.dialog.EditField;
import com.salychevms.familienberatung.ui.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.VaadinRequest;
import jakarta.annotation.security.PermitAll;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
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
    private final DelegationService delegationService;

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

        this.currentFamily = familyService.getFamilyById(familyId);

        if (currentFamily == null || currentFamily.getStatus().equals(RecordStatus.INVALID)) {
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
    }

    private void buildBreadCrumbs() {
        HorizontalLayout bcrumbs = new HorizontalLayout();
        bcrumbs.setSpacing(true);
        bcrumbs.setPadding(true);
        bcrumbs.setAlignItems(Alignment.CENTER);

        RouterLink l1 = new RouterLink("Übresicht", OverviewView.class);
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
        box.setPadding(true);
        box.setWidthFull();

        HorizontalLayout header = new HorizontalLayout();

        if (lvl != 10 && currentFamily.getStatus().equals(RecordStatus.ACTIVE) && !currentFamily.isCaseClosed()) {
            Button editButton = new Button(VaadinIcon.EDIT.create());
            editButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            editButton.addClassName("edit-btn");
            header.add(editButton);
        }

        String fullName = currentFamilyMember.getFirstName() + " " + currentFamilyMember.getLastName();
        H2 title = new H2("Familienmitglied: " + fullName);
        header.add(title);

        String fStatus;
        String status;
        if (currentFamily.getStatus().equals(RecordStatus.INVALID))
            fStatus = status = "GELÖSCHT";
        else if (currentFamily.getStatus().equals(RecordStatus.ARCHIVED))
            fStatus = status = "ARCHIV";
        else if (currentFamily.getStatus().equals(RecordStatus.BLOCKED))
            fStatus = status = "BLOCKIERT";
        else if (currentFamilyMember.isInvalid()) {
            fStatus = "AKTIV";
            status = "GELÖSCHT";
        } else
            fStatus = status = "AKTIV";

        String fName = "Familie: " + currentFamily.getFamilyName() + " | Status: " + fStatus;
        Span familyName = new Span(fName);

        String line = "Mitglied ID: " + currentFamilyMember.getId() + " | Status: " + status;
        Span info = new Span(line);

        box.add(header, familyName, info);
        add(box);
    }

    private void buildPersonalData() {
        VerticalLayout block = new VerticalLayout();
        block.setSpacing(false);
        block.setPadding(true);
        block.setWidthFull();

        block.getStyle().set("border", "1px solid #ddd").set("border-radius", "6px").set("padding", "10px");

        HorizontalLayout header = new HorizontalLayout();
        header.setAlignItems(Alignment.CENTER);
        header.setSpacing(true);

        if (lvl != 10 && currentFamily.getStatus().equals(RecordStatus.ACTIVE) && !currentFamily.isCaseClosed()) {
            Button editButton = new Button(VaadinIcon.EDIT.create());
            editButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            editButton.addClassName("edit-btn");

            editButton.addClickListener(e -> EditDialogFactory.openEditDialog(
                    "Persönliche Daten ändern", List.of(
                            new EditField("gender", "Gender", EditField.Type.SELECT,
                                    currentFamilyMember.getGender(), true, null,
                                    List.of(FamilyMemberGender.values())),
                            new EditField("birthDate", "Geburtsdatum", EditField.Type.DATE,
                                    currentFamilyMember.getBirthDate(), true, null, null),
                            new EditField("birthCity", "Geburtsstadt", EditField.Type.TEXT,
                                    currentFamilyMember.getBirthCity(), true, 255, null),
                            new EditField("birthCountry", "Geburtsland", EditField.Type.TEXT,
                                    currentFamilyMember.getBirthCountry(), true, 255, null)),
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

        String birthDateText = currentFamilyMember.getBirthDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));

        Span gender = new Span("Gender: " + genderText);
        Span birthDate = new Span("Geburtsdatum: " + birthDateText);
        Span birthCity = new Span("Geburtsstadt: " + currentFamilyMember.getBirthCity());
        Span birthCountry = new Span("Geburtsland: " + currentFamilyMember.getBirthCountry());

        block.add(header, gender, birthDate, birthCity, birthCountry);
        add(block);
    }
}