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
import java.io.IOException;
import java.util.List;

import com.googlecode.vicovre.annotations.Annotation;
import com.googlecode.vicovre.recordings.DefaultLayout;
import com.googlecode.vicovre.recordings.HarvestSource;
import com.googlecode.vicovre.recordings.Metadata;
import com.googlecode.vicovre.recordings.Recording;
import com.googlecode.vicovre.recordings.UnfinishedRecording;
import com.googlecode.vicovre.recordings.db.insecure.HarvestSourceListener;
import com.googlecode.vicovre.recordings.db.insecure.RecordingListener;
import com.googlecode.vicovre.recordings.db.insecure.UnfinishedRecordingListener;

public interface RecordingDatabase {

    public String[] getKnownVenueServers();

    public void addVenueServer(String url);

    public File getFile(String folder);

    public List<String> getSubFolders(String folder);

    public boolean addFolder(String parent, String folder) throws IOException;

    public void deleteFolder(String folder) throws IOException;

    public boolean canReadFolder(String folder);

    public boolean canWriteFolder(String folder);

    public Metadata getFolderMetadata(String folder);

    public void setFolderMetadata(String folder, Metadata metadata)
        throws IOException;

    public void addHarvestSource(HarvestSource harvestSource)
            throws IOException;

    public void deleteHarvestSource(HarvestSource harvestSource)
            throws IOException;

    public void updateHarvestSource(HarvestSource harvestSource)
            throws IOException;

    public HarvestSource getHarvestSource(String folder, String id);

    public List<HarvestSource> getHarvestSources(String folder);

    public void addHarvestSourceListener(HarvestSourceListener listener);

    public void addUnfinishedRecording(UnfinishedRecording recording,
            HarvestSource creator)
            throws IOException;

    public void finishUnfinishedRecording(UnfinishedRecording recording)
            throws IOException;

    public void deleteUnfinishedRecording(UnfinishedRecording recording)
            throws IOException;

    public void updateUnfinishedRecording(UnfinishedRecording recording)
            throws IOException;

    public UnfinishedRecording getUnfinishedRecording(String folder, String id);

    public List<UnfinishedRecording> getUnfinishedRecordings(String folder);

    public void addUnfinishedRecordingListener(
            UnfinishedRecordingListener listener);

    public void addRecording(Recording recording, UnfinishedRecording creator)
            throws IOException;

    public void deleteRecording(Recording recording) throws IOException;

    public void moveRecording(Recording recording, String newFolder)
        throws IOException;

    public Recording getRecording(String folder, String id);

    public List<Recording> getRecordings(String folder);

    public void updateRecordingMetadata(Recording recording)
            throws IOException;

    public void updateRecordingLayouts(Recording recording) throws IOException;

    public void updateRecordingLifetime(Recording recording)
            throws IOException;

    public void updateRecordingAnnotations(Recording recording)
            throws IOException;

    public void addRecordingListener(RecordingListener recordingListener);

    public void setDefaultLayout(String folder, DefaultLayout layout)
        throws IOException;

    public void addAnnotation(Recording recording, Annotation annotation)
        throws IOException;

    public void deleteAnnotation(Recording recording, Annotation annotation)
        throws IOException;

    public void updateAnnotation(Recording recording, Annotation annotation)
        throws IOException;

    public void shutdown();
}
