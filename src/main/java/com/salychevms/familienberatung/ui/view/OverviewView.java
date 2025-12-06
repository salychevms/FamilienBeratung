package com.salychevms.familienberatung.ui.view;

import com.salychevms.familienberatung.model.Consultation;
import com.salychevms.familienberatung.model.Employee;
import com.salychevms.familienberatung.model.Family;
import com.salychevms.familienberatung.model.RecordStatus;
import com.salychevms.familienberatung.service.AuthService;
import com.salychevms.familienberatung.service.ConsultationService;
import com.salychevms.familienberatung.service.FamilyService;
import com.salychevms.familienberatung.ui.layout.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Route(value = "overview", layout = MainLayout.class)
@PageTitle("Übersicht")
@RequiredArgsConstructor
public class OverviewView extends VerticalLayout implements BeforeEnterObserver {

    private final AuthService authService;
    private final FamilyService familyService;
    private final ConsultationService consultationService;

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        Employee employee = authService.getCurrentEmployee();
        if (employee == null) {
            beforeEnterEvent.forwardTo("login");
        }
    }

    @PostConstruct
    private void init() {
        setPadding(true);
        setSpacing(true);
        setHeightFull();
        setJustifyContentMode(JustifyContentMode.BETWEEN);

        VerticalLayout top = new VerticalLayout();
        top.setPadding(false);
        top.setSpacing(false);
        top.setWidthFull();

        Employee e = authService.getCurrentEmployee();
        if (e == null) return;

        int lvl = e.getRole().getAccessLevel();

        H2 title = new H2(buildTitle(e, lvl));

        int famCount = getFamiliesCount(e, lvl);
        int conCount = getConsultationsCount(e, lvl);
        int hours = getHours(e, lvl);

        Paragraph p1 = new Paragraph("Familien: " + famCount);
        Paragraph p2 = new Paragraph("Beratungen: " + conCount);
        Paragraph p3 = new Paragraph("Stunden (Min): " + hours);

        VerticalLayout stats = new VerticalLayout(p1, p2, p3);
        stats.setPadding(false);
        stats.setSpacing(false);

        HorizontalLayout nav = new HorizontalLayout();
        nav.setSpacing(true);
        nav.add(makeNavButton("Familien", FamiliesView.class));
        /*nav.add(new Button("Mitglieder", ev ->
                getUI().ifPresent(ui -> ui.navigate(MembersView.class))));
        nav.add(new Button("Beratungen", ev ->
                getUI().ifPresent(ui -> ui.navigate(ConsultationsView.class))));
        nav.add(new Button("Dokumente", ev ->
                getUI().ifPresent(ui -> ui.navigate(DocumentsView.class))));
        nav.add(new Button("Delegationen", ev ->
                getUI().ifPresent(ui -> ui.navigate(DelegationsView.class))));
        if (lvl >= 80)
            nav.add(new Button("Mitarbeiter", ev ->
                    getUI().ifPresent(ui -> ui.navigate(EmployeesView.class))));*/

        top.add(title, stats, nav);

        VerticalLayout bottom = new VerticalLayout();
        bottom.setPadding(false);
        bottom.setSpacing(false);

        Paragraph info = new Paragraph("Info-Board (Service)");
        info.getStyle().set("margin-top", "30px");
        bottom.add(info);
        add(top, bottom);
    }

    private String buildTitle(Employee e, int lvl) {
        return switch (lvl) {
            case 50 -> "Berater*in: " + e.getFirstName() + " " + e.getLastName();
            case 80 -> "Projektleiter*in: " + e.getFirstName() + " " + e.getLastName();
            case 100 -> "Admin: " + e.getFirstName() + " " + e.getLastName();
            case 10 -> "Read only: " + e.getFirstName() + " " + e.getLastName();
            default -> "Mitarbeiter*in: " + e.getFirstName() + " " + e.getLastName();
        };
    }

    private int getFamiliesCount(Employee e, int lvl) {
        if (lvl == 50) {
            return familyService.getFamiliesByAssignedEmployee(e.getLogin(), e.getId()).size();
        }
        return familyService.getFamilies(e.getLogin()).size();
    }

    private int getHours(Employee e, int lvl) {
        List<Family> f;
        if (lvl == 50) {
            f = familyService.getFamiliesByAssignedEmployee(e.getLogin(), e.getId());

        } else {
            f = familyService.getFamilies(e.getLogin());
        }

        int countOfHours = 0;
        for (Family f2 : f) {
            if (!f2.getStatus().equals(RecordStatus.INVALID)) {
                List<Consultation> c = consultationService.getConsultationsByFamily(f2, e);
                for (Consultation c2 : c) {
                    countOfHours += c2.getDurationMinutes();
                }
            }
        }
        return countOfHours;
    }

    private int getConsultationsCount(Employee e, int lvl) {
        List<Family> f;
        if (lvl == 50) {
            f = familyService.getFamiliesByAssignedEmployee(e.getLogin(), e.getId());
        } else {
            f = familyService.getFamilies(e.getLogin());
        }
        int countOfConsultations = 0;
        for (Family f2 : f) {
            if (!f2.getStatus().equals(RecordStatus.INVALID))
                countOfConsultations += consultationService.getConsultationsByFamily(f2, e).size();
        }
        return countOfConsultations;
    }

    private Button makeNavButton(String text, Class<? extends Component> target) {
        Button button = new Button(text, e -> {
            getUI().ifPresent(ui -> ui.navigate(target));
        });
        button.setWidth("150px");
        return button;
    }
}
