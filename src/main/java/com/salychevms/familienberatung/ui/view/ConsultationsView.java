package com.salychevms.familienberatung.ui.view;

import com.salychevms.familienberatung.model.Employee;
import com.salychevms.familienberatung.service.AuthService;
import com.salychevms.familienberatung.service.EmployeeService;
import com.salychevms.familienberatung.service.FamilyService;
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

import java.util.Optional;

@Slf4j
@Route(value = "consultations/:familyId?", layout = MainLayout.class)
@PageTitle("Beratungen")
@RequiredArgsConstructor
@PermitAll
public class ConsultationsView extends VerticalLayout implements BeforeEnterObserver {
    private final AuthService authService;
    private final FamilyService familyService;
    private final EmployeeService employeeService;

    private Long familyId;
    private Employee currentEmployee;

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Employee e = authService.getCurrentEmployee();
        if (e == null || e.isArchived() || !e.isActive()) {
            event.forwardTo("login");
            return;
        }

        currentEmployee=employeeService.findByLogin(e.getLogin());

        Optional<Long> fam = event.getRouteParameters().getLong("familyId");
        this.familyId = fam.orElse(null);

        buildUI(e);
    }

    private void buildUI(Employee e) {
        setPadding(true);
        setSpacing(true);

        String mode;

        if (familyId != null) {
            mode = "Modus: Beratungen der Familie: " +
                    familyService.getFamilyById(currentEmployee.getLogin(), familyId).getFamilyName();
        } else if (e.getRole().getAccessLevel() == 50) {
            mode = "Modus: Alle Beratungen für: "+currentEmployee.getLogin()+
                    " : "+currentEmployee.getFirstName()+" "+currentEmployee.getLastName();
        } else {
            mode = "Modus: Alle Beratungen";
        }

        add(new Span("Consultation view is still in development..."));
        add(new Span(mode));
    }
}
