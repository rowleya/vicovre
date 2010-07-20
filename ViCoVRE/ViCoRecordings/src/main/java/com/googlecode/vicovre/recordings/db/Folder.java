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
import org.xml.sax.SAXException;

import com.googlecode.vicovre.recordings.DefaultLayout;
import com.googlecode.vicovre.recordings.HarvestSource;
import com.googlecode.vicovre.recordings.Recording;
import com.googlecode.vicovre.recordings.UnfinishedRecording;

public interface Folder extends Comparable<Folder> {

    /**
     * Gets the file
     * @return The file
     */
    public File getFile();

    /**
     * Returns the name
     * @return the name
     */
    public String getName();

    /**
     * Returns the description
     * @return the description
     */
    public String getDescription();

    /**
     * Returns the folders
     * @return the folders
     */
    public List<Folder> getFolders();

    /**
     * Gets a recording
     * @param id The id of the recording
     * @return The recording or null if doesn't exist
     */
    public Recording getRecording(String id);

    /**
     * Returns the recordings
     * @return the recordings
     */
    public List<Recording> getRecordings();

    public UnfinishedRecording getUnfinishedRecording(String id);

    public List<UnfinishedRecording> getUnfinishedRecordings();

    public HarvestSource getHarvestSource(String id);

    public List<HarvestSource> getHarvestSources();

    public List<DefaultLayout> getDefaultLayouts() throws IOException,
            SAXException;
}
