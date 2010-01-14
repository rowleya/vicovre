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
import java.util.List;
import java.util.Vector;

import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.googlecode.vicovre.media.protocol.memetic.RecordingConstants;
import com.googlecode.vicovre.recordings.db.DefaultLayoutReader;
import com.googlecode.vicovre.recordings.db.HarvestSourceReader;
import com.googlecode.vicovre.recordings.db.RecordingDatabase;
import com.googlecode.vicovre.recordings.db.RecordingReader;
import com.googlecode.vicovre.recordings.db.UnfinishedRecordingReader;
import com.googlecode.vicovre.repositories.harvestFormat.HarvestFormatRepository;
import com.googlecode.vicovre.repositories.layout.LayoutRepository;
import com.googlecode.vicovre.repositories.rtptype.RtpTypeRepository;
import com.googlecode.vicovre.utils.ExtensionFilter;
import com.googlecode.vicovre.utils.FolderFilter;
import com.googlecode.vicovre.utils.XmlIo;

/**
 * A folder of recordings (and other folders)
 *
 * @author Andrew G D Rowley
 * @version 1.0
 */
public class Folder implements Comparable<Folder> {

    private File file = null;

    private HashMap<String, Long> recordingCacheTime =
        new HashMap<String, Long>();

    private HashMap<String, Recording> recordingCache =
        new HashMap<String, Recording>();

    private RtpTypeRepository typeRepository = null;

    private LayoutRepository layoutRepository = null;

    private HarvestFormatRepository harvestFormatRepository = null;

    private RecordingDatabase database = null;

    /**
     * Creates a folder
     * @param file The real folder
     */
    public Folder(File file, RtpTypeRepository typeRepository,
            LayoutRepository layoutRepository,
            HarvestFormatRepository harvestFormatRepository,
            RecordingDatabase database) {
        this.file = file;
        this.typeRepository = typeRepository;
        this.layoutRepository = layoutRepository;
        this.harvestFormatRepository = harvestFormatRepository;
        this.database = database;
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
                    layoutRepository, harvestFormatRepository, database);
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
        synchronized (recordingCache) {
            File recordingFile = new File(file, id);
            File recordingIndex = new File(recordingFile,
                    RecordingConstants.RECORDING_INDEX);
            try {
                if (recordingFile.exists() && recordingIndex.exists()) {
                    if (!recordingCache.containsKey(id)
                            || (recordingCacheTime.get(id)
                                < recordingIndex.lastModified())) {
                        FileInputStream input = new FileInputStream(
                                recordingIndex);
                        Recording recording = RecordingReader.readRecording(
                                input, this, typeRepository,
                                layoutRepository);
                        input.close();
                        recordingCacheTime.put(id,
                                recordingIndex.lastModified());
                        recordingCache.put(id, recording);
                    }
                } else {
                    recordingCache.remove(id);
                }
            } catch (Exception e) {
                System.err.println("Warning: error reading recording "
                        + recordingFile);
                e.printStackTrace();
            }
            return recordingCache.get(id);
        }
    }

    /**
     * Returns the recordings
     * @return the recordings
     */
    public List<Recording> getRecordings() {
        Vector<Recording> recs = new Vector<Recording>();
        File[] recordingFiles = file.listFiles(new FolderFilter(true));
        for (File recordingFile : recordingFiles) {
            try {
                Recording recording = getRecording(recordingFile.getName());
                recs.add(recording);
            } catch (Exception e) {
                System.err.println("Warning: error reading recording "
                        + recordingFile);
                e.printStackTrace();
            }
        }
        Collections.sort(recs);
        return recs;
    }

    public UnfinishedRecording getUnfinishedRecording(int id) {
        File recordingIndex = new File(file,
                RecordingConstants.UNFINISHED_RECORDING_INDEX);
        try {
            if (recordingIndex.exists()) {
                FileInputStream input = new FileInputStream(recordingIndex);
                UnfinishedRecording recording =
                    UnfinishedRecordingReader.readRecording(input,
                            recordingIndex, this, typeRepository, database);
                input.close();
                return recording;
            }
        } catch (Exception e) {
            System.err.println("Warning: error reading unfinished recording "
                    + recordingIndex);
            e.printStackTrace();
        }
        return null;
    }

    public List<UnfinishedRecording> getUnfinishedRecordings() {
        Vector<UnfinishedRecording> recs = new Vector<UnfinishedRecording>();
        File[] recordingFiles = file.listFiles(new ExtensionFilter(
                RecordingConstants.UNFINISHED_RECORDING_INDEX));
        for (File recordingFile : recordingFiles) {
            try {
                FileInputStream input = new FileInputStream(recordingFile);
                UnfinishedRecording recording =
                    UnfinishedRecordingReader.readRecording(input,
                            recordingFile, this, typeRepository, database);
                input.close();
                recs.add(recording);
            } catch (Exception e) {
                System.err.println("Warning: error reading unfinished recording"
                        + recordingFile);
                e.printStackTrace();
            }
        }
        Collections.sort(recs);
        return recs;
    }

    public HarvestSource getHarvestSource(int id) {
        File harvestIndex = new File(file,
                RecordingConstants.HARVEST_SOURCE);
        try {
            if (harvestIndex.exists()) {
                FileInputStream input = new FileInputStream(harvestIndex);
                HarvestSource harvestSource =
                    HarvestSourceReader.readHarvestSource(input,
                            harvestFormatRepository, typeRepository, this);
                harvestSource.setFile(harvestIndex);
                input.close();
                return harvestSource;
            }
        } catch (Exception e) {
            System.err.println("Warning: error reading harvest source "
                    + harvestIndex);
            e.printStackTrace();
        }
        return null;
    }

    public List<HarvestSource> getHarvestSources() {
        Vector<HarvestSource> sources = new Vector<HarvestSource>();
        File[] sourceFiles = file.listFiles(new ExtensionFilter(
                RecordingConstants.HARVEST_SOURCE));
        for (File sourceFile : sourceFiles) {
            try {
                FileInputStream input = new FileInputStream(sourceFile);
                HarvestSource harvestSource =
                    HarvestSourceReader.readHarvestSource(input,
                            harvestFormatRepository, typeRepository, this);
                harvestSource.setFile(sourceFile);
                input.close();
                sources.add(harvestSource);
            } catch (Exception e) {
                System.err.println("Warning: error reading harvest source "
                        + sourceFile);
                e.printStackTrace();
            }
        }
        return sources;
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
