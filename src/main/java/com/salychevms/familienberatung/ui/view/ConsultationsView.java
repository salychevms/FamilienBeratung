package com.salychevms.familienberatung.ui.view;

import com.salychevms.familienberatung.model.*;
import com.salychevms.familienberatung.service.*;
import com.salychevms.familienberatung.ui.layout.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
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

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Route(value = "consultations/:familyId?", layout = MainLayout.class)
@PageTitle("Beratungen")
@RequiredArgsConstructor
@PermitAll
public class ConsultationsView extends VerticalLayout implements BeforeEnterObserver {
    private final AuthService authService;
    private final MemberService memberService;
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
    private HorizontalLayout filterLayout;
    private boolean filterVisible = false;
    private Button filterToggleButton;
    private TextField searchField;
    ComboBox<Member> familyFilter;
    ComboBox<Employee> employeeFilter;
    private Grid<Consultation> consultationGrid;
    private Member selectedMember;
    private List<Member> currentFamilies = new ArrayList<>();
    private DatePicker fromDate;
    private DatePicker toDate;

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

        if (familyId == null) {
            List<Delegation> delegations;
            List<Member> families;

            if (lvl == 50) {
                families = new ArrayList<>(memberService.getMembersByAssignedEmployee(currentEmployee.getLogin(),
                        currentEmployee).stream().filter(f ->
                        !f.getStatus().equals(RecordStatus.INVALID)).toList());
                delegations = new ArrayList<>(delegationService.getDelegationsByToEmployee(currentEmployee).stream()
                        .filter(delegationService::isDelegationActive)
                        .filter(d -> d.getToEmployee().equals(currentEmployee)).toList());
                for (Delegation dlg : delegations)
                    if (!dlg.getMember().getStatus().equals(RecordStatus.INVALID))
                        families.add(dlg.getMember());
                currentFamilies = families;
            } else {
                families = new ArrayList<>(memberService.getMembers().stream()
                        .filter(f -> !f.getStatus().equals(RecordStatus.INVALID)).toList());
                currentFamilies = families;
            }
        }

        this.currentDelegations.clear();
        if (lvl == 50)
            this.currentDelegations = new ArrayList<>(delegationService.getDelegationsByToEmployee(currentEmployee).stream()
                    .filter(delegationService::isDelegationActive)
                    .filter(d -> d.getMember().getStatus().equals(RecordStatus.ACTIVE)).toList());
        else
            this.currentDelegations = new ArrayList<>(delegationService.getDelegations());

        this.currentConsultations.clear();
        if (lvl == 50) {
            List<Consultation> cList = consultationService.getAll();
            for (Consultation c : cList) {
                if ((c.getEmployee().equals(currentEmployee) ||
                        c.getMember().getAssignedEmployee().equals(currentEmployee)) &&
                        (!c.isInvalid() && !c.getMember().getStatus().equals(RecordStatus.INVALID)))
                    this.currentConsultations.add(c);
            }
        } else this.currentConsultations = new ArrayList<>(consultationService.getAll().stream()
                .filter(c -> !c.isInvalid()).toList());

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
        buildTitle();
        buildTopBar();
        buildFilters();
        buildActionButtons();
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
            RouterLink families = new RouterLink("Familien", MembersView.class);

