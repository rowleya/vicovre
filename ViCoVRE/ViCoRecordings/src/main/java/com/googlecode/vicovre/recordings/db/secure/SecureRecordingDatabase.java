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

import com.googlecode.vicovre.recordings.DefaultLayout;
import com.googlecode.vicovre.recordings.HarvestSource;
import com.googlecode.vicovre.recordings.Metadata;
import com.googlecode.vicovre.recordings.Recording;
import com.googlecode.vicovre.recordings.UnfinishedRecording;
import com.googlecode.vicovre.recordings.db.RecordingDatabase;
import com.googlecode.vicovre.recordings.db.insecure.HarvestSourceListener;
import com.googlecode.vicovre.recordings.db.insecure.RecordingListener;
import com.googlecode.vicovre.recordings.db.insecure.UnfinishedRecordingListener;
import com.googlecode.vicovre.security.AlreadyExistsException;
import com.googlecode.vicovre.security.UnauthorizedException;
import com.googlecode.vicovre.security.db.ReadOnlyACL;
import com.googlecode.vicovre.security.db.ReadOnlyEntity;
import com.googlecode.vicovre.security.db.Role;
import com.googlecode.vicovre.security.db.SecurityDatabase;
import com.googlecode.vicovre.security.db.WriteOnlyEntity;

public class SecureRecordingDatabase implements RecordingDatabase {

    public static final String HARVEST_ID_PREFIX = "harvestSource";

    public static final String READ_RECORDING_ID_PREFIX = "readRecording";

    public static final String CHANGE_RECORDING_ID_PREFIX = "editRecording";

    public static final String PLAY_RECORDING_ID_PREFIX = "playRecording";

    public static final String UNFINISHED_ID_PREFIX = "unfinishedRecording";

    public static final String READ_FOLDER_PREFIX = "readFolder";

    public static final String WRITE_FOLDER_PREFIX = "writeFolder";

    private RecordingDatabase database = null;

    private SecurityDatabase securityDatabase = null;

    public SecureRecordingDatabase(RecordingDatabase database,
            SecurityDatabase securityDatabase) {
        this.database = database;
        this.securityDatabase = securityDatabase;
    }

    public String[] getKnownVenueServers() {
        return database.getKnownVenueServers();
    }

    public void addVenueServer(String url) {
        database.addVenueServer(url);
    }

    public File getFile(String folder) {
        return database.getFile(folder);
    }

    private void checkRead(String folder) {
        if (!canReadFolder(folder)) {
            throw new UnauthorizedException(
                "You do not have permission to read this folder");
        }
    }

    private void checkWrite(String folder) {
        if (!canWriteFolder(folder)) {
            throw new UnauthorizedException(
                "You do not have permission to write to this folder");
        }
    }

    public List<String> getSubFolders(String folder) {
        checkRead(folder);
        return database.getSubFolders(folder);
    }

    public void addHarvestSource(HarvestSource harvestSource)
            throws IOException {
        checkWrite(harvestSource.getFolder());
        securityDatabase.createAcl(null, null, harvestSource.getFolder(),
                HARVEST_ID_PREFIX + harvestSource.getId(), false, true);
        database.addHarvestSource(harvestSource);
    }

    public void updateHarvestSource(HarvestSource harvestSource)
            throws IOException {
        if (!securityDatabase.isAllowed(harvestSource.getFolder(),
                HARVEST_ID_PREFIX + harvestSource.getId(), false)) {
            throw new UnauthorizedException(
                    "Only the owner of the harvest source can edit it");
        }
        database.updateHarvestSource(harvestSource);
    }

    public void deleteHarvestSource(HarvestSource harvestSource)
            throws IOException {
        securityDatabase.deleteAcl(harvestSource.getFolder(),
                HARVEST_ID_PREFIX + harvestSource.getId());
        database.deleteHarvestSource(harvestSource);
    }

    public HarvestSource getHarvestSource(String folder, String id) {
        if (!securityDatabase.isAllowed(folder,
                HARVEST_ID_PREFIX + id, false)) {
            throw new UnauthorizedException(
                    "Only the owner of the harvest source can see it");
        }
        return database.getHarvestSource(folder, id);
    }

