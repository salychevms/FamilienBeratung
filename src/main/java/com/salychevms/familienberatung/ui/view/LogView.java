package com.salychevms.familienberatung.ui.view;

import com.salychevms.familienberatung.model.Employee;
import com.salychevms.familienberatung.service.AuthService;
import com.salychevms.familienberatung.service.EmployeeService;
import com.salychevms.familienberatung.ui.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
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
@Route(value = "log", layout = MainLayout.class)
@PageTitle("Log")
@PermitAll
@RequiredArgsConstructor
public class LogView extends VerticalLayout implements BeforeEnterObserver {
    private final EmployeeService employeeService;
    private final AuthService authService;

    @Override
    public void beforeEnter(BeforeEnterEvent event){
        Employee authorized=authService.getCurrentEmployee();
        if(authorized==null || !authorized.isActive()||authorized.isArchived()){
            event.forwardTo("login");
            return;
        }

        buildUI();
    }

    private void buildUI(){
        VerticalLayout layout = new VerticalLayout();
        Span sorry=new Span("Sorry, this page is in development");
        layout.add(sorry);

        Button back= new Button("to Overview",
                event -> getUI().ifPresent(ui -> ui.navigate(OverviewView.class)));
        layout.add(back);
        add(layout);
    }
}
