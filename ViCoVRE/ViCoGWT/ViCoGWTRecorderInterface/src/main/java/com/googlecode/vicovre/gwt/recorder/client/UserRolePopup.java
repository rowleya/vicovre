package com.googlecode.vicovre.gwt.recorder.client;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.googlecode.vicovre.gwt.client.json.JSONUser;
import com.googlecode.vicovre.gwt.recorder.client.rest.UserRoleSetter;
import com.googlecode.vicovre.gwt.utils.client.ModalPopup;

public class UserRolePopup extends ModalPopup<VerticalPanel>
        implements ClickHandler {

    private String username = null;

    private ListBox roleList = new ListBox(false);

    private String url = null;

    private Button okButton = new Button("OK");

    private Button cancelButton = new Button("Cancel");

    public UserRolePopup(String username, String role, String url) {
        super(new VerticalPanel());
        this.username = username;
        this.url = url;

        VerticalPanel panel = getWidget();
        panel.setWidth("300px");

        roleList.setWidth("100%");
        roleList.addItem("Administrator", JSONUser.ROLE_ADMINISTRATOR);
        roleList.addItem("Writer", JSONUser.ROLE_WRITER);
        roleList.addItem("User", JSONUser.ROLE_USER);

        for (int i = 0; i < roleList.getItemCount(); i++) {
            if (roleList.getValue(i).equals(role)) {
                roleList.setItemSelected(i, true);
            }
        }

        HorizontalPanel buttonPanel = new HorizontalPanel();
        buttonPanel.setWidth("100%");
        buttonPanel.add(cancelButton);
        buttonPanel.add(okButton);
        buttonPanel.setCellHorizontalAlignment(okButton,
                HorizontalPanel.ALIGN_RIGHT);

        panel.add(new Label("Select a role for the user:"));
        panel.add(roleList);
        panel.add(buttonPanel);

        okButton.addClickHandler(this);
        cancelButton.addClickHandler(this);
    }

    public void onClick(ClickEvent event) {
        if (event.getSource() == cancelButton) {
            hide();
        } else if (event.getSource() == okButton) {
            String role = roleList.getValue(roleList.getSelectedIndex());
            UserRoleSetter.set(url, this, username, role);
        }
    }

}
