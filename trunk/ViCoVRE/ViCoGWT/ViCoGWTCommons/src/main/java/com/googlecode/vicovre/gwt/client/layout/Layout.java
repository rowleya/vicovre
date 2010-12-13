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

package com.googlecode.vicovre.gwt.client.layout;

import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import com.google.gwt.core.client.JsArray;
import com.googlecode.vicovre.gwt.client.json.JSONLayout;
import com.googlecode.vicovre.gwt.client.json.JSONLayoutPosition;

public class Layout {

    private String name = null;

    private HashMap<String, LayoutPosition> positions =
        new HashMap<String, LayoutPosition>();

    private int maxX = 0;

    private int maxY = 0;

    private int minX = 0;

    private int minY = 0;

    public Layout(JSONLayout layout) {
        List<LayoutPosition> positions = new Vector<LayoutPosition>();
        JsArray<JSONLayoutPosition> jsonPositions = layout.getPositions();
        for (int i = 0; i < jsonPositions.length(); i++) {
            LayoutPosition position = new LayoutPosition(jsonPositions.get(i));
            positions.add(position);
        }
        init(layout.getName(), positions);
    }

    public Layout(String name, List<LayoutPosition> positions) {
        init(name, positions);
    }

    private void init(String name, List<LayoutPosition> positions) {
        this.name = name;

        maxX = 0;
        maxY = 0;
        minX = Integer.MAX_VALUE;
        minY = Integer.MAX_VALUE;

        for (LayoutPosition position : positions) {
            this.positions.put(position.getName(), position);

            maxX = Math.max(maxX, position.getX() + position.getWidth());
            maxY = Math.max(maxY, position.getY() + position.getHeight());
            minX = Math.min(minX, position.getX());
            minY = Math.min(minY, position.getY());
        }
    }

    public String getName() {
        return name;
    }

    public LayoutPosition getPosition(String name) {
        return positions.get(name);
    }

    public List<LayoutPosition> getPositions() {
        return new Vector<LayoutPosition>(positions.values());
    }

    public int getMinX() {
        return minX;
    }

    public int getMinY() {
        return minY;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMaxY() {
        return maxY;
    }

    public int getWidth() {
        return maxX + minX;
    }

    public int getHeight() {
        return maxY + minY;
    }

    public void setName(String name) {
        this.name = name;
    }

}
