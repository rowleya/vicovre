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

package com.googlecode.vicovre.media.wiimote;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

import wiiusej.values.IRSource;

public class WiimoteCalibrationPanel extends JPanel {

    private WiimoteCalibrationComponent camera =
        new WiimoteCalibrationComponent();

    private JSlider sensitivity = new JSlider(1, 5);

    private JPanel sensorBarTop = new JPanel();

    private JPanel sensorBarBottom = new JPanel();

    private JLabel status = new JLabel("Wiimote Disconnected");

    public WiimoteCalibrationPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel sensitivityLabel = new JLabel(
                "Sensitivity (Use + and - to adjust"
                + " so that only the correct number of lights are visible)");

        add(status);
        add(Box.createVerticalStrut(10));
        add(sensorBarTop);
        add(camera);
        add(sensorBarBottom);
        add(Box.createVerticalStrut(10));
        add(sensitivityLabel);
        add(sensitivity);

        status.setAlignmentX(CENTER_ALIGNMENT);
        sensorBarTop.setAlignmentX(CENTER_ALIGNMENT);
        camera.setAlignmentX(CENTER_ALIGNMENT);
        sensorBarBottom.setAlignmentX(CENTER_ALIGNMENT);
        sensitivityLabel.setAlignmentX(CENTER_ALIGNMENT);
        sensitivity.setAlignmentX(CENTER_ALIGNMENT);

        sensitivity.setEnabled(false);
        sensitivity.setMajorTickSpacing(1);
        sensitivity.setPaintLabels(true);

        sensorBarTop.setLayout(new BoxLayout(sensorBarTop, BoxLayout.Y_AXIS));
        sensorBarTop.setBorder(BorderFactory.createEtchedBorder());
        sensorBarTop.add(new JLabel(
                "Sensor Bar at Top - use Down arrow to move to bottom"));
        sensorBarBottom.setLayout(new BoxLayout(sensorBarBottom,
                BoxLayout.Y_AXIS));
        sensorBarBottom.setBorder(BorderFactory.createEtchedBorder());
        sensorBarBottom.add(new JLabel(
                "Sensor Bar at Bottom - use Up arrow to move to top"));
    }

    public void setSensitivity(int sensitivity) {
        this.sensitivity.setValue(sensitivity);
    }

    public void setSensorBarTop() {
        sensorBarTop.setVisible(true);
        sensorBarBottom.setVisible(false);
    }

    public void setSensorBarBottom() {
        sensorBarTop.setVisible(false);
        sensorBarBottom.setVisible(true);
    }

    public void setConnected() {
        status.setText("Wiimote Connected");
    }

    public void setDisconnected() {
        status.setText("Wiimote Disconnected");
    }

    public void setPoints(IRSource[] points) {
        camera.setPoints(points);
    }

}
