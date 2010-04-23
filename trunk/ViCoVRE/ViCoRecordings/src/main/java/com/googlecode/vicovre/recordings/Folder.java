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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;

import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.googlecode.vicovre.media.protocol.memetic.RecordingConstants;
import com.googlecode.vicovre.recordings.db.DefaultLayoutReader;
import com.googlecode.vicovre.recordings.db.FolderFilter;
import com.googlecode.vicovre.recordings.db.HarvestSourceReader;
import com.googlecode.vicovre.recordings.db.RecordingDatabase;
import com.googlecode.vicovre.recordings.db.RecordingReader;
import com.googlecode.vicovre.recordings.db.UnfinishedRecordingReader;
import com.googlecode.vicovre.repositories.harvestFormat.HarvestFormatRepository;
import com.googlecode.vicovre.repositories.layout.LayoutRepository;
import com.googlecode.vicovre.repositories.rtptype.RtpTypeRepository;
import com.googlecode.vicovre.utils.ExtensionFilter;
import com.googlecode.vicovre.utils.XmlIo;

/**
 * A folder of recordings (and other folders)
 *
 * @author Andrew G D Rowley
 * @version 1.0
 */
public class Folder implements Comparable<Folder> {

    private File file = null;

    private HashMap<String, Recording> recordings =
        new HashMap<String, Recording>();

    private HashSet<File> recordingsLoaded = new HashSet<File>();

    private HashMap<String, HarvestSource> harvestSources =
        new HashMap<String, HarvestSource>();

    private HashMap<String, UnfinishedRecording> unfinishedRecordings =
        new HashMap<String, UnfinishedRecording>();

    private RtpTypeRepository typeRepository = null;

    private LayoutRepository layoutRepository = null;

    private HarvestFormatRepository harvestFormatRepository = null;

    private RecordingDatabase database = null;

    private boolean readOnly = false;

    /**
     * Creates a folder
     * @param file The real folder
     */
    public Folder(File file, RtpTypeRepository typeRepository,
            LayoutRepository layoutRepository,
            HarvestFormatRepository harvestFormatRepository,
            RecordingDatabase database, boolean readOnly) {
        this.file = file;
        this.typeRepository = typeRepository;
        this.layoutRepository = layoutRepository;
        this.harvestFormatRepository = harvestFormatRepository;
        this.database = database;
        this.readOnly = readOnly;

        readRecordings();
        if (!readOnly) {
            readUnfinishedRecordings();
            readHarvestSources();
        }
    }

    /**
     * Gets the file
     * @return The file
     */
    public File getFile() {
        return file;
    }

    /**
     * Returns the name
     * @return the name
     */
    public String getName() {
        String name = file.getName();
        File description = new File(file, RecordingConstants.NAME);
        try {
            if (description.exists()) {
                FileInputStream input = new FileInputStream(description);
                Node doc = XmlIo.read(input);
                name = XmlIo.readValue(doc, "name");
                input.close();
            }
        } catch (Exception e) {
            System.err.println("Warning: Error reading folder metadata");
            e.printStackTrace();
        }
        return name;
    }

    /**
     * Sets the name
     * @param name the name to set
     * @throws IOException
     */
    public void setName(String name) throws IOException {
        File outputFile = new File(file, RecordingConstants.NAME);
        if (!name.equals(file.getName())) {
            PrintWriter output = new PrintWriter(outputFile);
            output.println("<folder>");
            output.println("<name value=\"" + name + "\">");
            output.println("</folder>");
            output.close();
        } else if (outputFile.exists()) {
            outputFile.delete();
        }
    }

    /**
     * Returns the description
     * @return the description
     */
    public String getDescription() {
        String name = null;
        File description = new File(file, RecordingConstants.DESCRIPTION);
        try {
            if (description.exists()) {
                FileInputStream input = new FileInputStream(description);
                Node doc = XmlIo.read(input);
                name = XmlIo.readValue(doc, "description");
                input.close();
            }
        } catch (Exception e) {
            System.err.println("Warning: Error reading folder metadata");
            e.printStackTrace();
        }
        return name;
    }

    /**
     * Sets the description
     * @param description the description to set
     */
    public void setDescription(String description) throws IOException {
        File outputFile = new File(file, RecordingConstants.DESCRIPTION);
        if (description != null) {
            PrintWriter output = new PrintWriter(outputFile);
            output.println("<folder>");
            output.println("<description value=\"" + description + "\">");
            output.println("</folder>");
            output.close();
        } else if (outputFile.exists()) {
            outputFile.delete();
        }
    }

    /**
     * Returns the folders
     * @return the folders
     */
    public List<Folder> getFolders() {
        Vector<Folder> folders = new Vector<Folder>();
        File[] files = file.listFiles(new FolderFilter(false));
        for (File folderFile : files) {
            Folder folder = new Folder(folderFile, typeRepository,
                    layoutRepository, harvestFormatRepository, database,
                    readOnly);
            folders.add(folder);
        }
        Collections.sort(folders);
        return folders;
    }

