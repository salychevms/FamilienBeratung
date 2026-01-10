package com.salychevms.familienberatung.ui.view;

import com.salychevms.familienberatung.model.*;
import com.salychevms.familienberatung.service.*;
import com.salychevms.familienberatung.ui.layout.MainLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Slf4j
@Route(value = "consultations/:familyId?", layout = MainLayout.class)
@PageTitle("Beratungen")
@RequiredArgsConstructor
@PermitAll
public class ConsultationsView extends VerticalLayout implements BeforeEnterObserver {
    private final AuthService authService;
    private final FamilyService familyService;
    private final EmployeeService employeeService;
    private final ConsultationService consultationService;
    private final DelegationService delegationService;

    private Long familyId;
    private Employee currentEmployee;
    private int lvl;
    private List<Delegation> currentDelegations = new ArrayList<>();
    private List<Consultation> currentConsultations = new ArrayList<>();
    private List<Employee> currentEmployees = new ArrayList<>();
    private boolean initialized = false;
    private LocalDate dateFrom = null;
    private LocalDate dateTo = null;

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Employee e = authService.getCurrentEmployee();
        if (e == null || e.isArchived() || !e.isActive()) {
            event.forwardTo("login");
            return;
        }

        this.currentEmployee = employeeService.findByLogin(e.getLogin());
        this.lvl = currentEmployee.getRole().getAccessLevel();

        Optional<Long> fam = event.getRouteParameters().getLong("familyId");
        this.familyId = fam.orElse(null);

        this.currentDelegations.clear();
        if (lvl == 50) this.currentDelegations = delegationService.getDelegationsByToEmployee(currentEmployee);
        else this.currentDelegations = delegationService.getDelegations();

        this.currentConsultations.clear();
        if (lvl == 50) {
            List<Consultation> cList = consultationService.getAll();
            for (Consultation c : cList) {
                if ((c.getEmployee().equals(currentEmployee)
                        || c.getFamily().getAssignedEmployee().equals(currentEmployee))
                        && (!c.isInvalid() && !c.getFamily().getStatus().equals(RecordStatus.INVALID)))
                    this.currentConsultations.add(c);
            }
        } else this.currentConsultations = consultationService.getAll();

        this.currentEmployees.clear();
        for (Consultation c : currentConsultations) {
            if (!currentEmployees.contains(c.getEmployee()))
                this.currentEmployees.add(c.getEmployee());
        }

        if (!initialized) {
            initialized = true;
            buildUI();
        }
    }

    private void buildUI() {
        buildBreadCrumbs();
    }

    private void buildBreadCrumbs() {
        HorizontalLayout bcrumbs = new HorizontalLayout();
        bcrumbs.setSpacing(true);
        bcrumbs.setPadding(true);
        bcrumbs.setAlignItems(Alignment.CENTER);

        Span separator = new Span(" >> ");
        separator.getStyle().set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)");
        RouterLink root = new RouterLink("Übersicht", OverviewView.class);
        bcrumbs.add(root, separator);

        if (familyId != null) {
            RouterLink families = new RouterLink("Familien", FamiliesView.class);

            Span separator1 = new Span(" >> ");
            separator1.getStyle().set("font-size", "var(--lumo-font-size-s)")
                    .set("color", "var(--lumo-secondary-text-color)");
            Family f = familyService.getFamilyById(familyId);
            RouterLink family = new RouterLink("Familie: " + f.getFamilyName(), FamilyDetailsView.class,
                    new RouteParameters("id", familyId.toString()));

            Span separator2 = new Span(" >> ");
            separator2.getStyle().set("font-size", "var(--lumo-font-size-s)")
                    .set("color", "var(--lumo-secondary-text-color)");

            bcrumbs.add(families, separator1, family, separator2);
        }

        Span current = new Span("Beratungen");
        current.getStyle().set("font-size", "var(--lumo-font-size-s)").set("font-weight", "bold")
                .set("color", "var(--lumo-body-text-color)");

        bcrumbs.add(current);
        add(bcrumbs);
    }

    private String formatDuration(int durationMinutes) {
        if (durationMinutes < 0) return "00:00";
        int hours = durationMinutes / 60;
        int minutes = durationMinutes % 60;

        return String.format("%02d:%02d", hours, minutes);
    }

    private boolean matchesSearch(Consultation c, String query) {
        if (query == null || query.isBlank()) return true;

        String q = query.toLowerCase().trim();
        if (c.getFamily() != null && c.getFamily().getFamilyName() != null
                && c.getFamily().getFamilyName().toLowerCase().contains(q)) return true;
        if (c.getTopic() != null && c.getTopic().toLowerCase().contains(q)) return true;
        if (c.getDescription() != null && c.getDescription().toLowerCase().contains(q)) return true;
        return c.getResult() != null && c.getResult().toLowerCase().contains(q);
    }

    private boolean isInDateRange(Consultation c) {
        if (dateFrom == null && dateTo == null) return true;
        if (c.getDateTime() == null) return false;

        LocalDate d = c.getDateTime().toLocalDate();

        if (dateFrom != null && d.isBefore(dateFrom)) return false;
        if (dateTo != null && d.isAfter(dateTo)) return false;

        return !d.isAfter(LocalDate.now());
    }

    private boolean isDelegatedAt(Consultation c) {
        if (currentDelegations == null || currentDelegations.isEmpty()) return false;
        if (c.getFamily() == null || c.getDateTime() == null) return false;

        LocalDate d = c.getDateTime().toLocalDate();

        for (Delegation del : currentDelegations) {
            if (del.getFamily() == null) continue;
            if (!del.getFamily().getId().equals(c.getFamily().getId())) continue;

            LocalDate start = del.getStartDate();
            LocalDate end = del.getEndDate();

            if ((start == null || !d.isBefore(start)) && (end == null || !d.isAfter(end))) return true;
        }
        return false;
    }

    private List<Consultation> applyFilters(Employee employeeFilter, String query) {
        List<Consultation> result = new ArrayList<>();
        for (Consultation c : currentConsultations) {
            if (familyId != null && !c.getFamily().getId().equals(familyId)) continue;
            if (employeeFilter != null && !c.getEmployee().equals(employeeFilter)) continue;
            if (!isInDateRange(c)) continue;
            if (!matchesSearch(c, query)) continue;
            result.add(c);
        }
        return result;
    }

    private void applySort(List<Consultation> cList, String sortKey) {
        if (sortKey == null) return;

        switch (sortKey) {
            case "DATE_DESC" -> cList.sort((a, b) -> b.getDateTime().compareTo(a.getDateTime()));
            case "DATE_ASC" -> cList.sort(Comparator.comparing(Consultation::getDateTime));
            case "FAMILY_ASC" -> cList.sort((a, b) ->
                    a.getFamily().getFamilyName().compareToIgnoreCase(b.getFamily().getFamilyName()));
            case "FAMILY_DESC" -> cList.sort((a, b) ->
                    b.getFamily().getFamilyName().compareToIgnoreCase(a.getFamily().getFamilyName()));
            case "EMPLOYEE_ASC" -> cList.sort((a, b) ->
                    a.getFamily().getAssignedEmployee().getLastName()
                            .compareToIgnoreCase(b.getFamily().getAssignedEmployee().getLastName()));
            case "EMPLOYEE_DESC" -> cList.sort((a, b) ->
                    b.getFamily().getAssignedEmployee().getLastName()
                            .compareToIgnoreCase(a.getFamily().getAssignedEmployee().getLastName()));
        }
    }
}