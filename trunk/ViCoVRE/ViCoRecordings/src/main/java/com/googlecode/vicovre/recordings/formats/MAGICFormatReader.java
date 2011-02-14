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

package com.googlecode.vicovre.recordings.formats;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Vector;

import org.w3c.dom.Node;

import com.googlecode.vicovre.recordings.HarvestedEvent;
import com.googlecode.vicovre.recordings.Metadata;
import com.googlecode.vicovre.repositories.harvestFormat.HarvestFormatReader;
import com.googlecode.vicovre.repositories.harvestFormat.HarvestedItem;
import com.googlecode.vicovre.utils.XmlIo;

public class MAGICFormatReader implements HarvestFormatReader {

    private static final SimpleDateFormat DATE_FORMAT =
        new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public HarvestedItem[] readItems(InputStream input) throws IOException {
        try {
            Vector<HarvestedEvent> events = new Vector<HarvestedEvent>();
            Node doc = XmlIo.read(input);
            Node[] nodes = XmlIo.readNodes(doc, "magic_event");
            for (Node node : nodes) {
                try {
                    HarvestedEvent event = new HarvestedEvent();
                    event.setStartDate(DATE_FORMAT.parse(
                            XmlIo.readContent(node, "dtstart")));
                    event.setEndDate(DATE_FORMAT.parse(
                            XmlIo.readContent(node, "dtend")));

                    String type = XmlIo.readContent(node, "type");
                    String name = XmlIo.readContent(node, "summary");
                    String url = XmlIo.readContent(node, "url");
                    String location = XmlIo.readContent(node, "location");

                    Metadata metadata =
                        new Metadata("name", name);
                    metadata.setValue("type", type, false, true, false);
                    metadata.setValue("url", url, false, true, false);
                    metadata.setValue("location", location, false, true, false);
                    type = type.substring(0, 1).toUpperCase()
                        + type.substring(1);

                    if (type.equalsIgnoreCase("regular lecture")
                            || type.equalsIgnoreCase("extra lecture")) {
                        String courseCode = XmlIo.readContent(node,
                                "course_code");
                        metadata.setValue("courseCode",
                                courseCode,
                                false, true, false);
                        metadata.setValue("courseTitle",
                                XmlIo.readContent(node, "course_title"),
                                false, true, false);
                        metadata.setValue("lecturerName",
                                XmlIo.readContent(node, "lecturer_name"),
                                false, true, false);
                        String description =
                            "<b><a target='_blank' href='${url}'>"
                                + "${coursCode}: ${courseTitle}</a></b>\n"
                                + "${type} given by ${lecturerName}"
                                + " at ${location}";
                        metadata.setValue("description", description,
                                true, false, true);
                        event.setSubFolder(courseCode);
                    } else if (type.equals("extra event")) {
                        metadata.setValue("organiserName",
                                XmlIo.readContent(node, "orgainser_name"),
                                false, true, true);
                        metadata.setValue("speakerName",
                                XmlIo.readContent(node, "speaker_name"),
                                false, true, true);
                        String description =
                            "<b><a target='_blank' href='${url}'>"
                            + "Extra Event: ${speakerName}</a></b>\n"
                            + "${type} organised by ${organiserName}"
                            + "at ${location}";
                        metadata.setValue("description", description,
                                true, false, true);
                    } else {
                        System.err.println("Warning - Unknown event type "
                                + type);
                    }

                    event.setMetadata(metadata);
                    if (event.getEndDate().getTime()
                            > System.currentTimeMillis()) {
                        events.add(event);
                    }
                } catch (Exception e) {
                    System.err.println("Warning: error reading metadata");
                    e.printStackTrace();
                }
            }
            return events.toArray(new HarvestedEvent[0]);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

}
