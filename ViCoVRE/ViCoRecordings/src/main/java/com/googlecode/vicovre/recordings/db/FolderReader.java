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
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Vector;

import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.googlecode.vicovre.media.protocol.memetic.RecordingConstants;
import com.googlecode.vicovre.recordings.Folder;
import com.googlecode.vicovre.recordings.HarvestSource;
import com.googlecode.vicovre.recordings.Recording;
import com.googlecode.vicovre.recordings.UnfinishedRecording;
import com.googlecode.vicovre.repositories.harvestFormat.HarvestFormatRepository;
import com.googlecode.vicovre.repositories.layout.LayoutRepository;
import com.googlecode.vicovre.repositories.rtptype.RtpTypeRepository;
import com.googlecode.vicovre.utils.XmlIo;

/**
 * A reader of folders
 * @author Andrew G D Rowley
 * @version 1.0
 */
public class FolderReader {

    public static Folder readFolder(File directory,
            RtpTypeRepository typeRepository, LayoutRepository layoutRepository,
            HarvestFormatRepository harvestFormatRepository,
            RecordingDatabase database)
            throws SAXException, IOException {
        Folder folder = new Folder(directory);
        File description = new File(directory, RecordingConstants.DESCRIPTION);
        if (description.exists()) {
            FileInputStream input = new FileInputStream(description);
            Node doc = XmlIo.read(input);
            XmlIo.setString(doc, folder, "name");
            XmlIo.setString(doc, folder, "description");
            input.close();
        } else {
            folder.setName(directory.getName());
        }

        Vector<Folder> subFolders = new Vector<Folder>();
        Vector<Recording> recordings = new Vector<Recording>();
        Vector<HarvestSource> harvestSources = new Vector<HarvestSource>();
        Vector<UnfinishedRecording> unfinishedRecordings =
            new Vector<UnfinishedRecording>();
        for (File file : directory.listFiles()) {
            try {
                if (file.isDirectory()) {
                    File recordingIndex = new File(file,
                            RecordingConstants.RECORDING_INDEX);
                    if (recordingIndex.exists()) {
                        FileInputStream input = new FileInputStream(recordingIndex);
                        Recording recording = RecordingReader.readRecording(input,
                                file, typeRepository, layoutRepository);
                        recordings.add(recording);
                        input.close();
                    } else {
                        Folder subFolder = readFolder(file,
                                typeRepository, layoutRepository,
                                harvestFormatRepository, database);
                        subFolders.add(subFolder);
                    }
                } else if (file.getName().endsWith(
                        RecordingConstants.HARVEST_SOURCE)) {
                    FileInputStream input = new FileInputStream(file);
                    HarvestSource harvestSource =
                        HarvestSourceReader.readHarvestSource(input,
                                harvestFormatRepository, typeRepository, folder);
                    harvestSource.setFile(file);
                    input.close();
                    harvestSources.add(harvestSource);
                } else if (file.getName().endsWith(
                        RecordingConstants.UNFINISHED_RECORDING_INDEX)) {

                    FileInputStream input = new FileInputStream(file);
                    UnfinishedRecording recording =
                        UnfinishedRecordingReader.readRecording(input, file,
                                folder, typeRepository, database);
                    input.close();
                    unfinishedRecordings.add(recording);
                }
            } catch (Exception e) {
                System.err.println("Warning: " + e.getMessage());
            }
        }
        folder.setFolders(subFolders);
        folder.setRecordings(recordings);
        folder.setHarvestSources(harvestSources);
        folder.setUnfinishedRecordings(unfinishedRecordings);
        return folder;
    }

    public static void writeFolder(Folder folder) throws IOException {
        if (!folder.getName().equals(folder.getFile().getName())
                || folder.getDescription() != null) {
            File description = new File(folder.getFile(),
                    RecordingConstants.DESCRIPTION);
            PrintWriter writer = new PrintWriter(description);
            writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            writer.println("<folder>");
            if (!folder.getName().equals(folder.getFile().getName())) {
                XmlIo.writeValue(folder, "name", writer);
            }
            if (folder.getDescription() != null) {
                XmlIo.writeValue(folder, "description", writer);
            }
            writer.println("</folder>");
        }
    }
}
