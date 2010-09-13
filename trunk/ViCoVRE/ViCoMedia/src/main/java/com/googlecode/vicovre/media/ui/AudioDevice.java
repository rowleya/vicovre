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

package com.googlecode.vicovre.media.ui;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.media.CannotRealizeException;
import javax.media.MediaLocator;
import javax.media.NoDataSourceException;
import javax.media.NoPlayerException;
import javax.media.NoProcessorException;
import javax.media.format.AudioFormat;
import javax.media.format.UnsupportedFormatException;
import javax.media.protocol.DataSource;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;
import javax.media.rtp.RTPConnector;
import javax.media.rtp.RTPManager;
import javax.media.rtp.SendStream;
import javax.sound.sampled.BooleanControl;
import javax.sound.sampled.CompoundControl;
import javax.sound.sampled.Control;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.Port;

import ag3.interfaces.types.ClientProfile;

import com.googlecode.vicovre.media.Misc;
import com.googlecode.vicovre.media.effect.CloneEffect;
import com.googlecode.vicovre.media.processor.SimpleProcessor;
import com.googlecode.vicovre.media.protocol.sound.JavaSoundStream;

/**
 * An audio device
 * @author Andrew G D Rowley
 * @version 1.0
 */
public class AudioDevice {

    private RTPManager sendManager = null;

    private SimpleProcessor processor = null;

    private SendStream sendStream = null;

    private CloneEffect cloneEffect = null;

    private String name = null;

    private HashSet<String> lines = new HashSet<String>();

    private HashMap<String, FloatControl> volumeControls =
        new HashMap<String, FloatControl>();

    private HashMap<String, BooleanControl> selectControls =
        new HashMap<String, BooleanControl>();

    private BooleanControl originallySelectedPort = null;

    private HashMap<String, Float> originalVolumes =
        new HashMap<String, Float>();

    private DataSource dataSource = null;

    private boolean started = false;

    private boolean prepared = false;

    private ClientProfile profile = null;

    private String tool = null;

    /**
     * Creates a new AudioDevice
     * @param name The name of the device
     */
    public AudioDevice(String name, ClientProfile profile, String tool) {
        this.name = name;
        this.profile = profile;
        this.tool = tool;
    }

    public String getName() {
        return name;
    }

    private HashMap<String, Control> getControls(Control[] controls,
            HashMap<String, Control> currentControls) {
        for (int i = 0; i < controls.length; i++) {
            currentControls.put(controls[i].getType().toString(), controls[i]);
            if (controls[i] instanceof CompoundControl) {
                getControls(((CompoundControl) controls[i]).getMemberControls(),
                        currentControls);
            }
        }
        return currentControls;
    }

    public void test() throws LineUnavailableException,
            IOException {
        Mixer mixer = JavaSoundStream.getPortMixer(name);
        Line.Info[] infos = mixer.getSourceLineInfo();
        for (int j = 0; j < infos.length; j++) {
            String portName = ((Port.Info) infos[j]).getName();
            lines.add(portName);
            Line line = mixer.getLine(infos[j]);
            line.open();
            HashMap<String, Control> controls = getControls(line.getControls(),
                    new HashMap<String, Control>());
            FloatControl volume = (FloatControl) controls.get("Volume");
            BooleanControl select = (BooleanControl)
                controls.get("Select");
            volumeControls.put(portName, volume);
            selectControls.put(portName, select);
            if ((select != null) && select.getValue()) {
                originallySelectedPort = select;
            }
            if (volume != null) {
                originalVolumes.put(portName, volume.getValue());
            }
        }

        dataSource =
            new com.googlecode.vicovre.media.protocol.sound.DataSource();
        dataSource.setLocator(
                new MediaLocator("sound://rate=44100&channels=1&bits=16&mixer="
                + name));
        dataSource.connect();
        dataSource.disconnect();
    }