            Span separator1 = new Span(" >> ");
            separator1.getStyle().set("font-size", "var(--lumo-font-size-s)")
                    .set("color", "var(--lumo-secondary-text-color)");
            Member f = memberService.getMemberById(familyId);
            RouterLink family = new RouterLink("Familie: " + f.getLastName(), MemberView.class,
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

    private void buildTitle() {
        VerticalLayout content = new VerticalLayout();
        content.setSpacing(false);
        content.setPadding(false);
        content.setWidthFull();

        HorizontalLayout title = new HorizontalLayout();
        title.setSpacing(false);
        title.setPadding(false);
        title.setWidthFull();

        String text = "Beratungen: ";
        if (familyId != null) {
            Member f = memberService.getMemberById(familyId);
            text += f.getLastName();
        } else
            text += "alle";

        H2 titleText = new H2(text);
        title.add(titleText);

        Span emp = new Span(currentEmployee.getRole().getLabel() + ": " + currentEmployee.getFirstName() +
                " " + currentEmployee.getLastName());

        content.add(title, emp);
        add(content);
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

        Button searchButton = new Button(VaadinIcon.SEARCH.create(), e -> refreshGrid());
        Button resetButton = new Button(VaadinIcon.REFRESH.create(), e -> {
            searchField.clear();
            familyFilter.clear();
            employeeFilter.clear();
            fromDate.clear();
            toDate.clear();
            filterVisible=false;
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

    private void buildActionButtons() {
        HorizontalLayout actionsRow = new HorizontalLayout();
        actionsRow.setSpacing(true);
        actionsRow.setAlignItems(FlexComponent.Alignment.CENTER);

        Button createButton = new Button("Neue Beratung", VaadinIcon.PLUS.create(),
                e -> openSelectFamilyDialog());
        createButton.getStyle().set("font-size", "16px").set("padding", "8px 16px");

        Button trashButton = new Button("Papierkorb", VaadinIcon.TRASH.create(),
                e -> buildConsultationsTrashDialog());
        trashButton.setWidth("90px");
        trashButton.setHeight("26px");
        trashButton.getStyle().set("font-size", "11px").set("padding", "2px 6px")
                .set("color", "#666").set("background", "#f2f2f2");

        if (lvl == 10) {
            createButton.setVisible(false);
            trashButton.setVisible(false);
        }

        actionsRow.add(createButton, trashButton);

        add(actionsRow);
    }

    private void buildFilters() {
        filterLayout = new HorizontalLayout();
        filterLayout.setAlignItems(Alignment.START);
        filterLayout.setSpacing(true);
        filterLayout.setPadding(true);
        filterLayout.setVisible(false);

        filterLayout.getStyle().set("background-color", "#fff3cd").set("border", "1px solid #e0c97f")
                .set("border-radius", "6px");

        VerticalLayout empLayout = new VerticalLayout();
        empLayout.setSpacing(false);
        empLayout.setPadding(false);
        empLayout.setWidthFull();

        VerticalLayout dateFromLayout = new VerticalLayout();
        dateFromLayout.setSpacing(false);
        dateFromLayout.setPadding(false);
        dateFromLayout.setWidthFull();

        VerticalLayout dateToLayout = new VerticalLayout();
        dateToLayout.setSpacing(false);
        dateToLayout.setPadding(false);
        dateToLayout.setWidthFull();

        if (familyId == null) {
            VerticalLayout familyLayout = new VerticalLayout();
            familyLayout.setSpacing(false);
            familyLayout.setPadding(false);
            familyLayout.setWidthFull();

            Span famLabel = new Span("Familie");
            famLabel.getStyle().set("margin-bottom", "0").set("font-weight", "bold");

            familyFilter = new ComboBox<>();
            familyFilter.setItems(currentFamilies);
            familyFilter.setItemLabelGenerator(Member::getLastName);
            familyFilter.setClearButtonVisible(true);
            familyFilter.addValueChangeListener(f -> refreshGrid());

            familyLayout.add(famLabel, familyFilter);
            filterLayout.add(familyLayout);
        }

        Span empLabel = new Span("Berater*in");
        empLabel.getStyle().set("margin-bottom", "0").set("font-weight", "bold");

        employeeFilter = new ComboBox<>();
        employeeFilter.setItems(currentEmployees);
        employeeFilter.setItemLabelGenerator(e -> e.getFirstName() + " " + e.getLastName());
        employeeFilter.setClearButtonVisible(true);
        employeeFilter.addValueChangeListener(e -> refreshGrid());

        empLayout.add(empLabel, employeeFilter);

        Span fromDateLabel = new Span("Von");
        fromDateLabel.getStyle().set("margin-bottom", "0").set("font-weight", "bold");

        fromDate = new DatePicker();
        fromDate.setMin(LocalDate.of(2026, 1, 1));
        fromDate.addValueChangeListener(e -> {
            dateFrom = e.getValue();
            refreshGrid();
        });

        dateFromLayout.add(fromDateLabel, fromDate);

        Span toDateLabel = new Span("Bis");
        toDateLabel.getStyle().set("margin-bottom", "0").set("font-weight", "bold");

        toDate = new DatePicker("");
        toDate.setMin(LocalDate.of(2026, 1, 1));
        toDate.addValueChangeListener(e -> {
            dateTo = e.getValue();
            refreshGrid();
        });

        dateToLayout.add(toDateLabel, toDate);

        filterLayout.add(empLayout, dateFromLayout, dateToLayout);
        add(filterLayout);
    }

    private void buildGrid() {
        consultationGrid = new Grid<>(Consultation.class, false);
        consultationGrid.setSizeFull();
        consultationGrid.setSelectionMode(Grid.SelectionMode.SINGLE);

        consultationGrid.addColumn(Consultation::getId)
                .setHeader("ID").setAutoWidth(true).setFlexGrow(0)
                .setComparator(Consultation::getId);
        consultationGrid.addColumn(c -> c.getMember().getLastName())
                .setHeader("Familie").setAutoWidth(true).setFlexGrow(0)
                .setComparator(c -> c.getMember().getLastName());
        consultationGrid.addColumn(c ->
                        c.getMember().getAssignedEmployee().getFirstName() + " " +
                                c.getMember().getAssignedEmployee().getLastName())
                .setHeader("Berater*in").setAutoWidth(true).setFlexGrow(0)
                .setComparator(c -> c.getMember().getAssignedEmployee().getFirstName() + " " +
                        c.getMember().getAssignedEmployee().getLastName());
        consultationGrid.addColumn(c -> c.getEmployee().getFirstName() + " " + c.getEmployee().getLastName())
                .setHeader("Durchgeführt von").setAutoWidth(true).setFlexGrow(0)
                .setComparator(c -> c.getEmployee().getFirstName() + " " + c.getEmployee().getLastName());
        consultationGrid.addColumn(c -> formatDate(c.getDateTime().toLocalDate()))
                .setHeader("Datum").setAutoWidth(true).setFlexGrow(0)
                .setComparator(c -> formatDate(c.getDateTime().toLocalDate()));
        consultationGrid.addColumn(c -> formatDuration(c.getDurationMinutes()))
                .setHeader("Dauer St.").setAutoWidth(true).setFlexGrow(0)
                .setComparator(c -> formatDuration(c.getDurationMinutes()));
        consultationGrid.addColumn(c -> delegationService.hasActiveDelegation(c.getMember()) ? "Ja" : "Nein")
                .setHeader("Delegiert").setAutoWidth(true).setFlexGrow(0)
                .setComparator(c -> delegationService.hasActiveDelegation(c.getMember()) ? "Ja" : "Nein");
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
        Member fam = familyFilter != null ? familyFilter.getValue() : null;

        for (Consultation c : currentConsultations) {
            if (familyId != null && !c.getMember().getId().equals(familyId)) continue;
            if (familyId == null && fam != null && !c.getMember().equals(fam)) continue;
            if (emp != null && !c.getEmployee().equals(emp)) continue;
            if (!isInDateRange(c)) continue;
            if (!matchesSearch(c, query)) continue;
            result.add(c);
        }
        consultationGrid.setItems(result);
    }

    private void openSelectFamilyDialog() {
        Dialog dialog = new Dialog();
        dialog.setModal(true);
        dialog.setCloseOnOutsideClick(false);
        dialog.setWidth("600px");

        Span title = new Span("Familie auswählen");
        title.getStyle().set("font-weight", "bold");

        ComboBox<Member> familyBox = new ComboBox<>("Familie (*)");
        familyBox.setWidthFull();
        familyBox.setItemLabelGenerator(Member::getLastName);

        List<Member> items = new ArrayList<>();

        if (familyId != null) {
            Member f = memberService.getMemberById(familyId);
            if (f != null) {
                items.add(f);
            }
        } else {
            List<Member> own = new ArrayList<>();
            if (lvl == 50)
                own = memberService.getMembersByAssignedEmployee(currentEmployee.getLogin(), currentEmployee);
            else if (lvl == 80 || lvl == 100)
                own = memberService.getMembers();
            items.addAll(own);

            for (Delegation d : currentDelegations) {
                Member f = d.getMember();
                if (f != null && !items.contains(f) && f.getStatus().equals(RecordStatus.ACTIVE) && !d.isExpired()
                        && !d.getStartDate().isBefore(LocalDate.now()) && !d.getEndDate().isAfter(LocalDate.now())
                        && d.getAbortedAt() == null)
                    items.add(f);
            }
        }

        familyBox.setItems(items);

        if (items.size() == 1) {
            familyBox.setValue(items.getFirst());
        }

        Button cancel = new Button("Abbrechen", e -> dialog.close());

        Button next = new Button("Bestätigen");
        next.setEnabled(false);

        familyBox.addValueChangeListener(
                e -> next.setEnabled(e.getValue() != null));

        next.addClickListener(e -> {
            this.selectedMember = familyBox.getValue();
            dialog.close();
            buildCreateConsultationDialog();
        });

        HorizontalLayout buttons = new HorizontalLayout(cancel, next);
        buttons.setWidthFull();
        buttons.setJustifyContentMode(JustifyContentMode.END);

        VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);
        content.add(title, familyBox);

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

        H2 title = new H2("Beratung für die Familie " + selectedMember.getLastName());
        dialog.add(title);

        DateTimePicker dateTime = new DateTimePicker("Datum und Uhrzeit (*)");
        dateTime.setMin(LocalDateTime.of(2026, 1, 1, 0, 0, 0));
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
        followUp.setMin(LocalDateTime.now());
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

    private void buildConsultationsTrashDialog() {
        Dialog dialog = new Dialog();
        dialog.setModal(true);
        dialog.setCloseOnOutsideClick(false);
        dialog.setWidth("1100px");
        dialog.setHeight("550px");

        Span title = new Span("Papierkorb: Beratungen");
        title.getStyle().set("font-weight", "bold");

        Grid<Consultation> grid = new Grid<>();
        grid.setSizeFull();
        grid.setSelectionMode(Grid.SelectionMode.SINGLE);

        grid.addColumn(Consultation::getId)
                .setHeader("ID").setAutoWidth(true).setFlexGrow(0)
                .setComparator(Consultation::getId);

        grid.addColumn(c -> c.getMember().getLastName())
                .setHeader("Familie").setAutoWidth(true).setFlexGrow(0)
                .setComparator(c -> c.getMember().getLastName());

        grid.addColumn(c -> c.getMember().getAssignedEmployee().getFirstName() + " " +
                        c.getMember().getAssignedEmployee().getLastName())
                .setHeader("Berater*in").setAutoWidth(true).setFlexGrow(0)
                .setComparator(c -> c.getMember().getAssignedEmployee().getFirstName() + " " +
                        c.getMember().getAssignedEmployee().getLastName());

        grid.addColumn(c -> c.getEmployee().getFirstName() + " " + c.getEmployee().getLastName())
                .setHeader("Dürchgeführt von").setAutoWidth(true).setFlexGrow(0)
                .setComparator(c -> c.getEmployee().getFirstName() + " " + c.getEmployee().getLastName());

        grid.addColumn(c -> formatDate(c.getDateTime().toLocalDate()))
                .setHeader("Datum").setAutoWidth(true).setFlexGrow(0)
                .setComparator(c -> c.getDateTime().toLocalDate());

        grid.addColumn(c -> formatDuration(c.getDurationMinutes()))
                .setHeader("Duration").setAutoWidth(true).setFlexGrow(0)
                .setComparator(Consultation::getDurationMinutes);

        grid.addColumn(c -> formatDate(c.getInvalidAt().toLocalDate()))
                .setHeader("Gelöscht am").setAutoWidth(true).setFlexGrow(0)
                .setComparator(c -> {
                    LocalDate d = c.getInvalidAt().toLocalDate();
                    return d == null ? "" : d.toString();
                });

        grid.addColumn(c -> {
                    Employee e = employeeService.findByLogin(c.getInvalidBy());
                    return e.getFirstName() + " " + e.getLastName();
                }).setHeader("Gelöscht von").setAutoWidth(true).setFlexGrow(0)
                .setComparator(c -> {
                    Employee e = employeeService.findByLogin(c.getInvalidBy());
                    return c.getInvalidBy() == null ? "" : e.getFirstName() + " " + e.getLastName();
                });

        if (currentEmployee.getRole().getAccessLevel() == 50) {
            grid.addColumn(new ComponentRenderer<>(c -> {
                        Span span = new Span();
                        if (c.getInvalidAt() != null) {
                            long days = Duration.between(c.getInvalidAt(), LocalDateTime.now()).toDays();
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
                    .setComparator(c -> {
                        LocalDateTime dt = c.getInvalidAt();
                        return dt == null ? "" : dt.toString();
                    });
        }

        grid.setItems(loadTrashConsultations());

        Button restore = new Button("Wiederherstellen");
        restore.setEnabled(false);

        Button close = new Button("Schließen", e -> dialog.close());

        grid.addSelectionListener(
                e -> restore.setEnabled(e.getFirstSelectedItem().isPresent()));

        restore.addClickListener(e -> {
            Consultation c = grid.asSingleSelect().getValue();
            if (c == null) return;

            Runnable restoreWithoutReason = () -> {
                try {
                    VaadinRequest req = VaadinRequest.getCurrent();
                    consultationService.restoreConsultation(c, c.getMember(), currentEmployee,
                            "Wiederherstellung über Papierkorb",
                            req != null ? req.getRemoteAddr() : "UNKNOWN",
                            req != null ? req.getHeader("User-Agent") : "UNKNOWN");
                    dialog.close();
                    refreshGrid();
                } catch (Exception ex) {
                    showOkDialog("Fehler", ex.getMessage());
                }
            };

            if (c.getInvalidAt() == null) {
                showOkDialog("Fehler", "Invalid-Datum fehlt.");
                return;
            }

            long days = Duration.between(c.getInvalidAt(), LocalDateTime.now()).toDays();

            if (days < 14) {
                restoreWithoutReason.run();
                return;
            }

            if (lvl == 80 || lvl == 100) {
                Dialog reasonDialog = new Dialog();
                reasonDialog.setModal(true);
                reasonDialog.setCloseOnOutsideClick(false);
                reasonDialog.setWidth("500px");

                Span t = new Span("Wiederherstellung nach 14 Tagen");
                t.getStyle().set("font-weight", "bold");

                TextArea reason = new TextArea("Begründung (*)");
                reason.setWidthFull();
                reason.setMinLength(5);

                Span error = new Span("Mindestens 5 Zeichen erforderlich");
                error.getStyle().set("color", "red");
                error.setVisible(false);

                Button cancel = new Button("Abbrechen", ev -> reasonDialog.close());

                Button save = new Button("Wiederherstellen", ev -> {
                    if (reason.getValue() == null || reason.getValue().isEmpty()
                            || reason.getValue().trim().length() < 5) {
                        error.setVisible(true);
                        return;
                    }

                    try {
                        VaadinRequest req = VaadinRequest.getCurrent();
                        consultationService.restoreConsultation(c, c.getMember(), currentEmployee,
                                reason.getValue().trim(),
                                req != null ? req.getRemoteAddr() : "UNKNOWN",
                                req != null ? req.getHeader("User-Agent") : "UNKNOWN");
                        reasonDialog.close();
                        dialog.close();
                        refreshGrid();
                    } catch (Exception ex) {
                        showOkDialog("Fehler", ex.getMessage());
                    }
                });
                HorizontalLayout btns = new HorizontalLayout(cancel, save);
                btns.setWidthFull();
                btns.setJustifyContentMode(JustifyContentMode.END);

                VerticalLayout content = new VerticalLayout();
                content.add(t, reason, error);

                reasonDialog.add(content);
                reasonDialog.getFooter().add(btns);
                reasonDialog.open();
                return;
            }
            showOkDialog("Nicht möglich", // CHANGED
                    "Älter als 14 Tage: nur Admin mit Begründung.");
        });

        HorizontalLayout buttons = new HorizontalLayout(restore, close);
        buttons.setWidthFull();
        buttons.setJustifyContentMode(JustifyContentMode.END);

        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        content.add(title, grid);

        dialog.add(content);
        dialog.getFooter().add(buttons);
        dialog.open();
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

        if (dateTime == null) errors.add("Datum und Uhrzeit müssen angegeben werden.");
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

            consultationService.createConsultation(selectedMember, currentEmployee, dateTime, duration, topic,
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

        String state = validateConsultationDate(dateTime, selectedMember.getCreatedAt());

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

    private List<Consultation> loadTrashConsultations() {
        List<Consultation> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (Consultation c : consultationService.getAll()) {
            if (!c.isInvalid()) continue;
            if (c.getInvalidAt() == null) continue;

            Member f = c.getMember();
            if (f == null) continue;

            if (!RecordStatus.ACTIVE.equals(f.getStatus())) continue;

            long days = Duration.between(c.getInvalidAt(), now).toDays();

            if (lvl == 50) {
                boolean isOwn = c.getEmployee().equals(currentEmployee)
                        || currentEmployee.getLogin().equals(c.getInvalidBy());

                if (!isOwn) continue;
                if (days > 14) continue;
            }
            result.add(c);
        }
        return result;
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
        if (c.getMember() != null && c.getMember().getLastName() != null
                && c.getMember().getLastName().toLowerCase().contains(q)) return true;
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
}