package com.salychevms.familienberatung.ui.view;

import com.salychevms.familienberatung.model.Delegation;
import com.salychevms.familienberatung.model.Employee;
import com.salychevms.familienberatung.model.Member;
import com.salychevms.familienberatung.enums.RecordStatus;
import com.salychevms.familienberatung.service.AuthService;
import com.salychevms.familienberatung.service.DelegationService;
import com.salychevms.familienberatung.service.EmployeeService;
import com.salychevms.familienberatung.service.MemberService;
import com.salychevms.familienberatung.ui.dialog.EditDialogFactory;
import com.salychevms.familienberatung.ui.dialog.EditField;
import com.salychevms.familienberatung.ui.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Route(value = "delegations/:familyId?", layout = MainLayout.class)
@PageTitle("Delegationen")
@PermitAll
@RequiredArgsConstructor
public class DelegationsView extends VerticalLayout implements BeforeEnterObserver {
    private final DelegationService delegationService;
    private final EmployeeService employeeService;
    private final MemberService memberService;
    private final AuthService authService;

    private Employee currentEmployee;
    private int lvl;
    private Employee currentDelegatedTo;
    private List<Delegation> currentDelegations;
    private List<Delegation> currentActiveDelegations;
    private TextField searchField;
    private Button filterToggleButton;
    private HorizontalLayout filterLayout;
    private boolean filterVisible = false;
    private List<Employee> currentToEmployees = new ArrayList<>();
    private List<Employee> currentFromEmployees = new ArrayList<>();
    private List<Member> currentFamilies = new ArrayList<>();
    private List<Member> currentDelegatedFamilies = new ArrayList<>();
    private List<Employee> currentConsultants = new ArrayList<>();
    private ComboBox<Employee> toEmployeeFilter;
    private ComboBox<Employee> fromEmployeeFilter;
    private ComboBox<Member> familyFilter;
    private ComboBox<String> isActiveFilter;
    private LocalDate fromDateFilter;
    private LocalDate toDateFilter;
    private Button addDelegationButton;
    private Employee selectedToEmployee;
    private Member selectedMember;
    private Employee selectedFromEmployee;
    private Grid<Delegation> delegationGrid;
    private Member currentMember;
    private DatePicker fromDate;
    private DatePicker toDate;

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Employee authorized = authService.getCurrentEmployee();
        if (authorized == null || authorized.isArchived() || !authorized.isActive()) {
            event.forwardTo("login");
            return;
        }

        Employee employee = employeeService.findByLogin(authorized.getLogin());
        if (employee == null) {
            event.forwardTo("login");
            return;
        } else this.currentEmployee = employee;

        lvl = currentEmployee.getRole().getAccessLevel();
        Employee toEmployee = employeeService.findByLogin(currentEmployee.getLogin());
        if (toEmployee == null || !toEmployee.isActive() || toEmployee.isArchived())
            this.currentDelegatedTo = null;
        else
            this.currentDelegatedTo = toEmployee;

        Long optFamId = event.getRouteParameters().getLong("familyId").orElse(null);
        if (optFamId != null) {
            this.currentMember = memberService.getMemberById(optFamId);
        }else this.currentMember =null;

        currentToEmployees.clear();
        currentFromEmployees.clear();
        currentDelegatedFamilies.clear();
        if (lvl != 50) {
            this.currentDelegations = delegationService.getDelegations().stream().sorted((a, b) -> {
                boolean aActive = delegationService.isDelegationActive(a);
                boolean bActive = delegationService.isDelegationActive(b);

                if (aActive && !bActive) return -1;
                if (bActive && !aActive) return 1;

                return a.getEndDate().compareTo(b.getEndDate());
            }).toList();
            this.currentActiveDelegations = delegationService.getAllActiveDelegations();
            this.currentFamilies = new ArrayList<>(memberService.getMembers().stream()
                    .filter(f -> f.getStatus().equals(RecordStatus.ACTIVE) && !f.isCaseClosed()
                            && !delegationService.hasActiveDelegation(f)).toList());
            this.currentConsultants = new ArrayList<>(employeeService.findAll().stream()
                    .filter(emp -> emp.getRole().getAccessLevel() != 10).toList());
        } else {
            this.currentDelegations = delegationService.getDelegationsByToEmployee(currentEmployee)
                    .stream().sorted((a, b) -> {
                        boolean aActive = delegationService.isDelegationActive(a);
                        boolean bActive = delegationService.isDelegationActive(b);

                        if (aActive && !bActive) return -1;
                        if (bActive && !aActive) return 1;

                        return a.getEndDate().compareTo(b.getEndDate());
                    }).toList();
            this.currentActiveDelegations = delegationService.getAllActiveDelegationsByToEmployee(currentEmployee);
            this.currentFamilies = new ArrayList<>(memberService.getMembersByAssignedEmployee(
                            currentEmployee.getLogin(), currentEmployee).stream()
                    .filter(f -> f.getStatus().equals(RecordStatus.ACTIVE) && !f.isCaseClosed()
                            && !delegationService.hasActiveDelegation(f)).toList());
            this.currentConsultants = new ArrayList<>(employeeService.findAll().stream()
                    .filter(emp -> emp.getRole().getAccessLevel() == 50
                            && emp.isActive() && !emp.isArchived()).toList());
        }

