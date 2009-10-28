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

package com.googlecode.vicovre.recordings.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.xml.sax.SAXException;

import com.googlecode.vicovre.media.protocol.memetic.RecordingConstants;
import com.googlecode.vicovre.recordings.Folder;
import com.googlecode.vicovre.recordings.HarvestSource;
import com.googlecode.vicovre.recordings.PlaybackManager;
import com.googlecode.vicovre.recordings.Recording;
import com.googlecode.vicovre.recordings.ReplayLayout;
import com.googlecode.vicovre.recordings.UnfinishedRecording;
import com.googlecode.vicovre.repositories.harvestFormat.HarvestFormatRepository;
import com.googlecode.vicovre.repositories.layout.LayoutRepository;
import com.googlecode.vicovre.repositories.rtptype.RtpTypeRepository;

/**
 * A database for recordings
 *
 * @author Andrew G D Rowley
 * @version 1.0
 */
public class RecordingDatabase {

    private static final String VENUE_SERVER_FILE = "venueServers.xml";

    private Vector<String> knownVenueServers = new Vector<String>();

    private Folder topLevelFolder = null;

    private HashMap<File, Folder> folders =
        new HashMap<File, Folder>();

    private RtpTypeRepository typeRepository = null;

    /**
     * Creates a Database
     * @param directory The directory containing the database
     * @param typeRepository The type repository
     * @param layoutRepository The layout repository
     * @throws SAXException
     * @throws IOException
     */
    public RecordingDatabase(String directory, RtpTypeRepository typeRepository,
            LayoutRepository layoutRepository,
            HarvestFormatRepository harvestFormatRepository)
            throws SAXException, IOException {
        this.typeRepository = typeRepository;
        File topLevel = new File(directory);
        topLevel.mkdirs();
        topLevelFolder = FolderReader.readFolder(topLevel,
                typeRepository, layoutRepository, harvestFormatRepository,
                this);
        traverseFolders(topLevelFolder);

        File venueServerFile = new File(topLevel, VENUE_SERVER_FILE);
        if (venueServerFile.exists()) {
            FileInputStream input = new FileInputStream(venueServerFile);
            String[] venueServers = VenueServerReader.readVenueServers(input);
            for (String server : venueServers) {
                knownVenueServers.add(server);
            }
            input.close();
        }
    }

    private void traverseFolders(Folder folder) {
        folders.put(folder.getFile(), folder);
        List<HarvestSource> harvestSources = folder.getHarvestSources();
        for (HarvestSource harvestSource : harvestSources) {
            harvestSource.scheduleTimer(this, typeRepository);
        }
        List<UnfinishedRecording> unfinishedRecordings =
            folder.getUnfinishedRecordings();
        for (UnfinishedRecording recording : unfinishedRecordings) {
            recording.updateTimers();
        }
        for (Folder subFolder : folder.getFolders()) {
            traverseFolders(subFolder);
        }
    }

    public String[] getKnownVenueServers() {
        return knownVenueServers.toArray(new String[0]);
    }

