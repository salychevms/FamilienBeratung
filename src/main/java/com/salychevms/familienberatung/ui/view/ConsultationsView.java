package com.salychevms.familienberatung.ui.view;

import com.salychevms.familienberatung.model.*;
import com.salychevms.familienberatung.service.*;
import com.salychevms.familienberatung.ui.layout.MainLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
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
    private final ConsultationService consultationService;
    private final DelegationService delegationService;

    private Long familyId;
    private Employee currentEmployee;
    private int lvl;
    private List<Delegation> delegations = new ArrayList<>();
    private List<Consultation> consultations = new ArrayList<>();
    private boolean initialized = false;

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Employee e = authService.getCurrentEmployee();
        if (e == null || e.isArchived() || !e.isActive()) {
            event.forwardTo("login");
            return;
        }

        this.currentEmployee = employeeService.findByLogin(e.getLogin());
        this.lvl = currentEmployee.getRole().getAccessLevel();

        Optional<Long> fam = event.getRouteParameters().getLong("familyId");
        this.familyId = fam.orElse(null);

        if (lvl == 50) this.delegations = delegationService.getDelegationsByToEmployee(currentEmployee);
        else this.delegations = delegationService.getDelegations();

        if (lvl == 50) {
            List<Consultation> cList = consultationService.getAll();
            for (Consultation c : cList) {
                if ((c.getEmployee().equals(currentEmployee)
                        || c.getFamily().getAssignedEmployee().equals(currentEmployee))
                        && (!c.isInvalid() && !c.getFamily().getStatus().equals(RecordStatus.INVALID)))
                    this.consultations.add(c);
            }
        } else this.consultations = consultationService.getAll();

        if (!initialized) {
            initialized = true;
            buildUI();
        }
    }

    private void buildUI() {

    }
}
