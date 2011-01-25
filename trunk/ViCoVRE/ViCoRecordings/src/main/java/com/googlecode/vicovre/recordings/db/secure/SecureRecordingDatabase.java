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

import com.googlecode.vicovre.annotations.Annotation;
import com.googlecode.vicovre.recordings.DefaultLayout;
import com.googlecode.vicovre.recordings.HarvestSource;
import com.googlecode.vicovre.recordings.Metadata;
import com.googlecode.vicovre.recordings.Recording;
import com.googlecode.vicovre.recordings.UnfinishedRecording;
import com.googlecode.vicovre.recordings.db.RecordingDatabase;
import com.googlecode.vicovre.recordings.db.insecure.HarvestSourceListener;
import com.googlecode.vicovre.recordings.db.insecure.RecordingListener;
import com.googlecode.vicovre.recordings.db.insecure.UnfinishedRecordingListener;
import com.googlecode.vicovre.security.UnauthorizedException;
import com.googlecode.vicovre.security.db.ReadOnlyACL;
import com.googlecode.vicovre.security.db.ReadOnlyEntity;
import com.googlecode.vicovre.security.db.Role;
import com.googlecode.vicovre.security.db.SecurityDatabase;
import com.googlecode.vicovre.security.db.UserListener;
import com.googlecode.vicovre.security.db.WriteOnlyEntity;

