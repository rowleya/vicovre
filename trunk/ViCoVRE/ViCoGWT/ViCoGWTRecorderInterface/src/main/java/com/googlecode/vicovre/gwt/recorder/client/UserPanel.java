package com.googlecode.vicovre.gwt.recorder.client;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.googlecode.vicovre.gwt.recorder.client.rest.UserDeleter;
import com.googlecode.vicovre.gwt.recorder.client.rest.UserRoleLoader;

public class UserPanel extends SimplePanel implements ClickHandler {

    private String url = null;

    private Label username = new Label();

    private Button deleteButton = new Button("Delete");

    private Button roleButton = new Button("Change Role");

    public UserPanel(String username, String url) {
        this.username.setText(username);
        this.username.setWidth("100%");
        this.url = url;

        setWidth("100%");
        DOM.setStyleAttribute(getElement(), "borderWidth", "1px");
        DOM.setStyleAttribute(getElement(), "borderStyle", "solid");

        HorizontalPanel panel = new HorizontalPanel();
        panel.setWidth("100%");
        add(panel);

        HorizontalPanel buttons = new HorizontalPanel();
        buttons.add(roleButton);
        buttons.add(deleteButton);

        panel.add(this.username);
        panel.add(buttons);
        panel.setCellWidth(buttons, "150px");
        panel.setCellHorizontalAlignment(buttons, HorizontalPanel.ALIGN_RIGHT);

        roleButton.addClickHandler(this);
        deleteButton.addClickHandler(this);
    }

    public void onClick(ClickEvent event) {
        if (event.getSource() == roleButton) {
            UserRoleLoader.load(url, username.getText());
        } else if (event.getSource() == deleteButton) {
            UserDeleter.delete(url, username.getText());
        }
    }
}
