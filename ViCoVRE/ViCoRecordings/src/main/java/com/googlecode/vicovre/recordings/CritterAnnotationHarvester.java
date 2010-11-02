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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.googlecode.vicovre.annotations.Annotation;
import com.googlecode.vicovre.utils.XmlIo;

public class CritterAnnotationHarvester {

    private static final String USER = "[A-Za-z0-9_]{1,15}";

    private static final String TAG = "[A-Za-z0-9_-]+";

    private static final Pattern RESPONSE_PATTERN =
        Pattern.compile("^@(" + USER + ")");

    private static final Pattern TAG_PATTERN =
        Pattern.compile("^|\\s#(" + TAG + ")");

    private static final Pattern PERSON_PATTERN =
        Pattern.compile("^|\\s@(" + USER + ")");

    private static final String ACCEPT = "application/xml";

    private URL url = null;

    public CritterAnnotationHarvester(String url)
            throws MalformedURLException {
        this.url = new URL(url);
    }

    public List<CritterEvent> getEvents() throws IOException, SAXException {
        URLConnection connection = url.openConnection();
        connection.addRequestProperty("Accept", ACCEPT);
        Node doc = XmlIo.read(connection.getInputStream());
        Node[] eventNodes = XmlIo.readNodes(doc, "event");
        List<CritterEvent> events = new Vector<CritterEvent>();
        for (Node eventNode : eventNodes) {
            String id = XmlIo.readContent(eventNode, "name");
            String tag = XmlIo.readContent(eventNode, "tag");
            CritterEvent event = new CritterEvent(id, tag);
            events.add(event);
        }
        return events;
    }

    public List<Annotation> harvestAnnotations(String eventId)
            throws SAXException, IOException {
        URL eventUrl = new URL(url, eventId);
        URLConnection connection = eventUrl.openConnection();
        Node doc = XmlIo.read(connection.getInputStream());
        Node event = XmlIo.readNode(doc, "event");
        String eventTag = XmlIo.readContent(event, "tag");
        if (eventTag.startsWith("#")) {
            eventTag = eventTag.substring(1);
        }
        Node[] tweets = XmlIo.readNodes(event, "tweet");
        List<Annotation> annotations = new Vector<Annotation>();
        HashMap<String, String> lastAnnotationByAuthor =
            new HashMap<String, String>();
        for (Node tweet : tweets) {
            String id = "twitter" + XmlIo.readAttr(tweet, "id", null);
            long timestamp = Long.parseLong(XmlIo.readAttr(tweet, "time", "0"))
                * 1000;
            String author = XmlIo.readAttr(tweet, "screen_name", "");
            String message = tweet.getTextContent();
            String responseTo = null;
            Vector<String> tags = new Vector<String>();
            Vector<String> people = new Vector<String>();

            Matcher responseMatcher = RESPONSE_PATTERN.matcher(message);
            if (responseMatcher.matches()) {
                String person = responseMatcher.group(1);
                String lastAnnotation = lastAnnotationByAuthor.get(person);
                if (lastAnnotation != null) {
                    responseTo = lastAnnotation;
                }
            }

            Matcher tagMatcher = TAG_PATTERN.matcher(message);
            while (tagMatcher.matches()) {
                String tag = tagMatcher.group(1);
                if (!tag.equals(eventTag)) {
                    tags.add(tag);
                }
            }

            Matcher personMatcher = PERSON_PATTERN.matcher(message);
            while (personMatcher.matches()) {
                people.add(personMatcher.group(1));
            }

            Annotation annotation = new Annotation(id, timestamp, author,
                    message, tags.toArray(new String[0]),
                    people.toArray(new String[0]), responseTo);
            lastAnnotationByAuthor.put(author, id);
            annotations.add(annotation);
        }
        return annotations;
    }
}
