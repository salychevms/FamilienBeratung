package com.salychevms.familienberatung.ui.view;

import com.salychevms.familienberatung.model.Employee;
import com.salychevms.familienberatung.service.AuthService;
import com.salychevms.familienberatung.ui.layout.MainLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Route(value = "member/:id/:familyId", layout = MainLayout.class)
@PageTitle("Mitglied")
@RequiredArgsConstructor
@PermitAll
public class MemberDetailsView extends VerticalLayout implements BeforeEnterObserver {

    private final AuthService authService;

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Employee employee = authService.getCurrentEmployee();
        if(employee==null || employee.isArchived() || !employee.isActive()){
            event.forwardTo("login");
            return;
        }

        buildUI();
    }

    private void buildUI() {
        setPadding(true);
        setSpacing(true);
        add(new Span("Still in development..."));
    }
}
