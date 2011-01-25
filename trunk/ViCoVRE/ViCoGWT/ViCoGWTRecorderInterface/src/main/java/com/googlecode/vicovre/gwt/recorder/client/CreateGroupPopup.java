package com.googlecode.vicovre.gwt.recorder.client;


import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.googlecode.vicovre.gwt.recorder.client.rest.GroupCreator;
import com.googlecode.vicovre.gwt.utils.client.ModalPopup;

public class CreateGroupPopup extends ModalPopup<FlexTable>
        implements ClickHandler {

    private PermissionExceptionPanel panel = null;

    private String url = null;

    private TextBox group = new TextBox();

    private Button ok = new Button("OK");

    private Button cancel = new Button("Cancel");

    public CreateGroupPopup(PermissionExceptionPanel panel, String url) {
        super(new FlexTable());
        this.panel = panel;
        this.url = url;

        FlexTable mainPanel = getWidget();
        mainPanel.setWidth("300px");
        mainPanel.setHeight("100px");

        mainPanel.setWidget(0, 0, new Label("Group Name:"));
        mainPanel.setWidget(0, 1, group);

        mainPanel.setWidget(1, 0, cancel);
        mainPanel.setWidget(1, 1, ok);

        mainPanel.getCellFormatter().setHorizontalAlignment(1, 1,
                HorizontalPanel.ALIGN_RIGHT);

        ok.addClickHandler(this);
        cancel.addClickHandler(this);
    }

    public String getGroup() {
        return group.getText();
    }

    public void onClick(ClickEvent event) {
        if (event.getSource() == ok) {
            GroupCreator.create(this, panel, url);
        } else if (event.getSource() == cancel) {
            hide();
        }
    }

}
