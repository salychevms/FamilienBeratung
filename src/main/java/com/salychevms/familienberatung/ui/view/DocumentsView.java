package com.salychevms.familienberatung.ui.view;

import com.salychevms.familienberatung.model.*;
import com.salychevms.familienberatung.service.*;
import com.salychevms.familienberatung.ui.dialog.EditDialogFactory;
import com.salychevms.familienberatung.ui.dialog.EditField;
import com.salychevms.familienberatung.ui.layout.MainLayout;
import com.vaadin.flow.component.UI;
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
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.streams.UploadHandler;
import jakarta.annotation.security.PermitAll;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Route(value = "documents/:familyId?", layout = MainLayout.class)
@PageTitle("Dokumente")
@RequiredArgsConstructor
@PermitAll
public class DocumentsView extends VerticalLayout implements BeforeEnterObserver {
    private final AuthService authService;
    private final FamilyService familyService;
    private final FamilyDocumentService familyDocumentService;
    private final EmployeeService employeeService;
    private final DelegationService delegationService;

    private Employee currentEmployee;
    private int lvl;
    private Family currentFamily;
    private long familyId;
    private List<FamilyDocument> currentDocuments;
    private List<Family> currentFamilies;
    private HorizontalLayout filterLayout;
    private boolean filterVisible = false;
    private Button searchButton;
    private Button resetButton;
    private Button filterToggleButton;
    private Button uploadButton;
    private Button trashButton;
    private TextField searchField;
    private ComboBox<Employee> assignedEmployeeFilter;
    private ComboBox<Employee> uploaderEmployeeFilter;
    private ComboBox<Family> familyFilter;
    private DatePicker dateFrom;
    private DatePicker dateTo;
    private LocalDate filterDateFrom;
    private LocalDate filterDateTo;
    private List<Employee> assignedEmployees = new ArrayList<>();
    private List<Employee> createdByEmployees = new ArrayList<>();
    private Grid<FamilyDocument> documentsGrid;

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Employee e = authService.getCurrentEmployee();
        if (e == null || !e.isActive() || e.isArchived()) {
            event.forwardTo("login");
            return;
        }

        this.currentEmployee = employeeService.findByLogin(e.getLogin());
        this.lvl = currentEmployee.getRole().getAccessLevel();

        List<Delegation> delegations = delegationService.getDelegations().stream().filter(d ->
                delegationService.isDelegationActive(d) && d.getToEmployee().equals(currentEmployee)
                        && (d.getFamily().getStatus().equals(RecordStatus.ACTIVE)
                        || d.getFamily().getStatus().equals(RecordStatus.ARCHIVED))).toList();

        Optional<Long> optFamilyId = event.getRouteParameters().getLong("familyId");
        if (optFamilyId.isPresent()) {
            this.currentFamily = null;
            this.familyId = 0;
            try {
                Family f = familyService.getFamilyById(optFamilyId.get());
                if (f.getStatus().equals(RecordStatus.ACTIVE) || f.getStatus().equals(RecordStatus.ARCHIVED))
                    if (lvl == 50 && f.getAssignedEmployee().equals(currentEmployee) || lvl == 80 || lvl == 100 || lvl == 10) {
                        this.currentFamily = f;
                        familyId = f.getId();
                    } else {
                        for (Delegation dlg : delegations) {
                            if (dlg.getFamily().equals(f)) {
                                this.currentFamily = f;
                                familyId = f.getId();
                            }
                        }
                    }
            } catch (Exception ex) {
                showOkDialog("Familie nicht verfügbar",
                        "Zugriff auf die Familie ist nicht mehr möglich. Es werden alle Dokumente angezeigt.");
            }
        } else {
            familyId = 0;
            currentFamily = null;
        }

