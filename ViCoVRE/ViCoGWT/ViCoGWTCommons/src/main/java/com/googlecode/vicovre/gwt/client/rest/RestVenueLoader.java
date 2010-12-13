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

package com.googlecode.vicovre.gwt.client.rest;

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;
import com.googlecode.vicovre.gwt.client.venue.Venue;
import com.googlecode.vicovre.gwt.client.venue.VenuePanel;
import com.googlecode.vicovre.gwt.utils.client.WaitPopup;

public class RestVenueLoader extends AbstractJSONRestCall {

    private static final WaitPopup LOAD_VENUES_POPUP =
        new WaitPopup("Loading Venues...",  true);

    private VenuePanel panel = null;

    private String url = null;

    public static void loadVenues(VenuePanel panel, String url) {
        RestVenueLoader loader = new RestVenueLoader(panel, url);
        loader.go();
    }

    public RestVenueLoader(VenuePanel panel, String url) {
        super(true);
        this.panel = panel;
        this.url = url + "venue/venues?url=" + panel.getVenueServer();
    }

    public void go() {
        LOAD_VENUES_POPUP.center();
        go(url);
    }

    protected void onError(String message) {
        LOAD_VENUES_POPUP.hide();
        displayError("Error loading venues: " + message);
    }

    protected void onSuccess(JSONObject object) {
        JSONValue venueValue = object.get("venues");
        JSONValue serverValue = object.get("venueServerUrl");
        if ((venueValue != null) && (serverValue != null)) {
            JSONString server = serverValue.isString();
            JSONArray venues = venueValue.isArray();
            if ((venues != null) && (server != null)) {
                Venue[] vs = new Venue[venues.size()];
                for (int i = 0; i < venues.size(); i++) {
                    JSONObject venue = venues.get(i).isObject();
                    if (venue != null) {
                        String name =
                            venue.get("name").isString().stringValue();
                        String url =
                            venue.get("uri").isString().stringValue();
                        vs[i] = new Venue(name, url);
                    }
                }
                panel.setVenues(server.stringValue(), vs);
                LOAD_VENUES_POPUP.hide();
            } else {
                onError("Wrong types in response");
                return;
            }
        } else {
            onError("Missing venues array or server in response");
            return;
        }
    }
}
