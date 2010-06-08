/*
 * @(#)VideoExtractor.java
 * Created: 23 Nov 2007
 * Version: 1.0
 * Copyright (c) 2005-2006, University of Manchester All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer. Redistributions in binary
 * form must reproduce the above copyright notice, this list of conditions and
 * the following disclaimer in the documentation and/or other materials
 * provided with the distribution. Neither the name of the University of
 * Manchester nor the names of its contributors may be used to endorse or
 * promote products derived from this software without specific prior written
 * permission.
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
 */

package com.googlecode.vicovre.web.play;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.imageio.ImageIO;
import javax.media.Buffer;
import javax.media.Multiplexer;
import javax.media.ResourceUnavailableException;
import javax.media.Time;
import javax.media.format.UnsupportedFormatException;
import javax.media.format.YUVFormat;
import javax.media.protocol.ContentDescriptor;
import javax.media.util.ImageToBuffer;

import com.googlecode.vicovre.codecs.ffmpeg.audio.AudioMixer;
import com.googlecode.vicovre.codecs.flv.JavaMultiplexer;
import com.googlecode.vicovre.media.MemeticFileReader;
import com.googlecode.vicovre.media.Misc;
import com.googlecode.vicovre.media.controls.FrameFillControl;
import com.googlecode.vicovre.media.controls.SetDurationControl;
import com.googlecode.vicovre.media.processor.OutputStreamDataSink;
import com.googlecode.vicovre.media.processor.SimpleProcessor;
import com.googlecode.vicovre.media.video.VideoMixer;
import com.googlecode.vicovre.repositories.rtptype.RtpTypeRepository;
import com.googlecode.vicovre.repositories.rtptype.impl.RtpTypeRepositoryXmlImpl;

/**
 * Extracts video from a Memetic stream
 *
 * @author Andrew G D Rowley
 * @version 1.0
 */
public class VideoExtractor {

    private Multiplexer multiplexer = null;

    private VideoMixer videoMixer = null;

    private AudioMixer audioMixer = null;

    private VideoMediaSource videoSource = null;

    private AudioMediaSource audioSource = null;

    private long audioOffset = 0;

    private long videoOffset = 0;

    private int videoTrack = 0;

    private int audioTrack = 0;

    private double generationSpeed = 0;

    /**
     * Creates a new VideoExtractor
     *
     * @param videoFilename The file from which to extract video
     * @param audioFilenames The files from which to extract audio
     * @param syncFilenames The files that should be synched with
     * @throws IOException
     * @throws UnsupportedFormatException
     * @throws ResourceUnavailableException
     */
    public VideoExtractor(String[] videoFilenames, Rectangle[] positions,
            String[] audioFilenames, String[] syncFilenames,
            int backgroundColour, RtpTypeRepository rtpTypeRepository)
            throws IOException, UnsupportedFormatException,
            ResourceUnavailableException {
        multiplexer = new JavaMultiplexer();
        multiplexer.setContentDescriptor(new ContentDescriptor("flv"));

        int numTracks = 0;
        MemeticFileReader[] videoReaders = null;
        if ((videoFilenames != null) && (videoFilenames.length > 0)) {
            videoReaders = new MemeticFileReader[videoFilenames.length];
            for (int i = 0; i < videoFilenames.length; i++) {
                videoReaders[i] = new MemeticFileReader(videoFilenames[i],
                        rtpTypeRepository);
            }
            videoTrack = numTracks;
            numTracks += 1;
        }
        MemeticFileReader[] audioReaders = null;
        if ((audioFilenames != null) && (audioFilenames.length > 0)) {
            audioReaders = new MemeticFileReader[audioFilenames.length];
            for (int i = 0; i < audioFilenames.length; i++) {
                audioReaders[i] = new MemeticFileReader(audioFilenames[i],
                        rtpTypeRepository);
            }
        }

        // Add an audio track even if there isn't one
        audioTrack = numTracks;
        numTracks += 1;
        multiplexer.setNumTracks(numTracks);

        if (videoReaders != null) {
            videoMixer = new VideoMixer(videoReaders, positions,
                    backgroundColour);
            videoSource = new VideoMediaSource(videoMixer, multiplexer,
                    videoTrack);
        }

        if (audioReaders != null) {
            audioMixer = new AudioMixer(audioReaders);
            audioSource = new AudioMediaSource(audioMixer, multiplexer,
                    audioTrack);
        }

        long earliestStart = Long.MAX_VALUE;
        if (syncFilenames != null) {
            for (int i = 0; i < syncFilenames.length; i++) {
                MemeticFileReader sync =
                    new MemeticFileReader(syncFilenames[i],rtpTypeRepository);
                if (sync.getStartTime() < earliestStart) {
                    earliestStart = sync.getStartTime();
                }
            }
        }

        if (audioMixer != null) {
            if (audioMixer.getStartTime() < earliestStart) {
                earliestStart = audioMixer.getStartTime();
            }
        }

        if (videoMixer != null) {
            if (videoMixer.getStartTime() < earliestStart) {
                earliestStart = videoMixer.getStartTime();
            }
        }

        if (audioMixer != null) {
            audioOffset = (audioMixer.getStartTime() - earliestStart)
                * 1000000L;
        }

        if (videoMixer != null) {
            videoOffset = (videoMixer.getStartTime() - earliestStart)
                * 1000000L;
        }

    }