public class SecureRecordingDatabase implements RecordingDatabase,
        UserListener {

    public static final String HARVEST_ID_PREFIX = "harvestSource";

    public static final String READ_RECORDING_ID_PREFIX = "readRecording";

    public static final String CHANGE_RECORDING_ID_PREFIX = "editRecording";

    public static final String ANNOTATE_RECORDING_ID_PREFIX =
        "annotateRecording";

    public static final String PLAY_RECORDING_ID_PREFIX = "playRecording";

    public static final String UNFINISHED_ID_PREFIX = "unfinishedRecording";

    public static final String READ_FOLDER_PREFIX = "readFolder";

    public static final String WRITE_FOLDER_PREFIX = "writeFolder";

    public static final String ANNOTATION_ID_PREFIX = "annotation";

    public static final String UNFINISHED_PREFIX = "unfinished";

    private RecordingDatabase database = null;

    private SecurityDatabase securityDatabase = null;

    private boolean defaultRecordingPlayPermission = false;

    private boolean defaultRecordingReadPermission = true;

    private boolean defaultRecordingAnnotationPermission = false;

    private boolean defaultFolderReadPermission = true;

    private boolean rootWritable = true;

    public SecureRecordingDatabase(RecordingDatabase database,
            SecurityDatabase securityDatabase) throws IOException {
        this.database = database;
        this.securityDatabase = securityDatabase;
        File homeFolder = getFile("home");
        homeFolder.mkdirs();
        securityDatabase.setUserHome("/home", WRITE_FOLDER_PREFIX,
                READ_FOLDER_PREFIX);
        securityDatabase.addUserListener(this);
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

    private void checkWrite(String creatorFolder, String creatorId,
            String folder) {
        if (!canWriteFolder(creatorFolder, creatorId, folder)) {
            throw new UnauthorizedException(
                "You do not have permission to write to this folder");
        }
    }

    public List<String> getSubFolders(String folder) {
        checkRead(folder);
        List<String> subfolders = database.getSubFolders(folder);
        if (subfolders != null) {
            Vector<String> authSubfolders = new Vector<String>();
            for (String subFolder : subfolders) {
                File subFolderFile = new File(folder, subFolder);
                if (canReadFolder(subFolderFile.getPath())) {
                    authSubfolders.add(subFolder);
                }
            }
            return authSubfolders;
        }
        return null;
    }

    public void addHarvestSource(HarvestSource harvestSource)
            throws IOException {
        checkWrite(null, null, harvestSource.getFolder());
        securityDatabase.createAcl(null, null, harvestSource.getFolder(),
                HARVEST_ID_PREFIX + harvestSource.getId(), false, true,
                Role.WRITER);
        createRecordingAcl(null, null, harvestSource.getFolder(),
                harvestSource.getId(), READ_RECORDING_ID_PREFIX,
                defaultRecordingReadPermission);
        createRecordingAcl(null, null, harvestSource.getFolder(),
                harvestSource.getId(), PLAY_RECORDING_ID_PREFIX,
                defaultRecordingPlayPermission);
        createRecordingAcl(null, null, harvestSource.getFolder(),
                harvestSource.getId(), ANNOTATE_RECORDING_ID_PREFIX,
                defaultRecordingAnnotationPermission);
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
        securityDatabase.deleteAcl(harvestSource.getFolder(),
                READ_RECORDING_ID_PREFIX + harvestSource.getId());
        securityDatabase.deleteAcl(harvestSource.getFolder(),
                PLAY_RECORDING_ID_PREFIX + harvestSource.getId());
        securityDatabase.deleteAcl(harvestSource.getFolder(),
                ANNOTATE_RECORDING_ID_PREFIX + harvestSource.getId());
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

    private void createRecordingAcl(String creatorFolder, String creatorId,
            String folder, String recordingId,
            String prefix, boolean allow) throws IOException {
        securityDatabase.createAcl(creatorFolder,
                UNFINISHED_ID_PREFIX + creatorId,
                folder, prefix + recordingId,
                allow, true, Role.WRITER);
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
        checkWrite(creatorFolder, HARVEST_ID_PREFIX + creatorId, folder);

        securityDatabase.createAcl(creatorFolder,
                HARVEST_ID_PREFIX + creatorId, folder,
                UNFINISHED_ID_PREFIX + recording.getId(), false, true,
                Role.WRITER);

        if (creator != null) {
            copyAcl(creatorFolder, creatorId, creatorFolder,
                    READ_RECORDING_ID_PREFIX + creatorId, folder,
                    recording.getId(), defaultRecordingReadPermission, true,
                    Role.WRITER);
            copyAcl(creatorFolder, creatorId, creatorFolder,
                    PLAY_RECORDING_ID_PREFIX + creatorId, folder,
                    recording.getId(), defaultRecordingPlayPermission, true,
                    Role.WRITER);
            copyAcl(creatorFolder, creatorId, creatorFolder,
                    ANNOTATE_RECORDING_ID_PREFIX + creatorId, folder,
                    recording.getId(), defaultRecordingAnnotationPermission,
                    true, Role.WRITER);
        } else {
            createRecordingAcl(creatorFolder, creatorId, folder,
                    recording.getId(), READ_RECORDING_ID_PREFIX,
                    defaultRecordingReadPermission);
            createRecordingAcl(creatorFolder, creatorId, folder,
                    recording.getId(), PLAY_RECORDING_ID_PREFIX,
                    defaultRecordingPlayPermission);
            createRecordingAcl(creatorFolder, creatorId, folder,
                    recording.getId(), ANNOTATE_RECORDING_ID_PREFIX,
                    defaultRecordingAnnotationPermission);
        }
        database.addUnfinishedRecording(recording, creator);
    }

    public void updateUnfinishedRecording(UnfinishedRecording recording)
            throws IOException {
        String folder = recording.getFolder();
        String id = UNFINISHED_ID_PREFIX + recording.getId();
        if (!securityDatabase.isAllowed(folder, id, false)) {
            throw new UnauthorizedException(
                    "Only the owner of the recording can edit it");
        }
        database.updateUnfinishedRecording(recording);
    }

    public void finishUnfinishedRecording(UnfinishedRecording recording)
            throws IOException {
        String folder = recording.getFolder();
        securityDatabase.deleteAcl(folder,
                UNFINISHED_ID_PREFIX + recording.getId());
        securityDatabase.deleteAcl(folder,
                READ_RECORDING_ID_PREFIX + recording.getId());
        securityDatabase.deleteAcl(folder,
                PLAY_RECORDING_ID_PREFIX + recording.getId());
        securityDatabase.deleteAcl(folder,
                ANNOTATE_RECORDING_ID_PREFIX + recording.getId());
        database.finishUnfinishedRecording(recording);
    }

    public void deleteUnfinishedRecording(UnfinishedRecording recording)
            throws IOException {
        String folder = recording.getFolder();
        securityDatabase.deleteAcl(folder,
                UNFINISHED_ID_PREFIX + recording.getId());
        securityDatabase.deleteAcl(folder,
                READ_RECORDING_ID_PREFIX + recording.getId());
        securityDatabase.deleteAcl(folder,
                PLAY_RECORDING_ID_PREFIX + recording.getId());
        securityDatabase.deleteAcl(folder,
                ANNOTATE_RECORDING_ID_PREFIX + recording.getId());
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


    private WriteOnlyEntity[] convert(ReadOnlyACL acl) {
        List<ReadOnlyEntity> aclExceptions = acl.getExceptions();
        WriteOnlyEntity[] exceptions =
            new WriteOnlyEntity[aclExceptions.size()];
        for (int i = 0; i < exceptions.length; i++) {
            ReadOnlyEntity entity = aclExceptions.get(i);
            exceptions[i] = new WriteOnlyEntity(entity.getName(),
                    entity.getType());
        }
        return exceptions;
    }

    private void copyAcl(String creatorFolder, String creatorId,
            String oldfolder, String oldId, String newfolder, String newId,
            boolean allowByDefault, boolean canProxy, Role requiredRole)
            throws IOException {
        ReadOnlyACL acl = securityDatabase.getAcl(oldfolder, oldId,
                allowByDefault);
        WriteOnlyEntity[] exceptions = convert(acl);
        securityDatabase.createAcl(creatorFolder, creatorId, newfolder, newId,
                acl.isAllow(), canProxy, requiredRole, exceptions);
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
        checkWrite(creatorFolder, UNFINISHED_ID_PREFIX + creatorId, folder);
        securityDatabase.createAcl(creatorFolder,
                UNFINISHED_ID_PREFIX + creatorId, folder,
                CHANGE_RECORDING_ID_PREFIX + recording.getId(), false, true,
                Role.WRITER);
        if (creator != null) {
            copyAcl(creatorFolder, UNFINISHED_ID_PREFIX + creatorId, folder,
                    READ_RECORDING_ID_PREFIX + creatorId, folder,
                    READ_RECORDING_ID_PREFIX + recording.getId(),
                    defaultRecordingReadPermission, true, Role.WRITER);
            copyAcl(creatorFolder, UNFINISHED_ID_PREFIX + creatorId, folder,
                    PLAY_RECORDING_ID_PREFIX + creatorId, folder,
                    PLAY_RECORDING_ID_PREFIX + recording.getId(),
                    defaultRecordingPlayPermission, true, Role.WRITER);
            copyAcl(creatorFolder, UNFINISHED_ID_PREFIX + creatorId, folder,
                    ANNOTATE_RECORDING_ID_PREFIX + creatorId, folder,
                    ANNOTATE_RECORDING_ID_PREFIX + recording.getId(),
                    defaultRecordingAnnotationPermission, true, Role.WRITER);
        } else {
            securityDatabase.createAcl(null, null, folder,
                    READ_RECORDING_ID_PREFIX + recording.getId(),
                    defaultRecordingReadPermission, true, Role.WRITER);
            securityDatabase.createAcl(null, null, folder,
                    PLAY_RECORDING_ID_PREFIX + recording.getId(),
                    defaultRecordingPlayPermission, true, Role.WRITER);
            securityDatabase.createAcl(null, null, folder,
                    ANNOTATE_RECORDING_ID_PREFIX + recording.getId(),
                    defaultRecordingAnnotationPermission, true, Role.WRITER);
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
        securityDatabase.deleteAcl(recording.getFolder(),
                ANNOTATE_RECORDING_ID_PREFIX + recording.getId());
        database.deleteRecording(recording);
    }

    public Recording getRecording(String folder, String id) {
        if (!securityDatabase.isAllowed(folder, READ_RECORDING_ID_PREFIX + id,
                     defaultRecordingReadPermission)
                && !securityDatabase.isAllowed(folder,
                        PLAY_RECORDING_ID_PREFIX + id,
                        defaultRecordingPlayPermission)) {
            throw new UnauthorizedException(
                    "You are not allowed to see this recording");
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
                              READ_RECORDING_ID_PREFIX + recording.getId(),
                              defaultRecordingReadPermission)
                       || securityDatabase.isAllowed(folder,
                              PLAY_RECORDING_ID_PREFIX + recording.getId(),
                              defaultRecordingPlayPermission)) {
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

    public void updateRecordingAnnotations(Recording recording)
            throws IOException {
        if (!securityDatabase.isAllowed(recording.getFolder(),
                ANNOTATE_RECORDING_ID_PREFIX + recording.getId(), false)) {
            throw new UnauthorizedException(
                    "You do not have permission to do this");
        }
        database.updateRecordingAnnotations(recording);
    }

    public boolean canEditRecording(String folder, String id) {
        return securityDatabase.isAllowed(folder,
                CHANGE_RECORDING_ID_PREFIX + id, false);
    }

    public boolean canPlayRecording(String folder, String id) {
        return securityDatabase.isAllowed(folder,
                PLAY_RECORDING_ID_PREFIX + id, defaultRecordingPlayPermission);
    }

    public boolean canReadRecording(String folder, String id) {
        return securityDatabase.isAllowed(folder,
                READ_RECORDING_ID_PREFIX + id, defaultRecordingReadPermission);
    }

    public boolean canAnnotateRecording(String folder, String id) {
        return securityDatabase.isAllowed(folder,
                ANNOTATE_RECORDING_ID_PREFIX + id,
                defaultRecordingAnnotationPermission);
    }

    public void shutdown() {
        database.shutdown();
    }

    public void setRecordingReadAcl(Recording recording, boolean isPublic)
            throws IOException {
        securityDatabase.setAcl(recording.getFolder(),
                READ_RECORDING_ID_PREFIX + recording.getId(), isPublic);
    }

    public void setRecordingPlayAcl(Recording recording, boolean isPublic,
            WriteOnlyEntity... exceptions) throws IOException {
        securityDatabase.setAcl(recording.getFolder(),
                PLAY_RECORDING_ID_PREFIX + recording.getId(),
                isPublic, exceptions);
    }

    public void setRecordingAnnotateAcl(Recording recording, boolean isPublic,
            WriteOnlyEntity... exceptions) throws IOException {
        securityDatabase.setAcl(recording.getFolder(),
                ANNOTATE_RECORDING_ID_PREFIX + recording.getId(),
                isPublic, exceptions);
    }

    public ReadOnlyACL getRecordingReadAcl(Recording recording) {
        return securityDatabase.getAcl(recording.getFolder(),
                READ_RECORDING_ID_PREFIX + recording.getId(),
                defaultRecordingReadPermission);
    }

    public ReadOnlyACL getRecordingPlayAcl(Recording recording) {
        return securityDatabase.getAcl(recording.getFolder(),
                PLAY_RECORDING_ID_PREFIX + recording.getId(),
                defaultRecordingPlayPermission);
    }

    public ReadOnlyACL getRecordingAnnotateAcl(Recording recording) {
        return securityDatabase.getAcl(recording.getFolder(),
                ANNOTATE_RECORDING_ID_PREFIX + recording.getId(),
                defaultRecordingAnnotationPermission);
    }

    public void setRecordingReadAcl(HarvestSource source,
            boolean isPublic)
            throws IOException {
        String folder = source.getFolder();
        securityDatabase.setAcl(folder,
            READ_RECORDING_ID_PREFIX + source.getId(), isPublic);
    }

    public void setRecordingPlayAcl(HarvestSource source,
            boolean isPublic, WriteOnlyEntity... exceptions)
            throws IOException {
        String folder = source.getFolder();
        securityDatabase.setAcl(folder,
            PLAY_RECORDING_ID_PREFIX + source.getId(),
            isPublic, exceptions);
    }

    public void setRecordingAnnotateAcl(HarvestSource source,
            boolean isPublic, WriteOnlyEntity... exceptions)
            throws IOException {
        String folder = source.getFolder();
        securityDatabase.setAcl(folder,
            ANNOTATE_RECORDING_ID_PREFIX + source.getId(),
            isPublic, exceptions);
    }

    public ReadOnlyACL getRecordingReadAcl(HarvestSource source) {
        return securityDatabase.getAcl(source.getFolder(),
                READ_RECORDING_ID_PREFIX + source.getId(),
                defaultRecordingReadPermission);
    }

    public ReadOnlyACL getRecordingPlayAcl(HarvestSource source) {
        return securityDatabase.getAcl(source.getFolder(),
                PLAY_RECORDING_ID_PREFIX + source.getId(),
                defaultRecordingPlayPermission);
    }

    public ReadOnlyACL getRecordingAnnotateAcl(HarvestSource source) {
        return securityDatabase.getAcl(source.getFolder(),
                ANNOTATE_RECORDING_ID_PREFIX + source.getId(),
                defaultRecordingAnnotationPermission);
    }

    public void setRecordingReadAcl(UnfinishedRecording recording,
            boolean isPublic)
            throws IOException {
        String folder = recording.getFolder();
        securityDatabase.setAcl(folder,
            READ_RECORDING_ID_PREFIX + recording.getId(), isPublic);
    }

    public void setRecordingPlayAcl(UnfinishedRecording recording,
            boolean isPublic, WriteOnlyEntity... exceptions)
            throws IOException {
        String folder = recording.getFolder();
        securityDatabase.setAcl(folder,
            PLAY_RECORDING_ID_PREFIX + recording.getId(),
            isPublic, exceptions);
    }

    public void setRecordingAnnotateAcl(UnfinishedRecording recording,
            boolean isPublic, WriteOnlyEntity... exceptions)
            throws IOException {
        String folder = recording.getFolder();
        securityDatabase.setAcl(folder,
            ANNOTATE_RECORDING_ID_PREFIX + recording.getId(),
            isPublic, exceptions);
    }

    public ReadOnlyACL getRecordingReadAcl(UnfinishedRecording recording) {
        return securityDatabase.getAcl(recording.getFolder(),
                READ_RECORDING_ID_PREFIX + recording.getId(),
                defaultRecordingReadPermission);
    }

    public ReadOnlyACL getRecordingPlayAcl(UnfinishedRecording recording) {
        return securityDatabase.getAcl(recording.getFolder(),
                PLAY_RECORDING_ID_PREFIX + recording.getId(),
                defaultRecordingPlayPermission);
    }

    public ReadOnlyACL getRecordingAnnotateAcl(UnfinishedRecording recording) {
        return securityDatabase.getAcl(recording.getFolder(),
                ANNOTATE_RECORDING_ID_PREFIX + recording.getId(),
                defaultRecordingAnnotationPermission);
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
         checkWrite(null, null, folder);
         database.setDefaultLayout(folder, layout);
    }

    public boolean addFolder(String parent, String folder) throws IOException {
        checkWrite(null, null, parent);
        securityDatabase.createAcl(null, null, parent + "/" + folder,
                READ_FOLDER_PREFIX, defaultFolderReadPermission, false,
                Role.WRITER);
        securityDatabase.createAcl(null, null, parent + "/" + folder,
                WRITE_FOLDER_PREFIX, false, false, Role.WRITER);
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
        boolean defaultPermission = defaultFolderReadPermission;
        if ((folder == null) || folder.equals("")) {
            defaultPermission = true;
        }
        return securityDatabase.isAllowed(folder, READ_FOLDER_PREFIX,
                defaultPermission);
    }

    private boolean canWriteFolder(String creatorFolder, String creatorId,
            String folder) {
        boolean defaultPermission = false;
        if ((folder == null) || folder.equals("")) {
            defaultPermission = rootWritable;
        }
        return securityDatabase.isAllowed(creatorFolder, creatorId, folder,
                WRITE_FOLDER_PREFIX, defaultPermission);
    }

    public boolean canWriteFolder(String folder) {
        boolean defaultPermission = false;
        if ((folder == null) || folder.equals("")) {
            defaultPermission = rootWritable;
        }
        return securityDatabase.isAllowed(folder, WRITE_FOLDER_PREFIX,
                defaultPermission);
    }

    public Metadata getFolderMetadata(String folder) {
        checkRead(folder);
        return database.getFolderMetadata(folder);
    }

    public void setFolderMetadata(String folder, Metadata metadata)
            throws IOException {
        checkWrite(null, null, folder);
        database.setFolderMetadata(folder, metadata);
    }

    public void setFolderReadAcl(String folder, boolean isPublic,
            WriteOnlyEntity... exceptions) throws IOException {
        securityDatabase.setAcl(folder, READ_FOLDER_PREFIX, isPublic,
                exceptions);
    }

    public ReadOnlyACL getFolderReadAcl(String folder) {
        return securityDatabase.getAcl(folder, READ_FOLDER_PREFIX,
                defaultFolderReadPermission);
    }

    public String getFolderOwner(String folder) {
        return securityDatabase.getOwner(folder, WRITE_FOLDER_PREFIX);
    }

    public void setFolderOwner(String folder, String newOwner)
            throws IOException {
        for (String subfolder : database.getSubFolders(folder)) {
            setFolderOwner(folder + "/" + subfolder, newOwner);
        }
        for (HarvestSource source : database.getHarvestSources(folder)) {
            securityDatabase.setAclOwner(folder,
                    HARVEST_ID_PREFIX + source.getId(), newOwner);
            securityDatabase.setAclOwner(folder,
                    READ_RECORDING_ID_PREFIX + source.getId(), newOwner);
            securityDatabase.setAclOwner(folder,
                    PLAY_RECORDING_ID_PREFIX + source.getId(), newOwner);
            securityDatabase.setAclOwner(folder,
                    ANNOTATE_RECORDING_ID_PREFIX + source.getId(), newOwner);
        }
        for (UnfinishedRecording recording :
                database.getUnfinishedRecordings(folder)) {
            securityDatabase.setAclOwner(folder,
                    UNFINISHED_ID_PREFIX + recording.getId(), newOwner);
            securityDatabase.setAclOwner(folder,
                    READ_RECORDING_ID_PREFIX + recording.getId(), newOwner);
            securityDatabase.setAclOwner(folder,
                    PLAY_RECORDING_ID_PREFIX + recording.getId(), newOwner);
            securityDatabase.setAclOwner(folder,
                    ANNOTATE_RECORDING_ID_PREFIX + recording.getId(), newOwner);
        }
        for (Recording recording : database.getRecordings(folder)) {
            securityDatabase.setAclOwner(folder,
                    CHANGE_RECORDING_ID_PREFIX + recording.getId(), newOwner);
            securityDatabase.setAclOwner(folder,
                    PLAY_RECORDING_ID_PREFIX + recording.getId(), newOwner);
            securityDatabase.setAclOwner(folder,
                    READ_RECORDING_ID_PREFIX + recording.getId(), newOwner);
            securityDatabase.setAclOwner(folder,
                    ANNOTATE_RECORDING_ID_PREFIX + recording.getId(), newOwner);
        }
        securityDatabase.setAclOwner(folder, READ_FOLDER_PREFIX, newOwner);
        securityDatabase.setAclOwner(folder, WRITE_FOLDER_PREFIX, newOwner);
    }

    public long getFolderLifetime(String folder) {
        checkRead(folder);
        return database.getFolderLifetime(folder);
    }

    public void setFolderLifetime(String folder, long lifetime)
            throws IOException {
        if (!securityDatabase.hasRole(Role.ADMINISTRATOR)) {
            throw new UnauthorizedException(
                "Only an administrator can update the lifetime of a folder");
        }
        database.setFolderLifetime(folder, lifetime);
    }

    public void addAnnotation(Recording recording,
            Annotation annotation) throws IOException {
        if (!canAnnotateRecording(recording.getFolder(), recording.getId())) {
            throw new UnauthorizedException(
                    "You do not have permission to annotate this recording");
        }
        securityDatabase.createAcl(recording.getFolder(), recording.getId(),
                ".annotations", ANNOTATION_ID_PREFIX + recording.getId()
                    + annotation.getId(), false,
                false, Role.AUTHUSER);
        database.addAnnotation(recording, annotation);
    }

    public void deleteAnnotation(Recording recording,
            Annotation annotation) throws IOException {
        securityDatabase.deleteAcl(".annotations",
                ANNOTATION_ID_PREFIX + recording.getId() + annotation.getId());
        database.deleteAnnotation(recording, annotation);
    }

    public void updateAnnotation(Recording recording,
            Annotation annotation) throws IOException {
        if (!securityDatabase.isAllowed(".annotations",
                ANNOTATION_ID_PREFIX + recording.getId() + annotation.getId(),
                false)) {
            throw new UnauthorizedException(
                    "Only the owner of an annotation can edit it");
        }
        database.updateAnnotation(recording, annotation);
    }

    public void moveRecording(Recording recording, String newFolder)
            throws IOException {
        checkWrite(null, null, recording.getFolder());
        checkWrite(null, null, newFolder);
        database.moveRecording(recording, newFolder);
        copyAcl(null, null, recording.getFolder(),
                CHANGE_RECORDING_ID_PREFIX + recording.getId(), newFolder,
                CHANGE_RECORDING_ID_PREFIX + recording.getId(),
                false, true, Role.WRITER);
        copyAcl(null, null, recording.getFolder(),
                READ_RECORDING_ID_PREFIX + recording.getId(), newFolder,
                READ_RECORDING_ID_PREFIX + recording.getId(),
                defaultRecordingReadPermission, true, Role.WRITER);
        copyAcl(null, null, recording.getFolder(),
                PLAY_RECORDING_ID_PREFIX + recording.getId(), newFolder,
                PLAY_RECORDING_ID_PREFIX + recording.getId(),
                defaultRecordingPlayPermission, true, Role.WRITER);
        copyAcl(null, null, recording.getFolder(),
                ANNOTATE_RECORDING_ID_PREFIX + recording.getId(), newFolder,
                ANNOTATE_RECORDING_ID_PREFIX + recording.getId(),
                defaultRecordingAnnotationPermission, true, Role.WRITER);
        securityDatabase.deleteAcl(recording.getFolder(),
                CHANGE_RECORDING_ID_PREFIX + recording.getId());
        securityDatabase.deleteAcl(recording.getFolder(),
                READ_RECORDING_ID_PREFIX + recording.getId());
        securityDatabase.deleteAcl(recording.getFolder(),
                PLAY_RECORDING_ID_PREFIX + recording.getId());
        securityDatabase.deleteAcl(recording.getFolder(),
                ANNOTATE_RECORDING_ID_PREFIX + recording.getId());
    }

    public void addUser(String username, Role role, String homeFolder) {
        if (role.equals(Role.WRITER) || role.equals(Role.ADMINISTRATOR)) {
            if (homeFolder != null) {
                File home = getFile(homeFolder);
                home.mkdirs();
            }
        }
    }

    public void changeRole(String username, Role role, String homeFolder) {
        addUser(username, role, homeFolder);
    }

    public void deleteUser(String username, Role role, String homeFolder) {
        File home = getFile(homeFolder);
        home.delete();
    }
}
