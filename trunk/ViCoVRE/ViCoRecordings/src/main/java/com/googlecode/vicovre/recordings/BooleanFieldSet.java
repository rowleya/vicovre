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

package com.googlecode.vicovre.recordings;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Vector;

/**
 * Represents a set of fields and values joined by a boolean
 * @author Andrew G D Rowley
 * @version 1.0
 */
public class BooleanFieldSet {

    public static final String OR_OPERATION = "OR";

    public static final String AND_OPERATION = "AND";

    public static final String NOT_OPERATION = "NOT";

    private Vector<String> fields = new Vector<String>();

    private Vector<String> values = new Vector<String>();

    private String operation = null;

    private Vector<BooleanFieldSet> sets = new Vector<BooleanFieldSet>();

    public BooleanFieldSet(String operation) {
        this.operation = operation;
    }

    public void addField(String field, String value) {
        fields.add(field);
        values.add(value);
    }

    public void addSet(BooleanFieldSet set) {
        sets.add(set);
    }

    private boolean testValue(Object object, String field, String value) {
    	System.err.println("        Testing if " + field + " = " + value);
        String methodName = "get" + field.substring(0, 1).toUpperCase()
            + field.substring(1);
        try {
            Method method = object.getClass().getMethod(methodName);
            String objectValue = method.invoke(object).toString();
            if (value == null) {
                return objectValue == null;
            }
            return value.equals(objectValue);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean test(Object object) {
        if (operation.equalsIgnoreCase(NOT_OPERATION)) {
            if (!fields.isEmpty()) {
                String field = fields.get(0);
                String value = values.get(0);
                return !testValue(object, field, value);
            } else if (!sets.isEmpty()) {
                return !sets.get(0).test(object);
            }
            throw new RuntimeException("Empty NOT set");
        } else if (operation.equalsIgnoreCase(AND_OPERATION)) {
            for (int i = 0; i < fields.size(); i++) {
                String field = fields.get(i);
                String value = values.get(i);
                if (!testValue(object, field, value)) {
                    return false;
                }
            }
            for (BooleanFieldSet set : sets) {
                if (!set.test(object)) {
                    return false;
                }
            }
            return true;
        } else if (operation.equalsIgnoreCase(OR_OPERATION)) {
            for (int i = 0; i < fields.size(); i++) {
                String field = fields.get(i);
                String value = values.get(i);
                if (testValue(object, field, value)) {
                    return true;
                }
            }
            for (BooleanFieldSet set : sets) {
                if (set.test(object)) {
                    return true;
                }
            }
            return false;
        }
        throw new RuntimeException("Unknown operation " + operation);
    }

    public String getOperation() {
        return operation;
    }

    public List<String> getFields() {
        return fields;
    }

    public String getValue(String field) {
        return values.get(fields.indexOf(field));
    }

    public List<BooleanFieldSet> getSets() {
        return sets;
    }
}
