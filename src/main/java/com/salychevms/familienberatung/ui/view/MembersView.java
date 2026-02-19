package com.salychevms.familienberatung.ui.view;

import com.salychevms.familienberatung.model.*;
import com.salychevms.familienberatung.service.*;
import com.salychevms.familienberatung.ui.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
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
import java.util.List;

@Slf4j
@Route(value = "members", layout = MainLayout.class)
@PageTitle("Alle Teilnehmer*inen")
@RequiredArgsConstructor
public class MembersView extends VerticalLayout implements BeforeEnterObserver {

    private final AuthService authService;
    private final MemberService memberService;
    private final DelegationService delegationService;
    private final ConsultationService consultationService;
    private final EmployeeService employeeService;

    private Employee currentEmployee;
    private List<Employee> currentEmployees;
    private int lvl;
    private TextField searchField;
    private Button filterToggleButton;
    private ComboBox<RecordStatus> statusFilter;
    private ComboBox<String> closedFilter;
    private ComboBox<String> delegatedFilter;
    private ComboBox<Employee> employeeFilter;
    private HorizontalLayout filterLayout;
    private Grid<Member> memberGrid;
    private List<Member> allMembers;
    private boolean filterVisible = false;
    private boolean initialized = false;

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        Employee employee = authService.getCurrentEmployee();
        if (employee == null || !employee.isActive() || employee.isArchived()) {
            beforeEnterEvent.forwardTo("login");
            return;
        }
        this.currentEmployee = employeeService.findByLogin(employee.getLogin());
        this.lvl = employee.getRole().getAccessLevel();

