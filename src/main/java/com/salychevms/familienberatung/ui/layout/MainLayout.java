package com.salychevms.familienberatung.ui.layout;

import com.salychevms.familienberatung.model.Employee;
import com.salychevms.familienberatung.service.AuthService;
import com.salychevms.familienberatung.ui.view.*;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.HighlightConditions;
import com.vaadin.flow.router.RouterLink;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@CssImport("./styles/main-layout.css")
@RequiredArgsConstructor
public class MainLayout extends AppLayout {
    private final AuthService authService;
    private Span timeSpan;

    @PostConstruct
    public void init() {
        buildHeader();
        buildSidebar();

        Employee e = authService.getCurrentEmployee();
        if (e == null) {
            UI.getCurrent().navigate("login");
            return;
        }
        // TODO: add SessionHeartbeatListener here
        // auto-reset logout timer
        startLogoutTimer();
    }

    private void buildHeader() {
        H1 title = new H1("Familienberatung");
        title.getStyle().set("margin", "0")
                .set("font-size", "24px")
                .set("color", "#0067A0");

        FlexLayout header = new FlexLayout(title);
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

        Employee e = authService.getCurrentEmployee();
        int lvl = (e != null) ? e.getRole().getAccessLevel() : 0;

        Button overview = new Button("Übersicht",
                event -> UI.getCurrent().navigate("overview"));
        overview.setWidthFull();
        overview.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        buttons.add(overview);

        Button families = new Button("Familien",
                event -> UI.getCurrent().navigate("families"));
        families.setWidthFull();
        families.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        buttons.add(families);

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

        Button delegations=new Button("Delegationen",
                event -> UI.getCurrent().navigate(DelegationsView.class));
        delegations.setWidthFull();
        delegations.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        buttons.add(delegations);

        if (lvl >= 80) {
            Button employees=new Button("Mitarbeiter*innen",
                    event -> UI.getCurrent().navigate(EmployeesView.class));
            employees.setWidthFull();
            employees.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

            Button log=new Button("Log",event -> UI.getCurrent().navigate(LogView.class));
            log.setWidthFull();
            log.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

            buttons.add(employees, log);
        }
        if (lvl == 100) {
            Button admPanel=new Button("Admin Panel",
                    event ->  UI.getCurrent().navigate(AdminView.class));
            admPanel.setWidthFull();
            admPanel.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

            buttons.add(admPanel);
        }

        top.add(buttons);

        VerticalLayout bottom = new VerticalLayout();
        bottom.setPadding(false);
        bottom.setSpacing(false);
        bottom.setAlignItems(FlexComponent.Alignment.START);

        Span labelSpan = new Span((e != null) ? e.getRole().getLabel() + ": " : "nicht gefunden");
        Span loginSpan = new Span((e != null) ? e.getLogin() : "nicht gefunden");
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
    }
}