        currentDocuments = new ArrayList<>();
        if (familyId == 0 || currentFamily == null) {
            currentFamilies = new ArrayList<>();
            if (lvl == 50) {
                currentFamilies = familyService.getFamiliesByAssignedEmployee(currentEmployee.getLogin(),
                        currentEmployee).stream().filter(f -> (f.getStatus().equals(RecordStatus.ARCHIVED)
                        || f.getStatus().equals(RecordStatus.ACTIVE))).toList();
                for (Family f : currentFamilies)
                    currentDocuments.addAll(familyDocumentService.getDocumentsByFamily(f, currentEmployee));
                for (Delegation dlg : delegations)
                    if (dlg.getToEmployee().equals(currentEmployee))
                        currentDocuments.addAll(familyDocumentService.getDocumentsByFamily(dlg.getFamily(), currentEmployee));
            } else if (lvl == 80 || lvl == 100) {
                currentFamilies = familyService.getFamilies().stream()
                        .filter(f -> !f.getStatus().equals(RecordStatus.INVALID)).toList();
                currentDocuments = familyDocumentService.getAllDocuments(currentEmployee).stream().filter(d ->
                        (d.getFamily().getStatus().equals(RecordStatus.ARCHIVED)
                                || d.getFamily().getStatus().equals(RecordStatus.ACTIVE))).toList();
            }
        } else
            currentDocuments.addAll(familyDocumentService.getDocumentsByFamily(currentFamily, currentEmployee)
                    .stream().filter(d -> (d.getFamily().getStatus().equals(RecordStatus.ARCHIVED)
                            || d.getFamily().getStatus().equals(RecordStatus.ACTIVE))).toList());

        assignedEmployees.clear();
        createdByEmployees.clear();
        Set<Employee> owners = new LinkedHashSet<>();
        Set<Employee> uploaders = new LinkedHashSet<>();

        for (FamilyDocument d : currentDocuments) {
            if (d.getFamily() != null && d.getFamily().getAssignedEmployee() != null)
                owners.add(d.getFamily().getAssignedEmployee());
            if (d.getUploadedByEmployee() != null)
                uploaders.add(d.getUploadedByEmployee());
        }
        assignedEmployees.addAll(owners);
        createdByEmployees.addAll(uploaders);

