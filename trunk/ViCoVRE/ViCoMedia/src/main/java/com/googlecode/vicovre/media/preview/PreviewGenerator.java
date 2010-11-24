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

package com.googlecode.vicovre.media.preview;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;

import javax.imageio.ImageIO;
import javax.media.Buffer;
import javax.media.Format;
import javax.media.Renderer;
import javax.media.format.RGBFormat;
import javax.media.format.UnsupportedFormatException;

import com.googlecode.vicovre.media.MemeticFileReader;
import com.googlecode.vicovre.media.processor.SimpleProcessor;
import com.googlecode.vicovre.repositories.rtptype.RtpTypeRepository;

public class PreviewGenerator implements Renderer {

    private static final HashMap<String, Long> PRE_SEEK =
        new HashMap<String, Long>();
    static {
        PRE_SEEK.put("h261/rtp", 30000000000L);
        PRE_SEEK.put("h261as/rtp", 30000000000L);
    }

    private DecimalFormat FORMAT = new DecimalFormat("00");

    private Format[] inputFormats = null;

    private File directory = null;

    private String prefix = null;

    private int imageCount = 0;

    private PreviewGenerator(File directory, String prefix) {
        this.directory = directory;
        this.prefix = prefix;
        inputFormats = new Format[]{
                new RGBFormat(null, Format.NOT_SPECIFIED,
                Format.intArray, Format.NOT_SPECIFIED,
                32, 0xFF0000, 0xFF00, 0xFF, 1,
                Format.NOT_SPECIFIED, Format.FALSE,
                Format.NOT_SPECIFIED)
            };
    }

    public Format[] getSupportedInputFormats() {
        return inputFormats;
    }

    public int process(Buffer buffer) {
        RGBFormat format = (RGBFormat) buffer.getFormat();
        Dimension size = format.getSize();
        imageCount += 1;
        BufferedImage image = new BufferedImage(size.width, size.height,
                BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, image.getWidth(), image.getHeight(),
                (int[]) buffer.getData(), 0, image.getWidth());
        File output = new File(directory,
                prefix + FORMAT.format(imageCount) + ".png");
        try {
            ImageIO.write(image, "PNG", output);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return BUFFER_PROCESSED_OK;
    }

    public Format setInputFormat(Format input) {
        if (input.matches(inputFormats[0])) {
            return input;
        }
        return null;
    }

    public void start() {
        // Does Nothing
    }

    public void stop() {
        // Does Nothing
    }

    public void close() {
        // Does Nothing
    }

    public String getName() {
        return "PreviewGenerator";
    }

    public void open() {
        // Does Nothing
    }

    public void reset() {
        // Does Nothing
    }

    public Object getControl(String className) {
        return null;
    }

    public Object[] getControls() {
        return new Object[0];
    }

    public static void generate(long duration, String streamFile,
            RtpTypeRepository typeRepository, File directory, String prefix,
            int noImages)
            throws IOException, UnsupportedFormatException {
        long nsBetweenImages = duration / noImages;

        PreviewGenerator generator = new PreviewGenerator(directory, prefix);
        MemeticFileReader reader = new MemeticFileReader(
                streamFile, typeRepository);
        Long preSeek = PRE_SEEK.get(reader.getFormat().getEncoding());
        if (preSeek == null) {
            preSeek = 0L;
        }

        for (int i = 0; i < noImages; i++) {
            SimpleProcessor processor = new SimpleProcessor(reader.getFormat(),
                    generator.getSupportedInputFormats()[0]);
            long seekTime = nsBetweenImages * i;
            reader.streamSeek((seekTime - preSeek) / 1000000);
            if (reader.readNextPacket()) {
                Buffer inputBuffer = reader.getBuffer();
                int result = processor.process(inputBuffer);

                reader.setTimestampOffset(seekTime - preSeek);
                while ((inputBuffer.getTimeStamp() < seekTime)
                        && reader.readNextPacket()) {
                    inputBuffer = reader.getBuffer();
                    result = processor.process(inputBuffer);
                }

                while (((result == OUTPUT_BUFFER_NOT_FILLED)
                        || (result == BUFFER_PROCESSED_FAILED))
                        && reader.readNextPacket()) {
                    inputBuffer = reader.getBuffer();
                    result = processor.process(inputBuffer);
                }
                if ((result == BUFFER_PROCESSED_OK)
                        || (result == INPUT_BUFFER_NOT_CONSUMED)) {
                    generator.process(processor.getOutputBuffer());
                }
            }

            processor.close();
        }

    }

}
