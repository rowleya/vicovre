/**
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

package com.googlecode.vicovre.recorder.dialog.data;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.httpclient.methods.multipart.Part;

import com.googlecode.vicovre.media.protocol.memetic.RecordingConstants;
import com.googlecode.vicovre.recordings.Stream;
import com.googlecode.vicovre.recordings.db.insecure.StreamReader;

/**
 * @author Andrew G D Rowley
 * @version 1.0
 */
public class StreamsPart extends Part {

    private static final int BUFFER_SIZE = 8196;

    private List<Stream> streams = null;

    private File directory = null;

    private long totalBytes = 0;

    private long bytesTransferred = 0;

    private StreamProgress progress = null;

    /**
     * Creates a new StreamsPart
     * @param streams The streams to add
     * @param directory The directory to read from
     */
    public StreamsPart(List<Stream> streams, File directory) {
        this.streams = streams;
        this.directory = directory;
        for (Stream stream : streams) {
            totalBytes += stream.getBytes();
        }
    }

    /**
     * Sets the stream progress object
     * @param progress The progress object
     */
    public void setStreamProgress(StreamProgress progress) {
        this.progress = progress;
    }

    /**
     * @see org.apache.commons.httpclient.methods.multipart.Part#getCharSet()
     */
    public String getCharSet() {
        return null;
    }

    /**
     * @see org.apache.commons.httpclient.methods.multipart.Part#
     *     getContentType()
     */
    public String getContentType() {
        return "application/zip";
    }

    /**
     * @see org.apache.commons.httpclient.methods.multipart.Part#getName()
     */
    public String getName() {
        return "streams";
    }

    /**
     * @see org.apache.commons.httpclient.methods.multipart.Part#
     *     getTransferEncoding()
     */
    public String getTransferEncoding() {
        return null;
    }

    /**
     * @see org.apache.commons.httpclient.methods.multipart.Part#lengthOfData()
     */
    protected long lengthOfData() {
        return -1;
    }

    private void writeFile(File file, ZipOutputStream out) throws IOException {
        BufferedInputStream instream = new BufferedInputStream(
                new FileInputStream(file));
        byte[] indata = new byte[BUFFER_SIZE];
        int bytesRead = instream.read(indata);

        ZipEntry entry = new ZipEntry(file.getName());
        entry.setSize(file.length());
        entry.setTime(file.lastModified());
        out.putNextEntry(entry);
        while (bytesRead > 0) {
            out.write(indata, 0, bytesRead);
            bytesRead = instream.read(indata);
            if (bytesRead > 0) {
                bytesTransferred += bytesRead;
                if (progress != null) {
                    progress.updateProgress(totalBytes, bytesTransferred);
                }
            }
        }
        instream.close();
        out.closeEntry();
    }

    /**
     * @see org.apache.commons.httpclient.methods.multipart.Part#sendData(
     *     java.io.OutputStream)
     */
    protected void sendData(OutputStream output) throws IOException {
        ZipOutputStream out = new ZipOutputStream(output);
        bytesTransferred = 0;
        for (Stream stream : streams) {
            File streamFile = new File(directory, stream.getSsrc());
            File streamIndex = new File(directory, stream.getSsrc()
                    + RecordingConstants.STREAM_INDEX);
            writeFile(streamFile, out);
            writeFile(streamIndex, out);

            ZipEntry entry = new ZipEntry(stream.getSsrc()
                    + RecordingConstants.STREAM_METADATA);
            out.putNextEntry(entry);
            StreamReader.writeStreamMetadata(stream, out);
            out.closeEntry();
        }
        out.finish();
        out.flush();
    }

}
