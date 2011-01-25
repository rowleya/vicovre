package com.googlecode.vicovre.gwt.recorder.client;

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.googlecode.vicovre.gwt.utils.client.ModalPopup;

public class EditUsersPopup extends ModalPopup<VerticalPanel>
        implements ClickHandler {

    private Button close = new Button("Close");

    public EditUsersPopup(String url, JsArrayString users) {
        super(new VerticalPanel());

        VerticalPanel mainPanel = getWidget();
        mainPanel.setWidth("600px");

        VerticalPanel userPanel = new VerticalPanel();
        userPanel.setWidth("100%");

        if (users != null) {
            for (int i = 0; i < users.length(); i++) {
                String user = users.get(i);
                if (!user.equals("admin")) {
                    UserPanel panel = new UserPanel(user, url);
                    userPanel.add(panel);
                }
            }
        }
        mainPanel.add(userPanel);

        mainPanel.add(close);
        close.addClickHandler(this);
    }

    public void onClick(ClickEvent event) {
        hide();
    }

}
