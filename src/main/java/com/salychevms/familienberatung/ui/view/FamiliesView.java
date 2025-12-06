package com.salychevms.familienberatung.ui.view;

import com.salychevms.familienberatung.model.Employee;
import com.salychevms.familienberatung.model.Family;
import com.salychevms.familienberatung.model.RecordStatus;
import com.salychevms.familienberatung.service.*;
import com.salychevms.familienberatung.ui.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Route(value = "families", layout = MainLayout.class)
@PageTitle("Familien")
@RequiredArgsConstructor
public class FamiliesView extends VerticalLayout implements BeforeEnterObserver {

    private final AuthService authService;
    private final FamilyService familyService;
    private final DelegationService delegationService;
    private final ConsultationService consultationService;
    private final EmployeeService employeeService;

    private Employee currentEmployee;
    private int accessLevel;
    private List<Family> families;

    private Span breadcrumbs;

    private TextField searchField;
    private Button searchButton;
    private Button resetButton;
    private Button filterToggleButton;
    private Button createButton;
    private Button trashButton;

    private Button listModeButton;
    private Button tilesModeButton;

    private ComboBox<RecordStatus> statusFilter;
    private ComboBox<String> closedFilter;
    private ComboBox<String> delegatedFilter;
    private ComboBox<Employee> employeeFilter;
    private ComboBox<String> sortFilter;
    private VerticalLayout filterLayout;

    private Grid<Family> familyGrid;

    private boolean filterVisible = false;
    private boolean initialized=false;

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        Employee employee=authService.getCurrentEmployee();
        if(employee==null){
            beforeEnterEvent.forwardTo("login");
            return;
        }
        this.currentEmployee=employee;
        this.accessLevel=employee.getRole().getAccessLevel();

