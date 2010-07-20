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

package com.googlecode.vicovre.recordings.db.secure;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Vector;

import org.xml.sax.SAXException;

import com.googlecode.vicovre.recordings.DefaultLayout;
import com.googlecode.vicovre.recordings.HarvestSource;
import com.googlecode.vicovre.recordings.Recording;
import com.googlecode.vicovre.recordings.UnfinishedRecording;
import com.googlecode.vicovre.recordings.db.Folder;
import com.googlecode.vicovre.security.UnauthorizedException;
import com.googlecode.vicovre.security.db.SecurityDatabase;

public class SecureFolder implements Folder {

    private Folder folder = null;

    private SecurityDatabase securityDatabase = null;

    private SecureRecordingDatabase recordingDatabase = null;

    public SecureFolder(Folder folder,
            SecurityDatabase securityDatabase,
            SecureRecordingDatabase recordingDatabase) {
        this.folder = folder;
        this.securityDatabase = securityDatabase;
        this.recordingDatabase = recordingDatabase;
    }

    public List<DefaultLayout> getDefaultLayouts() throws IOException,
            SAXException {
        return folder.getDefaultLayouts();
    }

    public String getDescription() {
        return folder.getDescription();
    }

    public File getFile() {
        return folder.getFile();
    }

    public List<Folder> getFolders() {
        List<Folder> folders = folder.getFolders();
        List<Folder> secureFolders = new Vector<Folder>();
        for (Folder folder : folders) {
            secureFolders.add(new SecureFolder(folder, securityDatabase,
                    recordingDatabase));
        }
        return secureFolders;
    }

    public HarvestSource getHarvestSource(String id) {
        HarvestSource source = folder.getHarvestSource(id);
        if (source != null) {
            if (!securityDatabase.isAllowed(recordingDatabase.getFolderName(
                    source.getFile().getParentFile()),
                    SecureRecordingDatabase.HARVEST_ID_PREFIX
                        + source.getId())) {
                throw new UnauthorizedException("Unable to access source "
                        + id);
            }
        }
        return source;
    }

    public List<HarvestSource> getHarvestSources() {
        List<HarvestSource> sources = folder.getHarvestSources();
        List<HarvestSource> secureSources = new Vector<HarvestSource>();
        for (HarvestSource source : sources) {
            if (securityDatabase.isAllowed(recordingDatabase.getFolderName(
                    source.getFile().getParentFile()),
                    SecureRecordingDatabase.HARVEST_ID_PREFIX
                        + source.getId())) {
                secureSources.add(source);
            }
        }
        return secureSources;
    }

    public String getName() {
        return folder.getName();
    }

    public Recording getRecording(String id) {
        Recording recording = folder.getRecording(id);
        if (recording != null) {
            if (!securityDatabase.isAllowed(recordingDatabase.getFolderName(
                    recording.getDirectory().getParentFile()),
                    SecureRecordingDatabase.READ_RECORDING_ID_PREFIX
                        + recording.getId())) {
                throw new UnauthorizedException("Unable to access recording "
                        + id);
            }
        }
        return recording;
    }

    public List<Recording> getRecordings() {
        List<Recording> recordings = folder.getRecordings();
        List<Recording> secureRecordings = new Vector<Recording>();
        for (Recording recording : recordings) {
            if (securityDatabase.isAllowed(recordingDatabase.getFolderName(
                    recording.getDirectory().getParentFile()),
                    SecureRecordingDatabase.READ_RECORDING_ID_PREFIX
                        + recording.getId())) {
                secureRecordings.add(recording);
            }
        }
        return secureRecordings;
    }

    public UnfinishedRecording getUnfinishedRecording(String id) {
        UnfinishedRecording recording = folder.getUnfinishedRecording(id);
        if (recording != null) {
            if (!securityDatabase.isAllowed(recordingDatabase.getFolderName(
                    recording.getFile().getParentFile()),
                    SecureRecordingDatabase.UNFINISHED_ID_PREFIX
                        + recording.getId())) {
                throw new UnauthorizedException("Unable to access recording "
                        + id);
            }
        }
        return recording;
    }

    public List<UnfinishedRecording> getUnfinishedRecordings() {
        List<UnfinishedRecording> recordings = folder.getUnfinishedRecordings();
        List<UnfinishedRecording> secureRecordings =
            new Vector<UnfinishedRecording>();
        for (UnfinishedRecording recording : recordings) {
            if (securityDatabase.isAllowed(recordingDatabase.getFolderName(
                    recording.getFile().getParentFile()),
                    SecureRecordingDatabase.UNFINISHED_ID_PREFIX
                        + recording.getId())) {
                secureRecordings.add(recording);
            }
        }
        return secureRecordings;
    }

    public int compareTo(Folder f) {
        return folder.compareTo(f);
    }

}