        int lvl = currentEmployee.getRole().getAccessLevel();
        List<Delegation> delegations = new ArrayList<>();
        List<Member> myOwnedMembers;
        allMembers = new ArrayList<>();
        currentEmployees = new ArrayList<>();
        if (lvl == 50) {
            myOwnedMembers = new ArrayList<>(memberService.getMembersByAssignedEmployee(currentEmployee.getLogin(),
                            currentEmployee).stream()
                    .filter(f -> !f.getStatus().equals(RecordStatus.INVALID)).toList());
            allMembers.addAll(myOwnedMembers);

            delegations = new ArrayList<>(delegationService.getDelegationsByToEmployee(currentEmployee).stream()
                    .filter(delegationService::isDelegationActive).toList());
            for (Delegation d : delegations)
                if (!d.getMember().getStatus().equals(RecordStatus.INVALID) && !allMembers.contains(d.getMember()))
                    allMembers.add(d.getMember());

            for (Member f : allMembers)
                if (f.getAssignedEmployee().isActive() && !f.getAssignedEmployee().isArchived() &&
                        !currentEmployees.contains(f.getAssignedEmployee()))
                    currentEmployees.add(f.getAssignedEmployee());

        } else if (lvl == 100 || lvl == 80 || lvl == 10) {
            allMembers = new ArrayList<>(memberService.getMembers()
                    .stream().filter(f -> !f.getStatus().equals(RecordStatus.INVALID)).toList());
            this.currentEmployees.addAll(new ArrayList<>(employeeService.findAll().stream()
                    .filter(empl -> empl.isActive() && !empl.isArchived()
                            && empl.getRole().getAccessLevel() != 10).toList()));
        }

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
        buildTitle();
        buildFilters();
        buildTopBar();
        buildGrid();
    }

    private void buildBreadcrumbs() {
        HorizontalLayout bcrumbs = new HorizontalLayout();
        bcrumbs.setSpacing(true);
        bcrumbs.setPadding(true);
        bcrumbs.setAlignItems(FlexComponent.Alignment.CENTER);

        RouterLink root = new RouterLink("Übersicht", OverviewView.class);

        Span separator = new Span(" >> ");
        separator.getStyle().set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)");

        Span current = new Span("Teilnehmer*innen");
        current.getStyle().set("font-size", "var(--lumo-font-size-s)")
                .set("font-weight", "bold")
                .set("color", "var(--lumo-body-text-color)");

        bcrumbs.add(root, separator, current);
        add(bcrumbs);
    }

    private void buildTitle() {
        VerticalLayout content = new VerticalLayout();
        content.setSpacing(false);
        content.setPadding(false);
        content.setWidthFull();

        HorizontalLayout title = new HorizontalLayout();
        title.setSpacing(false);
        title.setPadding(false);
        title.setWidthFull();

        H2 titleText = new H2("Teilnehmer*innen");
        title.add(titleText);

        content.add(title, getHLWithSpans(currentEmployee.getRole().getLabel() + ": ",
                currentEmployee.getFirstName() + " " + currentEmployee.getLastName()));
        add(content);
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
        Button resetButton = new Button(VaadinIcon.REFRESH.create(), e -> {
            searchField.clear();
            statusFilter.clear();
            employeeFilter.clear();
            delegatedFilter.clear();
            closedFilter.clear();
            filterVisible = false;
            filterLayout.setVisible(false);
            filterToggleButton.setText("Filter öffnen");
        });

        filterToggleButton = new Button("Filter öffnen", e -> {
            filterVisible = !filterVisible;
            filterLayout.setVisible(filterVisible);
            filterToggleButton.setText(filterVisible ? "Filter schließen" : "Filter öffnen");
        });

        searchRow.add(searchField, searchButton, resetButton, filterToggleButton);

        HorizontalLayout actionsRow = new HorizontalLayout();
        actionsRow.setSpacing(true);
        actionsRow.setAlignItems(FlexComponent.Alignment.CENTER);

        Button createButton = new Button("Teilnehmer*in", VaadinIcon.PLUS.create());
        createButton.getStyle().set("font-size", "16px").set("padding", "8px 16px");
        createButton.addClickListener(e -> openCreateMemberDialog());

        Button trashButton = new Button("Papierkorb", VaadinIcon.TRASH.create());
        trashButton.setWidth("90px");
        trashButton.setHeight("26px");
        trashButton.getStyle().set("font-size", "11px").set("padding", "2px 6px")
                .set("color", "#666").set("background", "#f2f2f2");
        trashButton.addClickListener(e -> openTrashDialog());

        if (lvl == 10) {
            createButton.setVisible(false);
            trashButton.setVisible(false);
        }

        actionsRow.add(createButton, trashButton);

        add(searchRow);
        add(filterLayout);
        add(actionsRow);
    }

    private void buildFilters() {
        filterLayout = new HorizontalLayout();
        filterLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        filterLayout.setSpacing(true);
        filterLayout.setPadding(true);
        filterLayout.getStyle().set("background-color", "#fff3cd")
                .set("border", "1px solid #e0c97f")
                .set("border-radius", "6px");
        filterLayout.setVisible(false);

        VerticalLayout empLayout = new VerticalLayout();
        empLayout.setSpacing(false);
        empLayout.setPadding(false);
        empLayout.setWidthFull();

        VerticalLayout statusLayout = new VerticalLayout();
        statusLayout.setSpacing(false);
        statusLayout.setPadding(false);
        statusLayout.setWidthFull();

        VerticalLayout caseCloseLayout = new VerticalLayout();
        caseCloseLayout.setSpacing(false);
        caseCloseLayout.setPadding(false);
        caseCloseLayout.setWidthFull();

        VerticalLayout delegatedLayout = new VerticalLayout();
        delegatedLayout.setSpacing(false);
        delegatedLayout.setPadding(false);
        delegatedLayout.setWidthFull();

        Span statusLabel = new Span("Status:");
        statusLabel.getStyle().set("font-weight", "bold");

        statusFilter = new ComboBox<>();
        statusFilter.setItems(RecordStatus.ACTIVE, RecordStatus.ARCHIVED, RecordStatus.BLOCKED);
        statusFilter.setClearButtonVisible(true);
        statusFilter.addValueChangeListener(e -> refreshGrid());

        statusLayout.add(statusLabel, statusFilter);

        Span closeLabel = new Span("Ablauf abgeschlossen:");
        closeLabel.getStyle().set("font-weight", "bold");

        closedFilter = new ComboBox<>();
        closedFilter.setItems("Ja", "Nein");
        closedFilter.setClearButtonVisible(true);
        closedFilter.addValueChangeListener(e -> refreshGrid());

        caseCloseLayout.add(closeLabel, closedFilter);

        Span delegatedLabel = new Span("Delegiert:");
        delegatedLabel.getStyle().set("font-weight", "bold");

        delegatedFilter = new ComboBox<>();
        delegatedFilter.setItems("Ja", "Nein");
        delegatedFilter.setClearButtonVisible(true);
        delegatedFilter.addValueChangeListener(e -> refreshGrid());

        delegatedLayout.add(delegatedLabel, delegatedFilter);

        if (lvl != 50) {
            Span employeeLabel = new Span("Berater*in:");
            employeeLabel.getStyle().set("font-weight", "bold");
            employeeFilter = new ComboBox<>();
            employeeFilter.setItems(currentEmployees);
            employeeFilter.setItemLabelGenerator(emp -> emp.getFirstName() + " " + emp.getLastName());
            employeeFilter.setClearButtonVisible(true);
            employeeFilter.addValueChangeListener(e -> refreshGrid());

            empLayout.add(employeeLabel, employeeFilter);
            filterLayout.add(empLayout);
        }

        filterLayout.add(statusLayout, caseCloseLayout, delegatedLayout);
    }

    private void buildGrid() {
        memberGrid = new Grid<>(Member.class, false);
        memberGrid.setSizeFull();
        memberGrid.setSelectionMode(Grid.SelectionMode.SINGLE);
        memberGrid.setItems(allMembers);

        memberGrid.addColumn(Member::getId).setHeader("ID").setAutoWidth(true).setFlexGrow(0)
                .setComparator(Member::getId);

        memberGrid.addColumn(new ComponentRenderer<>(f -> {
                    String zeusId = f.getZeusId();
                    Span span = new Span();
                    if (zeusId == null || zeusId.isBlank()) {
                        span.setText("kein");
                        span.getStyle().set("color", "red").set("font-weight", "bold");
                    } else span.setText(zeusId);
                    return span;
                })).setHeader("Zeus ID").setAutoWidth(true).setFlexGrow(0)
                .setComparator(f -> {
                    String zeusId = f.getZeusId();
                    return zeusId == null ? "" : zeusId;
                });

        memberGrid.addColumn(m->m.getFirstName()+" "+m.getLastName())
                .setHeader("Vor- und Nachname").setAutoWidth(true).setFlexGrow(0)
                .setComparator(Member::getLastName);

        memberGrid.addColumn(f -> f.isCaseClosed() ? "geschlossen" : "offen")
                .setHeader("Ablauf").setAutoWidth(true).setFlexGrow(0)
                .setComparator(Member::isCaseClosed);

        memberGrid.addColumn(f -> f.getAssignedEmployee() != null
                        ? f.getAssignedEmployee().getFirstName() + " " + f.getAssignedEmployee().getLastName() : "-")
                .setHeader("Berater*in").setAutoWidth(true).setFlexGrow(0)
                .setComparator(f -> f.getAssignedEmployee() != null ? f.getAssignedEmployee().getFirstName() : "-");

        memberGrid.addColumn(new ComponentRenderer<>(f -> {
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
                })).setHeader("Status").setAutoWidth(true).setFlexGrow(0)
                .setComparator(Member::getStatus);

        memberGrid.addColumn(consultationService::getFamilyConsultationsCount)
                .setHeader("Beratungen").setAutoWidth(true).setFlexGrow(0)
                .setComparator(consultationService::getFamilyConsultationsCount);

        memberGrid.addColumn(f -> {
                    int minutes = consultationService.getDurationTimeMinutesForFamilyCount(f);
                    return String.format("%d:%02d", minutes / 60, minutes % 60);
                }).setHeader("Gesamt St.").setAutoWidth(true).setFlexGrow(0)
                .setComparator(consultationService::getDurationTimeMinutesForFamilyCount);

        memberGrid.addColumn(f -> delegationService.hasActiveDelegation(f) ? "Ja" : "")
                .setHeader("Delegiert").setAutoWidth(true).setFlexGrow(0)
                .setComparator(f -> delegationService.hasActiveDelegation(f) ? "Ja" : "");

        memberGrid.addItemClickListener(e -> {
            Member member = e.getItem();
            getUI().ifPresent(ui -> ui.navigate(MemberView.class,
                    new RouteParameters("id", member.getId().toString())));
        });
        add(memberGrid);
    }

    private void refreshGrid() {
        List<Member> result = new ArrayList<>();

        String q = searchField != null ? searchField.getValue().trim().toLowerCase() : "";
        Employee emp = employeeFilter != null ? employeeFilter.getValue() : null;
        String closed = closedFilter != null ? closedFilter.getValue() : null;
        String delegated = delegatedFilter != null ? delegatedFilter.getValue() : null;
        RecordStatus status = statusFilter != null ? statusFilter.getValue() : null;

        for (Member f : allMembers) {
            if (!q.isBlank()) {
                boolean match = false;
                if (f.getLastName() != null && f.getLastName().toLowerCase().contains(q.toLowerCase()))
                    match = true;
                if (!match && f.getZeusId() != null && f.getZeusId().toLowerCase().contains(q.toLowerCase()))
                    match = true;
                if (!match && f.getPhone() != null && f.getPhone().toLowerCase().contains(q.toLowerCase()))
                    match = true;
                if (!match && f.getEmail() != null && f.getEmail().toLowerCase().contains(q.toLowerCase()))
                    match = true;
                if (!match && f.getStreet() != null && f.getStreet().toLowerCase().contains(q.toLowerCase()))
                    match = true;
                if (!match && f.getCity() != null && f.getCity().toLowerCase().contains(q.toLowerCase()))
                    match = true;
                if (!match && f.getHouseNumber() != null && f.getHouseNumber().toLowerCase().contains(q.toLowerCase()))
                    match = true;
                if (!match && f.getZip() != null && f.getZip().toLowerCase().contains(q.toLowerCase()))
                    match = true;
                if (!match && f.getNotes() != null && f.getNotes().toLowerCase().contains(q.toLowerCase()))
                    match = true;
                if (!match && f.getReasonDescription() != null
                        && f.getReasonDescription().toLowerCase().contains(q.toLowerCase()))
                    match = true;
                if (!match && f.getAssignedEmployee() != null) {
                    Employee e = f.getAssignedEmployee();
                    if (e.getFirstName() != null && e.getFirstName().toLowerCase().contains(q.toLowerCase()))
                        match = true;
                    if (e.getLastName() != null && e.getLastName().toLowerCase().contains(q.toLowerCase()))
                        match = true;
                }
                if (!match) continue;
            }
            if (status != null && !f.getStatus().equals(status)) continue;

            if (closed != null) {
                boolean isClosed = f.isCaseClosed();
                if ("Ja".equals(closed) && !isClosed) continue;
                if ("Nein".equals(closed) && isClosed) continue;
            }

            if (delegated != null) {
                boolean isDelegated = delegationService.hasActiveDelegation(f);
                if ("Ja".equals(delegated) && !isDelegated) continue;
                if ("Nein".equals(delegated) && isDelegated) continue;
            }

            if (emp != null) {
                if (f.getAssignedEmployee() == null || !f.getAssignedEmployee().equals(emp)) continue;
            }

            result.add(f);
        }
        memberGrid.setItems(result);
    }

    private void openTrashDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Papierkorb: Teilnehmer*innen");

        List<Member> trashMembers = loadTrashMembers();

        Grid<Member> trashGrid = new Grid<>(Member.class, false);
        trashGrid.setWidthFull();
        trashGrid.setHeight("400px");

        trashGrid.addColumn(Member::getId)
                .setHeader("ID").setAutoWidth(true).setFlexGrow(0)
                .setComparator(Member::getId);
        trashGrid.addColumn(new ComponentRenderer<>(f -> {
                    String zeusId = f.getZeusId();
                    Span span = new Span();
                    if (zeusId == null || zeusId.isBlank()) {
                        span.setText("kein");
                        span.getStyle().set("color", "red").set("font-weight", "bold");
                    } else span.setText(zeusId);
                    return span;
                })).setHeader("Zeus ID").setAutoWidth(true).setFlexGrow(0)
                .setComparator(f -> {
                    String zeusId = f.getZeusId();
                    return zeusId == null ? "" : zeusId;
                });
        trashGrid.addColumn(Member::getLastName)
                .setHeader("Nachname").setAutoWidth(true).setFlexGrow(0)
                .setComparator(Member::getLastName);

        trashGrid.addColumn(f -> f.isCaseClosed() ? "Schluss" : "läuft")
                .setHeader("Ablauf").setAutoWidth(true).setFlexGrow(0)
                .setComparator(Member::isCaseClosed);

        trashGrid.addColumn(f -> f.getAssignedEmployee() != null
                        ? f.getAssignedEmployee().getFirstName() + " " + f.getAssignedEmployee().getLastName() : "-")
                .setHeader("Berater*in").setAutoWidth(true).setFlexGrow(0)
                .setComparator(f -> f.getAssignedEmployee().getFirstName() + " " +
                        f.getAssignedEmployee().getLastName());

        trashGrid.addColumn(new ComponentRenderer<>(f -> {
                    Span span = new Span();
                    span.setText(f.getStatus().name());
                    span.getStyle().set("color", "grey").set("font-weight", "bold");
                    return span;
                })).setHeader("Status").setAutoWidth(true).setFlexGrow(0)
                .setComparator(Member::getStatus);

        trashGrid.addColumn(f -> {
                    Employee e = employeeService.findByLogin(f.getInvalidBy());
                    return e.getFirstName() + " " + e.getLastName();
                }).setHeader("Gelöscht von").setAutoWidth(true).setFlexGrow(0)
                .setComparator(f -> {
                    Employee e = employeeService.findByLogin(f.getInvalidBy());
                    return e.getFirstName() + " " + e.getLastName();
                });

        trashGrid.addColumn(f -> {
                    if (f.getInvalidAt() != null) {
                        LocalDateTime dt = f.getInvalidAt();
                        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
                        return dt.format(fmt);
                    }
                    return "";
                }).setHeader("Gelöscht am").setAutoWidth(true).setFlexGrow(0)
                .setComparator(f -> {
                    LocalDateTime dt = f.getInvalidAt();
                    return dt == null ? "" : dt.toString();
                });

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
                    })).setHeader("Noch verfügbar").setAutoWidth(true).setFlexGrow(0)
                    .setComparator(f -> {
                        LocalDateTime dt = f.getInvalidAt();
                        return dt == null ? "" : dt.toString();
                    });
        }

        trashGrid.setItems(trashMembers);

        Button restoreButton = new Button("Wiederherstellen");
        restoreButton.setEnabled(false);

        trashGrid.asSingleSelect().addValueChangeListener(e ->
                restoreButton.setEnabled(e.getValue() != null));

        restoreButton.addClickListener(e -> {
            Member selected = trashGrid.asSingleSelect().getValue();
            if (selected == null) return;

            LocalDateTime invalidAt = selected.getInvalidAt();
            boolean olderThan14 = false;
            if (invalidAt != null) {
                long days = Duration.between(invalidAt, LocalDateTime.now()).toDays();
                olderThan14 = days > 14;
            }
            int lvl = currentEmployee.getRole().getAccessLevel();

            Dialog confirm = new Dialog();
            confirm.setHeaderTitle("Teilnehmer*in wiederherstellen");

            VerticalLayout content = new VerticalLayout();
            Span msg = new Span("Wollen Sie den*die Teilnehmer*in\n \"" + selected.getLastName() + "\" \nwiederherstellen?");
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

                    memberService.restoreMember(selected.getId(), currentEmployee.getLogin(), r, ip, browser);
                    trashGrid.setItems(loadTrashMembers());
                    confirm.close();
                    Notification.show("Der*die Teilnehmer*in wurde wiederhergestellt", 3000, Notification.Position.MIDDLE);
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

    private void openCreateMemberDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Neuer*neue Teilnehmer*in");
        dialog.setModal(true);
        dialog.setResizable(false);
        dialog.setDraggable(false);
        dialog.setCloseOnOutsideClick(false);
        dialog.setWidth("800px");

        DatePicker joinedAtPicker = new DatePicker("Eintrittdatum (*)");
        joinedAtPicker.setWidthFull();
        joinedAtPicker.setRequiredIndicatorVisible(true);

        TextField firstName = new TextField("Vorname (*)");
        firstName.setWidthFull();
        firstName.setRequiredIndicatorVisible(true);

        TextField lastName = new TextField("Nachname (*)");
        lastName.setWidthFull();
        lastName.setRequiredIndicatorVisible(true);

        DatePicker birthDatePicker = new DatePicker("Geburtsdatum (*)");
        birthDatePicker.setWidthFull();
        birthDatePicker.setRequiredIndicatorVisible(true);

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

        TextArea reason = new TextArea("Grund der Beratung");
        reason.setWidthFull();

        TextArea notes = new TextArea("Interne Notizen");
        notes.setWidthFull();

        TextField zeusId = new TextField("Zeus ID");
        zeusId.setWidthFull();

        VerticalLayout employeeBlock = new VerticalLayout();
        employeeBlock.setSpacing(false);
        employeeBlock.setPadding(true);
        employeeBlock.getStyle().set("background-color", "#fafafa").set("border", "1px solid #ddd").
                set("border-radius", "6px").set("padding", "10px");

        ComboBox<Employee> assignedCombo = new ComboBox<>("Berater*in");
        assignedCombo.setItemLabelGenerator(emp ->
                emp.getFirstName() + " " + emp.getLastName() + " - " + emp.getRole().getLabel());
        assignedCombo.setPlaceholder("Bitte wählen...");
        assignedCombo.setWidthFull();

        TextField assignedDisplay = new TextField();
        assignedDisplay.setReadOnly(true);
        assignedDisplay.setWidthFull();
        assignedDisplay.getStyle().set("background-color", "#f2f2f2").set("color", "#666");

        Span noRights = new Span("Keine Bearbeitungsrechte");
        noRights.getStyle().set("font-size", "11px").set("color", "#888");

        Span empTitle = new Span("Zuständige/r Berater*in");
        empTitle.getStyle().set("font-weight", "bold");
        employeeBlock.add(empTitle);

        if (lvl == 80 || lvl == 100) {
            assignedCombo.setItems(currentEmployees);
            employeeBlock.add(assignedCombo);
        } else if (lvl == 50) {
            assignedDisplay.setValue(currentEmployee.getFirstName() + " " + currentEmployee.getLastName());
            employeeBlock.add(assignedDisplay, noRights);
        }

        Span mainTitle = new Span("Allgemeine Angaben");
        mainTitle.getStyle().set("font-weight", "bold");

        VerticalLayout mainBlock = new VerticalLayout();
        mainBlock.setSpacing(false);
        mainBlock.setPadding(true);
        mainBlock.getStyle().set("border", "1px solid #ddd").set("padding", "10px").set("border-radius", "6px");

        if (lvl == 50) {
            zeusId.setReadOnly(true);
            zeusId.getStyle().set("background-color", "#f2f2f2").set("color", "#666");
        }

        mainBlock.add(mainTitle, zeusId, joinedAtPicker, firstName, lastName, birthDatePicker);

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

        detailsBlock.add(detailsTitle, reason, notes);

        VerticalLayout content = new VerticalLayout(employeeBlock, mainBlock, contactBlock, addressBlock, detailsBlock);
        content.setWidthFull();

        dialog.add(content);

        Button save = new Button("Speichern");
        Button cancel = new Button("Abbrechen");

        save.addClickListener(e -> {
            List<String> errors = new ArrayList<>();

            if (birthDatePicker.getValue().isBefore(LocalDate.of(1935, 1, 1))) {
                errors.add("Geburtsdatum ist älter als 1935");
                birthDatePicker.getStyle().set("border", "1px solid red");
            } else birthDatePicker.getStyle().remove("border");
            if (birthDatePicker.getValue().isAfter(LocalDate.now())) {
                errors.add("Geburtsdatum kann nicht in Zukunft sein");
                birthDatePicker.getStyle().set("border", "1px solid red");
            } else birthDatePicker.getStyle().remove("border");
            if (birthDatePicker.isEmpty()) {
                errors.add("Geburtsdatum erforderlich");
                birthDatePicker.getStyle().set("border", "1px solid red");
            } else birthDatePicker.getStyle().remove("border");

            if (joinedAtPicker.getValue().isBefore(LocalDate.of(2026, 1, 1))) {
                errors.add("Eintrittdatum muss ab 01.01.2026 starten.");
                joinedAtPicker.getStyle().set("border", "1px solid red");
            } else joinedAtPicker.getStyle().remove("border");
            if (joinedAtPicker.getValue().isAfter(LocalDate.now())) {
                errors.add("Eintrittdatum kann nicht in Zukunft sein.");
                joinedAtPicker.getStyle().set("border", "1px solid red");
            } else joinedAtPicker.getStyle().remove("border");
            if (joinedAtPicker.isEmpty()) {
                errors.add("Eintrittdatum ist erforderlich.");
                joinedAtPicker.getStyle().set("border", "1px solid red");
            } else joinedAtPicker.getStyle().remove("border");

            if (lastName.isEmpty()) {
                errors.add("Nachname ist erforderlich.");
                lastName.getStyle().set("border", "1px solid red");
            } else lastName.getStyle().remove("border");

            if (firstName.isEmpty()) {
                errors.add("Vorname ist erforderlich.");
                firstName.getStyle().set("border", "1px solid red");
            } else firstName.getStyle().remove("border");

            Employee assignedEmployee = (lvl == 80 || lvl == 100) ? assignedCombo.getValue() : currentEmployee;

            if ((lvl == 80 || lvl == 100) && assignedEmployee == null) {
                errors.add("Berater*in muss ausgewählt werden.");
                assignedCombo.getStyle().set("border", "1px solid red");
            } else {
                assignedCombo.getStyle().remove("border");
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
                    showCreateConfirmDialog(dialog, zeusId.getValue(), birthDatePicker.getValue(),
                            joinedAtPicker.getValue(), firstName.getValue(), lastName.getValue(), street.getValue(),
                            houseNumber.getValue(), zip.getValue(), city.getValue(), phone.getValue(),
                            email.getValue(), reason.getValue(), notes.getValue(), assignedEmployee);
                });
                askContact.getFooter().add(yes, no);
                askContact.open();
                return;
            }
            showCreateConfirmDialog(dialog, zeusId.getValue(), birthDatePicker.getValue(), joinedAtPicker.getValue(),
                    firstName.getValue(), lastName.getValue(), street.getValue(), houseNumber.getValue(),
                    zip.getValue(), city.getValue(), phone.getValue(), email.getValue(), reason.getValue(),
                    notes.getValue(), assignedEmployee);
        });
        HorizontalLayout buttons = new HorizontalLayout(save, cancel);
        buttons.setWidthFull();
        buttons.setJustifyContentMode(JustifyContentMode.END);

        cancel.addClickListener(e -> dialog.close());
        dialog.getFooter().add(buttons);
        dialog.open();
    }

    private void showCreateConfirmDialog(Dialog parent, String zeusId, LocalDate birthDate, LocalDate joinedAt, String firstName,
                                         String lastName, String street, String houseNumber, String zip, String city,
                                         String phone, String email, String reason, String notes, Employee emp) {
        Dialog d = new Dialog();
        d.setHeaderTitle("Bestätigen");

        Button yes = new Button("Ja", e -> {
            d.close();
            parent.close();

            VaadinRequest req = VaadinRequest.getCurrent();
            String ip = req != null ? req.getRemoteAddr() : "UNKNOWN";
            String browser = req != null ? req.getHeader("User-Agent") : "UNKNOWN";

            memberService.createMember(zeusId, joinedAt, birthDate, firstName, lastName, street, houseNumber, zip, city, phone, email, reason,
                    notes, emp, currentEmployee.getLogin(), ip, browser);
            showSuccessDialog(firstName, lastName);
        });

        Button no = new Button("Nein", e -> d.close());

        d.add(new Span("Teilehmer*in speichern?"), new HorizontalLayout(yes, no));
        d.open();
    }

    private void showSuccessDialog(String firstName, String lastName) {
        Dialog d = new Dialog();
        d.setHeaderTitle("Gespeichert");
        d.add(new Span("Teilnehmer*in \"" + firstName + " " + lastName + "\" wurde erstellt."));
        d.add(new Button("Ok", e -> {
            getUI().ifPresent(ui -> ui.getPage().reload());
            d.close();
        }));
        d.open();
    }

    private List<Member> loadTrashMembers() {
        String login = currentEmployee.getLogin();
        int lvl = currentEmployee.getRole().getAccessLevel();

        List<Member> source;
        List<Member> result = new ArrayList<>();

        if (lvl == 100 || lvl == 80) {
            source = memberService.getMembers();
            for (Member f : source)
                if (f.getStatus().equals(RecordStatus.INVALID)) result.add(f);
        } else if (lvl == 50) {
            source = memberService.getMembersByAssignedEmployee(login, currentEmployee);
            for (Member f : source)
                if (f.getStatus().equals(RecordStatus.INVALID) &&
                        !f.getInvalidAt().isAfter(LocalDateTime.now().plusDays(14))) result.add(f);
        } else return List.of();
        return result;
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