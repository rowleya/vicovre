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

package com.googlecode.vicovre.codecs.ffmpeg.demuxer;

import java.awt.Component;

import javax.media.Buffer;
import javax.media.Format;
import javax.media.Time;
import javax.media.Track;
import javax.media.TrackListener;
import javax.media.control.FormatControl;
import javax.media.format.AudioFormat;
import javax.media.format.VideoFormat;

import com.googlecode.vicovre.codecs.ffmpeg.Utils;

public class FFMPEGTrack implements Track, FormatControl {

    protected static final int EOF_ERROR = -1;

    protected static final int NO_FRAME_ERROR = -2;

    protected static final int UNKNOWN_ERROR = -3;

    private FFMPEGDemuxer parent = null;

    private int track = -1;

    private Format format = null;

    private Format outputFormat = null;

    private Time startTime = TIME_UNKNOWN;

    private boolean enabled = true;

    private Time duration = DURATION_UNKNOWN;

    private int outputSize = 0;

    private long sequenceNumber = 0;

    private byte[] data = null;

    protected FFMPEGTrack(FFMPEGDemuxer parent, int track, Format format,
            Time startTime, Time duration, int maxDataLength) {
        this.parent = parent;
        this.track = track;
        this.format = format;
        this.startTime = startTime;
        this.duration = duration;

        this.outputFormat = format;
        this.outputSize = maxDataLength;
        data = new byte[outputSize * 2];
    }

    public Format getFormat() {
        return outputFormat;
    }

    public Time getStartTime() {
        return startTime;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Time mapFrameToTime(int frame) {
        return TIME_UNKNOWN;
    }

    public int mapTimeToFrame(Time time) {
        if (time.getNanoseconds() == 0) {
            return 0;
        }
        return FRAME_UNKNOWN;
    }

    public void readFrame(Buffer buffer) {
        if (enabled) {
            buffer.setSequenceNumber(sequenceNumber++);
            buffer.setFormat(outputFormat);
            buffer.setData(data);
            buffer.setOffset(0);
            buffer.setLength(data.length);
            int error = parent.readNextFrame(buffer, track);
            if (error != 0) {
                if (parent.isEndOfSource() || (error == EOF_ERROR)) {
                    buffer.setEOM(true);
                }
                buffer.setDiscard(true);
            }
        } else {
            buffer.setDiscard(true);
        }
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setTrackListener(TrackListener trackListener) {
        // Does Nothing
    }

    public Time getDuration() {
        return duration;
    }

    public Format[] getSupportedFormats() {
        if (format instanceof VideoFormat) {
            return Utils.getVideoFormats(null,
                    ((VideoFormat) format).getFrameRate());
        } else if (format instanceof AudioFormat) {
            return new Format[]{format};
        }
        return new Format[]{format};
    }

    public Format setFormat(Format format) {
        Format[] supported = getSupportedFormats();
        for (Format f : supported) {
            if (f.matches(format)) {
                outputFormat = f;
                if (f instanceof VideoFormat) {
                    parent.setStreamOutputVideoFormat(track, (VideoFormat) f);
                } else if (f instanceof AudioFormat) {
                    parent.setStreamAudioDecoded(track);
                }
                outputSize = parent.getOutputSize(track);
                data = new byte[outputSize];
                return f;
            }
        }
        return null;
    }

    public Component getControlComponent() {
        return null;
    }

}
