package com.googlecode.vicovre.gwt.recorder.client;


import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.googlecode.vicovre.gwt.recorder.client.rest.UserCreator;
import com.googlecode.vicovre.gwt.utils.client.ModalPopup;

public class CreateUserPopup extends ModalPopup<FlexTable>
        implements ClickHandler {

    private PermissionExceptionPanel panel = null;

    private String url = null;

    private TextBox username = new TextBox();

    private Button ok = new Button("OK");

    private Button cancel = new Button("Cancel");

    public CreateUserPopup(PermissionExceptionPanel panel, String url) {
        super(new FlexTable());
        this.panel = panel;
        this.url = url;

        FlexTable mainPanel = getWidget();
        mainPanel.setWidth("300px");
        mainPanel.setHeight("100px");

        mainPanel.setWidget(0, 0, new Label("E-mail address:"));
        mainPanel.setWidget(0, 1, username);

        mainPanel.setWidget(1, 0, cancel);
        mainPanel.setWidget(1, 1, ok);

        mainPanel.getCellFormatter().setHorizontalAlignment(1, 1,
                HorizontalPanel.ALIGN_RIGHT);

        ok.addClickHandler(this);
        cancel.addClickHandler(this);
    }

    public String getUsername() {
        return username.getText();
    }

    public void onClick(ClickEvent event) {
        if (event.getSource() == ok) {
            UserCreator.create(this, panel, url);
        } else if (event.getSource() == cancel) {
            hide();
        }
    }

}
