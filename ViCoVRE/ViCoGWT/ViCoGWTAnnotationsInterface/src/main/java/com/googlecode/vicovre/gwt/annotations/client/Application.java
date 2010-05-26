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

package com.googlecode.vicovre.gwt.annotations.client;

import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.VerticalPanel;

public class Application implements EntryPoint, ClickHandler,
        CloseHandler<Window>, SelectionHandler<TreeItem> {

    private Dictionary parameters = Dictionary.getDictionary("Parameters");

    private Tree annotationPanel = new Tree();

    private VerticalPanel userPanel = new VerticalPanel();

    private HashMap<String, User> users = new HashMap<String, User>();

    private HashMap<String, User> deletedUsers = new HashMap<String, User>();

    private HashMap<String, Annotation> annotations =
        new HashMap<String, Annotation>();

    private Vector<LiveAnnotationType> inlineButtons =
        new Vector<LiveAnnotationType>();

    private HashMap<PushButton, LiveAnnotationType> buttons =
        new HashMap<PushButton, LiveAnnotationType>();

    private HashMap<String, LiveAnnotationType> liveAnnotationTypes =
        new HashMap<String, LiveAnnotationType>();

    private MessageReceiver receiver = new MessageReceiver(this);

    private LoginPopup login = new LoginPopup(this);

    private HorizontalPanel buttonPanel = new HorizontalPanel();

    private HorizontalPanel bottomPanel = new HorizontalPanel();

    private ScrollPanel annotationScroll = new ScrollPanel(annotationPanel);

    private String author = null;

    private HandlerRegistration closeHandler = null;

    protected String getUrl() {
        String url = GWT.getHostPageBaseURL();
        String paramUrl = parameters.get("url");
        if (paramUrl.startsWith("/")) {
            paramUrl = paramUrl.substring(1);
        }
        if (!paramUrl.endsWith("/")) {
            paramUrl += "/";
        }
        return url + paramUrl;
    }

    protected String getAuthor() {
        return author;
    }

    public void onModuleLoad() {
        LiveAnnotationTypeReceiver.getLiveAnnotations(this);
    }

    public void loginDone(String name, String email) {
        author = email;
        receiver.start();
        closeHandler = Window.addCloseHandler(this);
    }

    public void setupInterface(List<LiveAnnotationType> laTypes) {
        VerticalPanel mainPanel = new VerticalPanel();
        mainPanel.setWidth("100%");
        mainPanel.setHeight("100%");

        HorizontalPanel topPanel = new HorizontalPanel();

        ScrollPanel userScroll = new ScrollPanel(userPanel);
        DOM.setStyleAttribute(annotationScroll.getElement(),
                "borderWidth", "1px");
        DOM.setStyleAttribute(annotationScroll.getElement(),
                "borderStyle", "solid");
        DOM.setStyleAttribute(userScroll.getElement(),
                "borderWidth", "1px");
        DOM.setStyleAttribute(userScroll.getElement(),
                "borderStyle", "solid");
        topPanel.add(annotationScroll);
        topPanel.add(userScroll);
        annotationScroll.setWidth("100%");
        annotationScroll.setHeight("100%");
        userScroll.setWidth("200px");
        userScroll.setHeight("100%");
        topPanel.setWidth("100%");
        topPanel.setHeight("100%");
        topPanel.setCellWidth(userScroll, "200px");

        buttonPanel.setHorizontalAlignment(HorizontalPanel.ALIGN_CENTER);
        buttonPanel.setWidth("100%");
        buttonPanel.setHeight("100%");
        for (LiveAnnotationType type : laTypes) {
            liveAnnotationTypes.put(type.getName(), type);
            if (type.getVisible().equals("true")) {
                String image = type.getImage();
                if (image.startsWith("/")) {
                    image = image.substring(1);
                }
                PushButton button = new PushButton(new Image(getUrl()
                        + image));
                button.setTitle(type.getName());
                button.setWidth("100px");
                button.setHeight("100px");
                buttonPanel.add(button);
                buttons.put(button, type);
                button.addClickHandler(this);
            } else {
                inlineButtons.add(type);
            }
        }

        bottomPanel.setWidth("100%");
        bottomPanel.setHeight("100px");
        bottomPanel.add(buttonPanel);

        mainPanel.add(topPanel);
        mainPanel.add(bottomPanel);
        mainPanel.setCellHeight(bottomPanel, "100px");
        RootPanel.get().add(mainPanel);

        HTML userHeading = new HTML("<b>Users</b><hr/>");
        userHeading.setWidth("100%");
        userPanel.setWidth("100%");
        userPanel.add(userHeading);

        annotationPanel.addSelectionHandler(this);

        login.center();
    }

    public void addUser(User user) {
        if (!users.containsKey(user.getEmail())) {
            User deleted = deletedUsers.get(user.getEmail());
            if (deleted != null) {
                deleted.setName(user.getName());
                users.put(deleted.getEmail(), deleted);
                deletedUsers.remove(user);
                userPanel.add(deleted.getLabel());
            } else {
                users.put(user.getEmail(), user);
                userPanel.add(user.getLabel());
            }
        }
    }

    public void removeUser(String email) {
        User user = users.remove(email);
        if (user != null) {
            deletedUsers.put(user.getEmail(), user);
            userPanel.remove(user.getLabel());
        }
    }

    public User getUser(String email) {
        return users.get(email);
    }

    public LiveAnnotationType getType(String name) {
        return liveAnnotationTypes.get(name);
    }

    public List<LiveAnnotationType> getInlineButtonTypes() {
        return inlineButtons;
    }

    public void addAnnotation(Annotation annotation) {
        String id = annotation.getId();
        Annotation currentAnnotation = annotations.get(id);
        if (currentAnnotation == null) {
            String relatesTo =
                annotation.getBodyItem("CrewLiveAnnotationRelatesTo");
            if (relatesTo == null) {
                annotationPanel.addItem(annotation.getItem());
                annotationScroll.setScrollPosition(
                        annotationPanel.getOffsetHeight());
            } else {
                Annotation relatesToAnnotation = annotations.get(relatesTo);
                relatesToAnnotation.getItem().addItem(annotation.getItem());
                relatesToAnnotation.getItem().setState(true);
            }
        } else {
            TreeItem currentItem = currentAnnotation.getItem();
            annotation.setItem(currentItem);
        }
        annotations.put(id, annotation);
    }

    public void close() {
        if (closeHandler != null) {
            closeHandler.removeHandler();
        }
        CloseSender.close(this);
        for (String email : users.keySet()) {
            User user = users.get(email);
            userPanel.remove(user.getLabel());
        }
        users.clear();
        annotations.clear();
        annotationPanel.clear();
        login.center();
    }

    public void onClick(ClickEvent event) {
        LiveAnnotationType laType = buttons.get(event.getSource());
        if (laType != null) {
            TimeReceiver.getTime(this, laType);
        }
    }

    public void clearPanel() {
        bottomPanel.clear();
    }

    public void displayButtonPanel() {
        bottomPanel.clear();
        bottomPanel.add(buttonPanel);
    }

    public void displayAnnotationPanel(LiveAnnotationType laType) {
        bottomPanel.remove(buttonPanel);
        bottomPanel.add(laType.getPanel());
    }

    public void onClose(CloseEvent<Window> event) {
        if (closeHandler != null) {
            closeHandler.removeHandler();
        }
        CloseSender.close(this);
    }

    public void onSelection(SelectionEvent<TreeItem> event) {
        event.getSelectedItem().setSelected(false);
    }
}
