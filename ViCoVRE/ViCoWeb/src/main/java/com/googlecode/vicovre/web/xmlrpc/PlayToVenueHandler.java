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

package com.googlecode.vicovre.web.xmlrpc;

import org.apache.xmlrpc.XmlRpcException;

import com.googlecode.vicovre.recordings.PlaybackManager;
import com.googlecode.vicovre.recordings.Recording;
import com.googlecode.vicovre.recordings.db.Folder;
import com.googlecode.vicovre.recordings.db.RecordingDatabase;

public class PlayToVenueHandler extends AbstractHandler {

    public PlayToVenueHandler(RecordingDatabase database) {
        super(database);
    }

    public Integer play(String folderPath, String id, String ag3VenueUrl,
            int seek) throws XmlRpcException {
        Folder folder = getFolder(folderPath);
        Recording recording = folder.getRecording(id);
        return PlaybackManager.play(recording, ag3VenueUrl, seek);
    }

    public Boolean stop(int id) {
        PlaybackManager.stop(id);
        return true;
    }

    public Boolean pause(int id) {
        PlaybackManager.pause(id);
        return true;
    }

    public Boolean resume(int id) {
        PlaybackManager.resume(id);
        return true;
    }

    public Boolean seek(int id, int seek) {
        PlaybackManager.seek(id, seek);
        return true;
    }

    public Integer getTime(int id) {
        return PlaybackManager.getTime(id);
    }

}
