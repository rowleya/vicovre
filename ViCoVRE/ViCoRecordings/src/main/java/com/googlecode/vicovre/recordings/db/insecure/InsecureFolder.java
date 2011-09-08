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

package com.googlecode.vicovre.recordings.db.insecure;

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
import com.googlecode.vicovre.recordings.HarvestSource;
import com.googlecode.vicovre.recordings.Metadata;
import com.googlecode.vicovre.recordings.Recording;
import com.googlecode.vicovre.recordings.ReplayLayout;
import com.googlecode.vicovre.recordings.UnfinishedRecording;
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
public class InsecureFolder {

    private File file = null;

    private Metadata metadata = null;

    private HashMap<String, Recording> recordings =
        new HashMap<String, Recording>();

    private HashSet<File> recordingsLoaded = new HashSet<File>();

    private HashMap<File, Long> recordingLastModified =
        new HashMap<File, Long>();

    private HashMap<String, HarvestSource> harvestSources =
        new HashMap<String, HarvestSource>();

    private HashMap<String, UnfinishedRecording> unfinishedRecordings =
        new HashMap<String, UnfinishedRecording>();

    private RtpTypeRepository typeRepository = null;

    private LayoutRepository layoutRepository = null;

    private HarvestFormatRepository harvestFormatRepository = null;

    private long defaultRecordingLifetime = 0;

    private boolean readOnly = false;

    private String folder = null;

    /**
     * Creates a folder
     * @param file The real folder
     */
    public InsecureFolder(File file, String folder,
            RtpTypeRepository typeRepository,
            LayoutRepository layoutRepository,
            HarvestFormatRepository harvestFormatRepository, boolean readOnly,
            long defaultRecordingLifetime) {
        this.file = file;
        this.folder = folder;
        this.typeRepository = typeRepository;
        this.layoutRepository = layoutRepository;
        this.harvestFormatRepository = harvestFormatRepository;
        this.readOnly = readOnly;
        this.defaultRecordingLifetime = LifetimeReader.readLifetime(file,
                defaultRecordingLifetime);

        readRecordings();
        if (!readOnly) {
            readUnfinishedRecordings();
            readHarvestSources();
        }
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    public Metadata getMetadata() {
        if (metadata == null) {
            return new Metadata("name", file.getName());
        }
        return metadata;
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
        return file.getName();
    }

    /**
     * Sets the name
     * @param name the name to set
     * @throws IOException
     */
    protected void setName(String name) throws IOException {
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
    protected void setDescription(String description) throws IOException {
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
    public List<String> getFolders() {
        Vector<String> folders = new Vector<String>();
        File[] files = file.listFiles(new FolderFilter(false));
        for (File folderFile : files) {
            folders.add(folderFile.getName());
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
        if (!recordings.containsKey(id)) {
            File recordingFile = new File(file, id);
            File index = new File(recordingFile,
                    RecordingConstants.RECORDING_INDEX);
            File inProgress = new File(recordingFile,
                    RecordingConstants.RECORDING_INPROGRESS);
            if (recordingFile.exists() && recordingFile.isDirectory()
                    && index.exists() && !inProgress.exists()) {
                try {
                    readRecording(recordingFile);
                } catch (Exception e) {
                    System.err.println("Warning: error reading recording "
                            + recordingFile);
                    e.printStackTrace();
                }
            }
        }
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

    private void readRecording(File recordingFile)
            throws SAXException, IOException {

    	boolean load = false;
        File index = new File(recordingFile,
                RecordingConstants.RECORDING_INDEX);
        if (!recordingsLoaded.contains(recordingFile)
                || recordingLastModified.get(recordingFile)
                    < index.lastModified()) {
        	load = true;
        }

        if (load) {
            FileInputStream input = new FileInputStream(index);
            Recording recording = RecordingReader.readRecording(input,
                    folder, recordingFile, typeRepository,
                    layoutRepository, defaultRecordingLifetime);
            if (recording == null) {
                throw new IOException("Recording "
                        + recordingFile.getName()
                        + " could not be read");
            }
            recordings.put(recording.getId(), recording);
            recordingsLoaded.add(recordingFile);
            recordingLastModified.put(recordingFile,
                    index.lastModified());
        }
    }

    private void readRecordings() {
        File[] recordingFiles = file.listFiles(new FolderFilter(true));
        HashSet<File> recordingsSeen = new HashSet<File>();
        for (File recordingFile : recordingFiles) {
            try {
                readRecording(recordingFile);
                recordingsSeen.add(recordingFile);
            } catch (Exception e) {
                System.err.println("Warning: error reading recording "
                        + recordingFile);
                e.printStackTrace();
            }
        }

        Vector<File> recordingsToRemove = new Vector<File>();
        for (File recordingFile : recordingsLoaded) {
            if (!recordingsSeen.contains(recordingFile)) {
                recordingsToRemove.add(recordingFile);
            }
        }

        for (File recordingFile : recordingsToRemove) {
            recordings.remove(recordingFile);
            recordingsLoaded.remove(recordingFile);
            recordingLastModified.remove(recordingFile);
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
                String id = recordingFile.getName();
                id = id.substring(0, id.length()
                    - RecordingConstants.UNFINISHED_RECORDING_INDEX.length());
                FileInputStream input = new FileInputStream(recordingFile);
                UnfinishedRecording recording =
                    UnfinishedRecordingReader.readRecording(input,
                            folder, id, file);
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
                String id = sourceFile.getName();
                id = id.substring(0, id.length()
                    - RecordingConstants.HARVEST_SOURCE.length());
                FileInputStream input = new FileInputStream(sourceFile);
                HarvestSource harvestSource =
                    HarvestSourceReader.readHarvestSource(input,
                            harvestFormatRepository, folder, id);
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

    protected void addRecording(Recording recording) {
        if (!readOnly) {
            recordings.put(recording.getId(), recording);
        }
    }

    protected void addUnfinishedRecording(UnfinishedRecording recording) {
        if (!readOnly) {
            unfinishedRecordings.put(recording.getId(), recording);
        }
    }

    protected void addHarvestSource(HarvestSource harvestSource) {
        if (!readOnly) {
            harvestSources.put(harvestSource.getId(), harvestSource);
        }
    }

    protected void deleteRecording(String id) {
        if (!readOnly) {
            recordings.remove(id);
        }
    }

    protected void deleteUnfinishedRecording(String id) {
        if (!readOnly) {
            unfinishedRecordings.remove(id);
        }
    }

    protected void deleteHarvestSource(String id) {
        if (!readOnly) {
            harvestSources.remove(id);
        }
    }

    public boolean equals(InsecureFolder folder) {
        return file.equals(folder.file);
    }

    public int hashCode() {
        return file.hashCode();
    }

    public int compareTo(InsecureFolder f) {
        return getName().compareTo(f.getName());
    }

    protected long getDefaultRecordingLifetime() {
        return defaultRecordingLifetime;
    }

    protected void setDefaultRecordingLifetime(long defaultRecordingLifetime) {
        this.defaultRecordingLifetime = defaultRecordingLifetime;
    }
}
