package com.salychevms.familienberatung.ui.view;

import com.salychevms.familienberatung.model.Employee;
import com.salychevms.familienberatung.service.AuthService;
import com.salychevms.familienberatung.service.EmployeeService;
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
@Route(value="consultation/:id", layout = MainLayout.class)
@PageTitle("Beratung")
@RequiredArgsConstructor
@PermitAll
public class ConsultationDetailsView extends VerticalLayout implements BeforeEnterObserver {
    private final AuthService authService;
    private final EmployeeService employeeService;

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Employee e=authService.getCurrentEmployee();
        if(e==null || e.isArchived() || !e.isActive()){
            event.forwardTo("login");
            return;
        }

        employeeService.findByLogin(e.getLogin());

        setPadding(true);
        setSpacing(true);

        add(new Span("Consultation details view is still in development..."));
    }
}
