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

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.xmlrpc.XmlRpcException;

import com.googlecode.vicovre.recordings.Folder;
import com.googlecode.vicovre.recordings.Recording;
import com.googlecode.vicovre.recordings.Stream;
import com.googlecode.vicovre.recordings.db.RecordingDatabase;

public class RecordingHandler extends AbstractHandler {

    public RecordingHandler(RecordingDatabase database) {
        super(database);
    }

    public Map<String, Object>[] getRecordings(String folderPath)
            throws XmlRpcException {
        Folder folder = getFolder(folderPath);
        List<Recording> recordings = folder.getRecordings();
        Map<String, Object>[] recs = new Map[recordings.size()];
        for (int i = 0; i < recordings.size(); i++) {
            Recording rec = recordings.get(i);
            recs[i] = new HashMap<String, Object>();
            recs[i].put("id", rec.getId());
            recs[i].put("metadata", getDetails(rec.getMetadata()));
            recs[i].put("startTime", rec.getStartTime());
            recs[i].put("duration", (int) rec.getDuration());
            List<Stream> streams = rec.getStreams();
            Map<String, Object>[] strms = new Map[streams.size()];
            for (int j = 0; j < streams.size(); j++) {
                Stream stream = streams.get(j);
                strms[j] = getDetails(stream, "rtpType", "recording");
            }
            recs[i].put("streams", strms);
            List<Long> pauseTimes = rec.getPauseTimes();
            Object[] pTimes = new Object[pauseTimes.size()];
            for (int j = 0; j < pauseTimes.size(); j++) {
                pTimes[j] = pauseTimes.get(j).intValue();
            }
            recs[i].put("pauseTimes", pTimes);
        }
        return recs;
    }

    public Boolean updateMetadata(String folderPath, String id,
            Map<String, Object> details) throws XmlRpcException {
        Folder folder = getFolder(folderPath);
        Recording recording = folder.getRecording(id);
        if (recording == null) {
            throw new XmlRpcException("Recording " + id + " not found");
        }
        fillIn(recording.getMetadata(), details);
        try {
            getDatabase().updateRecordingMetadata(recording);
        } catch (IOException e) {
            e.printStackTrace();
            throw new XmlRpcException(e.getMessage());
        }
        return true;
    }

    public Boolean deleteRecording(String folderPath, String id)
            throws XmlRpcException {
        Folder folder = getFolder(folderPath);
        Recording recording = folder.getRecording(id);
        if (recording == null) {
            throw new XmlRpcException("Recording " + id + " not found");
        }
        getDatabase().deleteRecording(recording);
        return true;
    }
}
