package com.salychevms.familienberatung.ui.view;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;

@Route("family")
public class FamilyDetailsView extends VerticalLayout implements HasUrlParameter<Long> {

    @Override
    public void setParameter(BeforeEvent e, Long id){
        add(new Span("Family details placeholder. Id="+id));
    }
}
