package com.salychevms.familienberatung.ui.view;

import com.salychevms.familienberatung.model.Employee;
import com.salychevms.familienberatung.model.Role;
import com.salychevms.familienberatung.service.AuthService;
import com.salychevms.familienberatung.service.EmployeeService;
import com.salychevms.familienberatung.service.RoleService;
import com.salychevms.familienberatung.service.ValidationService;
import com.salychevms.familienberatung.ui.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.VaadinRequest;
import jakarta.annotation.security.PermitAll;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Route(value = "employees", layout = MainLayout.class)
@PageTitle("Mitarbeiter*innen")
@PermitAll
@RequiredArgsConstructor
public class EmployeesView extends VerticalLayout implements BeforeEnterObserver {
    private final EmployeeService employeeService;
    private final AuthService authService;
    private final ValidationService validator;
    private final RoleService roleService;

    private Employee currentEmployee;
    private int lvl;
    private List<Employee> currentEmployees = new ArrayList<>();
    private HorizontalLayout filterLayout;
    private Grid<Employee> employeeGrid;
    private ComboBox<String> roleFilter;
    private ComboBox<String> statusFilter;
    private Button filterToggleButton;
    private TextField searchField;
    private List<String> currentRoleLabels = new ArrayList<>();
    private List<Role> currentRoles = new ArrayList<>();
    private boolean filterVisible = false;

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Employee authorized = authService.getCurrentEmployee();
        if (authorized == null || !authorized.isActive() || authorized.isArchived()) {
            event.forwardTo("login");
            return;
        }

        this.currentEmployee = employeeService.findByLogin(authorized.getLogin());
        if (currentEmployee == null || currentEmployee.isArchived() || !currentEmployee.isActive()) {
            event.forwardTo("login");
            return;
        }

        this.lvl = currentEmployee.getRole().getAccessLevel();
        if (lvl != 100 && lvl != 80) {
            event.forwardTo("login");
            return;
        }

        currentEmployees.clear();
        this.currentEmployees = new ArrayList<>(employeeService.findAll().stream()
                .filter(e -> e.getRole().getAccessLevel() <= lvl).toList());

        currentRoleLabels.clear();
        for (Employee e : currentEmployees)
            if (!currentRoleLabels.contains(e.getRole().getLabel()))
                currentRoleLabels.add(e.getRole().getLabel());

        currentRoles = roleService.getAll().stream().filter(
                r -> r != null && r.getAccessLevel() <= currentEmployee.getRole().getAccessLevel()).toList();

