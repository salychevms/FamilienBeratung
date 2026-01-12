package com.salychevms.familienberatung.ui.view;

import com.salychevms.familienberatung.model.Delegation;
import com.salychevms.familienberatung.model.Employee;
import com.salychevms.familienberatung.model.Family;
import com.salychevms.familienberatung.model.RecordStatus;
import com.salychevms.familienberatung.service.*;
import com.salychevms.familienberatung.ui.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.VaadinRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
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

    private List<Family> allFamilies;
    private List<Delegation> delegations;
    private List<Family> myOwnedFamilies;
    private boolean filterVisible = false;
    private boolean initialized = false;

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        Employee employee = authService.getCurrentEmployee();
        if (employee == null || !employee.isActive() || employee.isArchived()) {
            beforeEnterEvent.forwardTo("login");
            return;
        }
        this.currentEmployee = employee;
        this.accessLevel = employee.getRole().getAccessLevel();

        int lvl = currentEmployee.getRole().getAccessLevel();
        delegations = new ArrayList<>();
        myOwnedFamilies = new ArrayList<>();
        allFamilies = new ArrayList<>();
        if (lvl == 50) {
            myOwnedFamilies = familyService.getFamiliesByAssignedEmployee(currentEmployee.getLogin(), currentEmployee);
            allFamilies.addAll(myOwnedFamilies);
            delegations = delegationService.getDelegationsByToEmployee(currentEmployee);
            for (Delegation d : delegations)
                if (!d.isExpired() && d.getEndDate().isAfter(LocalDate.now())) allFamilies.add(d.getFamily());
        } else if (lvl == 100 || lvl == 80 || lvl == 10) allFamilies = familyService.getFamilies();

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
        buildFilters();
        buildTopBar();
        buildGrid();
        loadFamilies();
    }

    private void buildBreadcrumbs() {
        HorizontalLayout bcrumbs = new HorizontalLayout();
        bcrumbs.setSpacing(true);
        bcrumbs.setPadding(true);
        bcrumbs.setAlignItems(Alignment.CENTER);

        RouterLink root = new RouterLink("Übersicht", OverviewView.class);

        Span separator = new Span(" >> ");
        separator.getStyle().set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)");

        Span current = new Span("Familien");
        current.getStyle().set("font-size", "var(--lumo-font-size-s)")
                .set("font-weight", "bold")
                .set("color", "var(--lumo-body-text-color)");

        bcrumbs.add(root, separator, current);
        add(bcrumbs);
    }

    private void buildTopBar() {
        HorizontalLayout searchRow = new HorizontalLayout();
        searchRow.setSpacing(false);
        searchRow.setWidthFull();
        searchRow.setAlignItems(Alignment.CENTER);
        searchRow.setSpacing(true);

        searchField = new TextField();
        searchField.setPlaceholder("Suche...");
        searchField.setClearButtonVisible(true);
        searchField.setWidth("250px");

        searchButton = new Button(VaadinIcon.SEARCH.create(), e -> applyFilters());
        resetButton = new Button(VaadinIcon.REFRESH.create(), e -> {
            searchField.clear();
            statusFilter.clear();
            employeeFilter.clear();
            delegatedFilter.clear();
            filterVisible = false;
            filterLayout.setVisible(false);
            filterToggleButton.setText("Filter öffnen");
            applyFilters();
        });

        filterToggleButton = new Button("Filter öffnen", e -> {
            filterVisible = !filterVisible;
            filterLayout.setVisible(filterVisible);
            filterToggleButton.setText(filterVisible ? "Filter schließen" : "Filter öffnen");
        });

        searchRow.add(searchField, searchButton, resetButton, filterToggleButton);

        HorizontalLayout actionsRow = new HorizontalLayout();
        actionsRow.setSpacing(true);
        actionsRow.setAlignItems(Alignment.CENTER);

        createButton = new Button("Neue Familie", VaadinIcon.PLUS.create());
        createButton.getStyle().set("font-size", "16px").set("padding", "8px 16px");
        createButton.addClickListener(e -> openCreateFamilyDialog());

        trashButton = new Button("Papierkorb", VaadinIcon.TRASH.create());
        trashButton.setWidth("90px");
        trashButton.setHeight("26px");
        trashButton.getStyle().set("font-size", "11px").set("padding", "2px 6px")
                .set("color", "#666").set("background", "#f2f2f2");
        trashButton.addClickListener(e -> openTrashDialog());

        if (accessLevel == 10) {
            createButton.setVisible(false);
            trashButton.setVisible(false);
        }

        actionsRow.add(createButton, trashButton);

        HorizontalLayout modeRow = new HorizontalLayout();
        modeRow.setSpacing(true);

        listModeButton = new Button("≡");
        listModeButton.setWidth("20px");
        listModeButton.setHeight("20px");
        listModeButton.getStyle().set("font-size", "13px").set("padding", "0").set("min-width", "20px")
                .set("flex-grow", "0").set("flex-shrink", "0").set("box-sizing", "border-box");

        tilesModeButton = new Button("▦");
        tilesModeButton.setWidth("20px");
        tilesModeButton.setHeight("20px");
        tilesModeButton.getStyle().set("font-size", "13px").set("padding", "0").set("min-width", "20px")
                .set("flex-grow", "0").set("flex-shrink", "0").set("box-sizing", "border-box");

        modeRow.setVisible(accessLevel == 100);

        modeRow.add(listModeButton, tilesModeButton);

        add(searchRow);
        add(filterLayout);
        add(actionsRow);
        add(modeRow);
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
        statusFilter.setItems(RecordStatus.ACTIVE, RecordStatus.ARCHIVED, RecordStatus.BLOCKED);
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

        Span delegatedLabel = new Span("Delegiert:");
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
        familyGrid.setHeight("100%");
        familyGrid.addColumn(Family::getId).setHeader("ID").setWidth("70px").setFlexGrow(0);
        familyGrid.addColumn(new ComponentRenderer<>(f -> {
            String zeusId = f.getZeusId();
            Span span = new Span();
            if (zeusId == null || zeusId.isBlank()) {
                span.setText("kein");
                span.getStyle().set("color", "red").set("font-weight", "bold");
            } else span.setText(zeusId);
            return span;
        })).setHeader("Zeus ID").setAutoWidth(true).setFlexGrow(0);
        familyGrid.addColumn(Family::getFamilyName).setHeader("Familienname").setAutoWidth(true).setFlexGrow(0);
        familyGrid.addColumn(f -> f.isCaseClosed() ? "geschlossen" : "öffen").setHeader("Ablauf").setAutoWidth(true);
        familyGrid.addColumn(f -> f.getAssignedEmployee() != null
                        ? f.getAssignedEmployee().getFirstName() + " " + f.getAssignedEmployee().getLastName() : "-")
                .setHeader("Berater*in").setAutoWidth(true).setFlexGrow(0);
        familyGrid.addColumn(new ComponentRenderer<>(f -> {
            RecordStatus status = f.getStatus();
            Span span = new Span();
            if (status == RecordStatus.ARCHIVED) {
                span.setText("ARCHIV");
                span.getStyle().set("color", "#b58900").set("font-weight", "bold");
            } else if (status == RecordStatus.BLOCKED) {
                span.setText("BLOCKIET");
                span.getStyle().set("color", "red").set("font-weight", "bold");
            } else if (status == RecordStatus.ACTIVE) {
                span.setText("AKTIV");
                span.getStyle().set("color", "green").set("font-weight", "bold");
            }
            return span;
        })).setHeader("Status").setAutoWidth(true).getFlexGrow();
        familyGrid.addColumn(consultationService::getFamilyConsultationsCount)
                .setHeader("Beratungen").setAutoWidth(true);
        familyGrid.addColumn(f -> {
            int minutes = consultationService.getDurationTimeMinutesForFamilyCount(f);
            return String.format("%d:%02d", minutes / 60, minutes % 60);
        }).setHeader("Gesamt St.").setAutoWidth(true);
        familyGrid.addColumn(f -> isDelegated(f) ? "Ja" : "")
                .setHeader("Delegiert").setAutoWidth(true);
        familyGrid.addItemClickListener(e -> {
            Family family = e.getItem();
            getUI().ifPresent(ui -> ui.navigate(FamilyDetailsView.class,
                    new RouteParameters("id", family.getId().toString())));
        });
        add(familyGrid);
    }

    private void loadFamilies() {
        int lvl = accessLevel;
        List<Family> result = new ArrayList<>();
        if (lvl == 100 || lvl == 80 || lvl == 10) {
            List<Family> all = familyService.getFamilies();
            for (Family f : all) {
                if (!f.getStatus().equals(RecordStatus.INVALID)) result.add(f);
            }
            familyGrid.setItems(result);
            return;
        }

        if (lvl == 50) {
            List<Family> mine = familyService.getFamiliesByAssignedEmployee(currentEmployee.getLogin(), currentEmployee);
            for (Family f : mine) {
                if (!f.getStatus().equals(RecordStatus.INVALID)) result.add(f);
            }
            result.addAll(delegationService.getDelegatedFamilies(currentEmployee.getLogin()));
            familyGrid.setItems(result);
        }
    }

    private void applyFilters() {
        List<Family> filtered = new ArrayList<>();
        for (Family f : allFamilies) {
            if (!f.getStatus().equals(RecordStatus.INVALID)) filtered.add(f);
        }

        Employee employee = employeeFilter.getValue();
        RecordStatus status = statusFilter.getValue();
        String caseClosed = closedFilter.getValue();
        String onlyDelegated = delegatedFilter.getValue();
        String sort = sortFilter.getValue();

        String search = searchField.getValue();

        if (employee != null) filtered = filtered.stream().filter(f -> f.getAssignedEmployee() != null &&
                f.getAssignedEmployee().getId().equals(employee.getId())).toList();

        if (status != null && !status.equals("Alle")) filtered = filtered.stream().filter(f ->
                f.getStatus() == status).toList();

        if (caseClosed != null && !caseClosed.equals("Alle")) {
            boolean closed = caseClosed.equals("Ja");
            filtered = filtered.stream().filter(f -> f.isCaseClosed() == closed).toList();
        }

        if (onlyDelegated != null && !onlyDelegated.equals("Alle")) {
            if (onlyDelegated.equals("Ja")) filtered = filtered.stream().filter(this::isDelegated).toList();
            if (onlyDelegated.equals("Nein")) filtered = filtered.stream().filter(f -> !isDelegated(f)).toList();
        }

        if (search != null && !search.isBlank()) {
            String q = search.toLowerCase();

            filtered = filtered.stream().filter(f -> (f.getFamilyName() != null
                    && f.getFamilyName().toLowerCase().contains(q)) ||
                    (f.getZeusId() != null && f.getZeusId().toLowerCase().contains(q)) ||
                    (f.getCitizenship() != null && f.getCitizenship().toLowerCase().contains(q)) ||
                    (f.getEmail() != null && f.getEmail().toLowerCase().contains(q)) ||
                    (f.getPhone() != null && f.getPhone().toLowerCase().contains(q)) ||
                    (f.getReasonDescription() != null && f.getReasonDescription().toLowerCase().contains(q)) ||
                    (f.getLanguages() != null && f.getLanguages().toLowerCase().contains(q))).toList();

        }

        if (sort != null && !sort.isEmpty()) {
            switch (sort) {
                case "Name A-Z" -> filtered.sort(Comparator.comparing(Family::getFamilyName,
                        Comparator.nullsLast(String::compareToIgnoreCase)));
                case "Name Z-A" -> filtered.sort(Comparator.comparing(Family::getFamilyName,
                        Comparator.nullsLast(String::compareToIgnoreCase)).reversed());
                case "Neu zuerst" -> filtered.sort(Comparator.comparing(Family::getCreatedAt,
                        Comparator.nullsLast(LocalDateTime::compareTo)).reversed());
                case "Alt zuerst" -> filtered.sort(Comparator.comparing(Family::getCaseClosedAt,
                        Comparator.nullsLast(LocalDateTime::compareTo)));
                case "Nach Berater*in" -> filtered.sort(Comparator.comparing(f -> {
                    if (f.getAssignedEmployee() != null) return "";
                    return f.getAssignedEmployee().getLastName();
                }, Comparator.nullsLast(String::compareToIgnoreCase)));
                case "Nach Status" -> filtered.sort(Comparator.comparing(f -> f.getStatus().name()));
            }
        }
        familyGrid.setItems(filtered);
    }

    private void openTrashDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Papierkorb: Familien");

        List<Family> trashFamilies = loadTrashFamilies();

        Grid<Family> trashGrid = new Grid<>(Family.class, false);
        trashGrid.setWidthFull();
        trashGrid.setHeight("400px");

        trashGrid.addColumn(Family::getId).setHeader("ID").setWidth("70px").setFlexGrow(0);
        trashGrid.addColumn(new ComponentRenderer<>(f -> {
            String zeusId = f.getZeusId();
            Span span = new Span();
            if (zeusId == null || zeusId.isBlank()) {
                span.setText("kein");
                span.getStyle().set("color", "red").set("font-weight", "bold");
            } else span.setText(zeusId);
            return span;
        })).setHeader("Zeus ID").setAutoWidth(true).setFlexGrow(0);
        trashGrid.addColumn(Family::getFamilyName).setHeader("Familienname").setAutoWidth(true).setFlexGrow(0);
        trashGrid.addColumn(f -> f.isCaseClosed() ? "Schluss" : "läuft").setHeader("Ablauf").setAutoWidth(true);
        trashGrid.addColumn(f -> f.getAssignedEmployee() != null
                        ? f.getAssignedEmployee().getFirstName() + " " + f.getAssignedEmployee().getLastName() : "-")
                .setHeader("Berater*in").setAutoWidth(true).setFlexGrow(0);
        trashGrid.addColumn(new ComponentRenderer<>(f -> {
            Span span = new Span();
            span.setText(f.getStatus().name());
            span.getStyle().set("color", "grey").set("font-weight", "bold");
            return span;
        })).setHeader("Status").setAutoWidth(true).getFlexGrow();
        trashGrid.addColumn(f->{
            Employee e=employeeService.findByLogin(f.getInvalidBy());
            return e.getFirstName()+" "+e.getLastName();
        }).setHeader("Gelöscht von").setAutoWidth(true).setFlexGrow(0);
        trashGrid.addColumn(f -> {
            if (f.getInvalidAt() != null) {
                LocalDateTime dt = f.getInvalidAt();
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
                return dt.format(fmt) + " Uhr";
            }
            return "";
        }).setHeader("Gelöscht am").setAutoWidth(true).setFlexGrow(0);
        if (currentEmployee.getRole().getAccessLevel() == 50) {
            trashGrid.addColumn(new ComponentRenderer<>(f -> {
                Span span = new Span();
                if (f.getInvalidAt() != null) {
                    long days = Duration.between(f.getInvalidAt(), LocalDateTime.now()).toDays();
                    int left = 14 - (int) days;
                    span = new Span(String.valueOf(left));
                    if (left <= 4) {
                        span.getStyle().set("color", "red").set("font-weight", "bold");
                    } else {
                        span.getStyle().set("color", "black").set("font-weight", "bold");
                    }
                } else {
                    span.setText("kA");
                    span.getStyle().set("color", "red").set("font-weight", "bold");
                }
                return span;
            })).setHeader("Noch verfügbar").setAutoWidth(true).setFlexGrow(0);
        }

        trashGrid.setItems(trashFamilies);

        Button restoreButton = new Button("Wiederherstellen");
        restoreButton.setEnabled(false);

        trashGrid.asSingleSelect().addValueChangeListener(e ->
                restoreButton.setEnabled(e.getValue() != null));

        restoreButton.addClickListener(e -> {
            Family selected = trashGrid.asSingleSelect().getValue();
            if (selected == null) return;

            LocalDateTime invalidAt = selected.getInvalidAt();
            boolean olderThan14 = false;
            if (invalidAt != null) {
                long days = Duration.between(invalidAt, LocalDateTime.now()).toDays();
                olderThan14 = days > 14;
            }
            int lvl = currentEmployee.getRole().getAccessLevel();

            Dialog confirm = new Dialog();
            confirm.setHeaderTitle("Familie wiederherstellen");

            VerticalLayout content = new VerticalLayout();
            Span msg = new Span("Wollen Sie die Familie\n \"" + selected.getFamilyName() + "\" \nwiederherstellen?");
            content.add(msg);

            TextArea reason = null;
            if ((lvl == 80 || lvl == 100) && olderThan14) {
                reason = new TextArea("Begründung");
                reason.setWidthFull();
                reason.setMaxLength(255);
                content.add(reason);
            }

            TextArea finalReason = reason;

            Button ok = new Button("Bestätigen", ev -> {
                String r;
                if (finalReason != null) {
                    r = finalReason.getValue();
                    if (r == null || r.isBlank()) {
                        Notification.show("Bitte Begründung eingeben.", 3000, Notification.Position.MIDDLE);
                        return;
                    }
                } else {
                    r = "Wiederhergestellt";
                }
                try {
                    VaadinRequest req = VaadinRequest.getCurrent();
                    String ip = req != null ? req.getRemoteAddr() : "UNKNOWN";
                    String browser = req != null ? req.getHeader("User-Agent") : "UNKNOWN";

                    familyService.restoreFamily(selected.getId(), currentEmployee.getLogin(), r, ip, browser);
                    trashGrid.setItems(loadTrashFamilies());
                    confirm.close();
                    Notification.show("Familie wurde wiederhergestellt", 3000, Notification.Position.MIDDLE);
                    getUI().ifPresent(ui -> ui.getPage().reload());
                } catch (Exception ex) {
                    confirm.close();
                    Notification.show("Fehler" + ex.getMessage(), 3000, Notification.Position.MIDDLE);
                }
            });

            Button cancel = new Button("Abbrechen", ev -> confirm.close());
            HorizontalLayout btns = new HorizontalLayout(ok, cancel);
            btns.setJustifyContentMode(JustifyContentMode.END);

            confirm.add(content, btns);
            confirm.setWidth("600px");
            confirm.open();
        });

        Button close = new Button("Schließen", ev -> dialog.close());

        HorizontalLayout bottom = new HorizontalLayout(restoreButton, close);
        bottom.setJustifyContentMode(JustifyContentMode.END);

        dialog.add(new VerticalLayout(trashGrid, bottom));
        dialog.setWidth("1100px");
        dialog.setHeight("550px");
        dialog.setModal(true);
        dialog.setDraggable(false);
        dialog.setResizable(false);
        dialog.setCloseOnOutsideClick(false);
        dialog.open();
    }

    private void openCreateFamilyDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Neue Famile");
        dialog.setModal(true);
        dialog.setResizable(false);
        dialog.setDraggable(false);
        dialog.setCloseOnOutsideClick(false);
        dialog.setWidth("800px");

        TextField familyName = new TextField("Familienname (*)");
        familyName.setWidthFull();
        familyName.setRequiredIndicatorVisible(true);

        TextField street = new TextField("Straße");
        street.setWidthFull();

        TextField houseNumber = new TextField("Hausnummer");
        houseNumber.setWidthFull();

        TextField zip = new TextField("PLZ");
        zip.setWidthFull();

        TextField city = new TextField("Stadt");
        city.setWidthFull();

        TextField phone = new TextField("Telefonnummer");
        phone.setWidthFull();

        TextField email = new TextField("E-Mail");
        email.setWidthFull();

        TextField citizenship = new TextField("Staatsangehörigkeit");
        citizenship.setWidthFull();

        TextField languages = new TextField("Sprachen");
        languages.setWidthFull();

        TextArea reason = new TextArea("Grund der Beratung");
        reason.setWidthFull();

        TextArea notes = new TextArea("Interne Notizen");
        notes.setWidthFull();

        TextField zeusId = new TextField("Zeus ID");
        zeusId.setWidthFull();

        final Employee[] assignedEmployeeFinal = new Employee[1];
        final ComboBox<Employee>[] assignedCombo = new ComboBox[1];
        final TextField[] assignedDisplay = new TextField[1];
        final Span[] noRights = new Span[1];

        List<Employee> employees = employeeService.findAll().stream().filter(Employee::isActive)
                .filter(e -> !e.isArchived()).toList();

        if (accessLevel == 80 || accessLevel == 100) {
            assignedCombo[0] = new ComboBox<>("Berater*in");
            assignedCombo[0].setWidthFull();
            assignedCombo[0].setItems(employees);
            assignedCombo[0].setItemLabelGenerator(emp ->
                    emp.getFirstName() + " " + emp.getLastName() + " - " + emp.getRole().getLabel());
            assignedCombo[0].setPlaceholder("Bitte wählen...");
            assignedCombo[0].setWidthFull();
        } else if (accessLevel == 50) {
            assignedEmployeeFinal[0] = currentEmployee;

            assignedDisplay[0] = new TextField("Zuständige/r Berater*in");
            assignedDisplay[0].setValue(currentEmployee.getFirstName() + " " + currentEmployee.getLastName());
            assignedDisplay[0].setReadOnly(true);
            assignedDisplay[0].setWidthFull();
            assignedDisplay[0].getStyle().set("background-color", "#f2f2f2").set("color", "#666");

            noRights[0] = new Span("Keine Bearbeitungsrechte");
            noRights[0].getStyle().set("font-size", "11px").set("color", "#888");
        }

        Span empTitle = new Span("Zuständige/r Berater*in");
        empTitle.getStyle().set("font-weight", "bold");

        VerticalLayout employeeBlock = new VerticalLayout();
        employeeBlock.setSpacing(false);
        employeeBlock.setPadding(true);
        employeeBlock.getStyle().set("background-color", "#fafafa").set("border", "1px solid #ddd").
                set("border-radius", "6px").set("padding", "10px");

        employeeBlock.add(empTitle);

        if (assignedCombo[0] != null) employeeBlock.add(assignedCombo[0]);
        else employeeBlock.add(assignedDisplay[0], noRights[0]);

        Span mainTitle = new Span("Allgemeine Angaben");
        mainTitle.getStyle().set("font-weight", "bold");

        VerticalLayout mainBlock = new VerticalLayout();
        mainBlock.setSpacing(false);
        mainBlock.setPadding(true);
        mainBlock.getStyle().set("border", "1px solid #ddd").set("padding", "10px").set("border-radius", "6px");

        if (accessLevel == 50) {
            zeusId.setReadOnly(true);
            zeusId.getStyle().set("background-color", "#f2f2f2").set("color", "#666");
        }

        mainBlock.add(mainTitle, familyName, zeusId);

        Span contactTitle = new Span("Kontaktdaten");
        contactTitle.getStyle().set("font-weight", "bold");

        VerticalLayout contactBlock = new VerticalLayout();
        contactBlock.setSpacing(false);
        contactBlock.setPadding(true);
        contactBlock.getStyle().set("border", "1px solid #ddd").set("padding", "10px").set("border-radius", "6px");

        contactBlock.add(contactTitle, phone, email);

        Span addressTitle = new Span("Adresse");
        addressTitle.getStyle().set("font-weight", "bold");

        HorizontalLayout streetRow = new HorizontalLayout(street, houseNumber);
        streetRow.setWidthFull();
        street.setWidth("70%");
        houseNumber.setWidth("30%");

        HorizontalLayout cityRow = new HorizontalLayout(zip, city);
        cityRow.setWidthFull();
        city.setWidth("70%");
        zip.setWidth("30%");

        VerticalLayout addressBlock = new VerticalLayout();
        addressBlock.setSpacing(false);
        addressBlock.setPadding(true);
        addressBlock.getStyle().set("border", "1px solid #ddd").set("padding", "10px").set("border-radius", "6px");

        addressBlock.add(addressTitle, streetRow, cityRow);

        Span detailsTitle = new Span("Weitere Angaben");
        detailsTitle.getStyle().set("font-weight", "bold");

        VerticalLayout detailsBlock = new VerticalLayout();
        detailsBlock.setSpacing(false);
        detailsBlock.setPadding(true);
        detailsBlock.getStyle().set("border", "1px solid #ddd").set("padding", "10px").set("border-radius", "6px");

        detailsBlock.add(detailsTitle, citizenship, languages, reason, notes);

        VerticalLayout content = new VerticalLayout(employeeBlock, mainBlock, contactBlock, addressBlock, detailsBlock);
        content.setWidthFull();

        dialog.add(content);

        Button save = new Button("Speichern");
        Button cancel = new Button("Abbrechen");

        save.addClickListener(e -> {
            List<String> errors = new ArrayList<>();

            if (familyName.isEmpty()) {
                errors.add("Familienname ist erforderlich.");
                familyName.getStyle().set("border", "1px solid red");
            } else familyName.getStyle().remove("border");

            final Employee[] finalEmp = new Employee[1];

            if (accessLevel == 80 || accessLevel == 100) {
                if (assignedCombo[0].getValue() == null) {
                    errors.add("Berater*in muss ausgewählt werden.");
                    assignedCombo[0].getStyle().set("border", "1px solid red");
                } else {
                    assignedCombo[0].getStyle().remove("border");
                    finalEmp[0] = assignedCombo[0].getValue();
                }
            } else {
                finalEmp[0] = assignedEmployeeFinal[0];
            }

            if (!errors.isEmpty()) {
                Notification.show(String.join("\n", errors), 4000, Notification.Position.MIDDLE);
                return;
            }

            if (phone.isEmpty() && email.isEmpty()) {
                Dialog askContact = new Dialog();
                askContact.setHeaderTitle("Kontaktdaten fehlen");

                Span msg = new Span("Möchten Sie Kontaktinformationen ergänzen?");
                askContact.add(msg);

                Button yes = new Button("Ja", f -> askContact.close());
                Button no = new Button("Nein", f -> {
                    askContact.close();
                    showCreateConfirmDialog(dialog, zeusId.getValue(), familyName.getValue(), street.getValue(),
                            houseNumber.getValue(), zip.getValue(), city.getValue(), phone.getValue(), email.getValue(),
                            citizenship.getValue(), languages.getValue(), reason.getValue(), notes.getValue(), finalEmp[0]);
                });
                askContact.getFooter().add(yes, no);
                askContact.open();
                return;
            }
            showCreateConfirmDialog(dialog, zeusId.getValue(), familyName.getValue(), street.getValue(),
                    houseNumber.getValue(), zip.getValue(), city.getValue(), phone.getValue(), email.getValue(),
                    citizenship.getValue(), languages.getValue(), reason.getValue(), notes.getValue(), finalEmp[0]);
        });
        HorizontalLayout buttons = new HorizontalLayout(save, cancel);
        buttons.setWidthFull();
        buttons.setJustifyContentMode(JustifyContentMode.END);

        cancel.addClickListener(e -> dialog.close());
        dialog.getFooter().add(buttons);
        dialog.open();
    }

    private void showCreateConfirmDialog(Dialog parent, String zeusId, String familyName, String street,
                                         String houseNumber, String zip, String city, String phone, String email,
                                         String citizenship, String languages, String reason, String notes, Employee emp) {
        Dialog d = new Dialog();
        d.setHeaderTitle("Bestätigen");

        Button yes = new Button("Ja", e -> {
            d.close();
            parent.close();

            VaadinRequest req = VaadinRequest.getCurrent();
            String ip = req != null ? req.getRemoteAddr() : "UNKNOWN";
            String browser = req != null ? req.getHeader("User-Agent") : "UNKNOWN";

            familyService.createFamily(zeusId, familyName, street, houseNumber, zip, city, phone, email, citizenship,
                    languages, reason, notes, emp, currentEmployee.getLogin(), ip, browser);
            showSuccessDialog(familyName);
            loadFamilies();
        });

        Button no = new Button("Nein", e -> d.close());

        d.add(new Span("Familie speichern?"), new HorizontalLayout(yes, no));
        d.open();
    }

    private void showSuccessDialog(String familyName) {
        Dialog d = new Dialog();
        d.setHeaderTitle("Gespeichert");
        d.add(new Span("Familie \"" + familyName + "\" wurde erstellt."));
        d.add(new Button("Ok", e -> d.close()));
        d.open();
    }

    private boolean isDelegated(Family f) {
        if (delegations.isEmpty()) return false;
        return delegations.stream().anyMatch(d -> d.getFamily().getId().equals(f.getId())
                && !d.isExpired() && d.getEndDate().isAfter(LocalDate.now()));
    }

    private List<Family> loadTrashFamilies() {
        String login = currentEmployee.getLogin();
        int lvl = currentEmployee.getRole().getAccessLevel();

        List<Family> source;
        List<Family> result = new ArrayList<>();

        if (lvl == 100 || lvl == 80) {
            source = familyService.getFamilies();
            for (Family f : source)
                if (f.getStatus().equals(RecordStatus.INVALID)) result.add(f);
        } else if (lvl == 50) {
            source = familyService.getFamiliesByAssignedEmployee(login, currentEmployee);
            for (Family f : source)
                if (f.getStatus().equals(RecordStatus.INVALID) &&
                        !f.getInvalidAt().isAfter(LocalDateTime.now().plusDays(14))) result.add(f);
        } else return List.of();
        return result;
    }
}