        for (Delegation d : currentDelegations) {
            if (d.getToEmployee().isActive() && !d.getToEmployee().isArchived()
                    && !currentToEmployees.contains(d.getToEmployee()))
                this.currentToEmployees.add(d.getToEmployee());
            if (d.getFromEmployee().isActive() && !d.getFromEmployee().isArchived()
                    && !currentFromEmployees.contains(d.getFromEmployee()))
                this.currentFromEmployees.add(d.getFromEmployee());
            if (d.getMember().getStatus().equals(RecordStatus.ACTIVE) && !d.getMember().isCaseClosed()
                    && !currentDelegatedFamilies.contains(d.getMember()))
                this.currentDelegatedFamilies.add(d.getMember());
        }

        removeAll();
        buildUI();
    }

    private void buildUI() {
        buildBreadCrumbs();
        buildHeader();
        buildTopBar();
        buildFilters();
        if (lvl != 10)
            buildActionButtons();
        buildGrid();
    }

    private void buildBreadCrumbs() {
        HorizontalLayout breadCrumbs = new HorizontalLayout();
        breadCrumbs.setSpacing(true);
        breadCrumbs.setPadding(true);
        breadCrumbs.setAlignItems(FlexComponent.Alignment.CENTER);

        RouterLink overview = new RouterLink("Übersicht", OverviewView.class);
        breadCrumbs.add(overview);

        if (currentMember != null) {
            RouterLink fams=new RouterLink("Familien", MembersView.class);
            RouterLink fam = new RouterLink("Familie: "+ currentMember.getLastName(),
                    MemberView.class, new RouteParameters("id", currentMember.getId().toString()));
            Span sp2 = new Span(" >> ");
            sp2.getStyle().set("font-size", "var(--lumo-font-size-s)").set("color", "var(--lumo-secondary-text-color)");
            Span sp3 = new Span(" >> ");
            sp3.getStyle().set("font-size", "var(--lumo-font-size-s)").set("color", "var(--lumo-secondary-text-color)");

            breadCrumbs.add(sp2, fams, sp3, fam);
        }

        Span del = new Span("Delegationen");
        del.getStyle().set("font-size", "var(--lumo-font-size-s)").set("font-weight", "bold")
                .set("color", "var(--lumo-body-text-color)");
        Span sp1 = new Span(" >> ");
        sp1.getStyle().set("font-size", "var(--lumo-font-size-s)").set("color", "var(--lumo-secondary-text-color)");
        breadCrumbs.add(sp1, del);

        add(breadCrumbs);
    }

    private void buildHeader() {
        VerticalLayout header = new VerticalLayout();
        header.setSpacing(false);
        header.setPadding(false);
        header.setWidthFull();

        HorizontalLayout titleLayout = new HorizontalLayout();
        H2 title = new H2();

        if (lvl == 50)
            title.setText("Delegationen: " + currentEmployee.getFirstName() + " " + currentEmployee.getLastName());
        else title.setText("Delegationen: alle");
        titleLayout.add(title);
        header.add(titleLayout, getHLWithSpans(currentEmployee.getRole().getLabel() + ": ",
                currentEmployee.getFirstName() + " " + currentEmployee.getLastName()));
        add(header);
    }

    private void buildTopBar() {
        HorizontalLayout searchRow = new HorizontalLayout();
        searchRow.setSpacing(true);
        searchRow.setWidthFull();
        searchRow.setAlignItems(FlexComponent.Alignment.CENTER);

        searchField = new TextField();
        searchField.setPlaceholder("Suche...");
        searchField.setClearButtonVisible(true);
        searchField.setWidth("250px");

        Button searchButton = new Button(VaadinIcon.SEARCH.create(), e -> refreshGrid());
        Button resetButton = new Button(VaadinIcon.REFRESH.create(),
                e -> {
                    searchField.clear();
                    fromEmployeeFilter.clear();
                    toEmployeeFilter.clear();
                    familyFilter.clear();
                    isActiveFilter.clear();
                    fromDate.clear();
                    toDate.clear();
                    filterVisible = false;
                    filterLayout.setVisible(false);
                    filterToggleButton.setText("Filter öffnen");
                    refreshGrid();
                });

        filterToggleButton = new Button("Filter öffnen", e -> {
            filterVisible = !filterVisible;
            filterLayout.setVisible(filterVisible);
            filterToggleButton.setText(filterVisible ? "Filter schließen" : "Filter öffnen");
        });

        searchRow.add(searchField, searchButton, resetButton, filterToggleButton);
        add(searchRow);
    }

    private void buildFilters() {
        filterLayout = new HorizontalLayout();
        filterLayout.setSpacing(true);
        filterLayout.setPadding(true);
        filterLayout.setVisible(false);
        filterLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        filterLayout.getStyle().set("background-color", "#fff3cd").set("border-radius", "6px")
                .set("border", "1px solid #e0c96f");

        if (lvl != 50) {
            VerticalLayout toEmpLayout = new VerticalLayout();
            toEmpLayout.setSpacing(false);
            toEmpLayout.setPadding(false);

            Span toEmpLabel = new Span("An wem delegiert");
            toEmpLabel.getStyle().set("font-weight", "bold");

            toEmployeeFilter = new ComboBox<>();
            toEmployeeFilter.setItems(currentToEmployees);
            toEmployeeFilter.setItemLabelGenerator(emp -> emp.getFirstName() + " " + emp.getLastName());
            toEmployeeFilter.setClearButtonVisible(true);
            toEmployeeFilter.addValueChangeListener(e -> refreshGrid());

            toEmpLayout.add(toEmpLabel, toEmployeeFilter);
            filterLayout.add(toEmpLayout);
        }

        VerticalLayout fromEmpLayout = new VerticalLayout();
        fromEmpLayout.setSpacing(false);
        fromEmpLayout.setPadding(false);

        Span fromEmpLabel = new Span("Von wem delegiert");
        fromEmpLabel.getStyle().set("font-weight", "bold");

        fromEmployeeFilter = new ComboBox<>();
        fromEmployeeFilter.setItems(currentFromEmployees);
        fromEmployeeFilter.setItemLabelGenerator(emp -> emp.getFirstName() + " " + emp.getLastName());
        fromEmployeeFilter.setClearButtonVisible(true);
        fromEmployeeFilter.addValueChangeListener(e -> refreshGrid());

        fromEmpLayout.add(fromEmpLabel, fromEmployeeFilter);

        VerticalLayout familyLayout = new VerticalLayout();
        familyLayout.setSpacing(false);
        familyLayout.setPadding(false);

        Span familyLabel = new Span("Familie");
        familyLabel.getStyle().set("font-weight", "bold");

        familyFilter = new ComboBox<>();
        familyFilter.setItems(currentFamilies);
        familyFilter.setItemLabelGenerator(Member::getLastName);
        familyFilter.setClearButtonVisible(true);
        familyFilter.addValueChangeListener(e -> refreshGrid());

        familyLayout.add(familyLabel, familyFilter);

        VerticalLayout isActiveLayout = new VerticalLayout();
        isActiveLayout.setSpacing(false);
        isActiveLayout.setPadding(false);

        Span isActiveLabel = new Span("Status");
        isActiveLabel.getStyle().set("font-weight", "bold");

        isActiveFilter = new ComboBox<>();
        isActiveFilter.setItems("Aktiv", "Abgelaufen", "Alle");
        isActiveFilter.setPlaceholder("Alle");
        isActiveFilter.setClearButtonVisible(true);
        isActiveFilter.addValueChangeListener(e -> refreshGrid());

        isActiveLayout.add(isActiveLabel, isActiveFilter);

        VerticalLayout fromDateLayout = new VerticalLayout();
        fromDateLayout.setSpacing(false);
        fromDateLayout.setPadding(false);

        Span fromDateLabel = new Span("Beginn");
        fromDateLabel.getStyle().set("font-weight", "bold");

        fromDate = new DatePicker();
        fromDate.setMin(LocalDate.of(2026, 1, 1));
        fromDate.addValueChangeListener(ev -> {
            fromDateFilter = ev.getValue();
            refreshGrid();
        });

        fromDateLayout.add(fromDateLabel, fromDate);

        VerticalLayout toDateLayout = new VerticalLayout();
        toDateLayout.setSpacing(false);
        toDateLayout.setPadding(false);

        Span toDateLabel = new Span("Ende");
        toDateLabel.getStyle().set("font-weight", "bold");

        toDate = new DatePicker();
        toDate.setMin(LocalDate.of(2026, 1, 1));
        toDate.addValueChangeListener(ev -> {
            toDateFilter = ev.getValue();
            refreshGrid();
        });

        toDateLayout.add(toDateLabel, toDate);

        filterLayout.add(fromEmpLayout, familyLayout, isActiveLayout, fromDateLayout, toDateLayout);
        add(filterLayout);
    }

    private void buildActionButtons() {
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setSpacing(true);
        buttonLayout.setAlignItems(FlexComponent.Alignment.CENTER);

        addDelegationButton = new Button("Delegation", VaadinIcon.PLUS.create());
        addDelegationButton.getStyle().set("font-size", "16px").set("padding", "8px 16px");
        addDelegationButton.addClickListener(e -> {
            Dialog dlg = new Dialog();
            dlg.setModal(true);
            dlg.setResizable(false);
            dlg.setDraggable(false);
            dlg.setCloseOnOutsideClick(false);
            dlg.setWidth("800px");

            VerticalLayout dlgLayout = new VerticalLayout();
            dlgLayout.setSpacing(false);
            dlgLayout.setPadding(true);
            dlgLayout.getStyle().set("border", "1px solid #ddd").set("padding", "10px").set("border-radius", "6px");

            H2 title = new H2("Neue Delegation");
            dlgLayout.add(title);

            ComboBox<Member> familyComboBox = new ComboBox<>("Familie auswählen (*)");
            familyComboBox.setItems(currentFamilies);
            familyComboBox.setItemLabelGenerator(Member::getLastName);
            familyComboBox.setClearButtonVisible(true);
            familyComboBox.setPlaceholder("Bitte Familie wählen...");
            dlgLayout.add(familyComboBox);

            Span consultant = new Span("Familie noch nicht ausgewählt!");
            consultant.getStyle().set("font-weight", "bold").set("color", "red");
            dlgLayout.add(consultant);

            List<Employee> filteredConsultants = new ArrayList<>();
            ComboBox<Employee> toEmpComboBox = new ComboBox<>("An wem delegieren (*)");
            toEmpComboBox.setPlaceholder("Bitte Berater*in wählen...");
            familyComboBox.addValueChangeListener(ev -> {
                selectedMember = ev.getValue();
                filteredConsultants.clear();
                if (selectedMember == null) {
                    consultant.setText("Familie noch nicht ausgewählt!");
                    consultant.getStyle().set("color", "red").set("font-weight", "bold");
                    selectedFromEmployee = null;
                    return;
                }
                selectedFromEmployee = selectedMember.getAssignedEmployee();
                consultant.setText("Berater*in: " + selectedFromEmployee.getFirstName() + " "
                        + selectedFromEmployee.getLastName());
                consultant.getStyle().set("font-weight", "bold").set("color", "black");
                for (Employee emp : currentConsultants)
                    if (!emp.equals(selectedFromEmployee))
                        filteredConsultants.add(emp);
                toEmpComboBox.setItems(filteredConsultants);
            });

            if (lvl == 50)
                toEmpComboBox.setItemLabelGenerator(emp -> emp.getFirstName() + " " + emp.getLastName());
            else
                toEmpComboBox.setItemLabelGenerator(emp -> emp.getRole().getLabel() + ": "
                        + emp.getFirstName() + " " + emp.getLastName());
            toEmpComboBox.setClearButtonVisible(true);
            dlgLayout.add(toEmpComboBox);

            HorizontalLayout datesLayout = new HorizontalLayout();
            datesLayout.setSpacing(true);
            datesLayout.setPadding(true);
            datesLayout.setWidthFull();

            DatePicker startDatePicker = new DatePicker("Startdatum (*)");
            startDatePicker.setMin(LocalDate.now());
            startDatePicker.setWidthFull();
            startDatePicker.setRequiredIndicatorVisible(true);
            startDatePicker.setValue(LocalDate.now());

            DatePicker endDatePicker = new DatePicker("Enddatum (*)");
            endDatePicker.setMin(LocalDate.now());
            endDatePicker.setWidthFull();
            endDatePicker.setRequiredIndicatorVisible(true);
            endDatePicker.setValue(LocalDate.now());

            datesLayout.add(startDatePicker, endDatePicker);
            dlgLayout.add(datesLayout);

            TextArea reasonText = new TextArea("Grund der Delegation (*)");
            reasonText.setWidthFull();
            reasonText.setHeight("130px");
            reasonText.setMaxLength(2000);
            reasonText.setMinLength(5);
            dlgLayout.add(reasonText);

            Button cancel = new Button("Abbrechen", ev -> dlg.close());
            Button save = new Button("Speichern", ev -> {
                LocalDate now = LocalDate.now();
                LocalDate startDate = startDatePicker.getValue();
                LocalDate endDate = endDatePicker.getValue();
                if (endDate == null || startDate == null || endDate.isBefore(startDate) || endDate.isBefore(now)
                        || startDate.isBefore(now) || reasonText.getStyle() == null || reasonText.getValue().length() < 5
                        || reasonText.getValue().isBlank() || toEmpComboBox.getValue() == null || selectedMember == null) {
                    showOkDialog("Fehler", "Alle Felder mit Zeichen (*) müssen ausgefüllt werden!");

                } else {
                    VaadinRequest req = VaadinRequest.getCurrent();
                    Delegation create = new Delegation();
                    create.setMember(selectedMember);
                    create.setFromEmployee(selectedFromEmployee);
                    create.setToEmployee(toEmpComboBox.getValue());
                    create.setReason(reasonText.getValue());
                    create.setStartDate(startDate);
                    create.setEndDate(endDate);
                    try {
                        delegationService.createDelegation(create, currentEmployee.getLogin(),
                                req != null ? req.getRemoteAddr() : "UNKNOWN",
                                req != null ? req.getHeader("User-Agent") : "UNKNOWN");
                        dlg.close();
                        getUI().ifPresent(ui -> ui.getPage().reload());
                    } catch (Exception ex) {
                        showOkDialog("Fehler", ex.getMessage());
                    }
                }
            });

            HorizontalLayout btns = new HorizontalLayout(cancel, save);
            btns.setWidthFull();
            btns.setJustifyContentMode(JustifyContentMode.END);

            dlg.add(dlgLayout);
            dlg.getFooter().add(btns);
            dlg.open();
        });

        buttonLayout.add(addDelegationButton);
        add(buttonLayout);
    }

    private void buildGrid() {
        delegationGrid = new Grid<>(Delegation.class, false);
        delegationGrid.setWidthFull();
        delegationGrid.setSelectionMode(Grid.SelectionMode.SINGLE);
        delegationGrid.setItems(currentDelegations);

        delegationGrid.addColumn(Delegation::getId)
                .setHeader("ID").setAutoWidth(true).setFlexGrow(0)
                .setComparator(Delegation::getId);
        delegationGrid.addColumn(d -> d.getMember().getLastName())
                .setHeader("Familie").setAutoWidth(true).setFlexGrow(0)
                .setComparator(d -> d.getMember().getLastName());
        delegationGrid.addColumn(d -> d.getFromEmployee().getFirstName() + " "
                        + d.getFromEmployee().getLastName())
                .setHeader("Familie gehört").setAutoWidth(true).setFlexGrow(0)
                .setComparator(d -> d.getFromEmployee().getFirstName());
        delegationGrid.addColumn(d -> d.getToEmployee().getFirstName() + " "
                        + d.getToEmployee().getLastName())
                .setHeader("An wem delegiert").setAutoWidth(true).setFlexGrow(0)
                .setComparator(d -> d.getToEmployee().getFirstName());
        delegationGrid.addColumn(new ComponentRenderer<>(d -> {
                    boolean expired = d.isExpired();
                    Span expSpan = new Span();
                    if (expired) {
                        expSpan.setText("ABGELAUFEN");
                        expSpan.getStyle().set("color", "red").set("font-weight", "bold");
                    } else {
                        expSpan.setText("AKTIV");
                        expSpan.getStyle().set("color", "green").set("font-weight", "bold");
                    }
                    return expSpan;
                }))
                .setHeader("Status").setAutoWidth(true).setFlexGrow(0)
                .setComparator(Delegation::isExpired);
        delegationGrid.addColumn(d -> formatDate(d.getEndDate()))
                .setHeader("Enddatum").setAutoWidth(true).setFlexGrow(0)
                .setComparator(Delegation::getEndDate);
        delegationGrid.addColumn(new ComponentRenderer<>(d -> {
            HorizontalLayout actions = new HorizontalLayout();
            actions.setSpacing(true);
            actions.setPadding(false);

            Button viewButton = new Button(VaadinIcon.EYE.create());
            viewButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            viewButton.addClassName("edit-btn");
            viewButton.setTooltipText("Ansehen");
            viewButton.addClickListener(ev -> openViewDialog(d));

            Button editButton = new Button(VaadinIcon.EDIT.create());
            editButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            editButton.addClassName("edit-btn");
            editButton.setTooltipText("Bearbeiten");
            editButton.addClickListener(ev -> {
                EditDialogFactory.openEditDialog("Delegation bearbeiten", List.of(
                        new EditField("endDate", "Enddatum", EditField.Type.DATE, d.getEndDate(),
                                true, null, null, LocalDate.now(), null),
                        new EditField("reason", "Grund", EditField.Type.TEXTAREA, d.getReason(),
                                true, 2000, null, null, null)
                ), values -> {
                    LocalDate newEndDate = values.get("endDate") != null ? (LocalDate) values.get("endDate")
                            : d.getEndDate();

                    String newReason = values.get("reason") != null ? values.get("reason").toString().trim()
                            : d.getReason();

                    if (newReason.length() < 5 || newReason.length() > 2000) {
                        showOkDialog("Fehler", "Text muss zwischen 5 und 2000 Zeichen enthalten");
                        return;
                    }
                    if (newEndDate == null) {
                        showOkDialog("Fehler", "Der Enddatum ist falsch");
                        return;
                    }

                    showConfirmDialog("Speichern", "Wollen Sie die Änderungen speichern?", () -> {
                        if (!delegationService.isDelegationActive(d)) {
                            showOkDialog("Fehler", "Der Delegation ist nicht mehr aktiv!");
                            return;
                        }

                        VaadinRequest req = VaadinRequest.getCurrent();
                        try {
                            delegationService.updateDelegation(currentEmployee.getLogin(), d, newEndDate, newReason,
                                    req != null ? req.getRemoteAddr() : "UNKNOWN",
                                    req != null ? req.getHeader("User-Agent") : "UNKNOWN");
                            getUI().ifPresent(ui -> ui.getPage().reload());
                        } catch (Exception ex) {
                            showOkDialog("Fehler", ex.getMessage());
                        }
                    }, () -> {
                    });
                });
            });

            Button abortButton = new Button(VaadinIcon.CLOSE_CIRCLE.create());
            abortButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            abortButton.addClassName("edit-btn");
            abortButton.getStyle().set("color", "red");
            abortButton.setTooltipText("Vorzeitig beenden");
            abortButton.addClickListener(ev -> showConfirmDialog("Delegation vorzeitich beenden",
                    "Wollen Sie Delegation vorzeitich Beenden?", () -> {
                        VaadinRequest req = VaadinRequest.getCurrent();
                        delegationService.manualAbortDelegation(currentEmployee.getLogin(), d,
                                req != null ? req.getRemoteAddr() : "UNKNOWN",
                                req != null ? req.getHeader("User-Agent") : "UNKNOWN");
                    }, () -> {
                    }));

            boolean active = delegationService.isDelegationActive(d);
            boolean familyOk = d.getMember().getStatus().equals(RecordStatus.ACTIVE) && !d.getMember().isCaseClosed();
            boolean canEditAndAbort = active && familyOk && (lvl != 10);

            editButton.setVisible(canEditAndAbort);
            abortButton.setVisible(canEditAndAbort);

            actions.add(viewButton, editButton, abortButton);
            return actions;
        })).setHeader("Aktionen").setAutoWidth(true).setFlexGrow(0);
        add(delegationGrid);
        refreshGrid();
    }

    private void refreshGrid() {
        if (delegationGrid == null || currentDelegations == null) return;
        String q = searchField != null ? searchField.getValue() : null;
        Employee toEmp = toEmployeeFilter != null ? toEmployeeFilter.getValue() : null;
        Employee fromEmp = fromEmployeeFilter != null ? fromEmployeeFilter.getValue() : null;
        Member fam = familyFilter != null ? familyFilter.getValue() : null;
        String active = isActiveFilter != null ? isActiveFilter.getValue() : null;

        List<Delegation> result = new ArrayList<>();

        for (Delegation d : currentDelegations) {
            if (d == null) continue;
            if (toEmp != null && !d.getToEmployee().equals(toEmp)) continue;
            if (fromEmp != null && !d.getFromEmployee().equals(fromEmp)) continue;
            if (fam != null && !d.getMember().equals(fam)) continue;
            if ("Aktiv".equals(active) && d.isExpired()) continue;
            if ("Abgelaufen".equals(active) && !d.isExpired()) continue;
            if (fromDateFilter != null && d.getStartDate().isBefore(fromDateFilter)) continue;
            if (toDateFilter != null && d.getEndDate().isAfter(toDateFilter)) continue;
            if (q != null && !q.isBlank()) {
                String qq = q.toLowerCase().trim();
                boolean match = d.getMember().getLastName().toLowerCase().contains(qq)
                        || d.getFromEmployee().getFirstName().toLowerCase().contains(qq)
                        || d.getFromEmployee().getLastName().toLowerCase().contains(qq)
                        || d.getToEmployee().getFirstName().toLowerCase().contains(qq)
                        || d.getToEmployee().getLastName().toLowerCase().contains(qq);
                if (!match) continue;
            }
            result.add(d);
        }
        delegationGrid.setItems(result);
    }

    private void openViewDialog(Delegation d) {
        Dialog dlg = new Dialog();
        dlg.setHeaderTitle("Delegation ansehen");
        dlg.setModal(true);
        dlg.setDraggable(false);
        dlg.setResizable(false);
        dlg.setCloseOnOutsideClick(false);
        dlg.setWidth("450px");

        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(true);
        content.setMaxWidth("400px");

        content.add(new Span("Familie: " + d.getMember().getLastName()),
                new Span("Delegiert von: " + d.getFromEmployee().getFirstName() + " "
                        + d.getFromEmployee().getLastName()),
                new Span("Delegiert an: " + d.getToEmployee().getFirstName() + " "
                        + d.getToEmployee().getLastName()),
                new Span("Zeitraum: " + formatDate(d.getStartDate()) + " - " +
                        formatDate(d.getEndDate())),
                new Span("Status: " + (delegationService.isDelegationActive(d) ? "AKTIV" : "ABGESCHLOSSEN")));

        TextArea reason = new TextArea("Grund der Delegation");
        reason.setReadOnly(true);
        reason.setWidthFull();
        reason.setMaxHeight("150px");
        reason.setValue(d.getReason());
        content.add(reason);

        VerticalLayout audit = new VerticalLayout();
        audit.setSpacing(false);
        audit.setPadding(false);
        audit.setWidthFull();
        audit.getStyle().set("border", "1px solid #ddd").set("border-radius", "6px").set("padding", "10px");

        Span title = new Span("Verlauf");
        title.getStyle().set("font-weight", "bold");
        audit.add(title);

        if (d.getCreatedAt() != null) audit.add(makeAuditLine("Erstellt am "
                + formatDate(d.getCreatedAt().toLocalDate()) + " um " + formatTime(d.getCreatedAt()) + " von "
                + empty(d.getCreatedBy())));
        if (d.getUpdatedAt() != null) audit.add(makeAuditLine("Geändert am "
                + formatDate(d.getUpdatedAt().toLocalDate()) + " um " + formatTime(d.getUpdatedAt()) + " von "
                + empty(d.getUpdatedBy())));
        if (d.getAbortedAt() != null) audit.add(makeAuditLine("Beendet am "
                + formatDate(d.getAbortedAt().toLocalDate()) + " um " + empty(d.getAbortedBy())
                + empty(d.getAbortedBy())));
        content.add(audit);

        Button close = new Button("Schließen", e -> dlg.close());
        dlg.add(content);
        dlg.getFooter().add(close);
        dlg.open();
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

    private String formatDate(LocalDate date) {
        if (date == null) return "";
        return date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
    }

    private String formatTime(LocalDateTime dt) {
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        return dt.format(timeFormatter);
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