    public List<HarvestSource> getHarvestSources(String folder) {
        checkRead(folder);
        List<HarvestSource> harvestSources = database.getHarvestSources(folder);
        Vector<HarvestSource> secureHarvestSources =
            new Vector<HarvestSource>();
        for (HarvestSource harvestSource : harvestSources) {
            if (securityDatabase.isAllowed(harvestSource.getFolder(),
                    HARVEST_ID_PREFIX + harvestSource.getId(), false)) {
                secureHarvestSources.add(harvestSource);
            }
        }
        return secureHarvestSources;
    }

    public void addUnfinishedRecording(UnfinishedRecording recording,
            HarvestSource creator)
            throws IOException {
        String creatorFolder = null;
        String creatorId = null;
        if (creator != null) {
            creatorFolder = creator.getFolder();
            creatorId = creator.getId();
        }
        String folder = recording.getFolder();
        checkWrite(folder);
        securityDatabase.createAcl(creatorFolder,
                HARVEST_ID_PREFIX + creatorId, folder,
                UNFINISHED_ID_PREFIX + recording.getId(), false, true);
        securityDatabase.createAcl(folder,
                UNFINISHED_ID_PREFIX + recording.getId(), folder,
                READ_RECORDING_ID_PREFIX + recording.getFinishedRecordingId(),
                true, true);
        securityDatabase.createAcl(folder,
                UNFINISHED_ID_PREFIX + recording.getId(), folder,
                PLAY_RECORDING_ID_PREFIX + recording.getFinishedRecordingId(),
                false, true);
        database.addUnfinishedRecording(recording, creator);
    }

    private void updateAclId(String creatorFolder, String creatorId,
            String folder, String oldId, String newId) throws IOException {
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
        securityDatabase.createAcl(creatorFolder, creatorId, folder, newId,
                acl.isAllow(), true, exceptions);
    }

    public void updateUnfinishedRecording(UnfinishedRecording recording)
            throws IOException {
        String folder = recording.getFolder();
        String id = UNFINISHED_ID_PREFIX + recording.getId();
        if (!securityDatabase.isAllowed(folder, id, false)) {
            throw new UnauthorizedException(
                    "Only the owner of the recording can edit it");
        }
        if ((recording.getOldFinishedRecordingId() != null) &&
                !recording.getOldFinishedRecordingId().equals(
                        recording.getFinishedRecordingId())) {
            String oldReadId = READ_RECORDING_ID_PREFIX
                + recording.getOldFinishedRecordingId();
            String newReadId = READ_RECORDING_ID_PREFIX
                + recording.getFinishedRecordingId();
            String oldPlayId = PLAY_RECORDING_ID_PREFIX
                + recording.getOldFinishedRecordingId();
            String newPlayId = PLAY_RECORDING_ID_PREFIX
                + recording.getFinishedRecordingId();

            updateAclId(folder, id, folder, oldReadId, newReadId);
            updateAclId(folder, id, folder, oldPlayId, newPlayId);
        }
        database.updateUnfinishedRecording(recording);
    }

    public void deleteUnfinishedRecording(UnfinishedRecording recording)
            throws IOException {
        String folder = recording.getFolder();
        securityDatabase.deleteAcl(folder,
                UNFINISHED_ID_PREFIX + recording.getId());
        securityDatabase.deleteAcl(folder,
                READ_RECORDING_ID_PREFIX + recording.getFinishedRecordingId());
        securityDatabase.deleteAcl(folder,
                PLAY_RECORDING_ID_PREFIX + recording.getFinishedRecordingId());
        database.deleteUnfinishedRecording(recording);
    }

    public UnfinishedRecording getUnfinishedRecording(String folder,
            String id) {
        if (!securityDatabase.isAllowed(folder,
                UNFINISHED_ID_PREFIX + id, false)) {
            throw new UnauthorizedException(
                    "Only the owner of the recording can see it");
        }
        return database.getUnfinishedRecording(folder, id);
    }

    public List<UnfinishedRecording> getUnfinishedRecordings(String folder) {
        checkRead(folder);
        List<UnfinishedRecording> recordings =
            database.getUnfinishedRecordings(folder);
        Vector<UnfinishedRecording> secureRecordings =
            new Vector<UnfinishedRecording>();
        for (UnfinishedRecording recording : recordings) {
            if (securityDatabase.isAllowed(folder,
                    UNFINISHED_ID_PREFIX + recording.getId(), false)) {
                secureRecordings.add(recording);
            }
        }
        return secureRecordings;
    }

