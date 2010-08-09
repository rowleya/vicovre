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

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import ag3.interfaces.types.NetworkLocation;

import com.googlecode.vicovre.media.protocol.memetic.RecordingConstants;
import com.googlecode.vicovre.recordings.db.Folder;
import com.googlecode.vicovre.recordings.db.RecordingDatabase;
import com.googlecode.vicovre.repositories.harvestFormat.HarvestFormat;
import com.googlecode.vicovre.repositories.harvestFormat.HarvestFormatReader;
import com.googlecode.vicovre.repositories.harvestFormat.HarvestedItem;
import com.googlecode.vicovre.repositories.rtptype.RtpTypeRepository;
import com.googlecode.vicovre.utils.Emailer;


/**
 * The source to harvest from
 * @author Andrew G D Rowley
 * @version 1.0
 */
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

    private File file = null;

    private Folder folder = null;

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

    private RecordingDatabase database = null;

    private RtpTypeRepository typeRepostory = null;

    private Timer timer = null;

    private Timer retryTimer = null;

    private String subFolderMetadataItem = null;

    private Emailer emailer = null;

    private class HarvestTask extends TimerTask {

        /**
         *
         * @see java.util.TimerTask#run()
         */
        public void run() {
            harvest(false);
        }

    }

    public HarvestSource(Folder folder, File file,
            RtpTypeRepository typeRepository, Emailer emailer) {
        this.file = file;
        this.folder = folder;
        this.typeRepostory = typeRepository;
        this.emailer = emailer;
    }

    public String getId() {
        return file.getName().substring(0, file.getName().indexOf(
                RecordingConstants.HARVEST_SOURCE));
    }

    public Folder getFolder() {
        return folder;
    }

    public File getFile() {
        return file;
    }

    /**
     * Returns the name
     * @return the name
     */
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

    public int getHour() {
        return hour;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }

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
    public String getStatus() {
        return status;
    }

    /**
     * Returns the ag3VenueServer
     * @return the ag3VenueServer
     */
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
     * Returns the subFolderMetadataItem
     * @return the subFolderMetadataItem
     */
    public String getSubFolderMetadataItem() {
        return subFolderMetadataItem;
    }

    /**
     * Sets the subFolderMetadataItem
     * @param subFolderMetadataItem the subFolderMetadataItem to set
     */
    public void setSubFolderMetadataItem(String subFolderMetadataItem) {
        this.subFolderMetadataItem = subFolderMetadataItem;
    }

    /**
     * Schedules the updating of the source
     * @param database The database to put new recordings into
     */
    public synchronized void scheduleTimer(RecordingDatabase database,
            RtpTypeRepository typeRepository) {
        stopTimer();
        this.database = database;
        if (!updateFrequency.equals(UPDATE_MANUALLY)) {
            if (updateFrequency.equals(UPDATE_ANUALLY)) {
                Calendar now = Calendar.getInstance();
                Calendar first = Calendar.getInstance();
                first.set(Calendar.HOUR_OF_DAY, hour);
                first.set(Calendar.MINUTE, minute);
                first.set(Calendar.SECOND, 0);
                first.set(Calendar.MILLISECOND, 0);
                first.set(Calendar.MONTH, month);
                first.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                if (first.before(now) || first.equals(now)) {
                    first.add(Calendar.YEAR, 1);
                }
                timer = new Timer();
                timer.schedule(new HarvestTask(), first.getTime());
                System.err.println("Harvest of " + url + " scheduled for "
                        + first.getTime());
            } else if (updateFrequency.equals(UPDATE_MONTHLY)) {
                Calendar now = Calendar.getInstance();
                Calendar first = Calendar.getInstance();
                first.set(Calendar.HOUR_OF_DAY, hour);
                first.set(Calendar.MINUTE, minute);
                first.set(Calendar.SECOND, 0);
                first.set(Calendar.MILLISECOND, 0);
                first.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                if (first.before(now) || first.equals(now)) {
                    first.add(Calendar.MONTH, 1);
                }
                timer = new Timer();
                timer.schedule(new HarvestTask(), first.getTime());
                System.err.println("Harvest of " + url + " scheduled for "
                        + first.getTime());
            } else if (updateFrequency.equals(UPDATE_WEEKLY)) {
                Calendar now = Calendar.getInstance();
                Calendar first = Calendar.getInstance();
                first.set(Calendar.HOUR_OF_DAY, hour);
                first.set(Calendar.MINUTE, minute);
                first.set(Calendar.SECOND, 0);
                first.set(Calendar.MILLISECOND, 0);
                first.set(Calendar.DAY_OF_WEEK, dayOfWeek);
                if (first.before(now) || first.equals(now)) {
                    first.add(Calendar.DAY_OF_MONTH, 7);
                }
                timer = new Timer();
                timer.schedule(new HarvestTask(), first.getTime());
                System.err.println("Harvest of " + url + " scheduled for "
                        + first.getTime());
            }
        }
    }

    public void setDatabase(RecordingDatabase database) {
        this.database = database;
    }

    /**
     * Stops the updating of the source
     */
    public synchronized void stopTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        if (retryTimer != null) {
            retryTimer.cancel();
            retryTimer = null;
        }
    }

    private String getMetadataItem(HarvestedEvent event) {
        RecordingMetadata metadata = event.getMetadata();
        if (metadata != null) {
            String methodName = "get"
                + subFolderMetadataItem.substring(0, 1).toUpperCase()
                + subFolderMetadataItem.substring(1);
            try {
                Method getMethod = metadata.getClass().getMethod(methodName);
                Object result = getMethod.invoke(metadata);
                return result.toString();
            } catch (Exception e) {
                // Do Nothing
            }
        }
        return null;
    }

    /**
     * Harvests the source
     */
    public void harvest(boolean manual) {
        scheduleTimer(database, typeRepostory);
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
                        HarvestedEvent event = (HarvestedEvent) item;
                        Folder eventFolder = folder;
                        if (subFolderMetadataItem != null) {
                            String subFolder = getMetadataItem(event);
                            if (subFolder != null) {
                                File folderFile = new File(folder.getFile(),
                                        subFolder);
                                folderFile.mkdirs();
                                Folder tmpFolder = database.getFolder(
                                        folderFile);
                                if (tmpFolder != null) {
                                    eventFolder = tmpFolder;
                                }
                            }
                        }
                        File file = File.createTempFile("recording",
                                RecordingConstants.UNFINISHED_RECORDING_INDEX,
                                eventFolder.getFile());
                        UnfinishedRecording recording = new UnfinishedRecording(
                                typeRepostory, eventFolder, file, database,
                                emailer);
                        recording.setMetadata(event.getMetadata());
                        recording.setStartDate(event.getStartDate());
                        recording.setStopDate(event.getEndDate());
                        recording.setAddresses(addresses);
                        recording.setAg3VenueServer(ag3VenueServer);
                        recording.setAg3VenueUrl(ag3VenueUrl);
                        database.addUnfinishedRecording(recording, this);
                    }
                }
            } else {
                throw new Exception("Format reader class " + formatClass
                        + " is not a HarvestFormatReader");
            }
            status = "OK";

        } catch (Exception e) {
            e.printStackTrace();
            status = "Failed: " + e.getMessage();
            if (!manual) {
                Calendar first = Calendar.getInstance();
                first.add(Calendar.MINUTE, 5);
                retryTimer = new Timer();
                retryTimer.schedule(new HarvestTask(), first.getTimeInMillis());
                System.err.println(
                        "Harvest failed, will retry in five minutes");
            }
        }
    }
}
