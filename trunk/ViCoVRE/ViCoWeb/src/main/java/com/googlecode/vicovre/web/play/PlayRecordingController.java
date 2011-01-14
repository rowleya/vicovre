/*
 * Copyright (c) 2008, University of Bristol
 * Copyright (c) 2008, University of Manchester
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
 * 3) Neither the names of the University of Bristol and the
 *    University of Manchester nor the names of their
 *    contributors may be used to endorse or promote products derived from this
 *    software without specific prior written permission.
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

package com.googlecode.vicovre.web.play;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import com.googlecode.vicovre.annotations.Annotation;
import com.googlecode.vicovre.recordings.CritterAnnotationHarvester;
import com.googlecode.vicovre.recordings.Recording;
import com.googlecode.vicovre.recordings.ReplayLayoutPosition;
import com.googlecode.vicovre.recordings.Stream;
import com.googlecode.vicovre.recordings.ReplayLayout;
import com.googlecode.vicovre.recordings.db.RecordingDatabase;
import com.googlecode.vicovre.repositories.layout.LayoutRepository;
import com.googlecode.vicovre.utils.URLLinkReplacer;
import com.googlecode.vicovre.web.play.metadata.MetadataAnnotation;
import com.googlecode.vicovre.web.play.metadata.MetadataAnnotationType;
import com.googlecode.vicovre.web.play.metadata.MetadataLayout;
import com.googlecode.vicovre.web.play.metadata.MetadataLayoutPosition;
import com.googlecode.vicovre.web.play.metadata.TextThumbnail;
import com.googlecode.vicovre.web.play.metadata.ThumbSorter;
import com.googlecode.vicovre.web.play.metadata.ThumbFileSorter;
import com.googlecode.vicovre.web.play.metadata.Thumbnail;

public class PlayRecordingController implements Controller {

    private static final String[] TAGS = new String[] {
        "Question", "Answer", "Link"
    };

    private static final String[] COLOURS = new String[] {
        "#61C4DC", "#FFFF99", "#5CD43A"
    };

    private static final byte[] FLV_TYPE = new byte[] {0x46, 0x4C, 0x56};

    private static final String[] SLIDE_COLOURS = {"#0000ff", "#ff0000"};

    private static final int SLIDE_INDEX = 0;

    private static final String SLIDE_TYPE = "Slide";

    private static final String DEFAULT_COLOUR = "#FFFFFF";

    private static final String DEFAULT_TYPE = "Note";

    private static final int FLV_DATA_TAG = 0x12;

    private static final int FLV_VERSION = 0x1;

    private static final int DATA_OFFSET = 0x9;

    private static final int BIT_SHIFT_0 = 0;

    private static final int BIT_SHIFT_8 = 8;

    private static final int BIT_SHIFT_16 = 16;

    private static final int BYTE_MASK = 0xFF;

    private RecordingDatabase database = null;

    private LayoutRepository layoutRepository = null;

    private String imageLocation = null;

    private CritterAnnotationHarvester critterHarvester = null;

    public PlayRecordingController(RecordingDatabase database,
            LayoutRepository layoutRepository, String imageLocation,
            CritterAnnotationHarvester critterHarvester) {
        this.database = database;
        this.layoutRepository = layoutRepository;
        this.imageLocation = imageLocation;
        this.critterHarvester = critterHarvester;
    }

    private Vector<Thumbnail> getSlides(String server, Recording recording,
            final String ssrc, long minStart, long maxEnd) {
        Vector<Thumbnail> thumb = new Vector<Thumbnail>();

        File dir = recording.getDirectory();
        String folder = recording.getFolder();

        FilenameFilter filter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return (name.startsWith(ssrc) && name.endsWith(".png")
                        && !name.contains("preview"));
            }
        };

        File[] children = dir.listFiles(filter);
        if (children != null) {
            Arrays.sort(children, new ThumbFileSorter(ssrc));
            Stream stream = recording.getStream(ssrc);
            long streamStartOffset = stream.getStartTime().getTime()
                - recording.getStartTime().getTime();
            long lastStart = (stream.getEndTime().getTime()
                - recording.getStartTime().getTime()) + streamStartOffset;
            if (lastStart > maxEnd) {
                lastStart = maxEnd;
            }
            for (int i = children.length - 1; i >= 0; i--) {
                String fname = children[i].getName();
                String[] fparts = fname.split("[_.]");
                if (fparts.length < 2) {
                    continue;
                }
                long start = Long.parseLong(fparts[1]) + streamStartOffset;
                if (start <= maxEnd) {
                    if (start < minStart) {
                        start = minStart;
                    }
                    long offsetStart = (start - minStart) / 1000;
                    long offsetEnd = (lastStart - minStart) / 1000;
                    String imgname = "/image.do?id=" + recording.getId()
                            + "&ssrc=" + ssrc + "&offset=" + fparts[1]
                            + "&folder=" + folder;
                    thumb.add(new Thumbnail(offsetStart, offsetEnd,
                            server + imgname));
                    lastStart = start;
                }
                if (start <= minStart) {
                    break;
                }
            }
        }
        return thumb;
    }

    // Converts an int into a 3-byte array
    private byte[] intTo24Bits(int value) {
        return new byte[] {(byte) ((value >> BIT_SHIFT_16) & BYTE_MASK),
                (byte) ((value >> BIT_SHIFT_8) & BYTE_MASK),
                (byte) ((value >> BIT_SHIFT_0) & BYTE_MASK)};
    }

    private void writeDataItem(DataOutputStream out, Object value)
            throws IOException {
        if (value == null) {
            out.write(2);
            out.writeShort(0);
        } else {
            Class < ? > cls = value.getClass();
            if (value instanceof String) {
                out.write(2);
                out.writeUTF((String) value);
            } else if (value instanceof Boolean) {
                out.write(1);
                if ((Boolean) value) {
                    out.write(1);
                } else {
                    out.write(0);
                }
            } else if (value instanceof Double) {
                out.write(0);
                out.writeDouble((Double) value);
            } else if (value instanceof Integer) {
                out.write(0);
                out.writeDouble((Integer) value);
            } else if (value instanceof Float) {
                out.write(0);
                out.writeDouble((Float) value);
            } else if (value instanceof Long) {
                out.write(0);
                out.writeDouble((Long) value);
            } else if (cls.isArray()) {
                int length = Array.getLength(value);
                out.write(8);
                out.writeInt(length);
                for (int i = 0; i < length; i++) {
                    out.writeUTF(String.valueOf(i));
                    writeDataItem(out, Array.get(value, i));
                }
                out.writeShort(0);
                out.write(9);
            } else if (value instanceof Collection<?>) {
                Collection< ? > c = (Collection < ? >) value;
                out.write(8);
                out.writeInt(c.size());
                Iterator < ? > iter = c.iterator();
                int i = 0;
                while (iter.hasNext()) {
                    out.writeUTF(String.valueOf(i));
                    writeDataItem(out, iter.next());
                    i++;
                }
                out.writeShort(0);
                out.write(9);
            } else if (value instanceof Map<?, ?>) {
                Map< ? , ? > m = (Map< ? , ? >) value;
                out.write(8);
                out.writeInt(m.size());
                Iterator< ? > iter = m.keySet().iterator();
                while (iter.hasNext()) {
                    String name = (String) iter.next();
                    out.writeUTF(name);
                    writeDataItem(out, m.get(name));
                }
            } else {
                out.write(3);
                while ((cls != null) && !cls.equals(Object.class)) {
                    Method[] methods = cls.getDeclaredMethods();
                    for (int i = 0; i < methods.length; i++) {
                        String name = methods[i].getName();
                        int mod = methods[i].getModifiers();
                        if (Modifier.isPublic(mod) && !Modifier.isStatic(mod)
                                && (methods[i].getParameterTypes().length == 0)) {
                            String var = null;
                            if (name.startsWith("get")) {
                                var = name.substring(3);
                            } else if (name.startsWith("is")) {
                                var = name.substring(2);
                            }
                            if (var != null) {
                                try {
                                    Object result = methods[i].invoke(value,
                                            new Object[0]);
                                    var = var.substring(0, 1).toLowerCase()
                                            + var.substring(1);
                                    out.writeUTF(var);
                                    writeDataItem(out, result);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                    cls = cls.getSuperclass();
                }
                out.writeShort(0);
                out.write(9);
            }
        }
    }

    public ModelAndView handleRequest(HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        Vector<MetadataAnnotation> metadataAnnotations =
            new Vector<MetadataAnnotation>();
        Vector<MetadataAnnotation> slideAnnotations =
            new Vector<MetadataAnnotation>();
        Vector<Thumbnail> thumbs = new Vector<Thumbnail>();
        Vector<Thumbnail> slideThumbs = new Vector<Thumbnail>();
        Vector<MetadataAnnotationType> metadataAnnotationTypes =
            new Vector<MetadataAnnotationType>();
        Vector<MetadataLayout> metadataLayouts = new Vector<MetadataLayout>();

        String server = request.getScheme() + "://" + request.getServerName()
                + ":" + request.getServerPort() + request.getContextPath();
        long duration = 0;
        long startTime = 0;
        String folder = request.getParameter("folder");
        System.err.println("Folder = " + folder);
        String recordingId = request.getParameter("recordingId");
        String stTime = request.getParameter("startTime");
        if (stTime != null) {
            try {
                startTime = Long.parseLong(stTime);
            } catch (NumberFormatException e) {
                startTime = 0;
            }
        }

        long minStart = Long.MAX_VALUE;
        if (recordingId != null) {
            Recording recording = database.getRecording(folder, recordingId);

            // Get the real start and end time based on the layout
            long maxEnd = 0;
            List<ReplayLayout> replayLayouts = recording.getReplayLayouts();
            Collections.sort(replayLayouts);
            ReplayLayout minStartLayout = null;
            ReplayLayout maxEndLayout = null;
            for (ReplayLayout replayLayout : replayLayouts) {
                if (replayLayout.getTime() < minStart) {
                    minStart = replayLayout.getTime();
                    minStartLayout = replayLayout;
                }
                if (replayLayout.getEndTime() > maxEnd) {
                    maxEnd = replayLayout.getEndTime();
                    maxEndLayout = replayLayout;
                }
                MetadataLayout metadataLayout = new MetadataLayout(
                        layoutRepository, recording, replayLayout);
                metadataLayouts.add(metadataLayout);
            }
            minStart = Long.MAX_VALUE;
            for (ReplayLayoutPosition position :
                    minStartLayout.getLayoutPositions()) {
                Stream stream = position.getStream();
                long streamStartOffset = stream.getStartTime().getTime()
                    - recording.getStartTime().getTime();
                if (streamStartOffset < minStart) {
                    minStart = streamStartOffset;
                }
            }
            if (minStart < minStartLayout.getTime()) {
                minStart = minStartLayout.getTime();
            }
            maxEnd = 0;
            for (ReplayLayoutPosition position :
                maxEndLayout.getLayoutPositions()) {
                Stream stream = position.getStream();
                long streamEndOffset = stream.getEndTime().getTime()
                    - recording.getStartTime().getTime();
                if (streamEndOffset > maxEnd) {
                    maxEnd = streamEndOffset;
                }
            }
            if (maxEnd > maxEndLayout.getEndTime()) {
                maxEnd = maxEndLayout.getEndTime();
            }
            if (maxEnd == 0) {
                maxEnd = recording.getDuration();
            }
            duration = maxEnd - minStart;

            // Get the slide changes
            for (MetadataLayout lay : metadataLayouts) {
                for (MetadataLayoutPosition layPos : lay.getLayoutPositions()) {
                    if (layPos.isChanges()) {
                        slideThumbs = getSlides(server, recording,
                                layPos.getStream().getSsrc(), minStart, maxEnd);
                    }
                }
            }

            String imageDir = server + imageLocation;
            if (!imageDir.endsWith("/")) {
                imageDir += "/";
            }

            if (slideThumbs.size() != 0) {
                MetadataAnnotationType matype = new MetadataAnnotationType(
                        SLIDE_TYPE, imageDir + SLIDE_TYPE + ".jpg",
                        SLIDE_TYPE, SLIDE_INDEX);
                if (!metadataAnnotationTypes.contains(matype)) {
                    metadataAnnotationTypes.add(matype);
                }
                int colourindex = 0;
                for (Thumbnail thumb : slideThumbs) {
                    slideAnnotations.add(
                        new MetadataAnnotation(thumb.getStart(), thumb.getEnd(),
                                SLIDE_TYPE, "",  SLIDE_COLOURS[colourindex]));
                    colourindex = (colourindex + 1) % SLIDE_COLOURS.length;
                }
            } else {
                MetadataAnnotationType type = new MetadataAnnotationType(
                        SLIDE_TYPE, "", "", SLIDE_INDEX);
                if (!metadataAnnotationTypes.contains(type)) {
                    metadataAnnotationTypes.add(type);
                }
            }

            List<Annotation> annotations = recording.getAnnotations();
            String critterEvent = recording.getMetadata().getValue(
                    "critterEvent");
            if ((critterEvent != null) && (critterHarvester != null)) {
                List<Annotation> critterAnnotations =
                    critterHarvester.harvestAnnotations(critterEvent);
                annotations.addAll(critterAnnotations);
            }

            if ((annotations != null) && !annotations.isEmpty()) {
                long annLength = duration / 100;
                for (Annotation annotation : annotations) {
                    double start = (annotation.getTimestamp()
                            - recording.getStartTime().getTime()) / 1000;
                    double end = start + (annLength / 1000);
                    boolean addAnnotation = true;
                    String text = URLLinkReplacer.format(
                            annotation.getMessage());

                    String type = DEFAULT_TYPE;
                    int index = TAGS.length + 1;
                    String colour = DEFAULT_COLOUR;
                    for (int i = 0; i < TAGS.length; i++) {
                        if (annotation.hasTag(TAGS[i])) {
                            index = i + 1;
                            type = TAGS[i];
                            colour = COLOURS[i];
                            break;
                        }
                    }

                    if (annotation.hasTag(SLIDE_TYPE)) {
                        for (MetadataAnnotation slideAnnotation
                                : slideAnnotations) {
                            if ((slideAnnotation.getStart() >= start)
                                    && (slideAnnotation.getEnd() <= end)) {
                                slideAnnotation.setText(text);
                                addAnnotation = false;
                            }
                        }
                    }

                    MetadataAnnotation metaAnn = new MetadataAnnotation(
                            start, end, type, text, colour);
                    if (addAnnotation &&
                            !metadataAnnotations.contains(metaAnn)) {
                        metadataAnnotations.add(metaAnn);
                    }

                    String image = imageDir + type + ".jpg";
                    TextThumbnail textThumb = new TextThumbnail(start, end,
                            image, text, type);
                    if (!thumbs.contains(textThumb)) {
                        thumbs.add(textThumb);
                    }
                    MetadataAnnotationType matype =
                        new MetadataAnnotationType(type, image,
                                type, index);
                    if (!metadataAnnotationTypes.contains(matype)) {
                        metadataAnnotationTypes.add(matype);
                    }
                }
            }
        }

        metadataAnnotations.addAll(slideAnnotations);
        thumbs.addAll(slideThumbs);
        Collections.sort(thumbs, new ThumbSorter());

        // for-loop sets the filename of an annotation which has no value at the
        // time to the filename of the last annotation that has one.
        if (thumbs.size() > 0) {
            String filename = thumbs.get(0).getFilename();
            for (Thumbnail thumb : thumbs) {
                String thumbFilename = thumb.getFilename();
                if (thumbFilename == null) {
                    thumb.setFilename(filename);
                } else {
                    filename = thumbFilename;
                }
            }
        }

        String autoGain = request.getParameter("agc");
        if (autoGain == null) {
            autoGain = "false";
        }

        String url = server + "/" + folder + "/" + recordingId
            + "/downloadRecording.do?format=video%2Fx-flv&start=" + minStart
            + "&x=0&y=0&opacity=1.0&agc=" + autoGain;

        HashMap<String, Object> values = new HashMap<String, Object>();
        Collections.sort(metadataAnnotationTypes);
        values.put("startTime", startTime);
        values.put("duration", duration);
        values.put("annotationTypes", metadataAnnotationTypes);
        values.put("layouts", metadataLayouts);
        values.put("annotations", metadataAnnotations);
        values.put("thumbnails", thumbs);
        values.put("url", url);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream data = new DataOutputStream(bytes);

        // OnMetaData
        data.write(2);
        data.writeUTF("onMetaData");

        // Write the data out
        writeDataItem(data, values);
        data.close();
        bytes.close();
        response.setContentType("video/x-flv");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Cache-Control", "must-revalidate");
        response.setDateHeader("Expires", System.currentTimeMillis());
        DataOutputStream out = new DataOutputStream(response.getOutputStream());

        // Write a header
        out.write(FLV_TYPE);
        out.write(FLV_VERSION);
        out.write(0);
        out.writeInt(DATA_OFFSET);

        // Header
        byte[] metadata = bytes.toByteArray();
        out.writeInt(0);
        out.write(FLV_DATA_TAG);
        out.write(intTo24Bits(metadata.length));
        out.writeInt(0);
        out.write(intTo24Bits(0));

        // Write the data
        out.write(metadata);
        out.writeInt(metadata.length + 11);
        out.close();
        return null;
    }

}
