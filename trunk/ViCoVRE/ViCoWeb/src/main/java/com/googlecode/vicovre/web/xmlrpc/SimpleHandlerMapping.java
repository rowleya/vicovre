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

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.server.AbstractReflectiveHandlerMapping;

public class SimpleHandlerMapping extends AbstractReflectiveHandlerMapping {

    public SimpleHandlerMapping(Map<String, Object> services)
            throws XmlRpcException {

        // Setup a RequestProcessorFactoryFactory
        SimpleRequestProcessorFactoryFactory factory =
            new SimpleRequestProcessorFactoryFactory();

        // Register all service beans with the factory
        for (Object serviceBean : services.values()) {
            factory.registerServiceBean(serviceBean.getClass(), serviceBean);
        }

        // Set the RequestFactoryFactory to be used for mapping
        setRequestProcessorFactoryFactory(factory);

        // Loop through the set
        Iterator<Entry<String, Object>> it = services.entrySet().iterator();
        while (it.hasNext()) {

            // Fetch from the map
            Entry<String, Object> entry = it.next();
            String serviceName = entry.getKey();
            Object serviceBean = entry.getValue();

            // Register service in the handler mapping
            registerPublicMethods(serviceName, serviceBean.getClass());
        }
    }
}
