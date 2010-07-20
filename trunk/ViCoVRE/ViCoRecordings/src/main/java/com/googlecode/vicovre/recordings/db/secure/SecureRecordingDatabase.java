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

import com.googlecode.vicovre.recordings.HarvestSource;
import com.googlecode.vicovre.recordings.Recording;
import com.googlecode.vicovre.recordings.UnfinishedRecording;
import com.googlecode.vicovre.recordings.db.Folder;
import com.googlecode.vicovre.recordings.db.RecordingDatabase;
import com.googlecode.vicovre.security.UnauthorizedException;
import com.googlecode.vicovre.security.db.SecurityDatabase;
import com.googlecode.vicovre.security.db.WriteOnlyEntity;

public class SecureRecordingDatabase implements RecordingDatabase {

    protected static final String HARVEST_ID_PREFIX = "harvestSource";

    protected static final String READ_RECORDING_ID_PREFIX = "readRecording";

    protected static final String CHANGE_RECORDING_ID_PREFIX = "editRecording";

    protected static final String UNFINISHED_ID_PREFIX = "unfinishedRecording";

    private RecordingDatabase database = null;

    private SecurityDatabase securityDatabase = null;

    public SecureRecordingDatabase(RecordingDatabase database,
            SecurityDatabase securityDatabase) {
        this.database = database;
        this.securityDatabase = securityDatabase;
    }

    protected String getFolderName(File file) {
        File root = database.getTopLevelFolder().getFile();
        return file.getName().substring(root.getName().length());
    }

    public void addHarvestSource(HarvestSource harvestSource)
            throws IOException {
        securityDatabase.createAcl(null, null,
                getFolderName(harvestSource.getFile().getParentFile()),
                HARVEST_ID_PREFIX + harvestSource.getId(), false, true);
        database.addHarvestSource(harvestSource);
    }

    public void addRecording(Recording recording, UnfinishedRecording creator)
            throws IOException {
        String creatorFolder = null;
        String creatorId = null;
        if (creator != null) {
            creatorFolder = getFolderName(creator.getFile().getParentFile());
            creatorId = creator.getId();
        }
        securityDatabase.createAcl(creatorFolder, creatorId,
                getFolderName(recording.getDirectory().getParentFile()),
                CHANGE_RECORDING_ID_PREFIX + recording.getId(), false, false);
        securityDatabase.createAcl(creatorFolder, creatorId,
                getFolderName(recording.getDirectory().getParentFile()),
                CHANGE_RECORDING_ID_PREFIX + recording.getId(), false, false);
        database.addRecording(recording, creator);
    }

    public void addUnfinishedRecording(UnfinishedRecording recording,
            HarvestSource creator)
            throws IOException {
        String creatorFolder = null;
        String creatorId = null;
        if (creator != null) {
            creatorFolder = getFolderName(creator.getFile().getParentFile());
            creatorId = creator.getId();
        }
        securityDatabase.createAcl(creatorFolder, creatorId,
                getFolderName(recording.getFile()),
                UNFINISHED_ID_PREFIX + recording.getId(), false, true);
        database.addUnfinishedRecording(recording, creator);
    }

    public void addVenueServer(String url) {
        database.addVenueServer(url);
    }

    public void deleteHarvestSource(HarvestSource harvestSource)
            throws IOException {
        securityDatabase.deleteAcl(
                getFolderName(harvestSource.getFile().getParentFile()),
                HARVEST_ID_PREFIX + harvestSource.getId());
        database.deleteHarvestSource(harvestSource);
    }

    public void deleteRecording(Recording recording) throws IOException {
        securityDatabase.deleteAcl(
                getFolderName(recording.getDirectory().getParentFile()),
                CHANGE_RECORDING_ID_PREFIX + recording.getId());
        securityDatabase.deleteAcl(
                getFolderName(recording.getDirectory().getParentFile()),
                READ_RECORDING_ID_PREFIX + recording.getId());
        database.deleteRecording(recording);
    }

    public void deleteUnfinishedRecording(UnfinishedRecording recording)
            throws IOException {
        securityDatabase.deleteAcl(
                getFolderName(recording.getFile().getParentFile()),
                UNFINISHED_ID_PREFIX + recording.getId());
        database.deleteUnfinishedRecording(recording);
    }

    public Folder getFolder(File path) {
        return new SecureFolder(database.getFolder(path),
                securityDatabase, this);
    }

    public String[] getKnownVenueServers() {
        return database.getKnownVenueServers();
    }

    public Folder getTopLevelFolder() {
        return new SecureFolder(database.getTopLevelFolder(),
                securityDatabase, this);
    }

    public void shutdown() {
        database.shutdown();
    }

    public void updateHarvestSource(HarvestSource harvestSource)
            throws IOException {
        if (!securityDatabase.isAllowed(
                getFolderName(harvestSource.getFile().getParentFile()),
                HARVEST_ID_PREFIX + harvestSource.getId())) {
            throw new UnauthorizedException(
                    "Only the owner of the harvest source can edit it");
        }
        database.updateHarvestSource(harvestSource);
    }

    public void updateRecordingLayouts(Recording recording) throws IOException {
        if (!securityDatabase.isAllowed(
                getFolderName(recording.getDirectory().getParentFile()),
                CHANGE_RECORDING_ID_PREFIX + recording.getId())) {
            throw new UnauthorizedException(
                    "Only the owner of the recording can edit it");
        }
        database.updateRecordingLayouts(recording);
    }

    public void updateRecordingMetadata(Recording recording)
            throws IOException {
        if (!securityDatabase.isAllowed(
                getFolderName(recording.getDirectory().getParentFile()),
                CHANGE_RECORDING_ID_PREFIX + recording.getId())) {
            throw new UnauthorizedException(
                    "Only the owner of the recording can edit it");
        }
        database.updateRecordingMetadata(recording);
    }

    public void updateUnfinishedRecording(UnfinishedRecording recording)
            throws IOException {
        if (!securityDatabase.isAllowed(
                getFolderName(recording.getFile().getParentFile()),
                UNFINISHED_ID_PREFIX + recording.getId())) {
            throw new UnauthorizedException(
                    "Only the owner of the recording can edit it");
        }
        database.updateUnfinishedRecording(recording);
    }

    public void setRecordingAcl(Recording recording, boolean isPublic,
            WriteOnlyEntity... exceptions) throws IOException {
        securityDatabase.setAcl(
                getFolderName(recording.getDirectory().getParentFile()),
                READ_RECORDING_ID_PREFIX + recording.getId(), isPublic,
                exceptions);
    }
}
