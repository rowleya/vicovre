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

package com.googlecode.vicovre.web.rest;

import java.awt.Dimension;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.media.Format;
import javax.media.format.AudioFormat;
import javax.media.format.VideoFormat;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import com.googlecode.vicovre.web.convert.ConvertSession;
import com.googlecode.vicovre.web.convert.ImportStream;

@Provider
public class MediaResponseWriter
    implements MessageBodyWriter<ConvertSession> {

    public long getSize(ConvertSession session, Class<?> type,
            Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    public boolean isWriteable(Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType) {
        return ConvertSession.class.isAssignableFrom(type);
    }

    public void writeTo(ConvertSession session, Class<?> type,
            Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> headerMap, OutputStream entityStream)
            throws IOException, WebApplicationException {
        PrintWriter writer = new PrintWriter(entityStream);
        writer.println(
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        writer.println("<streams>");
        for (String streamId : session.getStreamIds()) {
            writer.println("<stream>");
            writer.println("<id>" + streamId + "</id>");

            ImportStream stream = session.getStream(streamId);
            for (int i = 0; i < stream.getNoStreams(); i++) {
                writer.println("<id>" + i + "</id>");

                Format format = stream.getFormat(i);
                if (format instanceof AudioFormat) {
                    writer.println("<audio>");
                    AudioFormat af = (AudioFormat) format;
                    writer.println("<samplerate>" + af.getSampleRate()
                            + "</samplerate>");
                    writer.println("<samplesize>" + af.getSampleSizeInBits()
                            + "</samplesize>");
                    writer.println("<channels>" + af.getChannels()
                            + "</channels>");
                    writer.println("</audio>");
                } else if (format instanceof VideoFormat) {
                    writer.println("<video>");
                    VideoFormat vf = (VideoFormat) format;
                    Dimension size = vf.getSize();
                    writer.println("<width>" + size.width + "</width>");
                    writer.println("<height>" + size.height + "</height>");
                    writer.println("</video>");
                }
            }

            writer.println("</stream>");
        }
        writer.println("</streams>");
        writer.flush();
        writer.close();
    }

}
