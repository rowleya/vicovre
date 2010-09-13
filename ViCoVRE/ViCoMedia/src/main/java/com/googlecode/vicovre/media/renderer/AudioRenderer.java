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

package com.googlecode.vicovre.media.renderer;

import java.awt.Component;
import java.util.Vector;

import javax.media.Buffer;
import javax.media.Format;
import javax.media.GainChangeListener;
import javax.media.GainControl;
import javax.media.Renderer;
import javax.media.ResourceUnavailableException;
import javax.media.format.AudioFormat;
import javax.media.protocol.DataSource;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.BooleanControl;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;

import com.googlecode.vicovre.media.processor.SimpleProcessor;

public class AudioRenderer implements Renderer, GainControl {

    private static final Format[] INPUT_FORMATS =
        new Format[]{new AudioFormat(null)};

    private SimpleProcessor processor = null;

    private RenderingThread thread = null;

    private Vector<GainChangeListener> gainListeners =
        new Vector<GainChangeListener>();

    private float gainBeforeMute = 0.0f;

    private FloatControl gainControl = null;

    private BooleanControl muteControl = null;

    private SourceDataLine sourceDataLine = null;

    public Format[] getSupportedInputFormats() {
        return INPUT_FORMATS;
    }

    private javax.sound.sampled.AudioFormat convertFormat(AudioFormat format) {
        return new javax.sound.sampled.AudioFormat(
                (float) format.getSampleRate(), format.getSampleSizeInBits(),
                format.getChannels(), format.getSigned() == AudioFormat.SIGNED,
                format.getEndian() == AudioFormat.BIG_ENDIAN);
    }

    public void setDataSource(DataSource dataSource, int track) {
        thread = new RenderingThread(dataSource, track, this);
    }

    public int process(Buffer input) {
        int result = processor.process(input);
        if ((result == BUFFER_PROCESSED_OK)
                || (result == INPUT_BUFFER_NOT_CONSUMED)) {
            Buffer inputBuffer = processor.getOutputBuffer();
            byte[] data = (byte[]) inputBuffer.getData();
            int offset = inputBuffer.getOffset();
            int length = inputBuffer.getLength();
            while (length > 0) {
                int bytesWritten = sourceDataLine.write(data, offset, length);
                length -= bytesWritten;
                offset += bytesWritten;
            }
        }
        return result;
    }

    public Format setInputFormat(Format format) {
        try {
            processor = new SimpleProcessor(format,
                    new AudioFormat(AudioFormat.LINEAR));
            javax.sound.sampled.AudioFormat audioFormat =
                convertFormat((AudioFormat) processor.getOutputFormat());
            DataLine.Info info = new DataLine.Info(SourceDataLine.class,
                    audioFormat);
            sourceDataLine = (SourceDataLine) AudioSystem.getLine(info);
            sourceDataLine.open(audioFormat);
            sourceDataLine.start();
            gainControl = (FloatControl) sourceDataLine.getControl(
                    FloatControl.Type.MASTER_GAIN);
            muteControl = (BooleanControl) sourceDataLine.getControl(
                    BooleanControl.Type.MUTE);
            if (gainControl != null) {
                gainBeforeMute = gainControl.getValue();
            }
            return format;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void start() {
        if (thread != null) {
            thread.start();
        }
    }

    public void stop() {
        if (thread != null) {
            thread.close();
        }
    }

    public void close() {
        // Does Nothing
    }

    public String getName() {
        return "AudioRenderer";
    }

    public void open() throws ResourceUnavailableException {
        // Does Nothing
    }

    public void reset() {
        // Does Nothing
    }

    public Object getControl(String className) {
        if (className.equals(GainControl.class.getName())) {
            if (gainControl != null) {
                return this;
            }
        }
        return null;
    }

    public Object[] getControls() {
        if (gainControl != null) {
            return new Object[]{this};
        }
        return new Object[0];
    }

    public void addGainChangeListener(GainChangeListener listener) {
        gainListeners.add(listener);
    }

    public float getDB() {
        return (float) (Math.log10(gainControl.getValue()) + 20.0);
    }

    public float getLevel() {
        return gainControl.getValue();
    }

    public boolean getMute() {
        if (muteControl == null) {
            return gainControl.getValue() == 0.0f;
        }
        return muteControl.getValue();
    }

    public void removeGainChangeListener(GainChangeListener listener) {
        gainListeners.remove(listener);
    }

    public float setDB(float db) {
        return setLevel((float) (Math.pow(10.0, db / 20.0)));
    }

    public float setLevel(float level) {
        gainControl.setValue(level);
        return gainControl.getValue();
    }

    public void setMute(boolean mute) {
        if (muteControl == null) {
            if (mute) {
                gainBeforeMute = gainControl.getValue();
                gainControl.setValue(0.0f);
            } else {
                gainControl.setValue(gainBeforeMute);
            }
        } else {
            muteControl.setValue(mute);
        }
    }

    public Component getControlComponent() {
        return null;
    }

}
