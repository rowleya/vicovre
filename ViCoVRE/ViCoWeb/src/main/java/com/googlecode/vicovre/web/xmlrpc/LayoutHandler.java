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
import java.util.Vector;

import org.apache.xmlrpc.XmlRpcException;

import com.googlecode.vicovre.recordings.Folder;
import com.googlecode.vicovre.recordings.Recording;
import com.googlecode.vicovre.recordings.ReplayLayout;
import com.googlecode.vicovre.recordings.Stream;
import com.googlecode.vicovre.recordings.db.RecordingDatabase;
import com.googlecode.vicovre.repositories.layout.Layout;
import com.googlecode.vicovre.repositories.layout.LayoutPosition;
import com.googlecode.vicovre.repositories.layout.LayoutRepository;
import com.googlecode.vicovre.repositories.rtptype.RtpTypeRepository;
import com.googlecode.vicovre.media.Misc;
import com.googlecode.vicovre.media.screencapture.ScreenChangeDetector;

/**
 * Handler of layouts.
 * @author Andrew G D Rowley
 * @version 1.0
 */
public class LayoutHandler extends AbstractHandler {

    private LayoutRepository layoutRepository = null;

    private RtpTypeRepository typeRepository = null;

    /**
     * Creates a LayoutHandler.
     * @param database The database
     * @param layoutRepository The layout repository
     */
    public LayoutHandler(final RecordingDatabase database,
            final LayoutRepository layoutRepository,
            final RtpTypeRepository typeRepository) {
        super(database);
        this.layoutRepository = layoutRepository;
        this.typeRepository = typeRepository;
    }

    /**
     * Gets the layouts.
     * @return The layouts
     */
    public List<Map<String, Object>> getLayouts() {
        List<Layout> layouts = layoutRepository.findLayouts();
        Vector<Map<String, Object>> layoutList =
            new Vector<Map<String, Object>>();
        for (Layout layout : layouts) {
            Map<String, Object> layoutMap = new HashMap<String, Object>();
            layoutMap.put("name", layout.getName());
            List<LayoutPosition> positions = layout.getStreamPositions();
            List<Map<String, Object>> positionList =
                new Vector<Map<String, Object>>();
            for (LayoutPosition position : positions) {
                Map<String, Object> positionMap = new HashMap<String, Object>();
                positionMap.put("name", position.getName());
                positionMap.put("x", position.getX());
                positionMap.put("y", position.getY());
                positionMap.put("width", position.getWidth());
                positionMap.put("height", position.getHeight());
                positionMap.put("assignable", position.isAssignable());
                positionList.add(positionMap);
            }
            layoutMap.put("positions", positionList);
            layoutList.add(layoutMap);
        }
        return layoutList;
    }

    /**
     * Sets the layouts.
     * @param folderPath The path of the folder containing the recording
     * @param recordingId The id of the recording
     * @param layouts The layouts to set
     * @return true
     * @throws XmlRpcException if there is a problem
     */
    public Boolean setLayouts(final String folderPath, final String recordingId,
            final Object[] layouts) throws XmlRpcException {
        Folder folder = getFolder(folderPath);
        final Recording recording = folder.getRecording(recordingId);
        if (recording == null) {
            throw new XmlRpcException("Unknown recording " + recordingId);
        }

        List<ReplayLayout> replayLayouts = new Vector<ReplayLayout>();
        for (Object layoutObject : layouts) {
            Map<String, Object> layoutMap = (Map<String, Object>) layoutObject;
            String layoutName = (String) layoutMap.get("name");
            Integer time = (Integer) layoutMap.get("time");
            Integer endTime = (Integer) layoutMap.get("endTime");
            Map<String, Object> positions = (Map<String, Object>)
                layoutMap.get("positions");
            Object[] audioStreamList =
                (Object[]) layoutMap.get("audioStreams");

            Layout layout = layoutRepository.findLayout(layoutName);
            if (layout == null) {
                throw new XmlRpcException("Unknown layout " + layoutName);
            }

            ReplayLayout replayLayout = new ReplayLayout(layoutRepository);
            replayLayout.setName(layoutName);
            replayLayout.setRecording(recording);
            replayLayout.setTime(time.longValue());
            replayLayout.setEndTime(endTime.longValue());
            for (LayoutPosition position : layout.getStreamPositions()) {
                if (position.isAssignable()) {
                    String streamId = (String) positions.get(
                            position.getName());
                    if (streamId == null) {
                        throw new XmlRpcException(
                                "No stream specified for position "
                                + position.getName());
                    }
                    final Stream stream = recording.getStream(streamId);
                    if (stream == null) {
                        throw new XmlRpcException("Stream " + streamId
                                + "not found");
                    }
                    replayLayout.setStream(position.getName(), stream);

                    if (position.hasChanges()) {
                        try {
                            if (!Misc.isCodecsConfigured()) {
                                Misc.configureCodecs("/knownCodecs.xml");
                            }
                            Thread changeThread = new Thread() {
                                public void run() {
                                    try {
                                        ScreenChangeDetector changeDetector =
                                            new ScreenChangeDetector(
                                                recording.getDirectory(),
                                                stream.getSsrc(),
                                                typeRepository);
                                        changeDetector.run();
                                        changeDetector.close();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            };
                            changeThread.start();
                        } catch (Exception e) {
                            e.printStackTrace();
                            throw new XmlRpcException(e.getMessage());
                        }
                    }
                }
            }
            for (Object streamId : audioStreamList) {
                Stream stream = recording.getStream((String) streamId);
                if (stream == null) {
                    throw new XmlRpcException("Stream " + streamId
                            + "not found");
                }
                replayLayout.addAudioStream(stream);
            }

            replayLayouts.add(replayLayout);
        }
        recording.setReplayLayouts(null);
        recording.setReplayLayouts(replayLayouts);
        try {
            getDatabase().updateRecordingLayouts(recording);
        } catch (IOException e) {
            e.printStackTrace();
            throw new XmlRpcException(e.getMessage());
        }

        return true;
    }

}
