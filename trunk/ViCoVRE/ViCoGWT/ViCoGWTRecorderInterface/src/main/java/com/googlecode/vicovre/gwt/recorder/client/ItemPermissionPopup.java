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

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.googlecode.vicovre.gwt.client.json.JSONACL;
import com.googlecode.vicovre.gwt.recorder.client.rest.ItemPermissionSetter;
import com.googlecode.vicovre.gwt.utils.client.ModalPopup;

public class ItemPermissionPopup extends ModalPopup<VerticalPanel>
        implements ClickHandler, ChangeHandler {

    private Button ok = new Button("OK");

    private Button cancel = new Button("Cancel");

    private ListBox allowRequestList = new ListBox(false);

    private ListBox allowList = new ListBox(false);

    private PermissionExceptionPanel exceptionPanel = null;

    private String url = null;

    private String folder = null;

    private String id = null;

    private String type = null;

    public ItemPermissionPopup(String url, String folder, String type,
            String id, JsArrayString users, JsArrayString groups,
            JSONACL acl, JSONACL readAcl) {
        super(new VerticalPanel());
        this.url = url;
        this.folder = folder;
        this.type = type;
        this.id = id;

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

        exceptionPanel = new PermissionExceptionPanel(users, groups, acl);
        panel.add(exceptionPanel);

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

        if (readAcl.isAllow()) {
            allowRequestList.setSelectedIndex(0);
        } else {
            allowRequestList.setSelectedIndex(1);
        }

        ok.addClickHandler(this);
        cancel.addClickHandler(this);
    }



    public void onClick(ClickEvent event) {
        if (event.getSource() == cancel) {
            hide();
        } else if (event.getSource() == ok) {
            String[] exceptionTypes = exceptionPanel.getExceptionTypes();
            String[] exceptions = exceptionPanel.getExceptions();

            boolean allowRequest = allowRequestList.getValue(
                    allowRequestList.getSelectedIndex()).equals("true");
            boolean allow = allowList.getValue(
                    allowList.getSelectedIndex()).equals("true");

            ItemPermissionSetter.setPermissions(url, folder, type, id,
                    new String[]{"play", "read"}, this,
                    new boolean[]{allow, allowRequest},
                    new String[][]{exceptionTypes, null},
                    new String[][]{exceptions, null});
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
