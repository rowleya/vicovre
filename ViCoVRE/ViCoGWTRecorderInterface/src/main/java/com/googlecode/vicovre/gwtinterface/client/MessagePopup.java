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

package com.googlecode.vicovre.gwtinterface.client;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

public class MessagePopup extends ModalPopup<HorizontalPanel>
        implements ClickHandler {

    public static final String QUESTION = "images/question.png";

    public static final String ERROR = "images/error.png";

    public static final String WARNING = "images/warning.png";

    public static final String INFO = "images/info.png";

    private static final String[] BUTTON_NAMES = new String[]{"Yes", "No",
        "Cancel", "OK"};

    private MessageResponseHandler responseHandler = null;

    private Label messageLabel = new Label();

    public MessagePopup(String message,
            MessageResponseHandler responseHandler,
            String messageType, int... buttons) {
        super(new HorizontalPanel());
        this.responseHandler = responseHandler;
        messageLabel.setText(message);

        HorizontalPanel buttonPanel = new HorizontalPanel();
        buttonPanel.setHorizontalAlignment(HorizontalPanel.ALIGN_CENTER);
        buttonPanel.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
        for (int i = 0; i < buttons.length; i++) {
            Button button = new Button(BUTTON_NAMES[buttons[i]]);
            button.addClickHandler(this);
            buttonPanel.add(button);
            if ((i + 1) < buttons.length) {
                buttonPanel.add(new Label(" "));
            }
        }

        HorizontalPanel panel = getWidget();
        panel.setVerticalAlignment(VerticalPanel.ALIGN_MIDDLE);
        if (messageType != null) {
            Image image = new Image(messageType);
            image.setHeight("60px");
            image.setWidth("60px");
            panel.add(image);
            panel.add(new Label(" "));
        }


        VerticalPanel messagePanel = new VerticalPanel();
        messagePanel.setHorizontalAlignment(VerticalPanel.ALIGN_CENTER);
        messagePanel.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
        messagePanel.add(messageLabel);
        messagePanel.add(new Label(" "));
        messagePanel.add(buttonPanel);

        panel.add(messagePanel);
    }

    public void setMessage(String message) {
        messageLabel.setText(message);
    }

    public void onClick(ClickEvent event) {
        hide();
        if (responseHandler != null) {
            String source = ((Button) event.getSource()).getText();
            for (int i = 0; i < BUTTON_NAMES.length; i++) {
                if (source.equals(BUTTON_NAMES[i])) {
                    responseHandler.handleResponse(
                            new MessageResponse(i, this));
                }
            }
        }
    }

}
