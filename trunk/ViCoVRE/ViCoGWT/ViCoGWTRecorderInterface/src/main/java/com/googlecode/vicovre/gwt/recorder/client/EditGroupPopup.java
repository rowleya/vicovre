package com.googlecode.vicovre.gwt.recorder.client;


import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.googlecode.vicovre.gwt.recorder.client.rest.GroupUserEditor;
import com.googlecode.vicovre.gwt.utils.client.ModalPopup;

public class EditGroupPopup extends ModalPopup<VerticalPanel>
        implements ClickHandler {

    private String url = null;

    private ListBox users = new ListBox(true);

    private Button add = new Button("<-- Add");

    private Button remove = new Button("Remove -->");

    private Button clear = new Button("Clear");

    private ListBox members = new ListBox(true);

    private Button ok = new Button("OK");

    private Button cancel = new Button("Cancel");

    private String group = null;

    public EditGroupPopup(String group, String url, JsArrayString userList,
            JsArrayString memberList) {
        super(new VerticalPanel());
        this.url = url;
        this.group = group;

        VerticalPanel mainPanel = getWidget();

        HorizontalPanel memberPanel = new HorizontalPanel();
        memberPanel.setHorizontalAlignment(HorizontalPanel.ALIGN_CENTER);
        VerticalPanel memberButtonPanel = new VerticalPanel();
        memberButtonPanel.setHeight("100%");
        memberButtonPanel.setHorizontalAlignment(VerticalPanel.ALIGN_CENTER);
        memberButtonPanel.setVerticalAlignment(VerticalPanel.ALIGN_MIDDLE);
        memberButtonPanel.add(add);
        memberButtonPanel.add(remove);
        memberButtonPanel.add(clear);
        memberPanel.add(members);
        memberPanel.add(memberButtonPanel);
        memberPanel.add(users);
        members.setWidth("100%");
        users.setWidth("100%");
        memberPanel.setCellWidth(members, "300px");
        memberPanel.setCellWidth(memberButtonPanel, "100px");
        memberPanel.setCellWidth(users, "300px");
        mainPanel.add(memberPanel);

        HorizontalPanel buttonPanel = new HorizontalPanel();
        buttonPanel.setWidth("100%");
        buttonPanel.add(ok);
        buttonPanel.add(cancel);
        buttonPanel.setCellHorizontalAlignment(ok, HorizontalPanel.ALIGN_LEFT);
        buttonPanel.setCellHorizontalAlignment(cancel,
                HorizontalPanel.ALIGN_RIGHT);
        mainPanel.add(buttonPanel);

        ok.addClickHandler(this);
        cancel.addClickHandler(this);
        add.addClickHandler(this);
        remove.addClickHandler(this);
        clear.addClickHandler(this);

        for (int i = 0; i < userList.length(); i++) {
            users.addItem(userList.get(i));
        }

        if (memberList != null) {
            for (int i = 0; i < memberList.length(); i++) {
                moveItem(users, members, memberList.get(i));
            }
        }
    }

    private void moveItem(ListBox from, ListBox to, String itemValue) {
        for (int i = 0; i < from.getItemCount(); i++) {
            if (from.getValue(i).equals(itemValue)) {
                to.addItem(from.getItemText(i), from.getValue(i));
                from.removeItem(i);
                i--;
            }
        }
    }

    private void moveSelectedItems(ListBox from, ListBox to) {
        for (int i = 0; i < from.getItemCount(); i++) {
            if (from.isItemSelected(i)) {
                to.addItem(from.getItemText(i), from.getValue(i));
                from.removeItem(i);
                i--;
            }
        }
    }

    private void clearItems(ListBox from, ListBox to) {
        for (int i = 0; i < from.getItemCount(); i++) {
            to.addItem(from.getItemText(i), from.getValue(i));
        }
        from.clear();
    }

    public String[] getMembers() {
        String[] memberList = new String[members.getItemCount()];
        for (int i = 0; i < members.getItemCount(); i++) {
            memberList[i] = members.getValue(i);
        }
        return memberList;
    }

    public void onClick(ClickEvent event) {
        if (event.getSource() == ok) {
            GroupUserEditor.edit(this, group, url);
        } else if (event.getSource() == cancel) {
            hide();
        } else if (event.getSource() == add) {
            moveSelectedItems(users, members);
        } else if (event.getSource() == remove) {
            moveSelectedItems(members, users);
        } else if (event.getSource() == clear) {
            clearItems(members, users);
        }
    }

}
