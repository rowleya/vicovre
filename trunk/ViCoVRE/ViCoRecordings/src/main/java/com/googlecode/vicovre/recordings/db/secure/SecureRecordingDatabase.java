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

import com.googlecode.vicovre.recordings.HarvestSource;
import com.googlecode.vicovre.recordings.Recording;
import com.googlecode.vicovre.recordings.UnfinishedRecording;
import com.googlecode.vicovre.recordings.db.Folder;
import com.googlecode.vicovre.recordings.db.RecordingDatabase;
import com.googlecode.vicovre.security.AlreadyExistsException;
import com.googlecode.vicovre.security.UnauthorizedException;
import com.googlecode.vicovre.security.db.ReadOnlyACL;
import com.googlecode.vicovre.security.db.ReadOnlyEntity;
import com.googlecode.vicovre.security.db.Role;
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
        traverseFolders(database.getTopLevelFolder());
    }

    private void traverseFolders(Folder folder) {
        folder.setDatabase(this);
        List<HarvestSource> harvestSources = folder.getHarvestSources();
        for (HarvestSource harvestSource : harvestSources) {
            harvestSource.setDatabase(this);
        }
        List<UnfinishedRecording> unfinishedRecordings =
            folder.getUnfinishedRecordings();
        for (UnfinishedRecording recording : unfinishedRecordings) {
            recording.setDatabase(this);
        }
        for (Folder subFolder : folder.getFolders()) {
            traverseFolders(subFolder);
        }
    }

    protected String getFolderName(File file) {
        File root = database.getTopLevelFolder().getFile();
        if (file.equals(root)) {
            return "";
        }
        return file.getAbsolutePath().substring(
                root.getAbsolutePath().length());
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
        String folder = getFolderName(recording.getDirectory().getParentFile());
        securityDatabase.createAcl(creatorFolder,
                UNFINISHED_ID_PREFIX + creatorId, folder,
                CHANGE_RECORDING_ID_PREFIX + recording.getId(), false, true);
        try {
            securityDatabase.createAcl(creatorFolder,
                UNFINISHED_ID_PREFIX + creatorId, folder,
                READ_RECORDING_ID_PREFIX + recording.getId(), false, true);
        } catch (AlreadyExistsException e) {
            // Do Nothing
        }
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
        String folder = getFolderName(recording.getFile().getParentFile());
        securityDatabase.createAcl(creatorFolder,
                HARVEST_ID_PREFIX + creatorId, folder,
                UNFINISHED_ID_PREFIX + recording.getId(), false, true);
        securityDatabase.createAcl(folder,
                UNFINISHED_ID_PREFIX + recording.getId(), folder,
                READ_RECORDING_ID_PREFIX + recording.getFinishedRecordingId(),
                false, true);
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
        String folder = getFolderName(recording.getFile().getParentFile());
        securityDatabase.deleteAcl(folder,
                UNFINISHED_ID_PREFIX + recording.getId());
        securityDatabase.deleteAcl(folder,
            READ_RECORDING_ID_PREFIX + recording.getFinishedRecordingId());
        database.deleteUnfinishedRecording(recording);
    }

    public Folder getFolder(File path) {
        Folder folder = database.getFolder(path);
        folder.setDatabase(this);
        return new SecureFolder(folder, securityDatabase, this);
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

    public void updateRecordingLifetime(Recording recording)
            throws IOException {
        if (!securityDatabase.hasRole(Role.ADMINISTRATOR)) {
            throw new UnauthorizedException(
                "Only an administrator can update the lifetime of a recording");
        }
        database.updateRecordingLifetime(recording);
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
        String folder = getFolderName(recording.getFile().getParentFile());
        String id = UNFINISHED_ID_PREFIX + recording.getId();
        if (!securityDatabase.isAllowed(folder, id)) {
            throw new UnauthorizedException(
                    "Only the owner of the recording can edit it");
        }
        if ((recording.getOldFinishedRecordingId() != null) &&
                !recording.getOldFinishedRecordingId().equals(
                        recording.getFinishedRecordingId())) {
            String oldId = READ_RECORDING_ID_PREFIX
                + recording.getOldFinishedRecordingId();
            String newId = READ_RECORDING_ID_PREFIX
                + recording.getFinishedRecordingId();
            ReadOnlyACL acl = securityDatabase.getAcl(folder, oldId, false);
            securityDatabase.deleteAcl(folder, oldId);
            List<ReadOnlyEntity> aclExceptions = acl.getExceptions();
            WriteOnlyEntity[] exceptions =
                new WriteOnlyEntity[aclExceptions.size()];
            for (int i = 0; i < exceptions.length; i++) {
                ReadOnlyEntity entity = aclExceptions.get(i);
                exceptions[i] = new WriteOnlyEntity(entity.getName(),
                        entity.getType());
            }
            securityDatabase.createAcl(folder, id, folder, newId, acl.isAllow(),
                    true, exceptions);
        }
        database.updateUnfinishedRecording(recording);
    }

    public void setRecordingAcl(Recording recording, boolean isPublic,
            WriteOnlyEntity... exceptions) throws IOException {
        securityDatabase.setAcl(
                getFolderName(recording.getDirectory().getParentFile()),
                READ_RECORDING_ID_PREFIX + recording.getId(),
                isPublic, exceptions);
    }

    public ReadOnlyACL getRecordingAcl(Recording recording) {
        return securityDatabase.getAcl(
                getFolderName(recording.getDirectory().getParentFile()),
                READ_RECORDING_ID_PREFIX + recording.getId(), true);
    }

    public void setRecordingAcl(UnfinishedRecording recording,
            boolean isPublic, WriteOnlyEntity... exceptions)
            throws IOException {
        String folder = getFolderName(recording.getFile().getParentFile());
        securityDatabase.setAcl(folder,
            READ_RECORDING_ID_PREFIX + recording.getFinishedRecordingId(),
            isPublic, exceptions);
    }

    public ReadOnlyACL getRecordingAcl(UnfinishedRecording recording) {
        return securityDatabase.getAcl(
                getFolderName(recording.getFile().getParentFile()),
                READ_RECORDING_ID_PREFIX + recording.getFinishedRecordingId(),
                true);
    }

    public boolean canEditRecording(Recording recording) {
        return securityDatabase.isAllowed(
                getFolderName(recording.getDirectory().getParentFile()),
                CHANGE_RECORDING_ID_PREFIX + recording.getId());
    }
}
