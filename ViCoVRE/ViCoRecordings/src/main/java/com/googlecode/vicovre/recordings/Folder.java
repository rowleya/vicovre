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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

/**
 * A folder of recordings (and other folders)
 *
 * @author Andrew G D Rowley
 * @version 1.0
 */
public class Folder implements Comparable<Folder> {

    private File file = null;

    private String name = null;

    private String description = null;

    private HashMap<String, Folder> folders = new HashMap<String, Folder>();

    private HashMap<String, Recording> recordings =
        new HashMap<String, Recording>();

    private HashMap<Integer, UnfinishedRecording> unfinishedRecordings =
        new HashMap<Integer, UnfinishedRecording>();

    private HashMap<Integer, HarvestSource> harvestSources =
        new HashMap<Integer, HarvestSource>();

    /**
     * Creates a folder
     * @param file The real folder
     */
    public Folder(File file) {
        this.file = file;
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
     * Returns the description
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    public void addFolder(Folder folder) {
        folders.put(folder.getName(), folder);
    }

    /**
     * Returns the folders
     * @return the folders
     */
    public List<Folder> getFolders() {
        Vector<Folder> folders = new Vector<Folder>(this.folders.values());
        Collections.sort(folders);
        return folders;
    }

    /**
     * Sets the folders
     * @param folders the folders to set
     */
    public void setFolders(List<Folder> folders) {
        if (folders != null) {
            for (Folder folder : folders) {
                this.folders.put(folder.getName(), folder);
            }
        } else {
            this.folders.clear();
        }
    }

    /**
     * Adds a recording
     * @param recording The recording to add
     */
    public void addRecording(Recording recording) {
        recordings.put(recording.getId(), recording);
    }

    public void deleteRecording(Recording recording) {
        recordings.remove(recording.getId());
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
        Vector<Recording> recs = new Vector<Recording>(recordings.values());
        Collections.sort(recs);
        return recs;
    }

    /**
     * Sets the recordings
     * @param recordings the recordings to set
     */
    public void setRecordings(List<Recording> recordings) {
        if (recordings != null) {
            for (Recording recording : recordings) {
                this.recordings.put(recording.getId(), recording);
            }
        } else {
            this.recordings.clear();
        }
    }

    public void addUnfinishedRecording(UnfinishedRecording recording) {
        unfinishedRecordings.put(recording.getId(), recording);
    }

    public void deleteUnfinishedRecording(UnfinishedRecording recording) {
        unfinishedRecordings.remove(recording.getId());
    }

    public UnfinishedRecording getUnfinishedRecording(int id) {
        return unfinishedRecordings.get(id);
    }

    public List<UnfinishedRecording> getUnfinishedRecordings() {
        return new Vector<UnfinishedRecording>(unfinishedRecordings.values());
    }

    public void setUnfinishedRecordings(List<UnfinishedRecording> recordings) {
        if (recordings != null) {
            for (UnfinishedRecording recording : recordings) {
                this.unfinishedRecordings.put(recording.getId(), recording);
            }
        } else {
            this.unfinishedRecordings.clear();
        }
    }

    public void addHarvestSource(HarvestSource harvestSource) {
        harvestSources.put(harvestSource.getId(), harvestSource);
    }

    public void deleteHarvestSource(HarvestSource harvestSource) {
        harvestSources.remove(harvestSource.getId());
    }

    public HarvestSource getHarvestSource(int id) {
        return harvestSources.get(id);
    }

    public List<HarvestSource> getHarvestSources() {
        return new Vector<HarvestSource>(harvestSources.values());
    }

    public void setHarvestSources(List<HarvestSource> harvestSources) {
        if (harvestSources != null) {
            for (HarvestSource harvestSource : harvestSources) {
                this.harvestSources.put(harvestSource.getId(), harvestSource);
            }
        } else {
            this.harvestSources.clear();
        }
    }

    public boolean equals(Folder folder) {
        return file.equals(folder.file);
    }

    public int hashCode() {
        return file.hashCode();
    }

    public int compareTo(Folder f) {
        return name.compareTo(f.name);
    }
}
