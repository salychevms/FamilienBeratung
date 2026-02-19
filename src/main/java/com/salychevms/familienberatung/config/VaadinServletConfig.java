package com.salychevms.familienberatung.config;

import com.vaadin.flow.spring.SpringServlet;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.WebApplicationInitializer;

@Configuration
public class VaadinServletConfig implements WebApplicationInitializer {

    private final ApplicationContext applicationContext;

    public VaadinServletConfig(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void onStartup(ServletContext servletContext)
            throws ServletException {

        SpringServlet vaadinServlet =
                new SpringServlet(applicationContext, true);

        ServletRegistration.Dynamic registration =
                servletContext.addServlet("vaadin", vaadinServlet);

        registration.addMapping("/*");
        registration.setLoadOnStartup(1);
    }
}