    /**
     * Prepares the audio device for use
     * @param rtpConnector The connector to send using
     * @param audioFormat The audio format to send using
     * @param audioRtpType The rtp type to send using
     * @throws IOException
     * @throws UnsupportedFormatException
     * @throws NoDataSourceException
     * @throws NoProcessorException
     * @throws CannotRealizeException
     * @throws LineUnavailableException
     */
    public void prepare(RTPConnector rtpConnector, AudioFormat audioFormat,
            int audioRtpType) throws UnsupportedFormatException, IOException,
            LineUnavailableException {

        if (started) {
            return;
        }

        if (dataSource == null) {
            test();
        }

        PushBufferStream[] datastreams =
            ((PushBufferDataSource) dataSource).getStreams();

        try {
            processor = new SimpleProcessor(datastreams[0].getFormat(),
                    audioFormat);

            processor.printChain(System.err);

            System.err.println();
            System.err.println("Adding clone effect");
            cloneEffect = new CloneEffect();
            if (!processor.insertEffect(cloneEffect)) {
                System.err.println("Couldn't clone");
            }

            processor.printChain(System.err);

            sendManager = RTPManager.newInstance();
            sendManager.addFormat(audioFormat, audioRtpType);
            sendManager.initialize(rtpConnector);

            DataSource data = processor.getDataOutput(dataSource, 0);
            sendStream = sendManager.createSendStream(data, 0);
            sendStream.setSourceDescription(Misc.createSourceDescription(
                    profile, name, tool));
            dataSource.disconnect();
            prepared = true;
        } finally {
            if (!prepared) {
                if (processor != null) {
                    processor.close();
                }
            }
        }
    }

    /**
     * Gets the lines of the mixer
     * @return The lines
     */
    public Set<String> getLines() {
        return lines;
    }

    /**
     * Sets the volume of a line
     * @param line The line name
     * @param setVolume The volume to set
     */
    public void setLineVolume(String line, float setVolume) {
        FloatControl volume = volumeControls.get(line);
        volume.setValue(setVolume);
    }

    /**
     * Gets the volume of the line
     * @param line The line
     * @return The volume, or -1 if not available
     */
    public float getLineVolume(String line) {
        FloatControl volume = volumeControls.get(line);
        if (volume != null) {
            return volume.getValue();
        }
        return -1;
    }

    /**
     * Selects the given line
     * @param line The line to select
     */
    public void selectLine(String line) {
        BooleanControl select = selectControls.get(line);
        if (select != null) {
            select.setValue(true);
        }
    }

    /**
     * Reset to the original volumes
     */
    public void resetToOriginalVolumes() {
        BooleanControl select = originallySelectedPort;
        if (select != null) {
            select.setValue(true);
        }

        for (String line : lines) {
            FloatControl volume = volumeControls.get(line);
            if (volume != null) {
                volume.setValue(originalVolumes.get(line));
            }
        }
    }

    /**
     * Starts the audio device
     * @param listener The listener to send to
     * @param line The line to start
     * @throws NoPlayerException
     * @throws CannotRealizeException
     * @throws IOException
     */
    public void start(LocalStreamListener listener, String line)
            throws NoPlayerException, CannotRealizeException, IOException {
        long ssrc = sendStream.getSSRC();
        if (ssrc < 0) {
            ssrc = ssrc + (((long) Integer.MAX_VALUE + 1) * 2);
        }
        if (!started) {
            dataSource.connect();
            sendStream.start();
            processor.start(dataSource, 0);
            if (listener != null) {
                listener.addLocalAudio(name + " - " + line, cloneEffect,
                    volumeControls.get(line), ssrc);
            }
            started = true;
        } else {
            if (listener != null) {
                listener.removeLocalAudio(cloneEffect);
                listener.addLocalAudio(name + " - " + line, cloneEffect,
                        volumeControls.get(line), ssrc);
            }
        }
    }

    /**
     * Stops the audio device
     * @param listener The listener
     */
    public void stop(LocalStreamListener listener) {
        if (started) {
            if (listener != null) {
                listener.removeLocalAudio(cloneEffect);
            }
            processor.stop();
            try {
                sendStream.stop();
            } catch (IOException e) {
                e.printStackTrace();
            }
            dataSource.disconnect();
            started = false;
        }
    }

    public int hashCode() {
        return name.hashCode();
    }

    public boolean equals(Object obj) {
        if (obj instanceof AudioDevice) {
            return ((AudioDevice) obj).name.equals(name);
        }
        return false;
    }
}