    /**
     * Gets a recording
     * @param id The id of the recording
     * @return The recording or null if doesn't exist
     */
    public Recording getRecording(String id) {
        return recordings.get(id);
    }

    /**
     * Returns the recordings
     * @return the recordings
     */
    public List<Recording> getRecordings() {
        readRecordings();
        List<Recording> recs = new Vector<Recording>(recordings.values());
        Collections.sort(recs);
        return recs;
    }

    private void readRecordings() {
        File[] recordingFiles = file.listFiles(new FolderFilter(true));
        for (File recordingFile : recordingFiles) {
            try {
                if (!recordingsLoaded.contains(recordingFile)) {
                    FileInputStream input = new FileInputStream(
                        new File(recordingFile,
                                RecordingConstants.RECORDING_INDEX));
                    Recording recording = RecordingReader.readRecording(input,
                            this, typeRepository, layoutRepository);
                    if (recording == null) {
                        throw new Exception("Recording "
                                + recordingFile.getName()
                                + " could not be read");
                    }
                    recordings.put(recording.getId(), recording);
                }
            } catch (Exception e) {
                System.err.println("Warning: error reading recording "
                        + recordingFile);
                e.printStackTrace();
            }
        }
    }

    public UnfinishedRecording getUnfinishedRecording(String id) {
        return unfinishedRecordings.get(id);
    }

    public List<UnfinishedRecording> getUnfinishedRecordings() {
        List<UnfinishedRecording> recs = new Vector<UnfinishedRecording>(
                unfinishedRecordings.values());
        Collections.sort(recs);
        return recs;
    }

    private void readUnfinishedRecordings() {
        File[] recordingFiles = file.listFiles(new ExtensionFilter(
                RecordingConstants.UNFINISHED_RECORDING_INDEX));
        for (File recordingFile : recordingFiles) {
            try {
                FileInputStream input = new FileInputStream(recordingFile);
                UnfinishedRecording recording =
                    UnfinishedRecordingReader.readRecording(input,
                            recordingFile, this, typeRepository, database);
                input.close();
                if (recording == null) {
                    throw new Exception("Could not read unfinished recording");
                }
                unfinishedRecordings.put(recording.getId(), recording);
            } catch (Exception e) {
                System.err.println(
                        "Warning: error reading unfinished recording "
                        + recordingFile);
                e.printStackTrace();
            }
        }
    }

    public HarvestSource getHarvestSource(String id) {
        return harvestSources.get(id);
    }

    public List<HarvestSource> getHarvestSources() {
        List<HarvestSource> sources = new Vector<HarvestSource>(
                harvestSources.values());
        return sources;
    }

    private void readHarvestSources() {
        File[] sourceFiles = file.listFiles(new ExtensionFilter(
                RecordingConstants.HARVEST_SOURCE));
        for (File sourceFile : sourceFiles) {
            try {
                FileInputStream input = new FileInputStream(sourceFile);
                HarvestSource harvestSource =
                    HarvestSourceReader.readHarvestSource(input,
                            harvestFormatRepository, typeRepository, this,
                            sourceFile);
                input.close();
                if (harvestSource == null) {
                    throw new Exception("Could not read harvest source");
                }
                harvestSources.put(harvestSource.getId(), harvestSource);
            } catch (Exception e) {
                System.err.println("Warning: error reading harvest source "
                        + sourceFile);
                e.printStackTrace();
            }
        }
    }

    public List<DefaultLayout> getDefaultLayouts() throws IOException,
            SAXException {
        Vector<DefaultLayout> defaultLayouts = new Vector<DefaultLayout>();
        File[] layoutFiles = file.listFiles(new ExtensionFilter(
                RecordingConstants.LAYOUT));
        for (File layoutFile : layoutFiles) {
            FileInputStream input = new FileInputStream(layoutFile);
            DefaultLayout layout = DefaultLayoutReader.readLayout(input,
                    layoutRepository);
            defaultLayouts.add(layout);
            input.close();
        }

        return defaultLayouts;
    }

    public void addRecording(Recording recording) {
        if (!readOnly) {
            recordings.put(recording.getId(), recording);
        }
    }

    public void addUnfinishedRecording(UnfinishedRecording recording) {
        if (!readOnly) {
            unfinishedRecordings.put(recording.getId(), recording);
        }
    }

    public void addHarvestSource(HarvestSource harvestSource) {
        if (!readOnly) {
            harvestSources.put(harvestSource.getId(), harvestSource);
        }
    }

    public void deleteRecording(String id) {
        if (!readOnly) {
            recordings.remove(id);
        }
    }

    public void deleteUnfinishedRecording(String id) {
        if (!readOnly) {
            unfinishedRecordings.remove(id);
        }
    }

    public void deleteHarvestSource(String id) {
        if (!readOnly) {
            harvestSources.remove(id);
        }
    }

    public boolean equals(Folder folder) {
        return file.equals(folder.file);
    }

    public int hashCode() {
        return file.hashCode();
    }

    public int compareTo(Folder f) {
        return getName().compareTo(f.getName());
    }
}
