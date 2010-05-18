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

package com.googlecode.vicovre.gwt.recorder.client.xmlrpc;

import java.util.List;

import com.fredhat.gwt.xmlrpc.client.XmlRpcClient;
import com.fredhat.gwt.xmlrpc.client.XmlRpcRequest;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.googlecode.vicovre.gwt.client.VenuePanel;
import com.googlecode.vicovre.gwt.recorder.client.ActionLoader;
import com.googlecode.vicovre.gwt.recorder.client.Application;

public class VenueServerLoader implements AsyncCallback<List<Object>> {

    private ActionLoader loader = null;

    public static void loadVenues(ActionLoader loader) {
        XmlRpcClient xmlrpcClient = Application.getXmlRpcClient();

        // Get the known venue servers
        XmlRpcRequest<List<Object>> request = new XmlRpcRequest<List<Object>>(
                xmlrpcClient, "venue.getVenueServers", new Object[0],
                new VenueServerLoader(loader));
        request.execute();
    }

    private VenueServerLoader(ActionLoader loader) {
        this.loader = loader;
    }

    public void onFailure(Throwable error) {
        GWT.log("Error loading venue servers", error);
        loader.itemFailed("Error loading venue servers");
    }

    public void onSuccess(List<Object> venueServers) {
        for (Object v : venueServers) {
            if (v instanceof String) {
                VenuePanel.addVenueServer((String) v);
                GWT.log("Added server " + v, null);
            } else {
                onFailure(new Throwable("Item not a string"));
                return;
            }
        }
        loader.itemLoaded();
    }
}