    public void addRecording(Recording recording, UnfinishedRecording creator)
            throws IOException {
        String creatorFolder = null;
        String creatorId = null;
        if (creator != null) {
            creatorFolder = creator.getFolder();
            creatorId = creator.getId();
        }
        String folder = recording.getFolder();
        checkWrite(folder);
        securityDatabase.createAcl(creatorFolder,
                UNFINISHED_ID_PREFIX + creatorId, folder,
                CHANGE_RECORDING_ID_PREFIX + recording.getId(), false, true);
        try {
            securityDatabase.createAcl(creatorFolder,
                UNFINISHED_ID_PREFIX + creatorId, folder,
                READ_RECORDING_ID_PREFIX + recording.getId(), true, true);
        } catch (AlreadyExistsException e) {
            // Do Nothing
        }
        try {
            securityDatabase.createAcl(creatorFolder,
                UNFINISHED_ID_PREFIX + creatorId, folder,
                PLAY_RECORDING_ID_PREFIX + recording.getId(), false, true);
        } catch (AlreadyExistsException e) {
            // Do Nothing
        }
        database.addRecording(recording, creator);
    }

    public void deleteRecording(Recording recording) throws IOException {
        securityDatabase.deleteAcl(recording.getFolder(),
                CHANGE_RECORDING_ID_PREFIX + recording.getId());
        securityDatabase.deleteAcl(recording.getFolder(),
                READ_RECORDING_ID_PREFIX + recording.getId());
        securityDatabase.deleteAcl(recording.getFolder(),
                PLAY_RECORDING_ID_PREFIX + recording.getId());
        database.deleteRecording(recording);
    }

    public Recording getRecording(String folder, String id) {
        if (!securityDatabase.isAllowed(folder,
                READ_RECORDING_ID_PREFIX + id, true)) {
            throw new UnauthorizedException(
                    "Only the owner of the recording can see it");
        }
        Recording recording = database.getRecording(folder, id);
        if (recording != null) {
            return new SecureRecording(this, recording);
        }
        return null;
    }

    public List<Recording> getRecordings(String folder) {
        checkRead(folder);
        List<Recording> recordings = database.getRecordings(folder);
        if (recordings != null) {
            Vector<Recording> secureRecordings = new Vector<Recording>();
            for (Recording recording : recordings) {
                if (securityDatabase.isAllowed(folder,
                        READ_RECORDING_ID_PREFIX + recording.getId(), true)) {
                    secureRecordings.add(new SecureRecording(this, recording));
                }
            }
            return secureRecordings;
        }
        return null;
    }

