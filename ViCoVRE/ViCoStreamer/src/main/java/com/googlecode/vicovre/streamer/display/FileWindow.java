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

package com.googlecode.vicovre.streamer.display;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.media.Time;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.googlecode.vicovre.media.controls.PlayControl;

public class FileWindow extends VideoWindow implements ActionListener,
        ChangeListener {

    private static final Icon PLAY = new ImageIcon(
            FileWindow.class.getResource("/play.gif"));

    private static final Icon PAUSE = new ImageIcon(
            FileWindow.class.getResource("/pause.gif"));

    private JButton playPauseButton = new JButton(PLAY);

    private JSlider timeSlider = new JSlider();

    private PlayControl control = null;

    private boolean playing = false;

    private boolean timeUpdate = false;

    private Timer timer = null;

    private boolean constructed = false;

    public FileWindow(String name, VideoPanel panel) {
        super(name, panel);

        playPauseButton.addActionListener(this);
        timeSlider.addChangeListener(this);

        constructed = true;

        setUpInterface();
    }

    private void setUpInterface() {

        if (constructed && control != null) {
            JPanel controlPanel = new JPanel();
            controlPanel.setLayout(new BoxLayout(controlPanel,
                    BoxLayout.X_AXIS));
            playPauseButton.setMaximumSize(new Dimension(20, 20));
            controlPanel.add(playPauseButton);
            long duration = control.getDuration().getNanoseconds();
            synchronized (timeSlider) {
                timeUpdate = true;
                timeSlider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
                timeSlider.setMinimum(0);
                timeSlider.setMaximum((int) (duration  / 1000000));
                controlPanel.add(timeSlider);
                timeUpdate = false;
            }
            /*if (duration != 0) {
                timer = new Timer(250, this);
                timer.setRepeats(true);
                timer.start();
            } */
            add(controlPanel);
        }
    }

    protected void setPlayControl(PlayControl control) {
        this.control = control;
        setUpInterface();
    }

    protected void setPlayer(VideoPlayer player) {
        super.setPlayer(player);
        setUpInterface();
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(playPauseButton)) {
            if (!playing) {
                control.play();
                playPauseButton.setIcon(PAUSE);
                playing = true;
            } else {
                control.pause();
                playPauseButton.setIcon(PLAY);
                playing = false;
            }
        } else if (e.getSource().equals(timer)) {
            synchronized (timeSlider) {
                timeUpdate = true;
                long position = control.getPosition().getNanoseconds();
                timeSlider.setValue((int) (position / 1000000));
                timeUpdate = false;
            }
        }
    }

    public void stateChanged(ChangeEvent e) {
        synchronized (timeSlider) {
            if (!timeUpdate && !timeSlider.getValueIsAdjusting()) {
                timer.stop();
                timeUpdate = true;
                System.err.println("Seeking to " + (timeSlider.getValue() * 1000000L) + " of " + control.getDuration().getNanoseconds());
                Time newTime = control.seek(
                        new Time(timeSlider.getValue() * 1000000L));
                System.err.println("Actually seeked to " + newTime.getNanoseconds());
                timeSlider.setValue((int) (newTime.getNanoseconds() / 1000000));
                timeUpdate = false;
                timer.start();
            }
        }
    }
}
