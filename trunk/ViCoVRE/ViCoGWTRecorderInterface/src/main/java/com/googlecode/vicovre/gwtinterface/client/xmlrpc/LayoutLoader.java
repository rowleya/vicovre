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

package com.googlecode.vicovre.gwtinterface.client.xmlrpc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.fredhat.gwt.xmlrpc.client.XmlRpcClient;
import com.fredhat.gwt.xmlrpc.client.XmlRpcRequest;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.googlecode.vicovre.gwtinterface.client.Application;
import com.googlecode.vicovre.gwtinterface.client.Layout;
import com.googlecode.vicovre.gwtinterface.client.LayoutPosition;

public class LayoutLoader implements AsyncCallback<List<Object>> {

    private Application application = null;

    public static void loadLayouts(Application application) {
        new LayoutLoader(application);
    }

    private LayoutLoader(Application application) {
        this.application = application;
        XmlRpcClient client = Application.getXmlRpcClient();
        XmlRpcRequest<List<Object>> request = new XmlRpcRequest<List<Object>>(
                client, "layout.getLayouts", new Object[]{}, this);
        request.execute();
    }

    public void onFailure(Throwable error) {
        Application.showErrorLoading();
    }

    public void onSuccess(List<Object> layouts) {
        for (Object layoutObject : layouts) {
            Map<String, Object> layoutMap = (Map<String, Object>) layoutObject;
            String name = (String) layoutMap.get("name");
            List<Map<String, Object>> positionList =
                (List<Map<String, Object>>) layoutMap.get("positions");
            List<LayoutPosition> positions = new Vector<LayoutPosition>();
            for (Map<String, Object> positionMap : positionList) {
                String posName = (String) positionMap.get("name");
                Integer x = (Integer) positionMap.get("x");
                Integer y = (Integer) positionMap.get("y");
                Integer width = (Integer) positionMap.get("width");
                Integer height = (Integer) positionMap.get("height");
                Boolean assignable = (Boolean) positionMap.get("assignable");
                LayoutPosition position = new LayoutPosition(posName, x, y,
                        width, height, assignable);
                positions.add(position);
            }
            Layout layout = new Layout(name, positions);
            application.addLayout(layout);
        }
        application.finishLoadingLayouts();
    }


}