        removeAll();
        buildUI();
    }

    private void buildUI() {
        setSizeFull();
        setSpacing(true);
        setPadding(true);

        buildBreadCrumbs();
        buildTitle();
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

        RouterLink root = new RouterLink("Übersicht", OverviewView.class);

        Span separator = new Span(" >> ");
        separator.getStyle().set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)");

        Span current = new Span("Mitarbeiter*innen");
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

        H2 titleText = new H2("Mitarbeiter*innen");
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
            roleFilter.clear();
            statusFilter.clear();
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
        filterLayout.setAlignItems(Alignment.START);
        filterLayout.setSpacing(true);
        filterLayout.setPadding(true);
        filterLayout.setVisible(false);
        filterLayout.getStyle().set("background-color", "#fff3cd").set("border-radius", "6px")
                .set("border", "1px solid #e0c97f");

        VerticalLayout roleLayout = new VerticalLayout();
        roleLayout.setSpacing(false);
        roleLayout.setPadding(false);
        roleLayout.setWidthFull();

        VerticalLayout statusLayout = new VerticalLayout();
        statusLayout.setSpacing(false);
        statusLayout.setPadding(false);
        statusLayout.setWidthFull();

        Span roleLabel = new Span("Rolle:");
        roleLabel.getStyle().set("font-weight", "bold");

        roleFilter = new ComboBox<>();
        roleFilter.setItems(currentRoleLabels);
        roleFilter.setClearButtonVisible(true);
        roleFilter.addValueChangeListener(event -> refreshGrid());

        roleLayout.add(roleLabel, roleFilter);

        Span statusLabel = new Span("Status:");
        statusLabel.getStyle().set("font-weight", "bold");

        statusFilter = new ComboBox<>();
        statusFilter.setItems("AKTIV", "ARCHIV", "BLOCKIERT");
        statusFilter.setClearButtonVisible(true);
        statusFilter.addValueChangeListener(event -> refreshGrid());

        statusLayout.add(statusLabel, statusFilter);

        filterLayout.add(roleLayout, statusLayout);
        add(filterLayout);
    }

    private void buildActionButtons() {
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setSpacing(true);
        buttonLayout.setAlignItems(FlexComponent.Alignment.CENTER);

        Button createEmployee = new Button("Neue*r Mitarbeiter*in", VaadinIcon.PLUS.create());
        createEmployee.getStyle().set("font-size", "16px").set("padding", "8px 16px");
        createEmployee.addClickListener(e -> openCreateEmployeeDialog());

        buttonLayout.add(createEmployee);
        add(buttonLayout);
    }

    private void buildGrid() {
        employeeGrid = new Grid(Employee.class, false);
        employeeGrid.setSizeFull();
        employeeGrid.setSelectionMode(Grid.SelectionMode.SINGLE);
        employeeGrid.setItems(currentEmployees);

        employeeGrid.addColumn(Employee::getId)
                .setHeader("ID").setAutoWidth(true).setFlexGrow(0)
                .setComparator(Employee::getId);
        employeeGrid.addColumn(Employee::getLogin)
                .setHeader("Login").setAutoWidth(true).setFlexGrow(0)
                .setComparator(Employee::getLogin);
        employeeGrid.addColumn(e -> e.getFirstName() + " " + e.getLastName())
                .setHeader("Vor- und Nachname").setAutoWidth(true).setFlexGrow(0)
                .setComparator(Employee::getFirstName);
        employeeGrid.addColumn(e -> e.getRole().getLabel())
                .setHeader("Rolle").setAutoWidth(true).setFlexGrow(0)
                .setComparator(e -> e.getRole().getLabel());
        employeeGrid.addColumn(new ComponentRenderer<>(e -> {
                    Span s = new Span();
                    if (e.isArchived()) {
                        s.setText("ARCHIV");
                        s.getStyle().set("color", "#b58900").set("font-weight", "bold");
                    } else if (!e.isActive() && !e.isArchived()) {
                        s.setText("BLOCKIERT");
                        s.getStyle().set("color", "red").set("font-weight", "bold");
                    } else {
                        s.setText("AKTIV");
                        s.getStyle().set("color", "green").set("font-weight", "bold");
                    }
                    return s;
                })).setHeader("Status").setAutoWidth(true).setFlexGrow(0)
                .setComparator(e -> {
                    if (!e.isActive() && !e.isArchived()) return 2;
                    if (e.isArchived()) return 1;
                    return 0;
                });

        employeeGrid.addItemClickListener(e -> {
            Employee emp = e.getItem();
            getUI().ifPresent(ui -> ui.navigate(EmployeeDetailsView.class,
                    new RouteParameters("id", emp.getId().toString())));
        });
        add(employeeGrid);
    }

    private void refreshGrid() {
        List<Employee> result = new ArrayList<>();

        String q = searchField != null ? searchField.getValue().trim().toLowerCase() : "";
        String role = roleFilter != null ? roleFilter.getValue() : null;
        String status = statusFilter != null ? statusFilter.getValue() : null;

        for (Employee e : currentEmployees) {
            if (!q.isBlank()) {
                boolean match = false;
                if (e.getLogin() != null && e.getLogin().toLowerCase().contains(q)) match = true;
                if (!match && e.getFirstName() != null && e.getFirstName().toLowerCase().contains(q)) match = true;
                if (!match && e.getLastName() != null && e.getLastName().toLowerCase().contains(q)) match = true;
                if (!match && e.getEmail() != null && e.getEmail().toLowerCase().contains(q)) match = true;
                if (!match && e.getLandNumber() != null && e.getLandNumber().toLowerCase().contains(q)) match = true;
                if (!match && e.getMobileNumber() != null && e.getMobileNumber().toLowerCase().contains(q))
                    match = true;
                if (!match) continue;
            }

            if (role != null && !e.getRole().getLabel().equals(role)) continue;

            if (status != null) {
                if ("AKTIV".equals(status) && (e.isArchived() || !e.isActive())) continue;
                if ("ARCHIV".equals(status) && (!e.isArchived() || e.isActive())) continue;
                if ("BLOCKIERT".equals(status) && (e.isArchived() || e.isActive())) continue;
            }
            result.add(e);
        }
        employeeGrid.setItems(result);
    }

    private void openCreateEmployeeDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Neue*r Mitarbeiter*in");
        dialog.setModal(true);
        dialog.setResizable(false);
        dialog.setDraggable(false);
        dialog.setCloseOnOutsideClick(false);
        dialog.setWidth("800px");


        ComboBox<Role> role = new ComboBox<>("Rolle (*)");
        role.setItems(currentRoles);
        role.setItemLabelGenerator(Role::getLabel);
        role.setRequiredIndicatorVisible(true);
        role.setWidthFull();

        TextField login = new TextField("Login (*)");
        login.setRequiredIndicatorVisible(true);
        login.setWidthFull();

        TextField firstName = new TextField("Vorname (*)");
        firstName.setRequiredIndicatorVisible(true);
        firstName.setWidthFull();

        TextField lastName = new TextField("Nachname (*)");
        lastName.setRequiredIndicatorVisible(true);
        lastName.setWidthFull();

        TextField email = new TextField("E-Mail");
        email.setRequiredIndicatorVisible(false);
        email.setWidthFull();

        TextField mobileN = new TextField("Mobile Number");
        mobileN.setRequiredIndicatorVisible(false);
        mobileN.setWidthFull();

        TextField landN = new TextField("Land Number");
        landN.setRequiredIndicatorVisible(false);
        landN.setWidthFull();

        TextField notes = new TextField("Notizen");
        notes.setWidthFull();
        notes.setMaxLength(255);

        VerticalLayout content = new VerticalLayout(role, login, firstName, lastName, email, mobileN, landN, notes);
        content.setPadding(true);
        content.setSpacing(false);
        dialog.add(content);

        Button save = new Button("Speichern");
        Button cancel = new Button("Abbrechen", e -> dialog.close());

        save.addClickListener(e -> {
            if (login.isEmpty() || firstName.isEmpty() || lastName.isEmpty() || role.isEmpty()) {
                showOkDialog("Fehler", "Bitte alle Pflichtfelder (*) ausfüllen.");
                return;
            }
            showConfirmDialog("Bestätigen", "Mitarbeiter*in anlegen?", () -> {
                Role chosenRole = roleService.getRoleByLabel(currentEmployee, role.getValue().getLabel());
                if (chosenRole == null) {
                    showOkDialog("Fehler", "Rolle existiert nicht oder wurde nicht gefunden.");
                    return;
                }
                String password = generateValidPassword(login.getValue(), email.getValue(), mobileN.getValue(),
                        landN.getValue());
                Employee emp = new Employee();
                emp.setLogin(login.getValue());
                emp.setFirstName(firstName.getValue());
                emp.setLastName(lastName.getValue());
                emp.setEmail(email.getValue());
                emp.setMobileNumber(mobileN.getValue());
                emp.setLandNumber(landN.getValue());
                emp.setNotes(notes.getValue());
                System.out.println("ROLE ID = " +
                        (chosenRole != null ? chosenRole.getId() : "NULL"));
                emp.setRole(chosenRole);
                emp.setPassword(password);

                VaadinRequest req = VaadinRequest.getCurrent();
                employeeService.createEmployee(emp, currentEmployee.getLogin(),
                        req != null ? req.getRemoteAddr() : "UNKNOWN",
                        req != null ? req.getHeader("User-Agent") : "UNKNOWN");

                dialog.close();
                openAccessDataDialog(login.getValue(), password);
                refreshGrid();
            }, null);
        });
        HorizontalLayout buttons = new HorizontalLayout(save, cancel);
        buttons.setWidthFull();
        buttons.setJustifyContentMode(JustifyContentMode.END);

        dialog.getFooter().add(buttons);
        dialog.open();
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

    private String generateValidPassword(String login, String email, String mobileN, String landN) {
        SecureRandom rnd = new SecureRandom();

        String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lower = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String special = "!@#$%^&*()-_=+[]{}";
        String all = upper + lower + digits + special;

        int maxAttempts = 500;

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            String pwd = buildCandidatePassword(rnd, upper, lower, digits, special, all, 14); // ADDED
            try {
                validator.validatePasswordCreate(pwd, login, email, mobileN, landN);
                return pwd;
            } catch (RuntimeException ex) {
                log.debug("Failed validating password during creating new user: {}", ex.getMessage());
            }
        }

        throw new RuntimeException("Failed to generate valid password after "
                + maxAttempts + " attempts");
    }

    private String buildCandidatePassword(SecureRandom rnd, String upper, String lower,
                                          String digits, String special, String all,
                                          int length) {
        List<Character> chars = new ArrayList<>();

        chars.add(upper.charAt(rnd.nextInt(upper.length())));
        chars.add(lower.charAt(rnd.nextInt(lower.length())));
        chars.add(digits.charAt(rnd.nextInt(digits.length())));
        chars.add(special.charAt(rnd.nextInt(special.length())));

        while (chars.size() < length) {
            chars.add(all.charAt(rnd.nextInt(all.length())));
        }

        for (int i = chars.size() - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            char tmp = chars.get(i);
            chars.set(i, chars.get(j));
            chars.set(j, tmp);
        }

        StringBuilder sb = new StringBuilder();
        for (char c : chars) sb.append(c);

        return sb.toString();
    }

    private void openAccessDataDialog(String login, String password) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Zugangdaten.");
        dialog.setModal(true);
        dialog.setResizable(false);
        dialog.setDraggable(false);
        dialog.setCloseOnOutsideClick(false);
        dialog.setWidth("600px");

        Span warn = new Span("Bitte notieren Sie dieses Passwort. Es wird nur einmal angezeigt!");
        warn.getStyle().set("font-weight", "bold").set("color", "red");

        VerticalLayout layout = new VerticalLayout(getHLWithSpans("Login", login),
                getHLWithSpans("Password", password), warn);
        layout.setSpacing(true);
        layout.setPadding(false);

        Button ok = new Button("OK", e -> {
            getUI().ifPresent(ui->ui.getPage().reload());
            dialog.close();
        });
        HorizontalLayout btns = new HorizontalLayout(ok);
        btns.setWidthFull();
        btns.setJustifyContentMode(JustifyContentMode.END);

        dialog.getFooter().add(btns);
        dialog.add(layout);
        dialog.open();
    }
}
