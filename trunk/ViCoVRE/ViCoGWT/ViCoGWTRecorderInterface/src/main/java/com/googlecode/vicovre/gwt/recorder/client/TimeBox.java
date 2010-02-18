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

import java.util.Date;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;

/**
 * A box that will allow the user to select a time.
 * @author Andrew G D Rowley
 * @version 1.0
 */
public class TimeBox extends HorizontalPanel {

    private ListBox hour = new ListBox();

    private ListBox minute = new ListBox();

    private ListBox second = new ListBox();

    private Label secondSeparator = new Label(":");

    private int minutePrecision = 1;

    private int secondPrecision = 1;

    public TimeBox(int minutePrecision, int secondPrecision) {
        this.minutePrecision = minutePrecision;
        this.secondPrecision = secondPrecision;
        for (int i = 0; i < 23; i++) {
            hour.addItem(String.valueOf(i));
        }
        for (int i = 0; i < 60; i += minutePrecision) {
            String value = String.valueOf(i);
            if (i < 10) {
                value = "0" + value;
            }
            minute.addItem(value);
        }
        for (int i = 0; i < 60; i += secondPrecision) {
            String value = String.valueOf(i);
            if (i < 10) {
                value = "0" + value;
            }
            second.addItem(value);
        }
        add(hour);
        add(new Label(":"));
        add(minute);
        add(secondSeparator);
        add(second);
    }

    public void setValue(long value) {
        long remainder = value / 1000;
        long hours = remainder / 3600;
        remainder -= (hours * 3600);
        long minutes = remainder / 60;
        remainder -= (minutes * 60);
        long seconds = remainder;

        hour.setSelectedIndex((int) hours);
        minute.setSelectedIndex((int) (minutes / minutePrecision));
        second.setSelectedIndex((int) (seconds / secondPrecision));
    }

    public void setHour(int hour) {
        this.hour.setSelectedIndex(hour);
    }

    public void setMinute(int minute) {
        this.minute.setSelectedIndex(minute / minutePrecision);
    }

    public void setSecond(int second) {
        this.second.setSelectedIndex(second / secondPrecision);
    }

    public void setSecondsVisible(boolean secondsVisible) {
        second.setVisible(secondsVisible);
        secondSeparator.setVisible(secondsVisible);
        if (!secondsVisible) {
            second.setSelectedIndex(0);
        }
    }

    public void setEnabled(boolean enabled) {
        hour.setEnabled(enabled);
        minute.setEnabled(enabled);
        second.setEnabled(enabled);
    }

    public long getValue() {
        long value = hour.getSelectedIndex() * 3600;
        value += minute.getSelectedIndex() * minutePrecision * 60;
        value += second.getSelectedIndex() * secondPrecision;
        return value * 1000;
    }

    public int getHour() {
        return hour.getSelectedIndex();
    }

    public int getMinute() {
        return minute.getSelectedIndex() * minutePrecision;
    }

    public int getSecond() {
        return second.getSelectedIndex() * secondPrecision;
    }
}
