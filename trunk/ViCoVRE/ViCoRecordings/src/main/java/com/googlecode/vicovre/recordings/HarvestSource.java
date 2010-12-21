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

import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.googlecode.onevre.ag.types.network.NetworkLocation;
import com.googlecode.vicovre.repositories.harvestFormat.HarvestFormat;
import com.googlecode.vicovre.repositories.harvestFormat.HarvestFormatReader;
import com.googlecode.vicovre.repositories.harvestFormat.HarvestedItem;


/**
 * The source to harvest from
 * @author Andrew G D Rowley
 * @version 1.0
 */
@XmlRootElement(name="harvestSource")
@XmlAccessorType(XmlAccessType.NONE)
public class HarvestSource {

    /**
     * Updates the source manually
     */
    public static final String UPDATE_MANUALLY = "Manual";

    /**
     * Updates the source annually
     */
    public static final String UPDATE_ANUALLY = "Annual";

    /**
     * Updates the source monthly
     */
    public static final String UPDATE_MONTHLY = "Monthly";

    /**
     * Updates the source weekly
     */
    public static final String UPDATE_WEEKLY = "Weekly";

    private static final String DAY_VARIABLE = "$day";

    private static final String MONTH_VARIABLE = "$month";

    private static final String YEAR_VARIABLE = "$year";

    private String folder = null;

    private String id = null;

    private String name = null;

    private String url = null;

    private HarvestFormat format = null;

    private String updateFrequency = null;

    private String ag3VenueServer = null;

    private String ag3VenueUrl = null;

    private NetworkLocation[] addresses = null;

    private int month = 0;

    private int dayOfMonth = 0;

    private int dayOfWeek = 0;

    private int hour = 0;

    private int minute = 0;

    private String status = "OK";

    public HarvestSource() {
        // Does Nothing
    }

    public HarvestSource(String folder, String id) {
        this.folder = folder;
        this.id = id;
    }

    @XmlElement
    public String getId() {
        return id;
    }

    @XmlElement
    public String getFolder() {
        return folder;
    }

    /**
     * Returns the name
     * @return the name
     */
    @XmlElement
    public String getName() {
        return name;
    }

    /**
     * Sets the name
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the url
     * @return the url
     */
    @XmlElement
    public String getUrl() {
        return url;
    }

    /**
     * Sets the url
     * @param url the url to set
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Returns the format
     * @return the format
     */
    public HarvestFormat getFormat() {
        return format;
    }

    @XmlElement(name="format")
    public String getFormatName() {
        return format.getName();
    }

    /**
     * Sets the format
     * @param format the format to set
     */
    public void setFormat(HarvestFormat format) {
        this.format = format;
    }

    /**
     * Returns the updateFrequency
     * @return the updateFrequency
     */
    @XmlElement
    public String getUpdateFrequency() {
        return updateFrequency;
    }

    /**
     * Sets the updateFrequency
     * @param updateFrequency the updateFrequency to set
     */
    public void setUpdateFrequency(String updateFrequency) {
        this.updateFrequency = updateFrequency;
    }

    /**
     * Returns the month
     * @return the month
     */
    @XmlElement
    public int getMonth() {
        return month;
    }

    /**
     * Sets the month
     * @param month the month to set
     */
    public void setMonth(int month) {
        this.month = month;
    }

    /**
     * Returns the dayOfMonth
     * @return the dayOfMonth
     */
    @XmlElement
    public int getDayOfMonth() {
        return dayOfMonth;
    }

    /**
     * Sets the dayOfMonth
     * @param dayOfMonth the dayOfMonth to set
     */
    public void setDayOfMonth(int dayOfMonth) {
        this.dayOfMonth = dayOfMonth;
    }

    /**
     * Returns the dayOfWeek
     * @return the dayOfWeek
     */
    @XmlElement
    public int getDayOfWeek() {
        return dayOfWeek;
    }

    /**
     * Sets the dayOfWeek
     * @param dayOfWeek the dayOfWeek to set
     */
    public void setDayOfWeek(int dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    @XmlElement
    public int getHour() {
        return hour;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }

    @XmlElement
    public int getMinute() {
        return minute;
    }

    public void setMinute(int minute) {
        this.minute = minute;
    }

    /**
     * Gets the status
     * @return The status
     */
    @XmlElement
    public String getStatus() {
        return status;
    }

    /**
     * Returns the ag3VenueServer
     * @return the ag3VenueServer
     */
    @XmlElement
    public String getAg3VenueServer() {
        return ag3VenueServer;
    }

    /**
     * Sets the ag3VenueServer
     * @param ag3VenueServer the ag3VenueServer to set
     */
    public void setAg3VenueServer(String ag3VenueServer) {
        this.ag3VenueServer = ag3VenueServer;
    }

    /**
     * Returns the ag3VenueUrl
     * @return the ag3VenueUrl
     */
    @XmlElement
    public String getAg3VenueUrl() {
        return ag3VenueUrl;
    }

    /**
     * Sets the ag3VenueUrl
     * @param ag3VenueUrl the ag3VenueUrl to set
     */
    public void setAg3VenueUrl(String ag3VenueUrl) {
        this.ag3VenueUrl = ag3VenueUrl;
    }

    /**
     * Returns the addresses
     * @return the addresses
     */
    @XmlElement(name="address")
    @XmlJavaTypeAdapter(NetworkLocationAdapter.class)
    public NetworkLocation[] getAddresses() {
        return addresses;
    }

    /**
     * Sets the addresses
     * @param addresses the addresses to set
     */
    public void setAddresses(NetworkLocation[] addresses) {
        this.addresses = addresses;
    }

    /**
     * Harvests the source
     */
    public List<HarvestedEvent> harvest() throws HarvestException {
        List<HarvestedEvent> events = new Vector<HarvestedEvent>();
        String url = this.url;
        Date now = new Date();
        SimpleDateFormat day = new SimpleDateFormat("dd");
        SimpleDateFormat month = new SimpleDateFormat("MM");
        SimpleDateFormat year = new SimpleDateFormat("yyyy");
        url.replaceAll(DAY_VARIABLE, day.format(now));
        url.replaceAll(MONTH_VARIABLE, month.format(now));
        url.replaceAll(YEAR_VARIABLE, year.format(now));

        try {
            Class<?> formatClass = Class.forName(format.getDecodeClass());
            if (HarvestFormatReader.class.isAssignableFrom(formatClass)) {
                HarvestFormatReader reader =
                    (HarvestFormatReader) formatClass.newInstance();
                URL u = new URL(url);
                URLConnection connection = u.openConnection();
                HarvestedItem[] items =
                    reader.readItems(connection.getInputStream());
                for (HarvestedItem item : items) {
                    if (item instanceof HarvestedEvent) {
                        events.add((HarvestedEvent) item);
                    }
                }
            } else {
                throw new Exception("Format reader class " + formatClass
                        + " is not a HarvestFormatReader");
            }
            status = "OK";
            return events;

        } catch (Exception e) {
            status = "Failed: " + e.getMessage();
            throw new HarvestException(e);
        }
    }
}
