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

package com.googlecode.vicovre.gwtinterface.client;

import java.util.Date;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.datepicker.client.DateBox;

public class DateFormat implements DateBox.Format {

    /**
     *
     * @see com.google.gwt.user.datepicker.client.DateBox.DefaultFormat#format(
     *     com.google.gwt.user.datepicker.client.DateBox, java.util.Date)
     */
    public String format(DateBox dateBox, Date date) {
        if (date != null) {
            DateTimeFormat format = new DefaultDateTimeFormat();
            return format.format(date);
        }
        return "";
    }

    /**
     *
     * @see com.google.gwt.user.datepicker.client.DateBox.DefaultFormat#parse(
     *     com.google.gwt.user.datepicker.client.DateBox, java.lang.String,
     *     boolean)
     */
    public Date parse(DateBox dateBox, String date, boolean reportError) {

        try {
            return new DefaultDateTimeFormat().parse(date);
        } catch (Throwable t) {
            if (reportError) {
                dateBox.addStyleName("dateBoxFormatError");
            }
            return null;
        }
    }

    /**
     *
     * @see com.google.gwt.user.datepicker.client.DateBox.DefaultFormat#reset(
     *     com.google.gwt.user.datepicker.client.DateBox, boolean)
     */
    public void reset(DateBox dateBox, boolean abandon) {
        dateBox.removeStyleName("dateBoxFormatError");
    }
}
