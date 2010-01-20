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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Vector;

import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.googlecode.vicovre.media.protocol.memetic.RecordingConstants;
import com.googlecode.vicovre.recordings.Folder;
import com.googlecode.vicovre.recordings.Recording;
import com.googlecode.vicovre.recordings.RecordingMetadata;
import com.googlecode.vicovre.recordings.ReplayLayout;
import com.googlecode.vicovre.recordings.Stream;
import com.googlecode.vicovre.repositories.layout.LayoutRepository;
import com.googlecode.vicovre.repositories.rtptype.RtpTypeRepository;
import com.googlecode.vicovre.utils.ExtensionFilter;
import com.googlecode.vicovre.utils.XmlIo;

/**
 * Reads a recording from an index
 *
 * @author Andrew G D Rowley
 * @version 1.0
 */
public class RecordingReader {

    /**
     * Reads a recording
     * @param input The stream from which to read the input
     * @return The recording read
     * @throws IOException
     * @throws SAXException
     */
    public static Recording readRecording(InputStream input, Folder folder,
            RtpTypeRepository typeRepository, LayoutRepository layoutRepository)
            throws SAXException, IOException {
        Node doc = XmlIo.read(input);
        String id = XmlIo.readValue(doc, "id");
        Recording recording = new Recording(folder, id);

        String[] pauseTimes = XmlIo.readValues(doc, "pauseTime");
        for (String time : pauseTimes) {
            recording.addPauseTime(Long.valueOf(time));
        }

        File directory = recording.getDirectory();

        Vector<Stream> streams = new Vector<Stream>();
        File[] streamFiles = directory.listFiles(
                new ExtensionFilter(RecordingConstants.STREAM_INDEX));
        for (File file : streamFiles) {
            try {
                String ssrc = file.getName();
                ssrc = ssrc.substring(0, ssrc.indexOf(
                        RecordingConstants.STREAM_INDEX));
                Stream stream = StreamReader.readStream(directory, ssrc,
                        typeRepository);
                streams.add(stream);
            } catch (Exception e) {
                System.err.println("Warning: error reading stream " + file);
                e.printStackTrace();
            }
        }
        recording.setStreams(streams);

        Vector<ReplayLayout> layouts = new Vector<ReplayLayout>();
        File[] layoutFiles = directory.listFiles(
                new ExtensionFilter(RecordingConstants.LAYOUT));
        for (File file : layoutFiles) {
            try {
                FileInputStream inputLayout = new FileInputStream(file);
                ReplayLayout layout = LayoutReader.readLayout(inputLayout,
                        layoutRepository, recording);
                inputLayout.close();
                layouts.add(layout);
            } catch (Exception e) {
                System.err.println("Warning: error reading layout " + file);
                e.printStackTrace();
            }
        }
        recording.setReplayLayouts(layouts);

        File metadataFile = new File(directory, RecordingConstants.METADATA);
        if (metadataFile.exists()) {
            try {
                FileInputStream inputStream = new FileInputStream(metadataFile);
                RecordingMetadata metadata = RecordingMetadataReader.readMetadata(
                        inputStream);
                recording.setMetadata(metadata);
                inputStream.close();
            } catch (Exception e) {
                System.err.println("Warning: error reading metadata "
                        + metadataFile);
                e.printStackTrace();
            }
        }
        recording.updateTimes();
        return recording;
    }

    public static void writeRecording(Recording recording,
            OutputStream output) throws IOException {
        PrintWriter writer = new PrintWriter(output);
        writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        writer.println("<recording>");
        XmlIo.writeValue(recording, "id", writer);

        for (Long time : recording.getPauseTimes()) {
            XmlIo.writeValue("pauseTime", time.toString(), writer);
        }

        for (Stream stream : recording.getStreams()) {
            File streamOutput = new File(recording.getDirectory(),
                     stream.getSsrc() + RecordingConstants.STREAM_METADATA);
            FileOutputStream outputStream = new FileOutputStream(streamOutput);
            StreamReader.writeStreamMetadata(stream, outputStream);
            outputStream.close();
        }

        for (ReplayLayout layout : recording.getReplayLayouts()) {
            File layoutOutput = new File(recording.getDirectory(),
                    layout.getName() + RecordingConstants.LAYOUT);
            FileOutputStream outputLayout = new FileOutputStream(layoutOutput);
            LayoutReader.writeLayout(layout, outputLayout);
            outputLayout.close();
        }

        RecordingMetadata metadata = recording.getMetadata();
        if (metadata != null) {
            File metadataFile = new File(recording.getDirectory(),
                    RecordingConstants.METADATA);
            FileOutputStream outputStream = new FileOutputStream(metadataFile);
            RecordingMetadataReader.writeMetadata(metadata, outputStream);
            outputStream.close();
        }

        writer.println("</recording>");
        writer.flush();
    }

}
