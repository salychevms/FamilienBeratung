package com.salychevms.familienberatung.ui.view;

import com.salychevms.familienberatung.model.Consultation;
import com.salychevms.familienberatung.model.Employee;
import com.salychevms.familienberatung.model.Member;
import com.salychevms.familienberatung.model.RecordStatus;
import com.salychevms.familienberatung.service.AuthService;
import com.salychevms.familienberatung.service.ConsultationService;
import com.salychevms.familienberatung.service.MemberService;
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

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Route(value = "overview", layout = MainLayout.class)
@PageTitle("Übersicht")
@RequiredArgsConstructor
public class OverviewView extends VerticalLayout implements BeforeEnterObserver {

    private final AuthService authService;
    private final MemberService memberService;
    private final ConsultationService consultationService;

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        Employee employee = authService.getCurrentEmployee();
        if (employee == null || employee.isArchived()) {
            beforeEnterEvent.forwardTo("login");
            return;
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

        int memberCount = getMembersCount(e, lvl);
        int conCount = getConsultationsCount(e, lvl);
        int hours = getConsultationsHours(e, lvl);

        Paragraph p1 = new Paragraph("Teilnehmerzahl: " + memberCount);
        Paragraph p2 = new Paragraph("Beratungen: " + conCount);
        Paragraph p3 = new Paragraph("Beratungsstundenanzahl: " + (hours / 60) + ":" + String.format("%02d", hours % 60) + " St.");

        VerticalLayout stats = new VerticalLayout(p1, p2, p3);
        stats.setPadding(false);
        stats.setSpacing(false);

        HorizontalLayout nav = new HorizontalLayout();
        nav.setSpacing(true);
        nav.add(makeNavButton("Teilnehmer*innen", MembersView.class));
        /*nav.add(new Button("Beratungen", ev ->
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

    private int getMembersCount(Employee e, int lvl) {
        if (lvl == 50) {
            return memberService.getMembersByAssignedEmployee(e.getLogin(), e)
                    .stream().filter(f -> !f.getStatus().equals(RecordStatus.INVALID)).toList().size();
        }
        return memberService.getMembers()
                .stream().filter(f -> !f.getStatus().equals(RecordStatus.INVALID)).toList().size();
    }

    private int getConsultationsCount(Employee e, int lvl) {
        List<Member> members;
        List<Consultation> consultations = new ArrayList<>();
        if (lvl == 50) {
            members = memberService.getMembersByAssignedEmployee(e.getLogin(), e);
            for (Member f : members)
                if (!f.getStatus().equals(RecordStatus.INVALID))
                    consultations.addAll(consultationService.getConsultationsByFamily(f));
        } else {
            members = memberService.getMembers();
            for (Member f : members)
                if (!f.getStatus().equals(RecordStatus.INVALID))
                    consultations.addAll(consultationService.getConsultationsByFamily(f));
        }
        return consultations.size();
    }

    private int getConsultationsHours(Employee e, int lvl) {
        List<Member> members;
        List<Consultation> consultations = new ArrayList<>();
        if (lvl == 50) {
            members = memberService.getMembersByAssignedEmployee(e.getLogin(), e);
            for (Member f : members)
                if (!f.getStatus().equals(RecordStatus.INVALID))
                    consultations.addAll(consultationService.getConsultationsByFamily(f));
        } else {
            members = memberService.getMembers();
            for (Member f : members)
                if (!f.getStatus().equals(RecordStatus.INVALID))
                    consultations.addAll(consultationService.getConsultationsByFamily(f));
        }

        int hours = 0;
        for (Consultation c : consultations) {
            hours += c.getDurationMinutes();
        }
        return hours;
    }

    private Button makeNavButton(String text, Class<? extends Component> target) {
        Button button = new Button(text, e -> {
            getUI().ifPresent(ui -> ui.navigate(target));
        });
        button.setWidth("150px");
        return button;
    }
}
