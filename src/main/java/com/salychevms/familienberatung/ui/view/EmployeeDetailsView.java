package com.salychevms.familienberatung.ui.view;

import com.salychevms.familienberatung.model.Employee;
import com.salychevms.familienberatung.model.Role;
import com.salychevms.familienberatung.service.AuthService;
import com.salychevms.familienberatung.service.EmployeeService;
import com.salychevms.familienberatung.service.RoleService;
import com.salychevms.familienberatung.service.ValidationService;
import com.salychevms.familienberatung.ui.dialog.EditDialogFactory;
import com.salychevms.familienberatung.ui.dialog.EditField;
import com.salychevms.familienberatung.ui.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.VaadinRequest;
import jakarta.annotation.security.PermitAll;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Route(value = "employee/:id", layout = MainLayout.class)
@PageTitle("Mitarbeiter*in")
@PermitAll
@RequiredArgsConstructor
public class EmployeeDetailsView extends VerticalLayout implements BeforeEnterObserver {
    private final EmployeeService employeeService;
    private final AuthService authService;
    private final ValidationService validator;
    private final RoleService roleService;

    private Employee currentEmployee;
    private Employee selectedEmployee;
    private boolean owner;
    private int lvl;
    private final List<String> statusList = List.of("AKTIV", "BLOCKIERT", "ARCHIV");
    private List<Role> roles = new ArrayList<>();

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Employee authorized = authService.getCurrentEmployee();
        if (authorized == null || !authorized.isActive() || authorized.isArchived()
                || (authorized.getRole().getAccessLevel() != 100 && authorized.isPasswordChangeRequired())) {
            event.forwardTo("login");
            return;
        }

        this.currentEmployee = employeeService.findByLogin(authorized.getLogin());
        if (currentEmployee == null) {
            event.forwardTo("login");
            return;
        }
        this.lvl = currentEmployee.getRole().getAccessLevel();

        Optional<Long> optId = event.getRouteParameters().getLong("id");
        if (optId.isEmpty()) {
            event.forwardTo("employees");
            return;
        }

        this.selectedEmployee = employeeService.findById(optId.get());
        if (selectedEmployee == null) {
            event.forwardTo("employees");
            return;
        }

        this.owner = selectedEmployee.equals(currentEmployee);
        if ((lvl == 50 && !owner) || (lvl == 80 && selectedEmployee.getRole().getAccessLevel() == lvl && !owner)
                || (lvl == 80 && selectedEmployee.getRole().getAccessLevel() > 80)) {
            event.forwardTo("employees");
            return;
        }

        roles.clear();
        if (lvl >= 80)
            this.roles.addAll(new ArrayList<>(roleService.getAll().stream()
                    .filter(r -> r != null
                            && r.getAccessLevel() <= currentEmployee.getRole().getAccessLevel()).toList()));