    public void addVenueServer(String url) {
        synchronized (knownVenueServers) {
            try {
                if (!knownVenueServers.contains(url)) {
                    knownVenueServers.add(url);
                    File venueServerFile = new File(topLevelFolder.getFile(),
                            VENUE_SERVER_FILE);
                    FileOutputStream output = new FileOutputStream(
                            venueServerFile);
                    VenueServerReader.writeVenueServers(
                            knownVenueServers.toArray(new String[0]), output);
                    output.close();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Gets the top level folder
     * @return The folders
     */
    public Folder getTopLevelFolder() {
        return topLevelFolder;
    }

    public void addHarvestSource(HarvestSource harvestSource)
            throws IOException {
        Folder folder = harvestSource.getFolder();
        folder.addHarvestSource(harvestSource);
        if (harvestSource.getFile() == null) {
            File file = File.createTempFile("harvest",
                    RecordingConstants.HARVEST_SOURCE, folder.getFile());
            harvestSource.setFile(file);
        }
        File file = harvestSource.getFile();
        FileOutputStream output = new FileOutputStream(file);
        HarvestSourceReader.writeHarvestSource(harvestSource, output);
        output.close();
        harvestSource.scheduleTimer(this, typeRepository);
    }

    public void deleteHarvestSource(HarvestSource harvestSource) {
        Folder folder = harvestSource.getFolder();
        folder.deleteHarvestSource(harvestSource);
        File file = harvestSource.getFile();
        file.delete();
    }

    public void updateHarvestSource(HarvestSource harvestSource)
            throws IOException {
        addHarvestSource(harvestSource);
    }

    public void addUnfinishedRecording(UnfinishedRecording recording)
            throws IOException {
        Folder folder = recording.getFolder();
        folder.addUnfinishedRecording(recording);
        if (recording.getFile() == null) {
            File file = File.createTempFile("recording",
                    RecordingConstants.UNFINISHED_RECORDING_INDEX,
                    folder.getFile());
            recording.setFile(file);
        }
        File file = recording.getFile();
        FileOutputStream output = new FileOutputStream(file);
        UnfinishedRecordingReader.writeRecording(recording, output);
        output.close();
        recording.updateTimers();
    }

    public void deleteUnfinishedRecording(UnfinishedRecording recording) {
        recording.stopRecording();
        Folder folder = recording.getFolder();
        folder.deleteUnfinishedRecording(recording);
        File file = recording.getFile();
        String prefix = file.getName();
        prefix = prefix.substring(0, prefix.indexOf(
                        RecordingConstants.UNFINISHED_RECORDING_INDEX));
        File metadata = new File(folder.getFile(),
                prefix + RecordingConstants.METADATA);
        file.delete();
        metadata.delete();
    }

    public void updateUnfinishedRecording(UnfinishedRecording recording)
            throws IOException {
        addUnfinishedRecording(recording);
    }

    public void addRecording(Recording recording)
            throws IOException {
        recording.getDirectory().mkdirs();
        File parentFile = recording.getDirectory().getParentFile();
        Folder parent = folders.get(parentFile);
        parent.addRecording(recording);
        FileOutputStream output = new FileOutputStream(
                new File(recording.getDirectory(),
                        RecordingConstants.RECORDING_INDEX));
        RecordingReader.writeRecording(recording, output);
        output.close();
    }

    private void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                deleteDirectory(file);
            } else {
                file.delete();
            }
        }
        directory.delete();
    }

    public void deleteRecording(Recording recording) {
        File parentFile = recording.getDirectory().getParentFile();
        Folder parent = folders.get(parentFile);
        parent.deleteRecording(recording);
        deleteDirectory(recording.getDirectory());
    }

    public void updateRecordingMetadata(Recording recording)
            throws IOException {
        File metadataFile = new File(recording.getDirectory(),
                RecordingConstants.METADATA);
        FileOutputStream outputStream = new FileOutputStream(metadataFile);
        RecordingMetadataReader.writeMetadata(recording.getMetadata(),
                outputStream);
        outputStream.close();
    }

    public void updateRecordingLayouts(Recording recording) throws IOException {
        for (ReplayLayout layout : recording.getReplayLayouts()) {
            File layoutOutput = new File(recording.getDirectory(),
                    layout.getName() + RecordingConstants.LAYOUT);
            FileOutputStream outputLayout = new FileOutputStream(layoutOutput);
            LayoutReader.writeLayout(layout, outputLayout);
            outputLayout.close();
        }
    }

    public Folder getFolder(File path) {
        if (path == null || path.equals(topLevelFolder.getFile())) {
            return topLevelFolder;
        }
        return folders.get(path);
    }

    public Folder addFolder(Folder parent, String folderName) {
        File file = new File(parent.getFile(), folderName);
        file.mkdirs();
        Folder folder = new Folder(file);
        parent.addFolder(folder);
        return folder;
    }

    public void updateFolder(Folder folder) throws IOException {
        FolderReader.writeFolder(folder);
    }

    private void shutdown(Folder folder) {
        for (Folder f : folder.getFolders()) {
            shutdown(f);
        }
        for (HarvestSource source : folder.getHarvestSources()) {
            source.stopTimer();
        }
        for (UnfinishedRecording recording : folder.getUnfinishedRecordings()) {
            recording.stopTimers();
            recording.stopRecording();
        }
    }

    public void shutdown() {
        shutdown(topLevelFolder);
        PlaybackManager.shutdown();
    }


}
