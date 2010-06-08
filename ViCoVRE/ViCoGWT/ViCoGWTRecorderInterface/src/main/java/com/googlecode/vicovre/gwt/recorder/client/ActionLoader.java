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

import com.google.gwt.core.client.GWT;
import com.googlecode.vicovre.gwt.client.MessagePopup;
import com.googlecode.vicovre.gwt.client.MessageResponse;
import com.googlecode.vicovre.gwt.client.ModalPopup;
import com.googlecode.vicovre.gwt.client.WaitPopup;

public class ActionLoader {

    private ModalPopup<?> popupToLaunch = null;

    private int itemsToLoad = 0;

    private WaitPopup loading = null;

    private boolean fatal = false;

    private String errorMessage = null;

    public ActionLoader(ModalPopup<?> popupToLaunch, int itemsToLoad,
            String message, String errorMessage, boolean cancellable,
            boolean fatal) {
        this.popupToLaunch = popupToLaunch;
        this.itemsToLoad = itemsToLoad;
        this.errorMessage = errorMessage;
        this.fatal = fatal;
        loading = new WaitPopup(message, cancellable);
        loading.center();
    }

    public void itemLoaded() {
        if (loading.wasCancelled()) {
            return;
        }
        itemsToLoad -= 1;
        if (itemsToLoad == 0) {
            loading.hide();
            if (popupToLaunch != null) {
                popupToLaunch.center();
            }
        }
    }

    public void itemFailed(String error) {
        loading.hide();
        String errorMsg = errorMessage;
        if (errorMsg == null) {
            errorMsg = error;
        }
        if (fatal) {
            MessagePopup errorPopup = new MessagePopup(errorMsg, null,
                    MessagePopup.ERROR);
            errorPopup.center();
        } else {
            MessagePopup errorPopup = new MessagePopup(errorMsg, null,
                    MessagePopup.ERROR, MessageResponse.OK);
            errorPopup.center();
        }
    }

}