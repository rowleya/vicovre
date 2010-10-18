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
import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.Vector;

import com.googlecode.vicovre.recordings.db.RecordingDatabase;
import com.googlecode.vicovre.recordings.db.insecure.HarvestSourceListener;

public class Harvester implements HarvestSourceListener {

    private RecordingDatabase database = null;

    private HashMap<HarvestSource, Timer> timers =
        new HashMap<HarvestSource, Timer>();

    private HashMap<HarvestSource, Timer> retryTimers =
        new HashMap<HarvestSource, Timer>();

    private class HarvestTask extends TimerTask {

        private HarvestSource harvestSource = null;

        public HarvestTask(HarvestSource harvestSource) {
            this.harvestSource = harvestSource;
        }

        /**
         *
         * @see java.util.TimerTask#run()
         */
        public void run() {
            harvest(harvestSource, false);
        }

    }

    public Harvester(RecordingDatabase database) {
        this.database = database;
        database.addHarvestSourceListener(this);
    }

    public List<UnfinishedRecording> harvest(HarvestSource harvestSource) {
        return harvest(harvestSource, true);
    }

    private List<UnfinishedRecording> harvest(HarvestSource harvestSource,
            boolean manual) {
        if (!manual) {
            schedule(harvestSource);
        }
        try {
            List<HarvestedEvent> events = harvestSource.harvest();
            List<UnfinishedRecording> recordings =
                new Vector<UnfinishedRecording>();
            for (HarvestedEvent event : events) {
                String eventFolder = harvestSource.getFolder();
                if (event.getSubFolder() != null) {
                    String subFolder = event.getSubFolder();
                    eventFolder += "/" + subFolder;
                    File folderFile = database.getFile(eventFolder);
                    folderFile.mkdirs();
                }
                UnfinishedRecording recording = new UnfinishedRecording(
                        eventFolder, UUID.randomUUID().toString());
                recording.setMetadata(event.getMetadata());
                recording.setStartDate(event.getStartDate());
                recording.setStopDate(event.getEndDate());
                recording.setAddresses(harvestSource.getAddresses());
                recording.setAg3VenueServer(harvestSource.getAg3VenueServer());
                recording.setAg3VenueUrl(harvestSource.getAg3VenueUrl());
                try {
                    database.addUnfinishedRecording(recording, harvestSource);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                recordings.add(recording);
            }
            return recordings;
        } catch (Exception e) {
            e.printStackTrace();
            if (!manual) {
                Calendar first = Calendar.getInstance();
                first.add(Calendar.MINUTE, 5);
                Timer retryTimer = new Timer();
                retryTimer.schedule(new HarvestTask(harvestSource),
                        first.getTimeInMillis());
                System.err.println(
                        "Harvest failed, will retry in five minutes");
                retryTimers.put(harvestSource, retryTimer);
            }
            return null;
        }
    }

    public void schedule(HarvestSource harvestSource) {
        stopTimer(harvestSource);
        String updateFrequency = harvestSource.getUpdateFrequency();
        if (!updateFrequency.equals(HarvestSource.UPDATE_MANUALLY)) {
            if (updateFrequency.equals(HarvestSource.UPDATE_ANUALLY)) {
                Calendar now = Calendar.getInstance();
                Calendar first = Calendar.getInstance();
                first.set(Calendar.HOUR_OF_DAY, harvestSource.getHour());
                first.set(Calendar.MINUTE, harvestSource.getMinute());
                first.set(Calendar.SECOND, 0);
                first.set(Calendar.MILLISECOND, 0);
                first.set(Calendar.MONTH, harvestSource.getMonth());
                first.set(Calendar.DAY_OF_MONTH, harvestSource.getDayOfMonth());
                if (first.before(now) || first.equals(now)) {
                    first.add(Calendar.YEAR, 1);
                }
                Timer timer = new Timer();
                timer.schedule(new HarvestTask(harvestSource), first.getTime());
                System.err.println("Harvest of " + harvestSource.getUrl()
                        + " scheduled for " + first.getTime());
                timers.put(harvestSource, timer);
            } else if (updateFrequency.equals(HarvestSource.UPDATE_MONTHLY)) {
                Calendar now = Calendar.getInstance();
                Calendar first = Calendar.getInstance();
                first.set(Calendar.HOUR_OF_DAY, harvestSource.getHour());
                first.set(Calendar.MINUTE, harvestSource.getMinute());
                first.set(Calendar.SECOND, 0);
                first.set(Calendar.MILLISECOND, 0);
                first.set(Calendar.DAY_OF_MONTH, harvestSource.getDayOfMonth());
                if (first.before(now) || first.equals(now)) {
                    first.add(Calendar.MONTH, 1);
                }
                Timer timer = new Timer();
                timer.schedule(new HarvestTask(harvestSource), first.getTime());
                System.err.println("Harvest of " + harvestSource.getUrl()
                        + " scheduled for " + first.getTime());
                timers.put(harvestSource, timer);
            } else if (updateFrequency.equals(HarvestSource.UPDATE_WEEKLY)) {
                Calendar now = Calendar.getInstance();
                Calendar first = Calendar.getInstance();
                first.set(Calendar.HOUR_OF_DAY, harvestSource.getHour());
                first.set(Calendar.MINUTE, harvestSource.getMinute());
                first.set(Calendar.SECOND, 0);
                first.set(Calendar.MILLISECOND, 0);
                first.set(Calendar.DAY_OF_WEEK, harvestSource.getDayOfWeek());
                if (first.before(now) || first.equals(now)) {
                    first.add(Calendar.DAY_OF_MONTH, 7);
                }
                Timer timer = new Timer();
                timer.schedule(new HarvestTask(harvestSource), first.getTime());
                System.err.println("Harvest of " + harvestSource.getUrl()
                        + " scheduled for " + first.getTime());
                timers.put(harvestSource, timer);
            }
        }
    }


    /**
     * Stops the updating of the source
     */
    public synchronized void stopTimer(HarvestSource harvestSource) {
        Timer timer = timers.get(harvestSource);
        if (timer != null) {
            timer.cancel();
            timers.remove(harvestSource);
        }
        Timer retryTimer = retryTimers.get(harvestSource);
        if (retryTimer != null) {
            retryTimer.cancel();
            retryTimers.remove(harvestSource);
        }
    }



    public void shutdown() {
        Vector<HarvestSource> harvestSources = new Vector<HarvestSource>(
                timers.keySet());
        for (HarvestSource harvestSource : harvestSources) {
            stopTimer(harvestSource);
        }
    }

    public void sourceAdded(HarvestSource harvestSource) {
        schedule(harvestSource);
    }

    public void sourceDeleted(HarvestSource harvestSource) {
        stopTimer(harvestSource);
    }

    public void sourceUpdated(HarvestSource harvestSource) {
        schedule(harvestSource);
    }
}