        removeAll();
        buildUI();
    }

    private void buildUI() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        buildBreadCrumbs();
        buildHeader();
        buildTopBar();
        buildFilters();
        buildActionButtons();
        buildGrid();
    }

    private void buildBreadCrumbs() {
        HorizontalLayout bcrumbs = new HorizontalLayout();
        bcrumbs.setSpacing(true);
        bcrumbs.setPadding(true);
        bcrumbs.setAlignItems(FlexComponent.Alignment.CENTER);

        RouterLink overview = new RouterLink("Übersicht", OverviewView.class);
        bcrumbs.add(overview);

        if (currentFamily != null && familyId != 0) {
            Span s1 = new Span(" >> ");
            s1.getStyle().set("font-size", "var(--lumo-font-size-s)").set("color", "var(--lumo-secondary-text-color)");
            Span s2 = new Span(" >> ");
            s2.getStyle().set("font-size", "var(--lumo-font-size-s)").set("color", "var(--lumo-secondary-text-color)");

            RouterLink familiesLink = new RouterLink("Familien", FamiliesView.class);
            RouterLink familyLink = new RouterLink("Familie: " + currentFamily.getFamilyName(),
                    FamilyDetailsView.class, new RouteParameters("id", currentFamily.getId().toString()));
            bcrumbs.add(s1, familiesLink, s2, familyLink);
        }

        Span s = new Span(" >> ");
        s.getStyle().set("font-size", "var(--lumo-font-size-s)").set("color", "var(--lumo-secondary-text-color)");

        Span docs = new Span("Dokumente");
        docs.getStyle().set("font-size", "var(--lumo-font-size-s)").set("font-weight", "bold")
                .set("color", "var(--lumo-body-text-color)");
        bcrumbs.add(s, docs);
        add(bcrumbs);
    }

    private void buildHeader() {
        VerticalLayout header = new VerticalLayout();
        header.setSpacing(false);
        header.setPadding(false);
        header.setWidthFull();

        HorizontalLayout titleLayout = new HorizontalLayout();
        H2 title = new H2();
        if (familyId != 0)
            title.setText("Dokumente: " + currentFamily.getFamilyName());
        else
            title.setText("Dokumente: alle");
        title.getStyle().set("margin-bottom", "0");
        titleLayout.add(title);

        String role = currentEmployee.getRole().getLabel();
        String name = currentEmployee.getFirstName() + " " + currentEmployee.getLastName();
        Span employeeInfo = new Span(role + ": " + name);
        header.add(titleLayout, employeeInfo);
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

        searchButton = new Button(VaadinIcon.SEARCH.create(), e -> refreshGrid());

        resetButton = new Button(VaadinIcon.REFRESH.create(), e ->
                UI.getCurrent().navigate(DocumentsView.class));

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
        filterLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        filterLayout.setSpacing(true);
        filterLayout.setPadding(true);
        filterLayout.setVisible(false);

        filterLayout.getStyle().set("background-color", "#fff3cd").set("border-radius", "6px")
                .set("border", "1px solid #e0c97f");

        if (familyId == 0) {
            VerticalLayout famLayout = new VerticalLayout();
            famLayout.setSpacing(false);
            famLayout.setPadding(false);

            Span famLabel = new Span("Familie");
            famLabel.getStyle().set("font-weight", "bold");

            familyFilter = new ComboBox<>();
            familyFilter.setItems(currentFamilies);
            familyFilter.setItemLabelGenerator(Family::getFamilyName);
            familyFilter.setClearButtonVisible(true);
            familyFilter.addValueChangeListener(e -> refreshGrid());

            famLayout.add(famLabel, familyFilter);
            filterLayout.add(famLayout);
        }

        VerticalLayout assignedLayout = new VerticalLayout();
        assignedLayout.setSpacing(false);
        assignedLayout.setPadding(false);

        Span assignedLabel = new Span("Berater*in");
        assignedLabel.getStyle().set("font-weight", "bold");

        assignedEmployeeFilter = new ComboBox<>();
        assignedEmployeeFilter.setItems(assignedEmployees);
        assignedEmployeeFilter.setItemLabelGenerator(e -> e.getFirstName() + " " + e.getLastName());
        assignedEmployeeFilter.setClearButtonVisible(true);
        assignedEmployeeFilter.addValueChangeListener(e -> refreshGrid());

        assignedLayout.add(assignedLabel, assignedEmployeeFilter);

        VerticalLayout uploadLayout = new VerticalLayout();
        uploadLayout.setSpacing(false);
        uploadLayout.setPadding(false);

        Span uploadLabel = new Span("Hochgeladen von");
        uploadLabel.getStyle().set("font-weight", "bold");

        uploaderEmployeeFilter = new ComboBox<>();
        uploaderEmployeeFilter.setItems(createdByEmployees);
        uploaderEmployeeFilter.setItemLabelGenerator(e -> e.getFirstName() + " " + e.getLastName());
        uploaderEmployeeFilter.setClearButtonVisible(true);
        uploaderEmployeeFilter.addValueChangeListener(e -> refreshGrid());

        uploadLayout.add(uploadLabel, uploaderEmployeeFilter);

        VerticalLayout fromLayout = new VerticalLayout();
        fromLayout.setSpacing(false);
        fromLayout.setPadding(false);

        Span fromLabel = new Span("Von");
        fromLabel.getStyle().set("font-weight", "bold");

        dateFrom = new DatePicker();
        dateFrom.addValueChangeListener(e -> {
            filterDateFrom = e.getValue();
            refreshGrid();
        });

        fromLayout.add(fromLabel, dateFrom);

        VerticalLayout toLayout = new VerticalLayout();
        toLayout.setSpacing(false);
        toLayout.setPadding(false);

        Span toLabel = new Span("Bis");
        toLabel.getStyle().set("font-weight", "bold");

        dateTo = new DatePicker();
        dateTo.addValueChangeListener(e -> {
            filterDateTo = e.getValue();
            refreshGrid();
        });

        toLayout.add(toLabel, dateTo);

        filterLayout.add(assignedLayout, uploadLayout, fromLayout, toLayout);
        add(filterLayout);
    }

    private void buildActionButtons() {
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setSpacing(false);

        uploadButton = new Button("Hochladen", VaadinIcon.UPLOAD.create());
        uploadButton.addClickListener(e -> {
            if (currentFamily == null && familyId == 0) {
                Dialog pick = new Dialog();
                pick.setModal(true);
                pick.setWidth("400px");

                ComboBox<Family> familyComboBox = new ComboBox<>("Familie");
                familyComboBox.setItems(currentFamilies);
                familyComboBox.setItemLabelGenerator(Family::getFamilyName);
                familyComboBox.setWidthFull();

                Button next = new Button("Weiter");
                next.setEnabled(false);
                familyComboBox.addValueChangeListener(
                        ev -> next.setEnabled(ev.getValue() != null));
                next.addClickListener(ev -> {
                    currentFamily = familyComboBox.getValue();
                    familyId = currentFamily.getId();
                    pick.close();
                    uploadButton.click();
                });

                Button cancel = new Button("Abbrechen", ev -> pick.close());

                HorizontalLayout btns = new HorizontalLayout(cancel, next);
                btns.setJustifyContentMode(JustifyContentMode.END);
                btns.setWidthFull();

                pick.add(familyComboBox);
                pick.getFooter().add(btns);
                pick.open();
                return;
            }
            Dialog dlg = new Dialog();
            dlg.setModal(true);
            dlg.setWidth("600px");

            Span info = new Span("Hochladen für Familie: " + currentFamily.getFamilyName());

            Upload upload = new Upload((UploadHandler) event -> {
                try (InputStream in = event.getInputStream()) {
                    VaadinRequest req = VaadinRequest.getCurrent();

                    FamilyDocument saved = familyDocumentService.uploadDocument(currentFamily, event.getFileName(),
                            event.getContentType(), in, currentEmployee, req != null ? req.getRemoteAddr() : "UNKNOWN",
                            req != null ? req.getHeader("User-Agent") : "UNKNOW");
                    currentDocuments.add(saved);
                } catch (Exception ex) {
                    showOkDialog("Fehler", ex.getMessage());
                }
            });
            upload.setDropAllowed(true);
            upload.setMaxFiles(10);
            upload.setMaxFileSize(5 * 1024 * 1024);
            upload.addFileRejectedListener(ev ->
                    showOkDialog("Upload-Fehler", ev.getErrorMessage()));

            upload.addAllFinishedListener(ev -> {
                dlg.close();
                refreshGrid();
            });

            VerticalLayout content = new VerticalLayout(info, upload);
            content.setSpacing(true);

            dlg.add(content);
            dlg.open();
        });
        buttonLayout.add(uploadButton);
        add(buttonLayout);
    }

    private void buildGrid() {
        documentsGrid = new Grid();
        documentsGrid.setSizeFull();
        documentsGrid.setSelectionMode(Grid.SelectionMode.SINGLE);

        documentsGrid.addColumn(FamilyDocument::getId)
                .setHeader("ID").setAutoWidth(true).setFlexGrow(0)
                .setComparator(FamilyDocument::getId);
        documentsGrid.addColumn(FamilyDocument::getOriginalFileName)
                .setHeader("Dateiname").setAutoWidth(true).setFlexGrow(0)
                .setComparator(FamilyDocument::getOriginalFileName);
        documentsGrid.addColumn(d -> d.getFamily() != null ? d.getFamily().getFamilyName() : "")
                .setHeader("Familie").setAutoWidth(true).setFlexGrow(0)
                .setComparator(d -> d.getFamily() != null ? d.getFamily().getFamilyName() : "");
        documentsGrid.addColumn(d -> d.getUploadedByEmployee() != null
                        ? d.getUploadedByEmployee().getFirstName() + " " + d.getUploadedByEmployee().getLastName() : "")
                .setHeader("Hochgeladen von").setAutoWidth(true).setFlexGrow(0)
                .setComparator(d -> d.getUploadedByEmployee() != null
                        ? d.getUploadedByEmployee().getFirstName() : "");
        documentsGrid.addColumn(d -> d.getUpdatedAt() != null ? d.getUpdatedAt().toLocalDate()
                        .format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) : "")
                .setHeader("Datum").setAutoWidth(true).setFlexGrow(0)
                .setComparator(d -> d.getUpdatedAt() != null);
        documentsGrid.addColumn(d -> String.format("%.2f", (double) d.getFileSizeBytes() / 1024 / 1024))
                .setHeader("Größer (Mb)").setAutoWidth(true).setFlexGrow(0)
                .setComparator(d -> String.valueOf(d.getFileSizeBytes()));

        documentsGrid.addItemClickListener(e -> {
            if (lvl == 10) {
                return;
            }
            FamilyDocument d = e.getItem();
            if (d == null) {
                return;
            }
        });
        refreshGrid();
        add(documentsGrid);
    }

    private void refreshGrid() {
        List<FamilyDocument> result = new ArrayList<>();

        String q = searchField != null ? searchField.getValue() : null;
        Family fam = familyFilter != null ? familyFilter.getValue() : null;
        Employee assigned = assignedEmployeeFilter != null ? assignedEmployeeFilter.getValue() : null;
        Employee uploader = uploaderEmployeeFilter != null ? uploaderEmployeeFilter.getValue() : null;

        for (FamilyDocument d : currentDocuments) {
            if (d == null) continue;
            Family f = d.getFamily();
            if (f == null) continue;
            if (familyId != 0 && f.getId() != familyId) continue;
            if (familyId == 0 && fam != null && !f.equals(fam)) continue;
            if (assigned != null) {
                if (f.getAssignedEmployee() == null) continue;
                if (!f.getAssignedEmployee().equals(assigned)) continue;
            }
            if (uploader != null) {
                if (d.getUploadedByEmployee() == null) continue;
                if (!d.getUploadedByEmployee().equals(uploader)) continue;
            }
            if (!isInDateRange(d)) continue;
            if (!matchesSearch(d, q)) continue;
            result.add(d);
        }
        documentsGrid.setItems(result);
    }

    private void buildDocumentDialog(FamilyDocument d) {
        if (d == null) return;

        Dialog dialog = new Dialog();
        dialog.setModal(true);
        dialog.setDraggable(false);
        dialog.setResizable(false);
        dialog.setCloseOnOutsideClick(false);
        dialog.setWidth("900px");

        VerticalLayout root = new VerticalLayout();
        root.setSpacing(true);
        root.setPadding(true);
        root.setWidthFull();

        H2 title = new H2("Dokument ID: " + d.getId());
        root.add(title);
        Span famName = new Span("Familie: " + d.getFamily().getFamilyName());
        root.add(famName);

        HorizontalLayout nameLayout = new HorizontalLayout();
        nameLayout.setSpacing(false);
        nameLayout.setAlignItems(FlexComponent.Alignment.CENTER);

        if (lvl != 10 && !d.getFamily().getStatus().equals(RecordStatus.ARCHIVED) && !d.getFamily().isCaseClosed()
                && !d.getFamily().getStatus().equals(RecordStatus.BLOCKED)) {
            Button nameEdit = new Button(VaadinIcon.EDIT.create());
            nameEdit.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            nameEdit.addClassName("edit-btn");
            nameEdit.addClickListener(e -> {
                EditDialogFactory.openEditDialog("Dateiname ändern", List.of(new EditField("name", "Dateiname",
                                EditField.Type.TEXT, d.getOriginalFileName(), true, 255, null)),
                        values -> {
                            try {
                                String newName = values.get("name") != null ? values.get("name").toString().trim() : null;
                                if (newName == null || newName.isBlank()) return;
                                FamilyDocument doc = familyDocumentService.getDocument(d.getFamily(), currentEmployee, d.getId());
                                doc.setOriginalFileName(newName);
                                VaadinRequest req = VaadinRequest.getCurrent();
                                familyDocumentService.updateName(d, doc, currentEmployee,
                                        req != null ? req.getRemoteAddr() : "UNKNOWN",
                                        req != null ? req.getHeader("User-Agent") : "UNKNOWN");
                                getUI().ifPresent(ui -> ui.getPage().reload());
                            } catch (Exception ex) {
                                Dialog err = new Dialog();
                                err.setHeaderTitle("Fehler");
                                err.add(new Span(ex.getMessage()));
                                err.add(new Button("OK", x -> err.close()));
                                err.open();
                            }
                        });
            });
            nameLayout.add(nameEdit);
        }

        Span docName = new Span("Dateiname: " + d.getOriginalFileName());
        nameLayout.add(docName);
        root.add(nameLayout);

        Span size = new Span("Größe: " + d.getFileSizeBytes() + " Bytes");
        root.add(size);
        Span uploadedBy = new Span("Hochgeladen von: "
                + (d.getUploadedByEmployee() != null ? d.getUploadedByEmployee().getFirstName()
                + " " + d.getUploadedByEmployee().getLastName() : ""));
        root.add(uploadedBy);

        HorizontalLayout descHLayout = new HorizontalLayout();
        descHLayout.setSpacing(false);
        descHLayout.setAlignItems(FlexComponent.Alignment.CENTER);

        if (lvl != 10 && !d.getFamily().getStatus().equals(RecordStatus.ARCHIVED) && !d.getFamily().isCaseClosed()
                && !d.getFamily().getStatus().equals(RecordStatus.BLOCKED)) {
            Button descEdit = new Button(VaadinIcon.EDIT.create());
            descEdit.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            descEdit.addClassName("edit-btn");
            descEdit.addClickListener(e -> {
                EditDialogFactory.openEditDialog("Beschreibung ändern", List.of(new EditField(
                        "desc", "Beschreibung", EditField.Type.TEXTAREA, d.getDescription(),
                        false, 4000, null)), values -> {
                    try {
                        String text = values.get("desc") != null ? values.get("desc").toString().trim() : null;
                        FamilyDocument updated = new FamilyDocument();
                        updated.setDescription(text);
                        VaadinRequest req = VaadinRequest.getCurrent();
                        familyDocumentService.updateDescription(d, updated, currentEmployee,
                                req != null ? req.getRemoteAddr() : "UNKNOWN",
                                req != null ? req.getHeader("User-Agent") : "UNKNOWN");
                        getUI().ifPresent(ui -> ui.getPage().reload());
                    } catch (Exception ex) {
                        Dialog err = new Dialog();
                        err.setHeaderTitle("Fehler");
                        err.add(new Span(ex.getMessage()));
                        err.add(new Button("OK", x -> err.close()));
                        err.open();
                    }
                });
            });
            descHLayout.add(descEdit);
        }

        Span desc = new Span("Beschreibung");
        descHLayout.add(desc);

        TextArea descArea = new TextArea();
        descArea.setReadOnly(true);
        descArea.setWidthFull();
        descArea.setHeight("200px");
        descArea.setValue(d.getDescription());
        descArea.getStyle().set("white-space", "pre-wrap");

        root.add(descHLayout, descArea);

        root.add(buildAuditBlock(d));

        Button close = new Button("Schließen", e -> dialog.close());
        HorizontalLayout footer = new HorizontalLayout(close);
        footer.setJustifyContentMode(JustifyContentMode.END);
        footer.setWidthFull();

        dialog.add(root);
        dialog.getFooter().add(footer);
        dialog.open();
    }

    private VerticalLayout buildAuditBlock(FamilyDocument d) {
        VerticalLayout block = new VerticalLayout();
        block.setSpacing(false);
        block.setPadding(false);
        block.setWidthFull();

        block.getStyle().set("border", "1px solid #ddd").set("border-radius", "6px").set("padding", "10px");

        Span title = new Span("Verlauf");
        title.getStyle().set("font-weight", "bold");
        block.add(title);

        if (d.getUploadedAt() != null) {
            Employee up = d.getUploadedByEmployee();
            block.add(makeAuditLine("Erstellt am " + formatDate(d.getUploadedAt())
                    + " um " + formatTime(d.getUploadedAt())
                    + " von " + (up != null ? up.getLogin() : "kA")));
        }
        if (d.getUpdatedAt() != null) {
            block.add(makeAuditLine("Geändert am " + formatDate(d.getUpdatedAt())
                    + " um " + formatTime(d.getUpdatedAt()) + " von " + empty(d.getUpdatedBy())));
        }
        if (d.getInvalidAt() != null) {
            block.add(makeAuditLine("Gelöscht am " + formatDate(d.getInvalidAt())
                    + " um " + formatTime(d.getInvalidAt()) + " von " + empty(d.getInvalidBy())));
        }
        if (d.getInvalidAt() != null) {
            block.add("Wiederherstellt am " + formatDate(d.getRestoredAt())
                    + " um " + formatTime(d.getRestoredAt()) + " von " + empty(d.getRestoredBy()));
            if (d.getRestoredReason() != null) {
                block.add("Grund: " + d.getRestoredReason());
            }
        }
        return block;
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

    private boolean isInDateRange(FamilyDocument d) {
        LocalDateTime base = d.getUpdatedAt() != null ? d.getUpdatedAt() : d.getUploadedAt();
        if (base == null) return false;
        LocalDate date = base.toLocalDate();
        if (filterDateFrom != null && date.isBefore(filterDateFrom)) return false;
        if (filterDateTo != null && date.isAfter(filterDateTo)) return false;

        return true;
    }

    private boolean matchesSearch(FamilyDocument d, String q) {
        if (q == null || q.isBlank()) return true;
        String qq = q.toLowerCase().trim();
        if (d.getOriginalFileName() != null && d.getOriginalFileName().toLowerCase().contains(qq)) return true;
        if (d.getDescription() != null && d.getDescription().toLowerCase().contains(qq)) return true;
        Family f = d.getFamily();
        if (f != null && f.getFamilyName() != null && f.getFamilyName().toLowerCase().contains(qq)) return true;
        return false;
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

    private String formatDate(LocalDateTime dt) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        return dt.format(dateFormatter);
    }

    private String formatTime(LocalDateTime dt) {
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        return dt.format(timeFormatter);
    }
}
