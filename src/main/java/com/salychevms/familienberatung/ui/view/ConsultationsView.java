package com.salychevms.familienberatung.ui.view;

import com.salychevms.familienberatung.model.*;
import com.salychevms.familienberatung.service.*;
import com.salychevms.familienberatung.ui.layout.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.*;
import jakarta.annotation.security.PermitAll;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
    private LocalDate dateFrom = null;
    private LocalDate dateTo = null;
    private VerticalLayout filterLayout;
    private boolean filterVisible = false;
    private Button searchButton;
    private Button resetButton;
    private Button filterToggleButton;
    private Button createButton;
    private Button trashButton;
    private TextField searchField;
    ComboBox<Employee> employeeFilter;
    private DatePicker fromDate;
    private DatePicker toDate;
    private ComboBox<String> sortFilter;
    private Grid<Consultation> consultationGrid;

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Employee e = authService.getCurrentEmployee();
        if (e == null || e.isArchived() || !e.isActive()) {
            event.forwardTo("login");
            return;
        }

        this.currentEmployee = employeeService.findByLogin(e.getLogin());
        this.lvl = currentEmployee.getRole().getAccessLevel();

        this.familyId = event.getRouteParameters().getLong("familyId").orElse(null);


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

        removeAll();
        buildUI();
    }

    private void buildUI() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        buildBreadCrumbs();
        buildTopBar();
        buildFilters();
        buildGrid();
        refreshGrid();
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

    private void buildTopBar() {
        HorizontalLayout searchRow = new HorizontalLayout();
        searchRow.setWidthFull();
        searchRow.setAlignItems(Alignment.CENTER);
        searchRow.setSpacing(true);

        searchField = new TextField();
        searchField.setPlaceholder("Suche...");
        searchField.setClearButtonVisible(true);
        searchField.setWidth("250px");

        searchButton = new Button(VaadinIcon.SEARCH.create(), e -> refreshGrid());
        resetButton = new Button(VaadinIcon.REFRESH.create(), e ->
                UI.getCurrent().navigate(ConsultationsView.class));

        filterToggleButton = new Button("Filter öffnen", e -> {
            filterVisible = !filterVisible;
            filterLayout.setVisible(filterVisible);
            filterToggleButton.setText(filterVisible ? "Filter schließen" : "Filter öffnen");
        });

        searchRow.add(searchField, searchButton, resetButton, filterToggleButton);

        HorizontalLayout actionsRow = new HorizontalLayout();
        actionsRow.setSpacing(true);
        actionsRow.setAlignItems(Alignment.CENTER);

        createButton = new Button("Beratung", VaadinIcon.PLUS.create());
        createButton.getStyle().set("font-size", "16px").set("padding", "8px 16px");

        trashButton = new Button("Papierkorb", VaadinIcon.TRASH.create());
        trashButton.setWidth("90px");
        trashButton.setHeight("26px");
        trashButton.getStyle().set("font-size", "11px").set("padding", "2px 6px")
                .set("color", "#666").set("background", "#f2f2f2");

        if (lvl == 10) {
            createButton.setVisible(false);
            trashButton.setVisible(false);
        }

        actionsRow.add(createButton, trashButton);

        add(searchRow, actionsRow);
    }

    private void buildFilters() {
        filterLayout = new VerticalLayout();
        filterLayout.setWidthFull();
        filterLayout.setSpacing(true);
        filterLayout.setPadding(true);
        filterLayout.setVisible(false);

        filterLayout.getStyle().set("background-color", "#fff3cd").set("border", "1px solid #e0c97f")
                .set("border-radius", "6px");

        employeeFilter = new ComboBox<>("Berater*in");
        employeeFilter.setItems(currentEmployees);
        employeeFilter.setItemLabelGenerator(e -> e.getFirstName() + " " + e.getLastName());
        employeeFilter.setClearButtonVisible(true);
        employeeFilter.addValueChangeListener(e -> refreshGrid());

        fromDate = new DatePicker("Von");
        fromDate.addValueChangeListener(e -> {
            dateFrom = e.getValue();
            refreshGrid();
        });

        toDate = new DatePicker("Bis");
        toDate.addValueChangeListener(e -> {
            dateTo = e.getValue();
            refreshGrid();
        });

        sortFilter = new ComboBox<>("Sortierung");
        sortFilter.setItems("Datum ↓", "Datum ↑", "Familie A-Z", "Familie Z-A", "Berater*in A-Z", "Berater*in Z-A");
        sortFilter.setClearButtonVisible(true);
        sortFilter.addValueChangeListener(e -> refreshGrid());

        filterLayout.add(employeeFilter, fromDate, toDate, sortFilter);
        add(filterLayout);
    }

    private void buildGrid() {
        consultationGrid = new Grid<>();
        consultationGrid.setSizeFull();
        consultationGrid.setSelectionMode(Grid.SelectionMode.SINGLE);

        consultationGrid.addColumn(Consultation::getId)
                .setHeader("ID").setAutoWidth(true);
        consultationGrid.addColumn(c -> c.getFamily().getFamilyName())
                .setHeader("Familie").setAutoWidth(true);
        consultationGrid.addColumn(c ->
                        c.getFamily().getAssignedEmployee().getFirstName() + " " +
                                c.getFamily().getAssignedEmployee().getLastName())
                .setHeader("Berater*in").setAutoWidth(true);
        consultationGrid.addColumn(c -> c.getEmployee().getFirstName() + " " + c.getEmployee().getLastName())
                .setHeader("Durchgeführt von").setAutoWidth(true);
        consultationGrid.addColumn(c -> formatDate(c.getDateTime().toLocalDate()))
                .setHeader("Datum").setAutoWidth(true);
        consultationGrid.addColumn(c -> formatDuration(c.getDurationMinutes()))
                .setHeader("Dauer St.").setAutoWidth(true);
        consultationGrid.addColumn(c -> isDelegatedAt(c) ? "Ja" : "Nein")
                .setHeader("Delegiert").setAutoWidth(true);
        consultationGrid.addItemClickListener(e -> {
            Consultation c = e.getItem();
            UI.getCurrent().navigate(ConsultationDetailsView.class,
                    new RouteParameters("id", c.getId().toString()));
        });

        add(consultationGrid);
    }

    private void refreshGrid() {
        List<Consultation> result = new ArrayList<>();

        String query = searchField != null ? searchField.getValue() : null;

        Employee emp = employeeFilter != null ? employeeFilter.getValue() : null;
        for (Consultation c : currentConsultations) {
            if (familyId != null && !c.getFamily().getId().equals(familyId)) continue;
            if (emp != null && !c.getEmployee().equals(emp)) continue;
            if (!isInDateRange(c)) continue;
            if (!matchesSearch(c, query)) continue;

            result.add(c);
        }

        String sort = sortFilter != null ? sortFilter.getValue() : null;
        if ("Datum ↓".equals(sort))
            result.sort((a, b) -> b.getDateTime().compareTo(a.getDateTime()));
        if ("Datum ↑".equals(sort)) result.sort(Comparator.comparing(Consultation::getDateTime));
        if ("Familie A-Z".equals(sort)) result.sort((a, b) -> a.getFamily().getFamilyName()
                .compareToIgnoreCase(b.getFamily().getFamilyName()));
        if ("Familie Z-A".equals(sort)) result.sort((a, b) -> b.getFamily().getFamilyName()
                .compareToIgnoreCase(a.getFamily().getFamilyName()));
        if ("Berater*in A-Z".equals(sort)) result.sort((a, b) -> a.getEmployee().getLastName()
                .compareToIgnoreCase(b.getEmployee().getLastName()));
        if ("Berater*in Z-A".equals(sort)) result.sort((a, b) -> b.getEmployee().getLastName()
                .compareToIgnoreCase(a.getEmployee().getLastName()));
        consultationGrid.setItems(result);
    }

    private void resetFilter() {
        if (searchField != null) searchField.clear();
        if (employeeFilter != null) employeeFilter.clear();
        if (fromDate != null) fromDate.clear();
        if (toDate != null) toDate.clear();
        if (sortFilter != null) sortFilter.clear();

        dateFrom = null;
        dateTo = null;
        familyId = null;

        filterVisible = false;
        if (filterLayout != null) filterLayout.setVisible(false);
        if (filterToggleButton != null) filterToggleButton.setText("Filter öffnen");
    }

    private String formatDuration(int durationMinutes) {
        if (durationMinutes < 0) return "00:00";
        int hours = durationMinutes / 60;
        int minutes = durationMinutes % 60;

        return String.format("%02d:%02d", hours, minutes);
    }

    private String formatDate(LocalDate date) {
        if (date == null) return "";
        return date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
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
}