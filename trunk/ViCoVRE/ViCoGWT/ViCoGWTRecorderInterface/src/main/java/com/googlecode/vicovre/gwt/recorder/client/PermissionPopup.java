/**
 * Copyright (c) 2009, University of Manchester
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1) Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2) Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3) Neither the name of the and the University of Manchester nor the names of
 *    its contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 */

package com.googlecode.vicovre.gwt.recorder.client;

import java.util.Vector;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.googlecode.vicovre.gwt.client.ModalPopup;
import com.googlecode.vicovre.gwt.client.json.JSONACL;
import com.googlecode.vicovre.gwt.client.json.JSONACLEntity;
import com.googlecode.vicovre.gwt.client.json.JSONUser;
import com.googlecode.vicovre.gwt.recorder.client.rest.PermissionSetter;

public class PermissionPopup extends ModalPopup<VerticalPanel>
        implements ClickHandler, ChangeHandler {

    private Button ok = new Button("OK");

    private Button cancel = new Button("Cancel");

    private ListBox allowRequestList = new ListBox(false);

    private ListBox allowList = new ListBox(false);

    private ListBox userList = new ListBox(true);

    private ListBox userExceptionList = new ListBox(true);

    private ListBox groupList = new ListBox(true);

    private ListBox groupExceptionList = new ListBox(true);

    private ListBox roleList = new ListBox(true);

    private ListBox roleExceptionList = new ListBox(true);

    private Button addUserException = new Button("<-- Add");

    private Button removeUserException = new Button("Remove -->");

    private Button addGroupException = new Button("<-- Add");

    private Button removeGroupException = new Button("Remove -->");

    private Button addRoleException = new Button("<-- Add");

    private Button removeRoleException = new Button("Remove -->");

    private Button clearUserExceptions = new Button("Clear");

    private Button clearGroupExceptions = new Button("Clear");

    private Button clearRoleExceptions = new Button("Clear");

    private String url = null;

    private String folder = null;

    private String recordingId = null;

    public PermissionPopup(String url, String folder,
            String recordingId, JsArrayString users, JsArrayString groups,
            JSONACL acl, JSONACL readAcl) {
        super(new VerticalPanel());
        this.url = url;
        this.folder = folder;
        this.recordingId = recordingId;

        VerticalPanel panel = getWidget();
        panel.setWidth("400px");
        panel.setHeight("320px");

        allowList.addChangeHandler(this);
        allowList.addItem("Allow everybody", "true");
        allowList.addItem("Allow nobody", "false");
        HorizontalPanel allowPanel = new HorizontalPanel();
        allowPanel.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
        allowPanel.add(allowList);
        allowPanel.add(new Label(" to play the recording except:"));
        panel.add(allowPanel);

        for (int i = 0; i < users.length(); i++) {
            userList.addItem(users.get(i));
        }
        panel.add(new HTML("<B>Users:</B>"));
        panel.add(createExceptionPanel(userList, userExceptionList,
                addUserException, removeUserException, clearUserExceptions));

        for (int i = 0; i < groups.length(); i++) {
            groupList.addItem(groups.get(i));
        }
        panel.add(new HTML("<B>Groups:</B>"));
        panel.add(createExceptionPanel(groupList, groupExceptionList,
                addGroupException, removeGroupException, clearGroupExceptions));

        roleList.addItem("Administrator", JSONUser.ROLE_ADMINISTRATOR);
        roleList.addItem("Writer", JSONUser.ROLE_WRITER);
        roleList.addItem("User", JSONUser.ROLE_USER);
        roleList.addItem("Guest", JSONUser.ROLE_GUEST);
        panel.add(new HTML("<B>Roles:</B>"));
        panel.add(createExceptionPanel(roleList, roleExceptionList,
                addRoleException, removeRoleException, clearRoleExceptions));

        allowRequestList.setEnabled(false);
        allowRequestList.addItem("Allow everybody", "true");
        allowRequestList.addItem("Allow nobody", "false");
        HorizontalPanel allowRequestPanel = new HorizontalPanel();
        allowRequestPanel.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
        allowRequestPanel.add(allowRequestList);
        allowRequestPanel.add(new Label(" to request access to the recording"));
        panel.add(allowRequestPanel);

        HorizontalPanel buttonPanel = new HorizontalPanel();
        buttonPanel.setWidth("100%");
        buttonPanel.add(ok);
        buttonPanel.add(cancel);
        buttonPanel.setCellHorizontalAlignment(ok, HorizontalPanel.ALIGN_LEFT);
        buttonPanel.setCellHorizontalAlignment(cancel,
                HorizontalPanel.ALIGN_RIGHT);
        panel.add(buttonPanel);

        if (acl.isAllow()) {
            allowList.setSelectedIndex(0);
        } else {
            allowList.setSelectedIndex(1);
            allowRequestList.setEnabled(true);
        }
        JsArray<JSONACLEntity> exceptions = acl.getExceptions();
        for (int i = 0; i < exceptions.length(); i++) {
            JSONACLEntity exception = exceptions.get(i);
            String type = exception.getType();
            if (type.equals("user")) {
                moveItem(userList, userExceptionList, exception.getName());
            } else if (type.equals("group")) {
                moveItem(groupList, groupExceptionList, exception.getName());
            } else if (type.equals("role")) {
                moveItem(roleList, roleExceptionList, exception.getName());
            }
        }
        if (readAcl.isAllow()) {
            allowRequestList.setSelectedIndex(0);
        } else {
            allowRequestList.setSelectedIndex(1);
        }

        ok.addClickHandler(this);
        cancel.addClickHandler(this);
    }

    private HorizontalPanel createExceptionPanel(ListBox list,
            ListBox exceptionList, Button addButton, Button removeButton,
            Button clearButton) {
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
        buttonPanel.add(clearButton);
        panel.add(exceptionList);
        panel.add(buttonPanel);
        panel.add(list);
        panel.setCellWidth(exceptionList, "150px");
        panel.setCellWidth(buttonPanel, "100px");
        panel.setCellWidth(list, "150px");

        addButton.addClickHandler(this);
        removeButton.addClickHandler(this);
        clearButton.addClickHandler(this);
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
        if (event.getSource() == cancel) {
            hide();
        } else if (event.getSource() == ok) {
            Vector<String> exceptionTypes = new Vector<String>();
            Vector<String> exceptions = new Vector<String>();
            for (int i = 0; i < userExceptionList.getItemCount(); i++) {
                exceptionTypes.add("user");
                exceptions.add(userExceptionList.getValue(i));
            }
            for (int i = 0; i < groupExceptionList.getItemCount(); i++) {
                exceptionTypes.add("group");
                exceptions.add(groupExceptionList.getValue(i));
            }
            for (int i = 0; i < roleExceptionList.getItemCount(); i++) {
                exceptionTypes.add("role");
                exceptions.add(roleExceptionList.getValue(i));
            }


            boolean allowRequest = allowList.getValue(
                    allowRequestList.getSelectedIndex()).equals("true");
            String[] requestExceptionTypes = null;
            String[] requestExceptions = null;
            if (!allowRequest) {
                requestExceptionTypes = exceptionTypes.toArray(new String[0]);
                requestExceptions = exceptions.toArray(new String[0]);
            }

            boolean allow = allowList.getValue(
                    allowList.getSelectedIndex()).equals("true");

            PermissionSetter.setPermissions(url, folder, recordingId,
                    new String[]{"play", "read"}, this,
                    new boolean[]{allow, allowRequest},
                    new String[][]{exceptionTypes.toArray(new String[0]),
                        requestExceptionTypes},
                    new String[][]{exceptions.toArray(new String[0]),
                        requestExceptions});
        } else if (event.getSource() == addUserException) {
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
        }
    }

    public void onChange(ChangeEvent event) {
        if (event.getSource() == allowList) {
            if (allowList.getValue(allowList.getSelectedIndex()).equals(
                    "true")) {
                allowRequestList.setEnabled(false);
                allowRequestList.setSelectedIndex(0);
            } else {
                allowRequestList.setEnabled(true);
            }
        }
    }

}
