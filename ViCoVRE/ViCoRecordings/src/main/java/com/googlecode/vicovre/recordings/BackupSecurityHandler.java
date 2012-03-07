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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

import com.googlecode.vicovre.recordings.db.secure.SecureRecordingDatabase;
import com.googlecode.vicovre.security.db.ACLListener;
import com.googlecode.vicovre.security.db.SecurityDatabase;

public class BackupSecurityHandler implements ACLListener {

    private static final long RETRY_TIMEOUT = 5 * 60 * 1000;

    private File securityDirectory = null;

    private File backupDirectory = null;

    private HashSet<String> performingOperation = new HashSet<String>();

    private byte[] buffer = new byte[8096];

    private class RetryTask extends TimerTask {

        private String folder = null;

        private String id = null;

        public RetryTask(String folder, String id) {
            this.folder = folder;
            this.id = id;
        }

        public void run() {
            ACLCreated(folder, id);
        }
    }

    public BackupSecurityHandler(String securityDir, String backupDir,
            SecurityDatabase database, boolean enabled) {
        if (enabled) {
            securityDirectory = new File(securityDir);
            backupDirectory = new File(backupDir);
            backupDirectory.mkdirs();
            database.addACLListener(this);
        }
    }

    private File getACLBackupFile(String folder, String id) {
        File aclFolder = new File(backupDirectory, folder);
        return new File(aclFolder, id + ".acl");
    }

    private File getACLFile(String folder, String id) {
        File aclFolder = new File(securityDirectory, folder);
        return new File(aclFolder, id + ".acl");
    }

    private void startOperation(String folder, String id) {
        synchronized (performingOperation) {
            while (performingOperation.contains(folder + "/" + id)) {
                try {
                    performingOperation.wait();
                } catch (InterruptedException e) {
                    // Do Nothing
                }
            }
            performingOperation.add(folder + "/" + id);
        }
    }

    private void finishOperation(String folder, String id) {
        synchronized (performingOperation) {
            performingOperation.remove(folder + "/" + id);
            performingOperation.notifyAll();
        }
    }

    private void copyAcl(String folder, String id) throws IOException {
        File backupAclFile = getACLBackupFile(folder, id);
        backupAclFile.getParentFile().mkdirs();
        FileOutputStream output = new FileOutputStream(backupAclFile);
        FileInputStream input = new FileInputStream(getACLFile(folder, id));
        int bytesRead = input.read(buffer);
        while (bytesRead != -1) {
            output.write(buffer, 0, bytesRead);
            bytesRead = input.read(buffer);
        }
        input.close();
        output.close();
    }

    private boolean isBackedUp(String id) {
        if (!id.startsWith(SecureRecordingDatabase.PLAY_RECORDING_ID_PREFIX) &&
                !id.startsWith(
                    SecureRecordingDatabase.CHANGE_RECORDING_ID_PREFIX) &&
                 !id.startsWith(
                     SecureRecordingDatabase.READ_RECORDING_ID_PREFIX) &&
                 !id.startsWith(
                     SecureRecordingDatabase.ANNOTATE_RECORDING_ID_PREFIX) &&
                 !id.startsWith(SecureRecordingDatabase.READ_FOLDER_PREFIX) &&
                 !id.startsWith(SecureRecordingDatabase.WRITE_FOLDER_PREFIX)) {
            return false;
        }
        return true;
    }

    public void ACLCreated(String folder, String id) {
        if (!isBackedUp(id)) {
            return;
        }
        startOperation(folder, id);
        try {
            copyAcl(folder, id);
        } catch (IOException e) {
            System.err.println("Warning - error creating backup of new ACL "
                    + folder + "/" + id + ": " + e.getMessage());
            Timer timer = new Timer();
            timer.schedule(new RetryTask(folder, id), RETRY_TIMEOUT);
        }
        finishOperation(folder, id);
    }

    public void ACLDeleted(String folder, String id) {
        if (!isBackedUp(id)) {
            return;
        }
        startOperation(folder, id);
        File backupAcl = getACLBackupFile(folder, id);
        backupAcl.delete();
        finishOperation(folder, id);
    }

    public void ACLUpdated(String folder, String id) {
        if (!isBackedUp(id)) {
            return;
        }
        startOperation(folder, id);
        try {
            copyAcl(folder, id);
        } catch (IOException e) {
            System.err.println("Warning - error creating backup of updated ACL "
                    + folder + "/" + id + ": " + e.getMessage());
            Timer timer = new Timer();
            timer.schedule(new RetryTask(folder, id), RETRY_TIMEOUT);
        }
        finishOperation(folder, id);
    }
}