        removeAll();
        buildUI();
    }

    private void buildUI() {
        setSizeFull();
        setSpacing(true);
        setPadding(true);

        buildBreadCrumbs();
        buildHeader();
        buildTopLayout();
        buildInfo();
        buildNotes();
        buildAccessButtons();
        buildAudit();
    }

    private void buildBreadCrumbs() {
        HorizontalLayout bcrumbs = new HorizontalLayout();
        bcrumbs.setSpacing(true);
        bcrumbs.setPadding(true);
        bcrumbs.setAlignItems(FlexComponent.Alignment.CENTER);

        RouterLink l1 = new RouterLink("Übersicht", OverviewView.class);
        RouterLink l2 = new RouterLink("Mitarbeiter*innen", EmployeesView.class);
        Span current = new Span("Mitarbeiter*in: " + selectedEmployee.getFirstName() +
                " " + selectedEmployee.getLastName());
        current.getStyle().set("font-size", "vat(--lumo-font-size-s)").set("font-weight", "bold")
                .set("color", "var(--lumo-body-text-color)");

        Span sp1 = new Span(" >> ");
        sp1.getStyle().set("font-size", "var(--lumo-font-size-s)").set("color", "var(--lumo-secondary-text-color)");
        Span sp2 = new Span(" >> ");
        sp2.getStyle().set("font-size", "var(--lumo-font-size-s)").set("color", "var(--lumo-secondary-text-color)");

        if (lvl == 80 || lvl == 100) bcrumbs.add(l1, sp1, l2, sp2, current);
        else bcrumbs.add(l1, sp1, current);

        add(bcrumbs);
    }

    private void buildHeader() {
        VerticalLayout box = new VerticalLayout();
        box.setSpacing(false);
        box.setPadding(false);
        box.setWidthFull();

        HorizontalLayout header = new HorizontalLayout();
        header.setSpacing(true);
        header.setAlignItems(FlexComponent.Alignment.CENTER);

        H2 title;
        if (!owner) title = new H2("Mitarbeiter*in: " + selectedEmployee.getFirstName() + " "
                + selectedEmployee.getLastName());
        else title = new H2("Mitarbeiter*in: " + selectedEmployee.getFirstName() + " " +
                selectedEmployee.getLastName() + " (Sie)");

        header.add(title);
        Span currentStatus = new Span();
        if (selectedEmployee.isArchived() && !selectedEmployee.isActive()) {
            currentStatus.setText("ARCHIV");
            currentStatus.getStyle().set("color", "#b58900").set("font-weight", "bold");
        } else if (!selectedEmployee.isArchived() && selectedEmployee.isActive()) {
            currentStatus.setText("AKTIV");
            currentStatus.getStyle().set("color", "green").set("font-weight", "bold");
        } else {
            currentStatus.setText("BLOCKIERT");
            currentStatus.getStyle().set("color", "red").set("font-weight", "bold");
        }
        box.add(header, getHLWithSpans("Rolle: ", selectedEmployee.getRole().getLabel()),
                getHL(new Span("Status: "), currentStatus));
        if (lvl == 100)
            box.add(getHLWithSpans("Verpflichted Password ändern: ",
                    selectedEmployee.isPasswordChangeRequired() ? "Ja" : "Nein"));

        add(box);
    }

    private void buildTopLayout() {
        if ((lvl == 100 || (lvl == 80 && selectedEmployee.getRole().getAccessLevel() < 80)) && !owner) {
            HorizontalLayout btns = new HorizontalLayout();
            btns.setSpacing(true);
            btns.setPadding(false);
            btns.setWidthFull();

            Button changeStatus = new Button("Status ändern");
            changeStatus.addClickListener(e -> {
                Dialog dialog = new Dialog("Status ändern");
                String currentStatus;
                if (selectedEmployee.isArchived()) currentStatus = "ARCHIV";
                else if (!selectedEmployee.isActive()) currentStatus = "BLOCKIERT";
                else currentStatus = "AKTIV";

                List<String> available = new ArrayList<>();
                if ("AKTIV".equals(currentStatus)) {
                    available.add("BLOCKIERT");
                    available.add("ARCHIV");
                } else available.add("AKTIV");

                ComboBox<String> cb = new ComboBox<>("Status");
                cb.setItems(available);
                cb.setRequired(true);
                cb.setWidthFull();

                Button cancel = new Button("Abbrechen", ev -> dialog.close());
                Button apply = new Button("Übernehmen", ev -> {
                    String v = cb.getValue();
                    if (v == null) return;

                    VaadinRequest req = VaadinRequest.getCurrent();
                    if ("AKTIV".equals(v))
                        if ("ARCHIV".equals(currentStatus))
                            employeeService.unarchiveEmployee(selectedEmployee.getId(), currentEmployee.getLogin(),
                                    req != null ? req.getRemoteAddr() : "UNKNOWN",
                                    req != null ? req.getHeader("User-Agent") : "UNKNOWN");
                        else
                            employeeService.activateEmployee(selectedEmployee.getId(), currentEmployee.getLogin(),
                                    req != null ? req.getRemoteAddr() : "UNKNOWN",
                                    req != null ? req.getHeader("User-Agent") : "UNKNOWN");
                    else if ("BLOCKIERT".equals(v))
                        employeeService.deactivateEmployee(selectedEmployee.getId(), currentEmployee.getLogin(),
                                req != null ? req.getRemoteAddr() : "UNKNOWN",
                                req != null ? req.getHeader("User-Agent") : "UNKNOWN");
                    else if ("ARCHIV".equals(v))
                        employeeService.archiveEmployee(selectedEmployee.getId(), currentEmployee.getLogin(),
                                req != null ? req.getRemoteAddr() : "UNKNOWN",
                                req != null ? req.getHeader("User-Agent") : "UNKNOWN");

                    dialog.close();
                    getUI().ifPresent(ui -> ui.getPage().reload());
                });

                HorizontalLayout btn = new HorizontalLayout(cancel, apply);
                btn.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
                btn.setWidthFull();

                dialog.add(cb);
                dialog.getFooter().add(btn);
                dialog.open();
            });

            if (lvl == 100) {
                Button changeRole = new Button("Rolle ändern");
                changeRole.addClickListener(e -> {
                    Dialog dialog = new Dialog("Rolle ändern");

                    ComboBox<Role> cb = new ComboBox<>("Rolle");
                    cb.setItems(roles);
                    cb.setItemLabelGenerator(Role::getLabel);
                    cb.setRequired(true);
                    cb.setWidthFull();

                    Button cancel = new Button("Abbrechen", ev -> dialog.close());
                    Button apply = new Button("Übernehmen", ev -> {
                        Role r = cb.getValue();
                        if (r == null) return;

                        VaadinRequest req = VaadinRequest.getCurrent();
                        employeeService.updateRole(selectedEmployee, r, currentEmployee.getLogin(),
                                req != null ? req.getRemoteAddr() : "UNKNOWN",
                                req != null ? req.getHeader("User-Agent") : "UNKNOWN");

                        dialog.close();
                        getUI().ifPresent(ui -> ui.getPage().reload());
                    });

                    HorizontalLayout btn = new HorizontalLayout(cancel, apply);
                    btn.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
                    btn.setWidthFull();

                    dialog.add(cb);
                    dialog.getFooter().add(btn);
                    dialog.open();
                });
                btns.add(changeRole);
            }
            btns.add(changeStatus);
            add(btns);
        }
    }

    private void buildInfo() {
        VerticalLayout block = new VerticalLayout();
        block.setSpacing(false);
        block.setMargin(false);
        block.setWidthFull();
        block.getStyle().set("border-radius", "6px").set("border", "1px solid #ddd").set("padding", "10px");

        HorizontalLayout titleLayout = new HorizontalLayout();
        titleLayout.setSpacing(true);
        titleLayout.setAlignItems(FlexComponent.Alignment.CENTER);

        boolean canEdit = lvl == 100 || (lvl == 80 && (owner || selectedEmployee.getRole().getAccessLevel() < 80))
                || (lvl == 50 && owner);

        if (canEdit) {
            Button edit = new Button(VaadinIcon.EDIT.create());
            edit.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            edit.addClassName("edit-btn");
            edit.addClickListener(e -> {
                EditDialogFactory.openEditDialog("Mitarbeiterdaten bearbeiten", List.of(
                        new EditField("firstName", "Vorname (*)", EditField.Type.TEXT,
                                selectedEmployee.getFirstName(), true, 50, null,
                                null, null),
                        new EditField("lastName", "Nachname (*)", EditField.Type.TEXT,
                                selectedEmployee.getLastName(), true, 50, null,
                                null, null),
                        new EditField("email", "E-Mail", EditField.Type.TEXT,
                                selectedEmployee.getEmail(), false, 255, null,
                                null, null),
                        new EditField("mobileNumber", "Mobilnummer", EditField.Type.TEXT,
                                selectedEmployee.getMobileNumber(), false, 30, null,
                                null, null),
                        new EditField("landNumber", "Festnetz", EditField.Type.TEXT,
                                selectedEmployee.getLandNumber(), false, 30, null,
                                null, null)

                ), values -> {
                    String firstname = values.get("firstName") != null ? values.get("firstName").toString() :
                            selectedEmployee.getFirstName();
                    String lastname = values.get("lastName") != null ? values.get("lastName").toString() :
                            selectedEmployee.getLastName();

                    if (firstname == null || lastname == null || firstname.isBlank() || lastname.isBlank()) {
                        showOkDialog("Fehler", "Vorname und Nachname sind Pflichtfelder!");
                        return;
                    }

                    Employee updated = new Employee();
                    if (values.get("firstName") != null) updated.setFirstName(values.get("firstName").toString());
                    if (values.get("lastName") != null) updated.setLastName(values.get("lastName").toString());
                    if (values.get("email") != null) updated.setEmail(values.get("email").toString());
                    if (values.get("mobileNumber") != null)
                        updated.setMobileNumber(values.get("mobileNumber").toString());
                    if (values.get("landNumber") != null) updated.setLandNumber(values.get("landNumber").toString());

                    VaadinRequest req = VaadinRequest.getCurrent();
                    employeeService.updateEmployee(selectedEmployee, updated, currentEmployee.getLogin(),
                            req != null ? req.getRemoteAddr() : "UNKNOWN",
                            req != null ? req.getHeader("User-Agent") : "UNKNOWN");
                    getUI().ifPresent(ui -> ui.getPage().reload());
                });
            });
            titleLayout.add(edit);
        }

        block.add(titleLayout,
                getHLWithSpans("Login: ", selectedEmployee.getLogin()),
                getHLWithSpans("Vorname: ", selectedEmployee.getFirstName()),
                getHLWithSpans("Nachname: ", selectedEmployee.getLastName()),
                getHLWithSpans("E-Mail: ", empty(selectedEmployee.getEmail())),
                getHLWithSpans("Mobilnummer: ", empty(selectedEmployee.getMobileNumber())),
                getHLWithSpans("Festnetz: ", empty(selectedEmployee.getLandNumber())));

        Span title = new Span("Allgemeine Informationen");
        title.getStyle().set("font-weight", "bold");
        titleLayout.add(title);

        add(block);
    }

    private void buildNotes() {
        if (!(lvl == 100 || (lvl == 80 && selectedEmployee.getRole().getAccessLevel() < 80))) return;

        VerticalLayout block = new VerticalLayout();
        block.setSpacing(false);
        block.setPadding(false);
        block.setWidthFull();
        block.getStyle().set("border", "1px solid #ddd").set("border-radius", "6px").set("padding", "10px");

        HorizontalLayout titleLayout = new HorizontalLayout();
        titleLayout.setSpacing(true);
        titleLayout.setAlignItems(FlexComponent.Alignment.CENTER);

        boolean canEdit = lvl == 100 || (lvl == 80 && selectedEmployee.getRole().getAccessLevel() < 80);

        if (canEdit) {
            Button edit = new Button(VaadinIcon.EDIT.create());
            edit.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            edit.addClassName("edit-btn");
            edit.addClickListener(e -> {
                EditDialogFactory.openEditDialog("Notizen bearbeiten", List.of(
                        new EditField("notes", "Notizen", EditField.Type.TEXTAREA,
                                selectedEmployee.getNotes(), false, 1000, null,
                                null, null)
                ), values -> {
                    Employee updated = new Employee();
                    updated.setNotes(values.get("notes") != null ? values.get("notes").toString() : "");

                    VaadinRequest req = VaadinRequest.getCurrent();
                    employeeService.updateEmployee(selectedEmployee, updated, currentEmployee.getLogin(),
                            req != null ? req.getRemoteAddr() : "UNKNOWN",
                            req != null ? req.getHeader("User-Agent") : "UNKNOWN");
                    getUI().ifPresent(ui -> ui.getPage().reload());
                });
            });
            titleLayout.add(edit);
        }

        Span title = new Span("Notizen");
        title.getStyle().set("font-weight", "bold");
        titleLayout.add(title);

        TextArea notesArea = new TextArea();
        notesArea.setReadOnly(true);
        notesArea.setWidthFull();
        notesArea.setHeight("200px");
        notesArea.setValue(selectedEmployee.getNotes() != null ? selectedEmployee.getNotes() : "");

        block.add(titleLayout, notesArea);
        add(block);
    }

    private void buildAccessButtons() {
        if (lvl == 100 || (lvl == 80 && (owner || selectedEmployee.getRole().getAccessLevel() < 80))
                || (lvl == 50 && owner)) {
            VerticalLayout block = new VerticalLayout();
            block.setSpacing(false);
            block.setPadding(false);
            block.setWidthFull();
            block.getStyle().set("border", "1px solid #ddd").set("border-radius", "6px").set("padding", "10px");

            Span title = new Span("Zugangdaten");
            title.getStyle().set("font-weight", "bold");
            block.add(title);

            HorizontalLayout btns = new HorizontalLayout();
            btns.setSpacing(true);
            btns.setPadding(false);

            Button changeLogin = new Button("Login ändern", event -> {
                showConfirmDialog("Login ändern", "Möchten Sie den Login wirklich ändern?", () -> {
                    Dialog dialog = new Dialog("Neuen Login setzen");
                    TextField loginField = new TextField("Neuer Login");
                    loginField.setRequired(true);
                    loginField.setMaxLength(50);
                    loginField.setWidthFull();

                    Button cancel = new Button("Abbrechen", e -> dialog.close());
                    Button apply = new Button("Übernehmen", e -> {
                        String newLogin = loginField.getValue();
                        if (newLogin == null || newLogin.isBlank()) {
                            showOkDialog("Fehler", "Login darf nicht leer sein");
                            return;
                        }
                        if (newLogin.equals(selectedEmployee.getLogin())) {
                            dialog.close();
                            return;
                        }

                        VaadinRequest req = VaadinRequest.getCurrent();
                        employeeService.changeLogin(selectedEmployee.getId(), newLogin, currentEmployee.getLogin(),
                                req != null ? req.getRemoteAddr() : "UNKNOWN",
                                req != null ? req.getHeader("User-Agent") : "UNKNOWN");

                        dialog.close();
                        showOkDialog("Login geändert", "Neuer Login: " + newLogin);
                        getUI().ifPresent(ui -> ui.getPage().reload());
                    });
                    HorizontalLayout buttons = new HorizontalLayout(cancel, apply);
                    buttons.setWidthFull();
                    buttons.setJustifyContentMode(JustifyContentMode.END);

                    dialog.add(loginField);
                    dialog.getFooter().add(buttons);
                    dialog.open();
                }, null);
            });
            Button changePassword = new Button("Passwort ändern", event -> {
                showConfirmDialog("Passwort ändern", "Möchten Sie das Passwort wirklich ändern?",
                        () -> {
                            String pwd = generateValidPassword(selectedEmployee.getLogin(), selectedEmployee.getEmail(),
                                    selectedEmployee.getMobileNumber(), selectedEmployee.getLandNumber());

                            VaadinRequest req = VaadinRequest.getCurrent();
                            employeeService.changePassword(selectedEmployee.getId(), pwd, !owner, currentEmployee.getLogin(),
                                    req != null ? req.getRemoteAddr() : "UNKNOWN",
                                    req != null ? req.getHeader("User-Agent") : "UNKNOWN");

                            Dialog result = new Dialog("Passwort geändert");
                            if (!owner)
                                result.add(getHLWithSpans("Passwort wurde geändert für: ",
                                        selectedEmployee.getFirstName() + " " + selectedEmployee.getLastName()));
                            else result.add("Ihr Passwort wurde geändert");
                            Span warn = new Span("Bitte notieren Sie dieses Passwort. Es wird nur einmal angezeigt!");
                            warn.getStyle().set("font-weight", "bold").set("color", "red");

                            Button ok = new Button("OK", e -> {
                                result.close();
                                getUI().ifPresent(ui -> ui.getPage().reload());
                            });

                            HorizontalLayout button = new HorizontalLayout(ok);
                            button.setWidthFull();
                            button.setJustifyContentMode(JustifyContentMode.END);

                            result.add(getHLWithSpans("Login: ", selectedEmployee.getLogin()),
                                    getHLWithSpans("Passwort: ", pwd), warn);
                            result.getFooter().add(button);
                            result.open();
                        }, () -> {
                        });
            });
            btns.add(changeLogin, changePassword);
            block.add(btns);

            add(block);
        }
    }

    private void buildAudit() {
        VerticalLayout block = new VerticalLayout();
        block.setSpacing(false);
        block.setPadding(false);
        block.setWidthFull();
        block.getStyle().set("border", "1px solid #ddd").set("border-radius", "6px").set("padding", "10px");

        Span title = new Span("Verlauf");
        title.getStyle().set("font-weight", "bold");
        block.add(title);

        if (selectedEmployee.getCreatedDate() != null)
            block.add(makeAuditLine("Erstellt am " + formatDate(selectedEmployee.getCreatedDate()) +
                    " um " + formatTime(selectedEmployee.getCreatedDate()) +
                    " von " + empty(selectedEmployee.getCreatedBy())));
        if (selectedEmployee.getUpdatedAt() != null)
            block.add(makeAuditLine("Geändert am " + formatDate(selectedEmployee.getUpdatedAt()) +
                    " um " + formatTime(selectedEmployee.getUpdatedAt()) +
                    " von " + empty(selectedEmployee.getUpdatedBy())));
        add(block);
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
                                          String digits, String special, String all, int length) {
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
