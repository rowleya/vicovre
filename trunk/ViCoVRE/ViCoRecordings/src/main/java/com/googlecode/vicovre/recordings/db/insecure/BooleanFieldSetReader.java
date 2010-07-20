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

package com.googlecode.vicovre.recordings.db.insecure;

import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.googlecode.vicovre.recordings.BooleanFieldSet;
import com.googlecode.vicovre.utils.XmlIo;

public class BooleanFieldSetReader {

    public static BooleanFieldSet readFieldSet(Node node)
            throws SAXException {
        Node operation = XmlIo.readNode(node, "operation");
        String op = XmlIo.readAttr(operation, "type", null);
        if (op == null) {
            throw new SAXException("Operation type missing");
        }
        BooleanFieldSet fieldSet = new BooleanFieldSet(op);

        Node[] fields = XmlIo.readNodes(operation, "field");
        for (Node field : fields) {
            String name = XmlIo.readAttr(field, "name", null);
            String value = XmlIo.readAttr(field, "value", null);
            fieldSet.addField(name, value);
        }

        Node[] operations = XmlIo.readNodes(operation, "operation");
        for (Node opNode : operations) {
            fieldSet.addSet(readFieldSet(opNode));
        }

        return fieldSet;
    }
}