    public void updateRecordingLayouts(Recording recording) throws IOException {
        if (!securityDatabase.isAllowed(recording.getFolder(),
                CHANGE_RECORDING_ID_PREFIX + recording.getId(), false)) {
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
        if (!securityDatabase.isAllowed(recording.getFolder(),
                CHANGE_RECORDING_ID_PREFIX + recording.getId(), false)) {
            throw new UnauthorizedException(
                    "Only the owner of the recording can edit it");
        }
        database.updateRecordingMetadata(recording);
    }

    public boolean canEditRecording(String folder, String id) {
        return securityDatabase.isAllowed(folder,
                CHANGE_RECORDING_ID_PREFIX + id, false);
    }

    public boolean canPlayRecording(String folder, String id) {
        return securityDatabase.isAllowed(folder,
                PLAY_RECORDING_ID_PREFIX + id, false);
    }

    public boolean canReadRecording(String folder, String id) {
        return securityDatabase.isAllowed(folder,
                READ_RECORDING_ID_PREFIX + id, true);
    }

    public void shutdown() {
        database.shutdown();
    }

    public void setRecordingReadAcl(Recording recording, boolean isPublic,
            WriteOnlyEntity... exceptions) throws IOException {
        securityDatabase.setAcl(recording.getFolder(),
                READ_RECORDING_ID_PREFIX + recording.getId(),
                isPublic, exceptions);
    }

    public void setRecordingPlayAcl(Recording recording, boolean isPublic,
            WriteOnlyEntity... exceptions) throws IOException {
        securityDatabase.setAcl(recording.getFolder(),
                PLAY_RECORDING_ID_PREFIX + recording.getId(),
                isPublic, exceptions);
    }

    public ReadOnlyACL getRecordingReadAcl(Recording recording) {
        return securityDatabase.getAcl(recording.getFolder(),
                READ_RECORDING_ID_PREFIX + recording.getId(), true);
    }

    public ReadOnlyACL getRecordingPlayAcl(Recording recording) {
        return securityDatabase.getAcl(recording.getFolder(),
                PLAY_RECORDING_ID_PREFIX + recording.getId(), false);
    }

    public void setRecordingReadAcl(UnfinishedRecording recording,
            boolean isPublic, WriteOnlyEntity... exceptions)
            throws IOException {
        String folder = recording.getFolder();
        securityDatabase.setAcl(folder,
            READ_RECORDING_ID_PREFIX + recording.getFinishedRecordingId(),
            isPublic, exceptions);
    }

    public void setRecordingPlayAcl(UnfinishedRecording recording,
            boolean isPublic, WriteOnlyEntity... exceptions)
            throws IOException {
        String folder = recording.getFolder();
        securityDatabase.setAcl(folder,
            PLAY_RECORDING_ID_PREFIX + recording.getFinishedRecordingId(),
            isPublic, exceptions);
    }

    public ReadOnlyACL getRecordingReadAcl(UnfinishedRecording recording) {
        return securityDatabase.getAcl(recording.getFolder(),
                READ_RECORDING_ID_PREFIX + recording.getFinishedRecordingId(),
                true);
    }

    public ReadOnlyACL getRecordingPlayAcl(UnfinishedRecording recording) {
        return securityDatabase.getAcl(recording.getFolder(),
                PLAY_RECORDING_ID_PREFIX + recording.getFinishedRecordingId(),
                false);
    }

    public void setRecordingOwner(Recording recording, String owner)
            throws IOException {
        String folder = recording.getFolder();
        securityDatabase.setAclOwner(folder,
                PLAY_RECORDING_ID_PREFIX + recording.getId(), owner);
        securityDatabase.setAclOwner(folder,
                READ_RECORDING_ID_PREFIX + recording.getId(), owner);
        securityDatabase.setAclOwner(folder,
                CHANGE_RECORDING_ID_PREFIX + recording.getId(), owner);
    }

    public void addHarvestSourceListener(HarvestSourceListener listener) {
        database.addHarvestSourceListener(listener);
    }

    public void addRecordingListener(RecordingListener listener) {
        database.addRecordingListener(listener);
    }

    public void addUnfinishedRecordingListener(
            UnfinishedRecordingListener listener) {
        database.addUnfinishedRecordingListener(listener);
    }

    public void setDefaultLayout(String folder, DefaultLayout layout)
            throws IOException {
         checkWrite(folder);
         database.setDefaultLayout(folder, layout);
    }

    public boolean addFolder(String parent, String folder) throws IOException {
        checkWrite(parent);
        securityDatabase.createAcl(null, null, parent + "/" + folder,
                WRITE_FOLDER_PREFIX, false, false);
        securityDatabase.createAcl(null, null, parent + "/" + folder,
                READ_FOLDER_PREFIX, true, false);
        return database.addFolder(parent, folder);
    }

    public void deleteFolder(String folder) throws IOException {
        File file = getFile(folder);
        if (file.isDirectory() && (file.listFiles().length == 0)) {
            securityDatabase.deleteAcl(folder, WRITE_FOLDER_PREFIX);
            securityDatabase.deleteAcl(folder, READ_FOLDER_PREFIX);
            database.deleteFolder(folder);
        } else {
            throw new IOException("The folder must be empty to be deleted");
        }
    }

    public boolean canReadFolder(String folder) {
        return securityDatabase.isAllowed(folder, READ_FOLDER_PREFIX, true);
    }

    public boolean canWriteFolder(String folder) {
        return securityDatabase.isAllowed(folder, WRITE_FOLDER_PREFIX, false);
    }

    public Metadata getFolderMetadata(String folder) {
        checkRead(folder);
        return database.getFolderMetadata(folder);
    }

    public void setFolderMetadata(String folder, Metadata metadata)
            throws IOException {
        checkWrite(folder);
        database.setFolderMetadata(folder, metadata);
    }

    public void setFolderReadAcl(String folder, boolean isPublic,
            WriteOnlyEntity... exceptions) throws IOException {
        securityDatabase.setAcl(folder, READ_FOLDER_PREFIX, isPublic,
                exceptions);
    }

    public ReadOnlyACL getFolderReadAcl(String folder) {
        return securityDatabase.getAcl(folder, READ_FOLDER_PREFIX, true);
    }

    public void setFolderOwner(String folder, String newOwner)
            throws IOException {
        securityDatabase.setAclOwner(folder, READ_FOLDER_PREFIX, newOwner);
        securityDatabase.setAclOwner(folder, WRITE_FOLDER_PREFIX, newOwner);
    }
}
