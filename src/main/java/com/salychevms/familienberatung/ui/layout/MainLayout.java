package com.salychevms.familienberatung.ui.layout;

import com.salychevms.familienberatung.model.Employee;
import com.salychevms.familienberatung.service.AuthService;
import com.salychevms.familienberatung.service.EmployeeService;
import com.salychevms.familienberatung.ui.view.*;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@CssImport("./styles/main-layout.css")
@RequiredArgsConstructor
public class MainLayout extends AppLayout {
    private final AuthService authService;
    private final EmployeeService employeeService;
    private Span timeSpan;
    private Employee currentEmployee;
    private int lvl;
    private boolean warningShown = false;

    @PostConstruct
    public void init() {
        Employee authorized = authService.getCurrentEmployee();
        if (authorized == null || !authorized.isActive() || authorized.isArchived()) {
            UI.getCurrent().navigate("login");
            return;
        }

        this.currentEmployee = employeeService.findByLogin(authorized.getLogin());
        if (currentEmployee == null) {
            UI.getCurrent().navigate("login");
            return;
        }
        this.lvl = currentEmployee.getRole().getAccessLevel();

        buildHeader();
        buildSidebar();

        UI ui = UI.getCurrent();
        ui.getSession().setAttribute("lastActivity", System.currentTimeMillis());

        initActivityTracking();
        startLogoutTimer();
    }

    private void buildHeader() {
        Image logo = new Image("/images/logo.png", "Logo");
        logo.setHeight("40px");

        H1 title = new H1("Familienberatung");
        title.getStyle().set("margin", "0")
                .set("font-size", "24px")
                .set("color", "#0067A0");

        FlexLayout header = new FlexLayout(logo, title);
        header.setWidthFull();
        header.getStyle().set("padding", "10px")
                .set("background", "white")
                .set("border-bottom", "1px solid #ddd");
        addToNavbar(header);
    }

    private void buildSidebar() {
        VerticalLayout menu = new VerticalLayout();
        menu.setPadding(false);
        menu.setSpacing(false);
        menu.setHeightFull();
        menu.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        VerticalLayout top = new VerticalLayout();
        top.setPadding(false);
        top.setSpacing(false);

        VerticalLayout buttons = new VerticalLayout();
        buttons.setWidthFull();
        buttons.setSpacing(true);
        buttons.setPadding(true);

        Button overview = new Button("Übersicht",
                event -> UI.getCurrent().navigate("overview"));
        overview.setWidthFull();
        overview.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        buttons.add(overview);

        Button members = new Button("Teilnehmer*innen",
                event -> UI.getCurrent().navigate("members"));
        members.setWidthFull();
        members.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        buttons.add(members);

        Button consultations = new Button("Beratungen",
                event -> UI.getCurrent().navigate(ConsultationsView.class));
        consultations.setWidthFull();
        consultations.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        buttons.add(consultations);

        Button docs = new Button("Dokumente",
                event -> UI.getCurrent().navigate(DocumentsView.class));
        docs.setWidthFull();
        docs.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        buttons.add(docs);

        Button delegations = new Button("Delegationen",
                event -> UI.getCurrent().navigate(DelegationsView.class));
        delegations.setWidthFull();
        delegations.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        buttons.add(delegations);

        if (lvl >= 80) {
            Button employees = new Button("Mitarbeiter*innen",
                    event -> UI.getCurrent().navigate(EmployeesView.class));
            employees.setWidthFull();
            employees.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

            Button log = new Button("Log", event -> UI.getCurrent().navigate(LogView.class));
            log.setWidthFull();
            log.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

            buttons.add(employees, log);
        }
        if (lvl == 100) {
            Button admPanel = new Button("Admin Panel",
                    event -> UI.getCurrent().navigate(AdminView.class));
            admPanel.setWidthFull();
            admPanel.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

            buttons.add(admPanel);
        }

        if (currentEmployee != null) {
            HorizontalLayout personalLayout = new HorizontalLayout();
            personalLayout.setSpacing(true);
            personalLayout.setPadding(true);
            personalLayout.setWidthFull();
            personalLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
            personalLayout.setAlignItems(FlexComponent.Alignment.END);

            Button personalEmployeeDetails = new Button("Meine Daten",
                    event -> UI.getCurrent().navigate(EmployeeDetailsView.class,
                            new RouteParameters("id", currentEmployee.getId().toString())));
            personalEmployeeDetails.setWidthFull();
            personalEmployeeDetails.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

            buttons.add(personalEmployeeDetails);
        }

        top.add(buttons);

        VerticalLayout bottom = new VerticalLayout();
        bottom.setPadding(false);
        bottom.setSpacing(false);
        bottom.setAlignItems(FlexComponent.Alignment.START);

        Span labelSpan = new Span(currentEmployee != null ? currentEmployee.getRole().getLabel() + ": " : "nicht gefunden");
        Span loginSpan = new Span(currentEmployee != null ? currentEmployee.getLogin() : "nicht gefunden");
        bottom.add(labelSpan, loginSpan);

        timeSpan = new Span("30:00");
        timeSpan.getStyle().set("margin-top", "20px")
                .set("font-weight", "bold")
                .set("color", "red");

        Button logout = new Button("Logout", e2 -> authService.logout());
        bottom.add(timeSpan, logout);
        menu.add(top, bottom);

        addToDrawer(menu);
    }

