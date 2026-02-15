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
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.streams.DownloadEvent;
import com.vaadin.flow.server.streams.UploadHandler;
import jakarta.annotation.security.PermitAll;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Semaphore;

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
    private Button filterToggleButton;
    private Button uploadButton;
    private TextField searchField;
    private ComboBox<String> fileTypeFilter;
    private ComboBox<Employee> uploaderEmployeeFilter;
    private ComboBox<Family> familyFilter;
    private LocalDate filterDateFrom;
    private LocalDate filterDateTo;
    private List<Employee> assignedEmployees = new ArrayList<>();
    private List<Employee> createdByEmployees = new ArrayList<>();
    private Grid<FamilyDocument> documentsGrid;
    private DatePicker dateFromFilter;
    private DatePicker dateToFilter;
    private static final Semaphore UPLOAD_LOCK = new Semaphore(1);
    @Value("${storage.base-path}")
    private String storageBasePath;

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Employee e = authService.getCurrentEmployee();
        if (e == null || !e.isActive() || e.isArchived()) {
            event.forwardTo("login");
            return;
        }

        this.currentEmployee = employeeService.findByLogin(e.getLogin());
        this.lvl = currentEmployee.getRole().getAccessLevel();

        List<Delegation> delegations = new ArrayList<>(delegationService.getDelegations().stream().filter(d ->
                delegationService.isDelegationActive(d) && d.getToEmployee().equals(currentEmployee)
                        && (d.getFamily().getStatus().equals(RecordStatus.ACTIVE)
                        || d.getFamily().getStatus().equals(RecordStatus.ARCHIVED))).toList());

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
                                break;
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
        currentFamilies = new ArrayList<>();
        if (familyId == 0 || currentFamily == null) {
            if (lvl == 50) {
                currentFamilies = new ArrayList<>(familyService.getFamiliesByAssignedEmployee(currentEmployee.getLogin(),
                        currentEmployee).stream().filter(f -> (f.getStatus().equals(RecordStatus.ARCHIVED)
                        || f.getStatus().equals(RecordStatus.ACTIVE))).toList());
                for (Family f : currentFamilies)
                    currentDocuments.addAll(familyDocumentService.getDocumentsByFamily(f, currentEmployee)
                            .stream().filter(this::isVisibleDoc).toList());
                for (Delegation dlg : delegations)
                    if (dlg.getToEmployee().equals(currentEmployee))
                        currentDocuments.addAll(familyDocumentService.getDocumentsByFamily(dlg.getFamily(), currentEmployee)
                                .stream().filter(this::isVisibleDoc).toList());
            } else if (lvl == 80 || lvl == 100) {
                currentFamilies = new ArrayList<>(familyService.getFamilies().stream()
                        .filter(f -> !f.getStatus().equals(RecordStatus.INVALID)).toList());
                currentDocuments = new ArrayList<>(familyDocumentService.getAllDocuments(currentEmployee)
                        .stream().filter(this::isVisibleDoc).toList());
            }
        } else
            currentDocuments.addAll(familyDocumentService.getDocumentsByFamily(currentFamily, currentEmployee)
                    .stream().filter(this::isVisibleDoc).toList());

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
        if (lvl != 10)
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

        Button searchButton = new Button(VaadinIcon.SEARCH.create(), e -> refreshGrid());

        Button resetButton = new Button(VaadinIcon.REFRESH.create(), e -> {
            searchField.clear();
            familyFilter.clear();
            fileTypeFilter.clear();
            dateFromFilter.clear();
            dateToFilter.clear();
            uploaderEmployeeFilter.clear();
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

        VerticalLayout fileTypeLayout = new VerticalLayout();
        fileTypeLayout.setSpacing(false);
        fileTypeLayout.setPadding(false);

        Span fileTypeLabel = new Span("Typ");
        fileTypeLabel.getStyle().set("font-weight", "bold");

        fileTypeFilter = new ComboBox<>();
        fileTypeFilter.setItems(new ArrayList<>(collectFileTypes()));
        fileTypeFilter.setClearButtonVisible(true);
        fileTypeFilter.addValueChangeListener(e -> refreshGrid());

        fileTypeLayout.add(fileTypeLabel, fileTypeFilter);

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

        dateFromFilter = new DatePicker();
        dateFromFilter.setMin(LocalDate.of(2026, 1, 1));
        dateFromFilter.addValueChangeListener(e -> {
            filterDateFrom = e.getValue();
            refreshGrid();
        });

        fromLayout.add(fromLabel, dateFromFilter);

        VerticalLayout toLayout = new VerticalLayout();
        toLayout.setSpacing(false);
        toLayout.setPadding(false);

        Span toLabel = new Span("Bis");
        toLabel.getStyle().set("font-weight", "bold");

        dateToFilter = new DatePicker();
        dateToFilter.setMin(LocalDate.of(2026, 1, 1));
        dateToFilter.addValueChangeListener(e -> {
            filterDateTo = e.getValue();
            refreshGrid();
        });

        toLayout.add(toLabel, dateToFilter);

        filterLayout.add(fileTypeLayout, uploadLayout, fromLayout, toLayout);
        add(filterLayout);
    }

    private void buildActionButtons() {
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setSpacing(true);
        buttonLayout.setAlignItems(FlexComponent.Alignment.CENTER);

        uploadButton = new Button("Hochladen", VaadinIcon.UPLOAD.create());
        uploadButton.addClickListener(e -> {
            if (currentFamily == null && familyId == 0) {
                Dialog pick = new Dialog();
                pick.setModal(true);
                pick.setWidth("400px");

                ComboBox<Family> familyComboBox = new ComboBox<>("Familie");
                familyComboBox.setItems(currentFamilies.stream().filter(
                        f -> f.getStatus().equals(RecordStatus.ACTIVE)).toList());
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
            dlg.setWidth("450px");

            Span info = new Span("Hochladen für Familie: " + currentFamily.getFamilyName());
            Span info2 = new Span("Max bis 5 MB/File");
            info2.getStyle().set("font-size", "11px").set("padding", "2px 6px").set("margin-bottom", "1px");
            Span info3 = new Span("Erlaubt: \".pdf\",\".doc\", \".docx\", \".xls\", \".xlsx\", \".jpg\", \".jpeg\", \".png\"");
            info3.getStyle().set("font-size", "11px").set("padding", "2px 6px").set("margin-bottom", "1px");

            Upload upload = new Upload((UploadHandler) event -> {
                boolean acquired = false;
                try (InputStream in = event.getInputStream()) {
                    UPLOAD_LOCK.acquire();
                    acquired = true;
                    VaadinRequest req = VaadinRequest.getCurrent();

                    FamilyDocument saved = familyDocumentService.uploadDocument(currentFamily, event.getFileName(),
                            event.getContentType(), in, currentEmployee, req != null ? req.getRemoteAddr() : "UNKNOWN",
                            req != null ? req.getHeader("User-Agent") : "UNKNOW");
                    currentDocuments.add(saved);
                } catch (Exception ex) {
                    UI ui = UI.getCurrent();
                    if (ui != null) ui.access(() -> showOkDialog("Upload-Fehler", ex.getMessage()));
                } finally {
                    if (acquired) UPLOAD_LOCK.release();
                }
            });
            upload.setAcceptedFileTypes(".pdf", ".doc", ".docx", ".xls", ".xlsx", ".jpg", ".jpeg", ".png");
            upload.setWidth("300px");
            upload.setHeight("300px");
            upload.setDropAllowed(true);
            upload.setMaxFiles(10);
            upload.setMaxFileSize(5 * 1024 * 1024);
            upload.addFileRejectedListener(ev ->
                    showOkDialog("Upload-Fehler", ev.getErrorMessage()));

            upload.addAllFinishedListener(ev -> {
                dlg.close();
                getUI().ifPresent(ui -> ui.getPage().reload());
            });

            VerticalLayout content = new VerticalLayout(info, info2, info3, upload);
            content.setSpacing(true);

            dlg.add(content);
            dlg.open();
        });

        Button trashButton = new Button("Papierkorb", VaadinIcon.TRASH.create());
        trashButton.setWidth("90px");
        trashButton.setHeight("26px");
        trashButton.getStyle().set("font-size", "11px").set("padding", "2px 6px")
                .set("color", "#666").set("background", "#f2f2f2");
        trashButton.addClickListener(e -> buildTrashDialog());

        buttonLayout.add(uploadButton, trashButton);
        add(buttonLayout);
    }

    private void buildGrid() {
        documentsGrid = new Grid<>(FamilyDocument.class, false);
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
        documentsGrid.addColumn(d -> String.format("%.2f", (double) d.getFileSizeBytes() / 1024 / 1024))
                .setHeader("Größer (Mb)").setAutoWidth(true).setFlexGrow(0)
                .setComparator(d -> String.valueOf(d.getFileSizeBytes()));
        if (lvl != 10) {
            documentsGrid.addColumn(new ComponentRenderer<>(d -> {
                HorizontalLayout hl = new HorizontalLayout();
                hl.setSpacing(true);

                HorizontalLayout dL = new HorizontalLayout();
                dL.setSpacing(true);

                HorizontalLayout pL = new HorizontalLayout();
                pL.setSpacing(true);

                Anchor download = new Anchor((DownloadEvent dEv) -> {
                    dEv.setFileName(d.getOriginalFileName());
                    if (d.getFileType() != null) dEv.setContentType(d.getFileType());
                    dEv.getResponse().setHeader("Content-Disposition", "attachment; filename*=UTF-8''"
                            + URLEncoder.encode(d.getOriginalFileName(), StandardCharsets.UTF_8));
                    try (InputStream in = Files.newInputStream(Paths.get(storageBasePath,
                            String.valueOf(d.getFamily().getId()), d.getStoredFileName()));
                         var out = dEv.getOutputStream()) {
                        in.transferTo(out);
                    }
                }, "");

                download.getElement().setAttribute("download", true);
                Button dlBtn = new Button(VaadinIcon.DOWNLOAD.create());
                dlBtn.setTooltipText("Herunterladen");
                dlBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
                dlBtn.addClassName("edit-btn");
                download.add(dlBtn);
                dL.add(download);

                if (isPreviewable(d)) {
                    String previewUrl = "/documents/preview/" + d.getId();

                    Anchor previewLink = new Anchor(previewUrl, "");
                    previewLink.setTarget("_blank");

                    Button preview = new Button(VaadinIcon.EYE.create());
                    preview.setTooltipText("Vorschau");
                    preview.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
                    preview.addClassName("edit-btn");

                    previewLink.add(preview);
                    pL.add(previewLink);
                }
                hl.add(dL, pL);
                return hl;
            })).setHeader("Aktionen").setAutoWidth(true).setFlexGrow(0);
        }
        documentsGrid.addItemClickListener(e -> {
            if (lvl == 10) {
                return;
            }
            FamilyDocument d = e.getItem();
            if (d == null) {
                return;
            }
            buildDocumentDialog(e.getItem());
        });
        refreshGrid();
        add(documentsGrid);
    }

    private void refreshGrid() {
        List<FamilyDocument> result = new ArrayList<>();

        String q = searchField != null ? searchField.getValue() : null;
        Family fam = familyFilter != null ? familyFilter.getValue() : null;
        String fileType = fileTypeFilter != null ? fileTypeFilter.getValue() : null;
        Employee uploader = uploaderEmployeeFilter != null ? uploaderEmployeeFilter.getValue() : null;

        for (FamilyDocument d : currentDocuments) {
            if (d == null) continue;
            Family f = d.getFamily();
            if (f == null) continue;
            if (familyId != 0 && f.getId() != familyId) continue;
            if (familyId == 0 && fam != null && !f.equals(fam)) continue;
            if (fileType != null) {
                if (d.getOriginalFileName() == null) continue;
                if (!Objects.equals(toReadableType(d.getOriginalFileName()), fileType)) continue;
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
                EditDialogFactory.openEditDialog("Dateiname ändern", List.of(new EditField("name",
                                "Dateiname", EditField.Type.TEXT, d.getOriginalFileName(), true,
                                255, null, null, null)),
                        values -> {
                            try {
                                String newName = values.get("name") != null ? values.get("name").toString().trim() : null;
                                if (newName == null || newName.isBlank()) return;
                                FamilyDocument doc = familyDocumentService.getDocument(currentEmployee, d.getId());
                                doc.setOriginalFileName(newName);
                                VaadinRequest req = VaadinRequest.getCurrent();
                                FamilyDocument document = familyDocumentService.updateName(d, doc, currentEmployee,
                                        req != null ? req.getRemoteAddr() : "UNKNOWN",
                                        req != null ? req.getHeader("User-Agent") : "UNKNOWN");
                                buildDocumentDialog(document);
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

        Span size = new Span("Größe: " + String.format("%.2f", (double) d.getFileSizeBytes() / 1024 / 1024) + " Mb");
        root.add(size);
        Span uploadedBy = new Span("Hochgeladen von: "
                + (d.getUploadedByEmployee() != null ? d.getUploadedByEmployee().getFirstName()
                + " " + d.getUploadedByEmployee().getLastName() : ""));
        Span uploadedAt = new Span("Hochgeladen am: " + (d.getUploadedAt() != null ? d.getUploadedAt() : ""));
        root.add(uploadedBy, uploadedAt);

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
                        "desc", "Beschreibung", EditField.Type.TEXTAREA, d.getDescription(), false,
                        4000, null, null, null)), values -> {
                    try {
                        String text = values.get("desc") != null ? values.get("desc").toString().trim() : null;
                        FamilyDocument updated = new FamilyDocument();
                        updated.setDescription(text);
                        VaadinRequest req = VaadinRequest.getCurrent();
                        FamilyDocument document = familyDocumentService.updateDescription(d, updated, currentEmployee,
                                req != null ? req.getRemoteAddr() : "UNKNOWN",
                                req != null ? req.getHeader("User-Agent") : "UNKNOWN");
                        buildDocumentDialog(document);
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
        descArea.setValue(d.getDescription() != null ? d.getDescription() : "");
        descArea.getStyle().set("white-space", "pre-wrap");

        root.add(descHLayout, descArea);

        root.add(buildAuditBlock(d));

        if (lvl != 10 || d.getFamily().getStatus().equals(RecordStatus.ACTIVE) || !d.getFamily().isCaseClosed()) {
            HorizontalLayout block = new HorizontalLayout();
            block.setSpacing(false);
            block.setPadding(false);
            block.setWidthFull();

            Button delete = new Button("Datei löschen", e -> {
                showConfirmDialog("Datei löschen", "Wollen Sie die Datei löschen?", () -> {
                    try {
                        VaadinRequest req = VaadinRequest.getCurrent();
                        familyDocumentService.invalidateDocument(d.getFamily(), d, currentEmployee,
                                req != null ? req.getRemoteAddr() : "UNKNOWN",
                                req != null ? req.getHeader("User-Agent") : "UNKNOWN");
                        dialog.close();
                        getUI().ifPresent(ui -> ui.getPage().reload());
                    } catch (Exception ex) {
                        showOkDialog("Fehler", ex.getMessage());
                        dialog.close();
                    }
                }, null);
            });
            delete.getStyle().set("color", "red");
            block.add(delete);
            root.add(block);
        }

        Button close = new Button("Schließen", e -> {
            dialog.close();
            getUI().ifPresent(ui -> ui.getPage().reload());
        });
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
            block.add(makeAuditLine("Wiederherstellt am " + formatDate(d.getRestoredAt())
                    + " um " + formatTime(d.getRestoredAt()) + " von " + empty(d.getRestoredBy())));
            if (d.getRestoredReason() != null) {
                block.add(makeAuditReason("Grund: " + d.getRestoredReason()));
            }
        }
        return block;
    }

    private void buildTrashDialog() {
        Dialog dialog = new Dialog();
        dialog.setModal(true);
        dialog.setCloseOnOutsideClick(false);
        dialog.setWidth("1100px");
        dialog.setHeight("550px");

        Span title = new Span("Papierkorb: Dokumente");
        title.getStyle().set("font-weight", "bold");

        Grid<FamilyDocument> grid = new Grid<>();
        grid.setSizeFull();
        grid.setSelectionMode(Grid.SelectionMode.SINGLE);

        grid.addColumn(FamilyDocument::getId)
                .setHeader("ID").setAutoWidth(true).setFlexGrow(0)
                .setComparator(FamilyDocument::getId);

        grid.addColumn(d -> d != null ? d.getOriginalFileName() : "")
                .setHeader("Dateiname").setAutoWidth(true).setFlexGrow(0)
                .setComparator(d -> d != null ? d.getOriginalFileName() : "");

        grid.addColumn(d -> d != null ? d.getFamily().getFamilyName() : "")
                .setHeader("Familie").setAutoWidth(true).setFlexGrow(0)
                .setComparator(d -> d != null ? d.getFamily().getFamilyName() : "");

        grid.addColumn(d -> String.format("%.2f", (double) d.getFileSizeBytes() / 1024 / 1024))
                .setHeader("Größe (Mb)").setAutoWidth(true).setFlexGrow(0)
                .setComparator(FamilyDocument::getFileSizeBytes);

        grid.addColumn(d -> {
                    Employee emp = employeeService.findByLogin(d.getInvalidBy());
                    return emp != null ? emp.getFirstName() + " " + emp.getLastName() : "";
                })
                .setHeader("Gelöscht von").setAutoWidth(true).setFlexGrow(0)
                .setComparator(d -> {
                    Employee emp = employeeService.findByLogin(d.getInvalidBy());
                    return emp != null ? emp.getFirstName() + " " + emp.getLastName() : "";
                });

        grid.addColumn(d -> formatDate(d.getInvalidAt()) + " um " + formatTime(d.getInvalidAt()) + " Uhr")
                .setHeader("Gelöscht am").setAutoWidth(true).setFlexGrow(0)
                .setComparator(FamilyDocument::getInvalidBy);

        if (currentEmployee.getRole().getAccessLevel() == 50) {
            grid.addColumn(new ComponentRenderer<>(d -> {
                        Span span = new Span();
                        if (d.getInvalidAt() != null) {
                            long days = Duration.between(d.getInvalidAt(), LocalDateTime.now()).toDays();
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

        grid.setItems(loadTrashDocs());

        Button restore = new Button("Wiederherstellen");
        restore.setEnabled(false);

        Button close = new Button("Schließen", e -> dialog.close());

        grid.addSelectionListener(
                e -> restore.setEnabled(e.getFirstSelectedItem().isPresent()));

        restore.addClickListener(e -> {
            FamilyDocument d = grid.asSingleSelect().getValue();
            if (d == null) return;

            Runnable restoreWithoutReason = () -> {
                try {
                    VaadinRequest req = VaadinRequest.getCurrent();
                    familyDocumentService.restoreDocument(d.getFamily(), d,
                            "Wiederherstellung über Papierkorb", currentEmployee,
                            req != null ? req.getRemoteAddr() : "UNKNOWN",
                            req != null ? req.getHeader("User-Agent") : "UNKNOWN");
                    dialog.close();
                    getUI().ifPresent(ui -> ui.getPage().reload());
                } catch (Exception ex) {
                    showOkDialog("Fehler", ex.getMessage());
                }
            };

            if (d.getInvalidAt() == null) {
                showOkDialog("Fehler", "Invalid-Datum fehlt.");
                return;
            }

            long days = Duration.between(d.getInvalidAt(), LocalDateTime.now()).toDays();

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
                        familyDocumentService.restoreDocument(d.getFamily(), d, reason.getValue().trim(),
                                currentEmployee, req != null ? req.getRemoteAddr() : "UNKNOWN",
                                req != null ? req.getHeader("User-Agent") : "UNKNOWN");
                        reasonDialog.close();
                        dialog.close();
                        getUI().ifPresent(ui -> ui.getPage().reload());
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
            showOkDialog("Nicht möglich", "Älter als 14 Tage: nur Admin/Lead mit Begründung.");
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

    private List<FamilyDocument> loadTrashDocs() {
        List<FamilyDocument> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (FamilyDocument d : familyDocumentService.getAllDocuments(currentEmployee)) {
            if (!d.isInvalid()) continue;
            if (d.getInvalidAt() == null) continue;

            Family f = d.getFamily();
            if (f == null) continue;

            if (!RecordStatus.ACTIVE.equals(f.getStatus())) continue;

            long days = Duration.between(d.getInvalidAt(), now).toDays();

            if (lvl == 50) {
                boolean isOwn = d.getFamily().getAssignedEmployee().equals(currentEmployee);
                boolean isDelegated = delegationService.hasActiveDelegationTo(d.getFamily(), currentEmployee);
                if (!isOwn && !isDelegated) continue;
                if (days > 14) continue;
            }
            result.add(d);
        }
        return result;
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

    private List<String> collectFileTypes() {
        return currentDocuments.stream().map(FamilyDocument::getOriginalFileName).filter(Objects::nonNull)
                .map(this::toReadableType).filter(Objects::nonNull).distinct().sorted().toList();
    }

    private String toReadableType(String name) {
        if (name == null) return null;

        String n = name.toLowerCase(Locale.ROOT);

        if (n.endsWith("pdf")) return "PDF";
        if (n.endsWith("doc") || n.endsWith("docx") || n.endsWith("word")) return "DOC/DOCX";
        if (n.endsWith("xls") || n.endsWith("xlsx") || n.endsWith("excel") || n.endsWith("sheet")) return "XLS/XLSX";
        if (n.endsWith("jpeg") || n.endsWith("jpg")) return "JPEG";
        if (n.endsWith("png")) return "PNG";
        return null;
    }

    private boolean isVisibleDoc(FamilyDocument d) {
        return !d.isInvalid() && d.getFamily() != null && (d.getFamily().getStatus().equals(RecordStatus.ACTIVE)
                || d.getFamily().getStatus().equals(RecordStatus.ARCHIVED));
    }

    private boolean isPreviewable(FamilyDocument d) {
        if (d == null || d.getOriginalFileName() == null) return false;
        String t = d.getOriginalFileName().toLowerCase(Locale.ROOT);
        return t.endsWith("pdf") || t.endsWith("jpg") || t.endsWith("jpeg") || t.endsWith("png");
    }
}