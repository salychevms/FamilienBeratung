package com.salychevms.familienberatung.ui;

import com.salychevms.familienberatung.service.AuthService;
import com.salychevms.familienberatung.ui.view.OverviewView;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

import java.awt.*;

@Route("login")
@PageTitle("Login")
@AnonymousAllowed
@RequiredArgsConstructor
public class LoginView extends VerticalLayout {
    private final AuthService authService;

    @PostConstruct
    public void init() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        buildUI();
    }

    private void buildUI(){
        H1 title = new H1("Familienberatung");

        TextField loginField = new TextField("Login");
        loginField.setWidth("250px");
        PasswordField passwordField = new PasswordField("Password");
        passwordField.setWidth("250px");
        Button loginButton = new Button("Login", e->
                login(loginField.getValue(), passwordField.getValue()));
        loginButton.setWidth("250px");

        add(title, loginField, passwordField, loginButton);
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
    }

    private void login(String login, String password){
        try {
            String ip= VaadinRequest.getCurrent().getRemoteAddr();

            String browser = VaadinRequest.getCurrent().getHeader("User-Agent");

            authService.login(login, password, ip, browser);
            UI.getCurrent().navigate(OverviewView.class);
        }catch (Exception e){
            Notification.show("Login fehlgeschlagen", 3000, Notification.Position.MIDDLE);
        }
    }
}