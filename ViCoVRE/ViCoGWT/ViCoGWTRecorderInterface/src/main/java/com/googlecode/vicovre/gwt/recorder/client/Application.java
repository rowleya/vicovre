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

import com.fredhat.gwt.xmlrpc.client.XmlRpcClient;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.RootPanel;
import com.googlecode.vicovre.gwt.recorder.client.rest.FolderLoader;
import com.googlecode.vicovre.gwt.recorder.client.rest.VenueServerLoader;

public class Application implements EntryPoint {

    public static final String XMLRPC_SERVER = "xmlrpcUrl";

    private static XmlRpcClient xmlrpcClient = null;

    private static Dictionary parameters = null;

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

    public static String getParam(String name) {
        return parameters.get(name);
    }

    public static XmlRpcClient getXmlRpcClient() {
        return xmlrpcClient;
    }

    public void onModuleLoad() {
        parameters = Dictionary.getDictionary("Parameters");
        String xmlrpcServer = getParam(XMLRPC_SERVER);
        if (xmlrpcServer.startsWith("/")) {
            xmlrpcServer = xmlrpcServer.substring(1);
        }
        String url = GWT.getHostPageBaseURL() + xmlrpcServer;
        xmlrpcClient = new XmlRpcClient(url);
        xmlrpcClient.setTimeoutMillis(120000);

        DockPanel topPanel = new DockPanel();
        topPanel.setWidth("100%");
        topPanel.setHeight("100%");
        topPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        topPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);

        StatusPanel status = new StatusPanel();
        status.setWidth("95%");

        String restUrl = getUrl();
        FolderPanel panel = new FolderPanel(restUrl);

        topPanel.add(status, DockPanel.NORTH);
        topPanel.add(panel, DockPanel.CENTER);

        topPanel.setCellHeight(status, "50px");
        RootPanel.get().add(topPanel);

        ActionLoader loader = new ActionLoader(null, 2,
                "Loading Application...",
                "There was an error loading the application.\n"
                + "Please refresh the page to try again.",
                false, true);
        VenueServerLoader.load(loader, restUrl);
        FolderLoader.load(panel, loader, restUrl);
    }
}
