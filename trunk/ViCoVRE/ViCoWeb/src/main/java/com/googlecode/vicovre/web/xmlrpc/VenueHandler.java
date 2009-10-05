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

package com.googlecode.vicovre.web.xmlrpc;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.xmlrpc.XmlRpcException;

import ag3.interfaces.VenueServer;
import ag3.interfaces.types.ConnectionDescription;

import com.googlecode.vicovre.recordings.db.RecordingDatabase;

public class VenueHandler extends AbstractHandler {

    private static final long VENUE_REFRESH_TIMEOUT = 3600000;

    private HashMap<String, Map<String, String>[]> knownVenues =
        new HashMap<String, Map<String, String>[]>();

    private HashMap<String, Long> knownVenuesCacheTime =
        new HashMap<String, Long>();

    public VenueHandler(RecordingDatabase database) {
        super(database);
    }

    public String[] getVenueServers() {
        return getDatabase().getKnownVenueServers();
    }

    public String getVenueServer(String server) throws MalformedURLException {
        URL url = null;
        try {
            url = new URL(server);
        } catch (MalformedURLException e) {
            url = new URL("https://" + server);
        }
        int port = url.getPort();
        if (port == -1) {
            port = 8000;
        }
        String protocol = url.getProtocol();
        if (protocol == null || protocol.equals("")) {
            protocol = "https";
        }
        final String venueServerUrl = protocol + "://"
            + url.getHost() + ":" + port + "/VenueServer";
        return venueServerUrl;
    }

    public Map<String, Object> getVenues(String server)
            throws XmlRpcException {
        try {
            Map<String, Object> details = new HashMap<String, Object>();
            String venueServerUrl = getVenueServer(server);
            Map<String, String>[] venues = knownVenues.get(venueServerUrl);
            long lastCacheTime = 0;
            if (venues != null) {
                lastCacheTime = knownVenuesCacheTime.get(venueServerUrl);
            }
            if ((venues == null) ||
                    ((System.currentTimeMillis() - lastCacheTime)
                            > VENUE_REFRESH_TIMEOUT)) {
                VenueServer venueServer = new VenueServer(venueServerUrl);
                ConnectionDescription[] connections = venueServer.getVenues();
                Arrays.sort(connections);
                venues = new Map[connections.length];
                for (int i = 0; i < venues.length; i++) {
                    venues[i] = new HashMap<String, String>();
                    venues[i].put("name", connections[i].getName());
                    venues[i].put("uri", connections[i].getUri());
                }
                knownVenues.put(venueServerUrl, venues);
                knownVenuesCacheTime.put(venueServerUrl,
                        System.currentTimeMillis());
                getDatabase().addVenueServer(venueServerUrl);
            }
            details.put("venues", venues);
            details.put("server", venueServerUrl);
            return details;
        } catch (Throwable e) {
            e.printStackTrace();
            throw new XmlRpcException(e.getClass().getName() + ":"
                    + e.getMessage());
        }
    }
}
