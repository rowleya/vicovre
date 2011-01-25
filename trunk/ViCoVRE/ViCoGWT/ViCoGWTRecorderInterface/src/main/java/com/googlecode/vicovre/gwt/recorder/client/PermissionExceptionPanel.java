package com.googlecode.vicovre.gwt.recorder.client;

import java.util.Vector;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.googlecode.vicovre.gwt.client.json.JSONACL;
import com.googlecode.vicovre.gwt.client.json.JSONACLEntity;
import com.googlecode.vicovre.gwt.client.json.JSONUser;
import com.googlecode.vicovre.gwt.recorder.client.rest.GroupDeleter;
import com.googlecode.vicovre.gwt.recorder.client.rest.GroupLoader;

public class PermissionExceptionPanel extends VerticalPanel
        implements ClickHandler {

    private ListBox userList = new ListBox(true);

    private ListBox userExceptionList = new ListBox(true);

    private ListBox groupList = new ListBox(true);

    private ListBox groupExceptionList = new ListBox(true);

    private ListBox roleList = new ListBox(true);

    private ListBox roleExceptionList = new ListBox(true);

    private Button addUserException = new Button("<-- Add");

    private Button removeUserException = new Button("Remove -->");

    private Button createUser = new Button("Create User");

    private Button addGroupException = new Button("<-- Add");

    private Button removeGroupException = new Button("Remove -->");

    private Button createGroup = new Button("Create Group");

    private Button editAddedGroup = new Button("Edit");

    private Button deleteAddedGroup = new Button("Delete");

    private Button editGroup = new Button("Edit");

    private Button deleteGroup = new Button("Delete");

    private Button addRoleException = new Button("<-- Add");

    private Button removeRoleException = new Button("Remove -->");

    private Button clearUserExceptions = new Button("Clear");

    private Button clearGroupExceptions = new Button("Clear");

    private Button clearRoleExceptions = new Button("Clear");

    private JsArrayString users = null;

    private String url = null;

    public PermissionExceptionPanel(JsArrayString users, JsArrayString groups,
            JSONACL acl, String url) {
        this.users = users;
        this.url = url;
        if (users != null) {
            for (int i = 0; i < users.length(); i++) {
                userList.addItem(users.get(i));
            }
        }
        add(new HTML("<B>Users:</B>"));
        add(createExceptionPanel(userList, userExceptionList,
                addUserException, removeUserException, clearUserExceptions,
                createUser, null, null, null, null));

        if (groups != null) {
            for (int i = 0; i < groups.length(); i++) {
                groupList.addItem(groups.get(i));
            }
        }
        add(new HTML("<B>Groups:</B>"));
        add(createExceptionPanel(groupList, groupExceptionList,
                addGroupException, removeGroupException, clearGroupExceptions,
                createGroup, editAddedGroup, deleteAddedGroup, editGroup,
                deleteGroup));

        roleList.addItem("Administrator", JSONUser.ROLE_ADMINISTRATOR);
        roleList.addItem("Writer", JSONUser.ROLE_WRITER);
        roleList.addItem("User", JSONUser.ROLE_USER);
        roleList.addItem("Guest", JSONUser.ROLE_GUEST);
        add(new HTML("<B>Roles:</B>"));
        add(createExceptionPanel(roleList, roleExceptionList,
                addRoleException, removeRoleException, clearRoleExceptions,
                null, null, null, null, null));

        JsArray<JSONACLEntity> exceptions = acl.getExceptions();
        if (exceptions != null) {
            for (int i = 0; i < exceptions.length(); i++) {
                JSONACLEntity exception = exceptions.get(i);
                String type = exception.getType();
                if (type.equals("user")) {
                    moveItem(userList, userExceptionList,
                            exception.getName());
                } else if (type.equals("group")) {
                    moveItem(groupList, groupExceptionList,
                            exception.getName());
                } else if (type.equals("role")) {
                    moveItem(roleList, roleExceptionList,
                            exception.getName());
                }
            }
        }
    }

    private HorizontalPanel createExceptionPanel(ListBox list,
            ListBox exceptionList, Button addButton, Button removeButton,
            Button clearButton, Button createButton,
            Button editAddedButton, Button deleteAddedButton,
            Button editButton, Button deleteButton) {
        VerticalPanel listPanel = new VerticalPanel();
        listPanel.setWidth("100%");
        listPanel.setHeight("100%");
        listPanel.add(list);
        if ((editButton != null) || (deleteButton != null)) {
            HorizontalPanel buttonPanel = new HorizontalPanel();
            buttonPanel.setWidth("100%");
            if (editButton != null) {
                buttonPanel.add(editButton);
                editButton.addClickHandler(this);
                editButton.setWidth("100%");
            }
            if (deleteButton != null) {
                buttonPanel.add(deleteButton);
                deleteButton.addClickHandler(this);
                deleteButton.setWidth("100%");
            }
            listPanel.add(buttonPanel);
        }
        VerticalPanel exceptionListPanel = new VerticalPanel();
        exceptionListPanel.setWidth("100%");
        exceptionListPanel.setHeight("100%");
        exceptionListPanel.add(exceptionList);
        if ((editAddedButton != null) || (deleteAddedButton != null)) {
            HorizontalPanel buttonPanel = new HorizontalPanel();
            buttonPanel.setWidth("100%");
            if (editAddedButton != null) {
                buttonPanel.add(editAddedButton);
                editAddedButton.addClickHandler(this);
                editAddedButton.setWidth("100%");
            }
            if (deleteAddedButton != null) {
                buttonPanel.add(deleteAddedButton);
                deleteAddedButton.addClickHandler(this);
                deleteAddedButton.setWidth("100%");
            }
            exceptionListPanel.add(buttonPanel);
        }

        list.setWidth("100%");
        list.setHeight("100%");
        exceptionList.setWidth("100%");
        exceptionList.setHeight("100%");
        HorizontalPanel panel = new HorizontalPanel();
        panel.setWidth("100%");
        panel.setHorizontalAlignment(HorizontalPanel.ALIGN_CENTER);
        VerticalPanel buttonPanel = new VerticalPanel();
        buttonPanel.setHeight("100%");
        buttonPanel.setHorizontalAlignment(VerticalPanel.ALIGN_CENTER);
        buttonPanel.setVerticalAlignment(VerticalPanel.ALIGN_MIDDLE);
        buttonPanel.add(addButton);
        buttonPanel.add(removeButton);
        if (createButton != null) {
            buttonPanel.add(createButton);
        }
        buttonPanel.add(clearButton);
        panel.add(exceptionListPanel);
        panel.add(buttonPanel);
        panel.add(listPanel);
        panel.setCellWidth(exceptionListPanel, "300px");
        panel.setCellWidth(buttonPanel, "100px");
        panel.setCellWidth(listPanel, "300px");

        addButton.addClickHandler(this);
        removeButton.addClickHandler(this);
        clearButton.addClickHandler(this);
        if (createButton != null) {
            createButton.addClickHandler(this);
        }
        return panel;
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

    public void onClick(ClickEvent event) {
        if (event.getSource() == addUserException) {
            moveSelectedItems(userList, userExceptionList);
        } else if (event.getSource() == removeUserException) {
            moveSelectedItems(userExceptionList, userList);
        } else if (event.getSource() == clearUserExceptions) {
            clearItems(userExceptionList, userList);
        } else if (event.getSource() == addGroupException) {
            moveSelectedItems(groupList, groupExceptionList);
        } else if (event.getSource() == removeGroupException) {
            moveSelectedItems(groupExceptionList, groupList);
        } else if (event.getSource() == clearGroupExceptions) {
            clearItems(groupExceptionList, groupList);
        } else if (event.getSource() == addRoleException) {
            moveSelectedItems(roleList, roleExceptionList);
        } else if (event.getSource() == removeRoleException) {
            moveSelectedItems(roleExceptionList, roleList);
        } else if (event.getSource() == clearRoleExceptions) {
            clearItems(roleExceptionList, roleList);
        } else if (event.getSource() == createUser) {
            CreateUserPopup popup = new CreateUserPopup(this, url);
            popup.center();
        } else if (event.getSource() == createGroup) {
            CreateGroupPopup popup = new CreateGroupPopup(this, url);
            popup.center();
        } else if (event.getSource() == editAddedGroup) {
            if (groupExceptionList.getSelectedIndex() >= 0) {
                String group = groupExceptionList.getValue(
                        groupExceptionList.getSelectedIndex());
                GroupLoader.load(url, group, users);
            }
        } else if (event.getSource() == deleteAddedGroup) {
            if (groupExceptionList.getSelectedIndex() >= 0) {
                String group = groupExceptionList.getValue(
                        groupExceptionList.getSelectedIndex());
                GroupDeleter.delete(this, group, url);
            }
        } else if (event.getSource() == editGroup) {
            if (groupList.getSelectedIndex() >= 0) {
                String group = groupList.getValue(
                        groupList.getSelectedIndex());
                GroupLoader.load(url, group, users);
            }
        } else if (event.getSource() == deleteGroup) {
            if (groupList.getSelectedIndex() >= 0) {
                String group = groupList.getValue(
                        groupList.getSelectedIndex());
                GroupDeleter.delete(this, group, url);
            }
        }
    }

    public String[] getExceptionTypes() {
        Vector<String> exceptionTypes = new Vector<String>();
        for (int i = 0; i < userExceptionList.getItemCount(); i++) {
            exceptionTypes.add("user");
        }
        for (int i = 0; i < groupExceptionList.getItemCount(); i++) {
            exceptionTypes.add("group");
        }
        for (int i = 0; i < roleExceptionList.getItemCount(); i++) {
            exceptionTypes.add("role");
        }
        return exceptionTypes.toArray(new String[0]);
    }

    public String[] getExceptions() {
        Vector<String> exceptions = new Vector<String>();
        for (int i = 0; i < userExceptionList.getItemCount(); i++) {
            exceptions.add(userExceptionList.getValue(i));
        }
        for (int i = 0; i < groupExceptionList.getItemCount(); i++) {
            exceptions.add(groupExceptionList.getValue(i));
        }
        for (int i = 0; i < roleExceptionList.getItemCount(); i++) {
            exceptions.add(roleExceptionList.getValue(i));
        }
        return exceptions.toArray(new String[0]);
    }

    public void addUser(String username) {
        userExceptionList.addItem(username);
        users.push(username);
    }

    public void addGroup(String group) {
        groupExceptionList.addItem(group);
    }

    public void deleteGroup(String group) {
        for (int i = 0; i < groupExceptionList.getItemCount(); i++) {
            if (groupExceptionList.getValue(i).equals(group)) {
                groupExceptionList.removeItem(i);
            }
        }
        for (int i = 0; i < groupList.getItemCount(); i++) {
            if (groupList.getValue(i).equals(group)) {
                groupList.removeItem(i);
            }
        }
    }

}
