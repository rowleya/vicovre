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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.fredhat.gwt.xmlrpc.client.XmlRpcClient;
import com.fredhat.gwt.xmlrpc.client.XmlRpcException;
import com.fredhat.gwt.xmlrpc.client.XmlRpcRequest;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.googlecode.vicovre.gwt.client.MessagePopup;
import com.googlecode.vicovre.gwt.client.MessageResponse;
import com.googlecode.vicovre.gwt.client.Venue;
import com.googlecode.vicovre.gwt.client.VenuePanel;
import com.googlecode.vicovre.gwt.client.WaitPopup;
import com.googlecode.vicovre.gwt.recorder.client.Application;

public class VenueLoader implements AsyncCallback<Map<String, Object>> {

    private static final HashMap<String, Venue[]> VENUES =
        new HashMap<String, Venue[]>();

    private static final LinkedList<VenuePanel> TO_LOAD =
        new LinkedList<VenuePanel>();


    private static final WaitPopup LOAD_VENUES_POPUP =
        new WaitPopup("Loading Venues...",  true);

    private static final MessagePopup ERROR_POPUP = new MessagePopup(
            "", null, MessagePopup.ERROR, MessageResponse.OK);

    private VenuePanel panel = null;

    public static void loadVenues(VenuePanel panel) {
        if (!TO_LOAD.isEmpty()) {
            TO_LOAD.addLast(panel);
        } else {
            TO_LOAD.addLast(panel);
            loadNextVenue();
        }
    }

    private static void loadNextVenue() {
        if (TO_LOAD.isEmpty()) {
            LOAD_VENUES_POPUP.hide();
            return;
        }
        LOAD_VENUES_POPUP.center();
        VenuePanel toLoad = TO_LOAD.getFirst();
        String serverToLoad = toLoad.getVenueServer();
        if (!VENUES.containsKey(serverToLoad)) {
            XmlRpcClient xmlRpcClient = Application.getXmlRpcClient();
            XmlRpcRequest<Map<String, Object>> request =
                new XmlRpcRequest<Map<String, Object>>(
                    xmlRpcClient, "venue.getVenues", new Object[]{serverToLoad},
                    new VenueLoader(toLoad));
            request.execute();
        } else {
            toLoad.setVenues(serverToLoad, VENUES.get(serverToLoad));
            TO_LOAD.removeFirst();
            loadNextVenue();
        }
    }

    private VenueLoader(VenuePanel panel) {
        this.panel = panel;
    }

    public void onFailure(Throwable error) {
        GWT.log("Error loading venues", error);
        TO_LOAD.clear();
        LOAD_VENUES_POPUP.hide();
        ERROR_POPUP.setMessage("Error loading venues:" + error.getMessage());
        ERROR_POPUP.center();
    }

    public void onSuccess(Map<String, Object> result) {
        List<Object> venueList = (List<Object>) result.get("venues");
        Venue[] venues = new Venue[venueList.size()];
        for (int i = 0; i < venues.length; i++) {
            if (venueList.get(i) instanceof Map) {
                Map<String, String> venue = (Map) venueList.get(i);
                String name = venue.get("name");
                String uri = venue.get("uri");
                venues[i] = new Venue(name, uri);
            } else {
                onFailure(new XmlRpcException(
                        "Result type incorrect"));
            }
        }
        String server = (String) result.get("server");
        VENUES.put(server, venues);
        panel.setVenues(server, venues);
        TO_LOAD.removeFirst();
        loadNextVenue();
    }

}
