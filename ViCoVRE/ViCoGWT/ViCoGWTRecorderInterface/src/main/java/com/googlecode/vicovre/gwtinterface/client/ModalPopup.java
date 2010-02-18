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

import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;

public class ModalPopup<T extends Widget> extends PopupPanel
        implements CloseHandler<PopupPanel> {

    private PopupPanel popup = new PopupPanel(false);

    private T widget = null;

    public ModalPopup(T widget) {
        super(false, true);
        this.widget = widget;

        DOM.setStyleAttribute(getElement(), "width", "100%");
        DOM.setStyleAttribute(getElement(), "height", "100%");
        DOM.setStyleAttribute(getElement(), "backgroundColor", "#000");
        DOM.setStyleAttribute(getElement(), "opacity", "0.70");
        DOM.setStyleAttribute(getElement(), "filter",  " alpha(opacity=70)");

        popup.add(widget);
    }

    public PopupPanel getPopup() {
        return popup;
    }

    public T getWidget() {
        return widget;
    }

    public void center() {
        show();
    }

    public void show() {
        super.show();
        popup.center();
    }

    public void hide() {
        popup.hide();
        super.hide();
    }

    public void onClose(CloseEvent<PopupPanel> event) {
        super.hide();
    }
}
