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

package com.googlecode.vicovre.gwt.recorder.client.rest.json;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.googlecode.vicovre.gwt.client.json.JSONMetadata;
import com.googlecode.vicovre.gwt.utils.client.StringDateTimeFormat;

public class JSONUnfinishedRecording extends JavaScriptObject {

    public static final StringDateTimeFormat DATE_FORMAT =
        new StringDateTimeFormat("yyyy-MM-dd'T'HH:mm:ss");

    protected JSONUnfinishedRecording() {
        // Does Nothing
    }

    public static final native JSONUnfinishedRecording parse(String json) /*-{
        return eval('(' + json + ')');
    }-*/;

    public final native String getId() /*-{
        return this.id;
    }-*/;

    public final native String getFolder() /*-{
        return this.folder;
    }-*/;

    public final native JSONMetadata getMetadata() /*-{
        return this.metadata;
    }-*/;

    public final native String getStartDate() /*-{
        return this.startDate;
    }-*/;

    public final native String getStopDate() /*-{
        return this.stopDate;
    }-*/;

    public final native String getAg3VenueServer() /*-{
        return this.ag3VenueServer;
    }-*/;

    public final native String getAg3VenueUrl() /*-{
        return this.ag3VenueUrl;
    }-*/;

    public final native JsArray<JSONNetworkLocation> getAddresses() /*-{
        return this.address;
    }-*/;

    public final native String getStatus() /*-{
        return this.status;
    }-*/;

    public final native String getEmailAddress() /*-{
        return this.emailAddress;
    }-*/;

    public final native String getRepeatFrequency() /*-{
        return this.repeatFrequency;
    }-*/;

    public final native int getRepeatStartHour() /*-{
        return this.repeatStartHour;
    }-*/;

    public final native int getRepeatStartMinute() /*-{
        return this.repeatStartMinute;
    }-*/;

    public final native int getRepeatDurationMinutes() /*-{
        return this.repeatDurationMinutes;
    }-*/;

    public final native int getRepeatItemFrequency() /*-{
        return this.repeatItemFrequency;
    }-*/;

    public final native int getRepeatDayOfWeek() /*-{
        return this.repeatDayOfWeek;
    }-*/;

    public final native int getRepeatDayOfMonth() /*-{
        return this.repeatStartHour;
    }-*/;

    public final native int getRepeatWeekNumber() /*-{
        return this.repeatWeekNumber;
    }-*/;

    public final native int getRepeatMonth() /*-{
        return this.repeatMonth;
    }-*/;

    public final native boolean getIgnoreWeekends() /*-{
        return this.ignoreWeekends;
    }-*/;
}
