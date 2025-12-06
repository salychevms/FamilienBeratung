package com.salychevms.familienberatung.ui.layout;

import com.salychevms.familienberatung.model.Employee;
import com.salychevms.familienberatung.service.AuthService;
import com.salychevms.familienberatung.ui.view.*;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.button.Button;
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
        menu.setWidth("220px");
        menu.setPadding(false);
        menu.setSpacing(false);
        menu.setHeightFull();
        menu.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        VerticalLayout top = new VerticalLayout();
        top.setPadding(false);
        top.setSpacing(false);

        Employee e = authService.getCurrentEmployee();
        int lvl = (e != null) ? e.getRole().getAccessLevel() : 0;

        RouterLink overview = new RouterLink("Übersicht", OverviewView.class);
        overview.setHighlightCondition(HighlightConditions.sameLocation());
        menu.add(overview);

        menu.add(new RouterLink("Familien", FamiliesView.class));
        /*menu.add(new RouterLink("Mitglieder", MembersView.class));
        menu.add(new RouterLink("Beratungen", ConsultationsView.class));
        menu.add(new RouterLink("Delegationen", DelegationsView.class));
        menu.add(new RouterLink("Dokumente", DocumentsView.class));
        if (lvl >= 80) menu.add(new RouterLink("Mitarbeiter", EmployeesView.class));
        if (lvl == 100) menu.add(new RouterLink("Admin Panel", AdminView.class));*/

        VerticalLayout bottom = new VerticalLayout();
        bottom.setPadding(false);
        bottom.setSpacing(false);
        bottom.setAlignItems(FlexComponent.Alignment.START);

        timeSpan = new Span("30:00");
        timeSpan.getStyle().set("margin-top", "20px")
                .set("font-weight", "bold")
                .set("color", "red");
        menu.add(timeSpan);

        Button logout = new Button("Logout", e2 -> authService.logout());
        bottom.add(timeSpan, logout);
        menu.add(top, bottom);

        addToDrawer(menu);
    }

    private void startLogoutTimer() {
    }
}