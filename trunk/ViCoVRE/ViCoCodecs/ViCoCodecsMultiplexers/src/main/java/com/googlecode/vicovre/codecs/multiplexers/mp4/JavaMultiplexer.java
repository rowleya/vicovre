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

package com.googlecode.vicovre.codecs.multiplexers.mp4;

import java.awt.Component;
import java.awt.Dimension;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Vector;

import javax.media.Buffer;
import javax.media.Format;
import javax.media.Time;
import javax.media.format.AudioFormat;
import javax.media.format.VideoFormat;
import javax.media.protocol.ContentDescriptor;

import com.googlecode.vicovre.codecs.utils.ByteArrayOutputStream;
import com.googlecode.vicovre.media.controls.SetDurationControl;
import com.googlecode.vicovre.media.format.BitRateFormat;
import com.googlecode.vicovre.media.multiplexer.BasicMultiplexer;

public class JavaMultiplexer extends BasicMultiplexer
        implements SetDurationControl {

    public static final String CONTENT_TYPE = "video/mp4";

    private boolean headerWritten = false;

    private long duration = 0;

    private long offset = 0;

    private long dataSize = 0;

    private long bytesWritten = 0;

    private byte[][] trackHeader = null;

    private double[] lastTimestamp = null;

    private Vector<Long>[] chunkOffsets = null;

    private long maxOffset = 0;

    private LinkedList<Chunk>[] chunkNoSamples = null;

    private Vector<Integer>[] sampleSize = null;

    private int[] allSampleSize = null;

    private LinkedList<Duration>[] durations = null;

    private Vector<Integer>[] keyFrames = null;

    private int[] noSamples = null;

    private int[] noChunks = null;

    private boolean sameBuffer = false;

    private byte[] moov = null;

    private int moovSize = 0;

    public JavaMultiplexer() {
        super(new ContentDescriptor[]{new ContentDescriptor(CONTENT_TYPE)},
            new Format[]{
                new VideoFormat("mpeg4"),
                new AudioFormat("aac")
            },
            32);
    }

    public int setNumTracks(int numtracks) {
        int tracks = super.setNumTracks(numtracks);
        trackHeader = new byte[tracks][];
        lastTimestamp = new double[tracks];
        chunkOffsets = new Vector[tracks];
        chunkNoSamples = new LinkedList[tracks];
        sampleSize = new Vector[tracks];
        allSampleSize = new int[tracks];
        durations = new LinkedList[tracks];
        keyFrames = new Vector[tracks];
        noSamples = new int[tracks];
        noChunks = new int[tracks];
        for (int i = 0; i < tracks; i++) {
            chunkOffsets[i] = new Vector<Long>();
            chunkNoSamples[i] = new LinkedList<Chunk>();
            sampleSize[i] = new Vector<Integer>();
            durations[i] = new LinkedList<Duration>();
            keyFrames[i] = new Vector<Integer>();
        }
        return tracks;
    }

    private Atom getFtyp() throws IOException {
        Atom ftyp = new Atom("ftyp");
        ftyp.writeBytes("isom");
        ftyp.writeInt(0x200);
        ftyp.writeBytes("isom");
        ftyp.writeBytes("iso2");
        ftyp.writeBytes("mp41");
        return ftyp;
    }

    private Atom getMvhd(long time, int version) throws IOException {
        Atom mvhd = new Atom("mvhd");
        mvhd.write(version);
        mvhd.writeInt24(0); // Flags
        if (version == 1) {
            mvhd.writeLong(time);
            mvhd.writeLong(time);
        } else {
            mvhd.writeInt((int) time);
            mvhd.writeInt((int) time);
        }
        mvhd.writeInt(1000); // Timescale = milliseconds
        if (version == 1) {
            mvhd.writeLong(duration);
        } else {
            mvhd.writeInt((int) duration);
        }
        mvhd.writeInt(0x00010000); // reserved (preferred rate) 1.0 = normal
        mvhd.writeShort(0x0100); // reserved (preferred volume) 1.0 = normal
        mvhd.writeShort(0); // reserved
        mvhd.writeInt(0); // reserved
        mvhd.writeInt(0); // reserved

        // Matrix structure
        mvhd.writeInt(0x00010000); // reserved
        mvhd.writeInt(0x0); // reserved
        mvhd.writeInt(0x0); // reserved
        mvhd.writeInt(0x0); // reserved
        mvhd.writeInt(0x00010000); // reserved
        mvhd.writeInt(0x0); // reserved
        mvhd.writeInt(0x0); // reserved
        mvhd.writeInt(0x0); // reserved
        mvhd.writeInt(0x40000000); // reserved

        mvhd.writeInt(0); // reserved (preview time)
        mvhd.writeInt(0); // reserved (preview duration)
        mvhd.writeInt(0); // reserved (poster time)
        mvhd.writeInt(0); // reserved (selection time)
        mvhd.writeInt(0); // reserved (selection duration)
        mvhd.writeInt(0); // reserved (current time)
        mvhd.writeInt(getNoTracks() + 1); // Next track id

        return mvhd;
    }

    private Atom getTkhd(int track, long time, int version, Format format,
            Dimension size) throws IOException {

        Atom tkhd = new Atom("tkhd");
        tkhd.write(version);
        tkhd.writeInt24(0xf); // Flags, 0xf = enabled
        if (version == 1) {
            tkhd.writeLong(time);
            tkhd.writeLong(time);
        } else {
            tkhd.writeInt((int) time);
            tkhd.writeInt((int) time);
        }
        tkhd.writeInt(track); // Track number
        tkhd.writeInt(0); // Reserved
        if (version == 1) {
            tkhd.writeLong(duration);
        } else {
            tkhd.writeInt((int) duration);
        }
        tkhd.writeLong(0); // Reserved
        tkhd.writeShort(0); // Layer
        tkhd.writeShort(0); // Alternate Group
        if (format instanceof AudioFormat) {
            tkhd.writeShort(0x0100); // Volume
        } else {
            tkhd.writeShort(0); // Volume 0 on video
        }
        tkhd.writeShort(0); // Reserved
        tkhd.writeInt(0x00010000); // Matrix structure
        tkhd.writeInt(0x0);
        tkhd.writeInt(0x0);
        tkhd.writeInt(0x0);
        tkhd.writeInt(0x00010000);
        tkhd.writeInt(0x0);
        tkhd.writeInt(0x0);
        tkhd.writeInt(0x0);
        tkhd.writeInt(0x40000000);
        tkhd.writeInt(size.width * 0x10000); // Fixed-point width
        tkhd.writeInt(size.height * 0x10000); // Fixed-point height

        return tkhd;
    }

    private Atom getMdhd(long time, int version, Format format)
            throws IOException {
        Atom mdhd = new Atom("mdhd");
        mdhd.write(version);
        mdhd.writeInt24(0); // Flags
        if (version == 1) {
            mdhd.writeLong(time);
            mdhd.writeLong(time);
        } else {
            mdhd.writeInt((int) time);
            mdhd.writeInt((int) time);
        }
        if (format instanceof VideoFormat) {
            mdhd.writeInt(1000); // Timescale - milliseconds
        } else if (format instanceof AudioFormat) {
            AudioFormat af = (AudioFormat) format;
            mdhd.writeInt((int) af.getSampleRate());
        }
        long dur = duration;
        if (format instanceof AudioFormat) {
            AudioFormat af = (AudioFormat) format;
            dur = (long) ((duration * 1000) / af.getSampleRate());
        }
        if (version == 1) {
            mdhd.writeLong(dur);
        } else {
            mdhd.writeInt((int) dur);
        }
        mdhd.writeShort(0); // Language (0 = english)
        mdhd.writeShort(0); // Quality
        return mdhd;
    }

    private Atom getHdlr(Format format) throws IOException {
        Atom hdlr = new Atom("hdlr");
        hdlr.write(0); // Version
        hdlr.writeInt24(0); // Flags
        hdlr.writeBytes("\0\0\0\0");
        if (format instanceof VideoFormat) {
            hdlr.writeBytes("vide");
        } else {
            hdlr.writeBytes("soun");
        }
        hdlr.writeInt(0); // Manufacturer
        hdlr.writeInt(0); // Flags
        hdlr.writeInt(0); // Flags Mask
        String name = null;
        if (format instanceof VideoFormat) {
            name = "VideoHandler";
        } else {
            name = "SoundHandler";
        }
        hdlr.write(name.length());
        hdlr.writeBytes(name);
        return hdlr;
    }

    private Atom getVmhd() throws IOException {
        Atom vmhd = new Atom("vmhd");
        vmhd.write(0); // Version
        vmhd.writeInt24(0); // Flags
        vmhd.writeShort(0); // Graphics Mode = copy
        vmhd.writeShort(0); // OpColour red
        vmhd.writeShort(0); // OpColour green
        vmhd.writeShort(0); // OpColour blue
        return vmhd;
    }

    private Atom getSmhd() throws IOException {
         Atom smhd = new Atom("smhd");
         smhd.write(0); // Version
         smhd.writeInt24(0); // Flags
         smhd.writeShort(0); // Balance
         smhd.writeShort(0); // Reserved
         return smhd;
    }

    private Atom getDref() throws IOException {
        Atom dref = new Atom("dref");
        dref.write(0); // Version
        dref.writeInt24(0); // Flags
        dref.writeInt(1); // Entry count
        dref.writeInt(0xc); // Entry size
        dref.writeBytes("url "); // Type
        dref.write(0); // Version
        dref.writeInt24(1); // Flags = self reference
        return dref;
    }

    private Atom getDinf() throws IOException {
        Atom dinf = new Atom("dinf");
        dinf.write(getDref());
        return dinf;
    }

    private int getDescriptionLength(int length) {
        int i;
        for(i = 1; (length >> (7 * i)) != 0; i++);
        return length + 1 + i;
    }

    private void writeDescription(Atom atom, int tag, int size)
            throws IOException {
        atom.write(tag);
        for(int i = getDescriptionLength(size) - size - 2; i > 0; i--) {
            atom.write((size >> (7 * i)) | 0x80);
        }
        atom.write(size & 0x7F);
    }

    private Atom getEsds(int track, Format format, byte[] extradata)
            throws IOException {
        Atom esds = new Atom("esds");
        esds.write(0); // Version
        esds.writeInt24(0); // Flags

        int extraDataLength = 0;
        if (extradata != null) {
            extraDataLength = getDescriptionLength(extradata.length);
        }

        // ES Descriptor
        writeDescription(esds, 0x3, 3
                + getDescriptionLength(13 + extraDataLength)
                + getDescriptionLength(1));
        esds.writeShort(track);
        esds.write(0);

        // Decoder Config Descriptor
        writeDescription(esds, 0x4, 13 + extraDataLength);
        if (format instanceof VideoFormat) {
            esds.write(32); // MPEG 4 Video
            esds.write(0x11); // Video
        } else {
            esds.write(64); // AAC Audio
            esds.write(0x15); // Audio
        }
        esds.writeInt24(0); // Buffer Size
        int bitrate = 0;
        int maxrate = 0;
        if (format instanceof BitRateFormat) {
            BitRateFormat brf = (BitRateFormat) format;
            bitrate = brf.getBitRate();
            maxrate = bitrate + brf.getTolerance();
        }
        esds.writeInt(maxrate);
        if (maxrate == bitrate) {
            esds.writeInt(bitrate);
        } else {
            esds.writeInt(0);
        }

        // Decoder Specific info
        if (extradata != null) {
            writeDescription(esds, 0x5, extraDataLength);
            esds.write(extradata);
        }

        // SL Descriptor
        writeDescription(esds, 0x6, 1);
        esds.write(0x2);

        return esds;
    }

    private Atom getMp4v(int track, Format format, Dimension size,
            byte[] extradata) throws IOException {
        Atom mp4v = new Atom("mp4v");
        mp4v.writeInt(0); // Reserved
        mp4v.writeShort(0); // Reserved
        mp4v.writeShort(1); // Data Reference
        mp4v.writeShort(0); // Codec stream version
        mp4v.writeShort(0); // Codec stream revision
        mp4v.writeInt(0); // Reserved
        mp4v.writeInt(0); // Reserved
        mp4v.writeInt(0); // Reserved
        mp4v.writeShort(size.width);
        mp4v.writeShort(size.height);
        mp4v.writeInt(0x00480000); // Horizontal resolution 72dpi
        mp4v.writeInt(0x00480000); // Vertical resolution 72dpi
        mp4v.writeInt(0); // Data Size
        mp4v.writeShort(1); // Frame count
        mp4v.write(0); // Compressor name size
        mp4v.write(new byte[31]); // Compressor name - set to 0
        mp4v.writeShort(24); // Depth
        mp4v.writeShort(0xFFFF); // Colour table ID

        mp4v.write(getEsds(track, format, extradata));
        return mp4v;
    }

    private Atom getMp4a(int track, Format format, byte[] extradata)
            throws IOException {
        Atom mp4a = new Atom("mp4a");
        mp4a.writeInt(0); // Reserved
        mp4a.writeShort(0); // Reserved
        mp4a.writeShort(1); // Data Reference
        mp4a.writeShort(0); // Version
        mp4a.writeShort(0); // Revision
        mp4a.writeInt(0); // Vendor
        mp4a.writeShort(1); // Channels 1 - mono 2 - stereo
        mp4a.writeShort(16); // Sample size
        mp4a.writeShort(0); // Compression ID
        mp4a.writeShort(0); // Packet size
        AudioFormat af = (AudioFormat) format;
        int sampleRate = (int) af.getSampleRate();
        mp4a.writeInt(sampleRate * 0x10000);

        mp4a.write(getEsds(track, format, extradata));
        return mp4a;
    }

    private Atom getStsd(int track, Format format, Dimension size,
            byte[] extradata) throws IOException {
         Atom stsd = new Atom("stsd");
         stsd.write(0); // Version
         stsd.writeInt24(0); // Flags
         stsd.writeInt(1); // No Entries
         if (format instanceof VideoFormat) {
             stsd.write(getMp4v(track, format, size, extradata));
         } else if (format instanceof AudioFormat) {
             stsd.write(getMp4a(track, format, extradata));
         }
         return stsd;
    }

    private Atom getStts(int track) throws IOException {
        Atom stts = new Atom("stts");
        stts.write(0); // Version
        stts.writeInt24(0); // Flags
        stts.writeInt(durations[track].size());
        for (Duration duration : durations[track]) {
            stts.writeInt(duration.getCount());
            stts.writeInt((int) duration.getDuration());
        }
        return stts;
    }

    private Atom getStss(int track) throws IOException {
        Atom stss = new Atom("stss");
        stss.write(0); // Version
        stss.writeInt24(0); // Flags
        stss.writeInt(keyFrames[track].size());
        for (int sample : keyFrames[track]) {
            stss.writeInt(sample + 1);
        }
        return stss;
    }

    private Atom getStsc(int track) throws IOException {
        Atom stsc = new Atom("stsc");
        stsc.write(0); // Version
        stsc.writeInt24(0); // Flags
        stsc.writeInt(chunkNoSamples[track].size());
        for (Chunk chunk : chunkNoSamples[track]) {
            stsc.writeInt(chunk.getFirstChunk() + 1);
            stsc.writeInt(chunk.getNoSamples());
            stsc.writeInt(0x1); // Sample description ID
        }
        return stsc;
    }

    private Atom getStsz(int track) throws IOException {
        Atom stsz = new Atom("stsz");
        stsz.write(0); // Version
        stsz.writeInt24(0); // Flags
        if (allSampleSize[track] != -1) {
            stsz.writeInt(allSampleSize[track]);
            stsz.writeInt(noSamples[track]);
        } else {
            stsz.writeInt(0);
            stsz.writeInt(sampleSize[track].size());
            for (int size : sampleSize[track]) {
                stsz.writeInt(size);
            }
        }
        return stsz;
    }

    private Atom getStco(int track) throws IOException {
        Atom stco = null;
        boolean large = false;
        if (maxOffset > Math.pow(2, 32)) {
            stco = new Atom("co64");
            large = true;
        } else {
            stco = new Atom("stco");
        }
        stco.write(0); // Version
        stco.writeInt24(0); // Flags
        stco.writeInt(chunkOffsets[track].size());
        for (long offset : chunkOffsets[track]) {
            if (large) {
                stco.writeLong(offset);
            } else {
                stco.writeInt((int) offset);
            }
        }
        return stco;
    }

    private Atom getStbl(int track, Format format, Dimension size,
            byte[] extradata) throws IOException {
        Atom stbl = new Atom("stbl");
        stbl.write(getStsd(track, format, size, extradata));
        stbl.write(getStts(track));
        if (!keyFrames[track].isEmpty()) {
            stbl.write(getStss(track));
        }
        stbl.write(getStsc(track));
        stbl.write(getStsz(track));
        stbl.write(getStco(track));
        return stbl;
    }

    private Atom getMinf(int track, Format format, Dimension size,
            byte[] extradata) throws IOException {
        Atom minf = new Atom("minf");
        if (format instanceof VideoFormat) {
            minf.write(getVmhd());
        } else {
            minf.write(getSmhd());
        }
        minf.write(getDinf());
        minf.write(getStbl(track, format, size, extradata));
        return minf;
    }

    private Atom getMdia(int track, long time, int version, Format format,
            Dimension size, byte[] extradata) throws IOException {
        Atom mdia = new Atom("mdia");
        mdia.write(getMdhd(time, version, format));
        mdia.write(getHdlr(format));
        mdia.write(getMinf(track, format, size, extradata));
        return mdia;
    }

    private Atom getTrak(int track, long time, int version, Format format,
            Dimension size, byte[] extradata) throws IOException {
        Atom trak = new Atom("trak");
        trak.write(getTkhd(track, time, version, format, size));
        trak.write(getMdia(track, time, version, format, size, extradata));
        return trak;
    }

    private Atom getMoov(long time, int version) throws IOException {
        Atom moov = new Atom("moov");
        moov.write(getMvhd(time, version));
        for (int i = 0; i < getNoTracks(); i++) {
            Format format = getTrackFormat(i);
            VideoFormat vf = null;
            Dimension size = null;
            if (format instanceof VideoFormat) {
                vf = (VideoFormat) format;
                size = vf.getSize();
            } else if (format instanceof AudioFormat) {
                size = new Dimension(0, 0);
            }
            moov.write(getTrak(i, time, version, format, size, trackHeader[i]));
        }
        return moov;
    }

    private void writeHeader(DataOutputStream out) throws IOException {

        // Find the bitrate and file length
        int bitrate = 0;
        int maxrate = 0;
        for (int i = 0; i < getNoTracks(); i++) {
            Format format = getTrackFormat(i);
            if (format instanceof BitRateFormat) {
                BitRateFormat brf = (BitRateFormat) format;
                bitrate += brf.getBitRate();
                maxrate += brf.getBitRate() + brf.getTolerance();
            }
        }
        dataSize = (maxrate / 8) * (duration / 1000);

        out.write(getFtyp().getBytes());
        Atom mdat = new Atom("mdat", dataSize);
        out.write(mdat.getBytes());
        if (dataSize > Math.pow(2, 32)) {
            dataSize -= 16;
        } else {
            dataSize -= 8;
        }
    }

    protected int read(byte[] buf, int off, int len, Buffer buffer, int track)
            throws IOException {
        if (!headerWritten) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream(buf, off,
                    len);
            DataOutputStream out = new DataOutputStream(bytes);
            writeHeader(out);
            headerWritten = true;
            out.flush();
            bytes.flush();
            bytesWritten += bytes.getCount();
            return bytes.getCount();
        }

        Format format = buffer.getFormat();
        setInputFormat(format, track);
        int size = buffer.getLength();
        int offset = buffer.getOffset();
        byte[] data = (byte[]) buffer.getData();

        if (!sameBuffer) {

            Object header = buffer.getHeader();
            if ((header != null) && (header instanceof byte[])
                    && (trackHeader[track] == null)) {
                trackHeader[track] = (byte[]) header;
            }

            int noSamples = 1;
            int sampleSize = size;
            this.sampleSize[track].add(size);

            if (allSampleSize[track] == 0) {
                allSampleSize[track] = sampleSize;
            } else if ((allSampleSize[track] != -1)
                    && (allSampleSize[track] != sampleSize)) {
                allSampleSize[track] = -1;
            }

            double timestamp = buffer.getTimeStamp() - this.offset;
            double realDuration = 0;
            if (format instanceof AudioFormat) {
                AudioFormat af = (AudioFormat) format;
                realDuration = af.getFrameSizeInBits() / af.getSampleSizeInBits()
                    / af.getChannels();
            } else {
                realDuration = (timestamp - lastTimestamp[track])
                        / noSamples / 1000000;
            }
            long duration = (long) realDuration;

            boolean isKeyFrame = (buffer.getFlags()
                    & Buffer.FLAG_KEY_FRAME) > 0;
            chunkOffsets[track].add(bytesWritten);
            maxOffset = Math.max(maxOffset, bytesWritten);
            if (chunkNoSamples[track].isEmpty()
                    || (chunkNoSamples[track].getLast().getNoSamples()
                            != noSamples)) {
                chunkNoSamples[track].addLast(new Chunk(noChunks[track],
                        noSamples));
            }
            if (durations[track].isEmpty()
                    || (durations[track].getLast().getDuration() != duration)) {
                durations[track].addLast(new Duration(duration));
            } else {
                durations[track].getLast().increment(noSamples);
            }

            if (isKeyFrame) {
                keyFrames[track].add(this.noSamples[track]);
            }


            this.noSamples[track] += noSamples;
            this.noChunks[track] += 1;
            bytesWritten += size;
            dataSize -= size;
            lastTimestamp[track] = timestamp - (realDuration - duration);
        }
        int toCopy = size;
        if (toCopy > len) {
            toCopy = len;
        }
        System.arraycopy(data, offset, buf, off, toCopy);
        if (toCopy < size) {
            buffer.setOffset(offset + toCopy);
            buffer.setLength(size - toCopy);
            sameBuffer = true;
        } else {
            sameBuffer = false;
            setResult(BUFFER_PROCESSED_OK, true);
        }
        return toCopy;
    }

    protected int readLast(byte[] buf, int off, int len) throws IOException {
        if (dataSize > 0) {
            long size = dataSize;
            if (size > len) {
                size = len;
            }
            Arrays.fill(buf, off, (int) (off + size), (byte) 0);
            dataSize -= size;
            return (int) size;
        }

        if (moov == null) {
            long time = (System.currentTimeMillis() / 1000) + (((1904L * 365) + 17) * 60 * 60 * 24);
            int version = 0;
            if (duration >= Math.pow(2, 32)) {
                version = 1;
            }
            moov = getMoov(time, version).getBytes();
            moovSize = moov.length;
        }

        if (moovSize > 0) {
            int size = moovSize;
            if (size > len) {
                size = len;
            }
            System.arraycopy(moov, moov.length - moovSize, buf, off, size);
            moovSize -= size;
            return size;
        }
        return -1;
    }

    public String getName() {
        return "ASF Multiplexer";
    }

    public void setOffset(Time offset) {
        this.offset = offset.getNanoseconds();
    }

    public void setDuration(Time duration) {
        this.duration = duration.getNanoseconds() / 1000000;
    }

    public Component getControlComponent() {
        return null;
    }

    public Object getControl(String s) {
        if (s.equals(SetDurationControl.class.getName())) {
            return this;
        }
        return null;
    }

    public Object[] getControls() {
        return new Object[]{this};
    }
}