    /**
     * Sets the speed of the generation of the flv
     *
     * A speed of <= 0 will generate the flv as fast as possible.
     * Otherwise, the number is the number of times faster than real-time.
     *
     * @param generationSpeed The generation speed
     */
    public void setGenerationSpeed(double generationSpeed) {
        this.generationSpeed = generationSpeed;
    }

    private long waitForNext(long startTime, long firstTimestamp,
            long timestamp) {
        if (generationSpeed <= 0) {
            return firstTimestamp;
        }
        timestamp = timestamp / 1000000;
        if (firstTimestamp == -1) {
            firstTimestamp = timestamp;
        }
        if ((timestamp - firstTimestamp) <= 10000) {
            return firstTimestamp;
        }
        long waitTime = (long) (((timestamp - firstTimestamp) - 10000)
                / generationSpeed);
        waitTime -= (long) ((System.currentTimeMillis() - startTime)
                / generationSpeed);

        if (waitTime > 0) {
            try {
                Thread.sleep(waitTime);
            } catch (InterruptedException e) {
                // Do Nothing
            }
        }
        return firstTimestamp;
    }

    /**
     * Transfers the data read to an
     * @param outputStream The outputstream to write to
     * @param offset The offset to start from (in milliseconds)
     * @param duration The duration to write (in milliseconds)
     * @param delay The delay before the stream starts (in milliseconds)
     * @throws IOException
     */
    public void transferToStream(OutputStream outputStream, long offsetShift,
            long offset, long duration, File firstFrame)
            throws IOException {
        SetDurationControl setDurationControl = (SetDurationControl)
            multiplexer.getControl(SetDurationControl.class.getName());
        if (setDurationControl != null) {
            setDurationControl.setDuration(new Time(duration * 1000000L));
        }
        OutputStreamDataSink dataSink = new OutputStreamDataSink(
                multiplexer.getDataOutput(), 0, outputStream);
        dataSink.start();

        // Seek to the start of the video and audio
        long audioEndTimestamp = (duration - offset) * 1000000L;
        long videoEndTimestamp = (duration - offset) * 1000000L;
        long videoTimestampOffset = 0;
        if ((videoMixer != null) && (audioMixer != null)) {
            videoMixer.streamSeek(offset - (videoOffset / 1000000L)
                    + offsetShift);
            audioMixer.streamSeek(offset - (audioOffset / 1000000L)
                    + offsetShift);
            long videoOffsetShift =
                (videoMixer.getOffset() - offset - offsetShift) * 1000000;
            long audioOffsetShift =
                (audioMixer.getOffset() - offset - offsetShift) * 1000000;
            videoTimestampOffset = videoOffset  + videoOffsetShift;
            audioMixer.setTimestampOffset(audioOffset + audioOffsetShift);
            videoSource.setTimestampOffset(offset * 1000000L);
            audioSource.setTimestampOffset(offset * 1000000L);
        } else if (videoMixer != null) {
            videoMixer.streamSeek(offset - (videoOffset / 1000000L)
                    + offsetShift);
            long videoOffsetShift =
                (videoMixer.getOffset() - offset - offsetShift) * 1000000;
            videoTimestampOffset = videoOffset  + videoOffsetShift;
            try {
                audioMixer = new AudioMixer(new MemeticFileReader[0]);
                audioSource = new AudioMediaSource(audioMixer, multiplexer,
                        audioTrack);
            } catch (Exception e) {
                // Does Nothing
            }
            videoSource.setTimestampOffset(offset * 1000000L);
            audioSource.setTimestampOffset(offset * 1000000L);
        } else if (audioMixer != null) {
            audioMixer.streamSeek(offset + offsetShift);
            audioMixer.setTimestampOffset(
                    (audioMixer.getOffset() - offset - offsetShift) * 1000000);
            audioSource.setTimestampOffset(offset * 1000000L);
        }

        boolean isAudioData = false;
        boolean isVideoData = false;

        // Fill in the first frame if possible
        if (videoSource != null) {
            FrameFillControl control = (FrameFillControl)
                videoSource.getControl("controls.FrameFillControl");
            if (control != null) {
                if (firstFrame != null) {
                    if (firstFrame.exists()) {
                        BufferedImage image = ImageIO.read(firstFrame);
                        Buffer buf = ImageToBuffer.createBuffer(image, -1);
                        try {
                            SimpleProcessor processor = new SimpleProcessor(
                                    buf.getFormat(),
                                    new YUVFormat(YUVFormat.YUV_420));
                            processor.process(buf);
                            Buffer out = processor.getOutputBuffer();
                            byte[] data = (byte[]) out.getData();
                            control.fillFrame(data);
                        } catch (UnsupportedFormatException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }


        // Output the first video frame
        if (videoMixer != null) {
            videoMixer.setTimestampOffset(videoTimestampOffset);
            isVideoData = videoSource.readNext();
            if (isVideoData) {
                videoSource.setTimestamp(0);
                videoSource.process();
                isVideoData = videoSource.readNext();
            }

        }

        // Read the first audio buffer
        if (audioMixer != null) {
            isAudioData = audioSource.readNext();
        }

        long startTime = System.currentTimeMillis();
        long firstTimestamp = -1;

        // Output the rest of the data
        while (isAudioData && isVideoData && !dataSink.isDone()) {
            long audioTimestamp = audioSource.getTimestamp();
            long videoTimestamp = videoSource.getTimestamp();

            //System.err.println("Audio ts = " + audioTimestamp + " os = " + mixer.getOffset() + " Video ts = " + videoTimestamp + " os = " + videoReader.getOffset());

            if (audioTimestamp < videoTimestamp) {
                if (audioTimestamp <= audioEndTimestamp) {
                    firstTimestamp = waitForNext(startTime, firstTimestamp,
                            audioTimestamp);
                    audioSource.process();
                    isAudioData = audioSource.readNext();
                } else {
                    isAudioData = false;
                }
            } else {
                if (videoTimestamp <= videoEndTimestamp) {
                    firstTimestamp = waitForNext(startTime, firstTimestamp,
                            videoTimestamp);
                    videoSource.process();
                    isVideoData = videoSource.readNext();
                } else {
                    isVideoData = false;
                }
            }
        }

        // Output audio remaining after the video has finished
        while (isAudioData && !dataSink.isDone()) {
            if (audioSource.getTimestamp() <= audioEndTimestamp) {
                firstTimestamp = waitForNext(startTime, firstTimestamp,
                        audioSource.getTimestamp());
                audioSource.process();
                isAudioData = audioSource.readNextBuffer();
            } else {
                isAudioData = false;
            }
        }

        // Output video remaining after the audio has finished
        while (isVideoData && !dataSink.isDone()) {
            if (videoSource.getTimestamp() <= videoEndTimestamp) {
                firstTimestamp = waitForNext(startTime, firstTimestamp,
                        videoSource.getTimestamp());
                videoSource.process();
                isVideoData = videoSource.readNext();
            } else {
                isVideoData = false;
            }
        }

        if (videoMixer != null) {
            videoMixer.close();
            videoSource.close();
        }
        if (audioMixer != null) {
            audioMixer.close();
            audioSource.close();
        }
        multiplexer.close();
        dataSink.close();
        System.err.println("Extractor finished");
    }

    public static void main(String[] args) throws Exception {
        if (!Misc.isCodecsConfigured()) {
            Misc.configureCodecs("/knownCodecs.xml");
        }
        VideoExtractor extractor = new VideoExtractor(
            // Video
            /*new String[]{
                "VicoWeb/target/recordings/2009-10-05_090000-000095270/1254428040"
                },
            */
            //"VicoWeb/target/recordings/2009-10-05_090000-000095270/1286981312",
            //"VicoWeb/target/recordings/2009-10-05_090000-000095270/3490601952",
            //"VicoWeb/target/recordings/2009-10-08_090000-002983902/1911227824",
            //"../../recordings/MAGIC/MAGIC002/2009-10-08_090000-002983902/2792696808",
            //"VicoWeb/target/recordings/2009-10-08_090000-002983902/1254543160",
            //"../../recordings/1273840957545552375448/2941173072",
            new String[]{
                //"../../recordings/127435969591176530449/3526413242",
                "../../recordings/127435969591176530449/3521524142",
                "../../recordings/127435969591176530449/1446065064",
            },

            new Rectangle[]{
                //new Rectangle(286, 30, 720, 540),
                new Rectangle(286, 30, 720, 540),
                new Rectangle(30, 30, 240, 196),
            },

            // Audio
            /*new String[]{
                "VicoWeb/target/recordings/2009-10-05_090000-000095270/548913710",
                "VicoWeb/target/recordings/2009-10-05_090000-000095270/251851200",
                "VicoWeb/target/recordings/2009-10-05_090000-000095270/267823638",
                "VicoWeb/target/recordings/2009-10-05_090000-000095270/282971832",
                "VicoWeb/target/recordings/2009-10-05_090000-000095270/66893508",
                "VicoWeb/target/recordings/2009-10-05_090000-000095270/163114337"
            }, */
            /*new String[]{
                "../../recordings/MAGIC/MAGIC002/2009-10-08_090000-002983902/124113515"
            }, */
            /*new String[]{
                "../../recordings/1273840957545552375448/193044552",
                "../../recordings/1273840957545552375448/2217839544",
                "../../recordings/1273840957545552375448/1184358165",
            }, */
            new String[]{
                "../../recordings/127435969591176530449/1448927216",
                "../../recordings/127435969591176530449/144994584",
                "../../recordings/127435969591176530449/199381944",
            },

            // Sync
            //null,
            /*new String[]{"VicoWeb/target/recordings/2009-10-05_090000-000095270/1254428040",
                    "VicoWeb/target/recordings/2009-10-05_090000-000095270/1286981312",
                    "VicoWeb/target/recordings/2009-10-05_090000-000095270/3490601952",}, */
            /*new String[]{
                    "../../recordings/MAGIC/MAGIC002/2009-10-08_090000-002983902/1911227824",
                    "../../recordings/MAGIC/MAGIC002/2009-10-08_090000-002983902/1254543160"
            }, */
            /*new String[]{
                "../../recordings/1273840957545552375448/3003136496",
            }, */
            new String[]{
                "../../recordings/127435969591176530449/3521524142",
            },
            0x000000,
            new RtpTypeRepositoryXmlImpl("/rtptypes.xml"));
        extractor.setGenerationSpeed(-1);
        FileOutputStream testout = new FileOutputStream("test.flv");
        extractor.transferToStream(testout, 0, 3000000, 3060000, null);
    }
}