    private void startLogoutTimer() {
        if (timeSpan == null) return;

        UI ui = UI.getCurrent();
        ui.setPollInterval(1000);
        ui.addPollListener(event -> {
            Long last = (Long) ui.getSession().getAttribute("lastActivity");
            if (last == null) return;
            long diff = System.currentTimeMillis() - last;
            long remaining = (30 * 60 * 1000) - diff;
            if (remaining <= 0) {
                authService.logout();
                return;
            }
            long sec = remaining / 1000;
            timeSpan.setText(String.format("%02d:%02d", sec / 60, sec % 60));

            if (remaining <= 10 * 60 * 1000)
                timeSpan.getStyle().set("color", "red");
            else
                timeSpan.getStyle().set("color", "black");

            if ((remaining <= 5 * 60 * 1000) && !warningShown) {
                warningShown = true;
                showSessionWarning();
            }
        });
    }

    private void initActivityTracking() {
        UI ui = UI.getCurrent();
        ui.getPage().executeJs("""
                const reset = () => {
                fetch('/heartbeat');
                };
                ['click','mousemove','keydown','scroll'].forEach(e=>
                document.addEventListener(e, reset, true)
                );""");
    }

    private void showSessionWarning() {
        Dialog dialog = new Dialog("Sitzung läuft bald ab!!!");
        Span text = new Span("Ihre Sitzung endet in 5 Minuten.");
        Span text2 = new Span("Sind Sie hier? Kliecken Sie bitte \"Ja\"");
        dialog.add(text, text2);

        UI ui = UI.getCurrent();
        ui.getPage().executeJs("""
                    window.logoutTimeout = setTimeout(() => {
                        window.location.href = '/login';
                    }, 300000);
                """);

        Button okButton = new Button("Ja", ev -> {
            markActivity();
            warningShown = false;
            ui.getPage().executeJs("""
                        if (window.logoutTimeout) {
                            clearTimeout(window.logoutTimeout);
                        }
                    """);
            dialog.close();
        });

        HorizontalLayout buttons = new HorizontalLayout(okButton);
        buttons.setWidthFull();
        buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        dialog.getFooter().add(buttons);
        dialog.open();
    }

    private void markActivity() {
        UI ui = UI.getCurrent();
        if (ui != null) {
            ui.getSession().setAttribute("lastActivity",
                    System.currentTimeMillis());
        }
    }
}