        if(!initialized){
            initialized=true;
            buildUI();
        }
    }

    private void buildUI() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        buildBreadcrumbs();
        buildTopBar();
        buildFilters();
        buildGrid();
        loadFamilies();
    }

    private void buildBreadcrumbs() {
        HorizontalLayout bcrumbs = new HorizontalLayout();
        bcrumbs.setSpacing(false);
        bcrumbs.setPadding(false);
        bcrumbs.setAlignItems(Alignment.CENTER);

        RouterLink root = new RouterLink("Übersicht", OverviewView.class);
        root.getStyle().set("font-size", "var(--lumo-font-size-s)")
                .set("colot", "var(--lumo-secondary-text-color)");

        Span separator = new Span(" >> ");
        separator.getStyle().set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)");

        Span current = new Span("Familien");
        current.getStyle().set("font-size", "var(--lumo-font-size-s)")
                .set("font-weight", "bold")
                .set("color", "var(--lumo-body-text-color)");

        bcrumbs.add(root, separator, current);
        add(bcrumbs);
        this.breadcrumbs = current;
    }

    private void buildTopBar() {
        HorizontalLayout top = new HorizontalLayout();
        top.setWidthFull();
        top.setAlignItems(Alignment.CENTER);
        top.setJustifyContentMode(JustifyContentMode.BETWEEN);

        HorizontalLayout left = new HorizontalLayout();
        left.setSpacing(true);
        left.setAlignItems(Alignment.CENTER);

        searchField = new TextField();
        searchField.setPlaceholder("Suche...");
        searchField.setClearButtonVisible(true);
        searchField.setWidth("250px");

        searchButton = new Button("Finden", e -> applyFilters());

        resetButton = new Button("Reset", e -> {
            searchField.clear();
            statusFilter.clear();
            closedFilter.clear();
            sortFilter.clear();
            if (employeeFilter != null) employeeFilter.clear();
            filterVisible = false;
            filterLayout.setVisible(false);
            applyFilters();
        });

        filterToggleButton = new Button("Filter öffnen", e -> {
            filterVisible = !filterVisible;
            filterLayout.setVisible(filterVisible);
            filterToggleButton.setText(filterVisible ? "Filter schließen" : "Filter öffnen");
        });

        left.add(searchField, searchButton, resetButton, filterToggleButton);

        HorizontalLayout right = new HorizontalLayout();
        right.setSpacing(true);
        right.setAlignItems(Alignment.CENTER);

        createButton = new Button("Neue Familie");
        trashButton = new Button("Papierkorb");
        listModeButton = new Button("Liste");
        tilesModeButton = new Button("Kacheln");

        if (accessLevel == 100) {
            createButton.setVisible(true);
            trashButton.setVisible(true);
            listModeButton.setVisible(true);
            tilesModeButton.setVisible(true);
        }
        if (accessLevel == 80 || accessLevel == 50) {
            createButton.setVisible(true);
            trashButton.setVisible(true);
            listModeButton.setVisible(false);
            tilesModeButton.setVisible(false);
        }
        if (accessLevel == 10) {
            createButton.setVisible(false);
            trashButton.setVisible(false);
            listModeButton.setVisible(false);
            tilesModeButton.setVisible(false);
        }

        right.add(createButton, trashButton, listModeButton, tilesModeButton);
        top.add(left, right);
        add(top);
    }

    private void buildFilters() {
        filterLayout = new VerticalLayout();
        filterLayout.setWidthFull();
        filterLayout.setSpacing(true);
        filterLayout.setPadding(true);
        filterLayout.getStyle().set("background-color", "#fff3cd")
                .set("border", "1px solid #e0c97f")
                .set("border-radius", "6px");
        filterLayout.setVisible(false);

        Span statusLabel = new Span("Status:");
        statusLabel.getStyle().set("font-weight", "bold");

        ComboBox<RecordStatus> statusFilter = new ComboBox<>();
        statusFilter.setItems(RecordStatus.values());
        statusFilter.setPlaceholder("Status wählen...");
        statusFilter.setClearButtonVisible(true);
        this.statusFilter = statusFilter;

        Span closeLabel = new Span("Case closed:");
        closeLabel.getStyle().set("font-weight", "bold");

        ComboBox<String> closedFilter = new ComboBox<>();
        closedFilter.setItems("Ja", "Nein", "Alle");
        closedFilter.setPlaceholder("Alle");
        closedFilter.setClearButtonVisible(true);
        this.closedFilter = closedFilter;

        Span delegatedLabel = new Span("Nur delegierte:");
        delegatedLabel.getStyle().set("font-weight", "bold");

        ComboBox<String> delegatedFilter = new ComboBox<>();
        delegatedFilter.setItems("Ja", "Nein", "Alle");
        delegatedFilter.setPlaceholder("Alle");
        delegatedFilter.setClearButtonVisible(true);
        this.delegatedFilter = delegatedFilter;

        if (accessLevel != 50) {
            Span employeeLabel = new Span("Berater*in:");
            employeeLabel.getStyle().set("font-weight", "bold");
            ComboBox<Employee> employeeFilter = new ComboBox<>();
            employeeFilter.setItems(employeeService.findAll());
            employeeFilter.setItemLabelGenerator(emp -> emp.getFirstName() + " " + emp.getLastName());
            employeeFilter.setPlaceholder("Mitarbeiter*in wählen:");
            employeeFilter.setClearButtonVisible(true);
            this.employeeFilter = employeeFilter;

            filterLayout.add(employeeLabel, employeeFilter);
        }

        Span sortLabel = new Span("Sortierung:");
        sortLabel.getStyle().set("font-weight", "bold");

        ComboBox<String> sortFilter = new ComboBox<>();
        sortFilter.setItems("Name A-Z", "Name Z-A", "Neu zuerst", "Alt zuerst", "Nach Berater*in", "Nach Status");
        sortFilter.setPlaceholder("Sortierung wählen");
        sortFilter.setClearButtonVisible(true);
        this.sortFilter = sortFilter;

        filterLayout.add(statusLabel, statusFilter,
                closeLabel, closedFilter,
                delegatedLabel, delegatedFilter,
                sortLabel, sortFilter);
        add(filterLayout);
    }

    private void buildGrid() {
        familyGrid = new Grid<>(Family.class, false);
        familyGrid.setWidthFull();
        familyGrid.setHeight("650px");
        familyGrid.addColumn(Family::getId).setHeader("ID").setWidth("70px").setFlexGrow(0);
        familyGrid.addColumn(f -> f.getFamilyName()).setHeader("Familienname").setAutoWidth(true);
        familyGrid.addColumn(f -> f.getAssignedEmployee() != null
                        ? f.getAssignedEmployee().getFirstName() + " " + f.getAssignedEmployee().getLastName() : "-")
                .setHeader("Berater*in").setAutoWidth(true);
        familyGrid.addColumn(f -> f.getStatus().name()).setHeader("Status").setAutoWidth(true);
        familyGrid.addColumn(f -> delegationService.isDelegated(f, currentEmployee) ? "Ja" : "Nein")
                .setHeader("Delegiert").setAutoWidth(true);
        familyGrid.addItemClickListener(e -> {
            Family family = e.getItem();
            getUI().ifPresent(ui -> ui.navigate(FamilyDetailsView.class, family.getId()));
        });
        add(familyGrid);
    }

    private void loadFamilies() {
    }

    private void applyFilters() {
    }
}
