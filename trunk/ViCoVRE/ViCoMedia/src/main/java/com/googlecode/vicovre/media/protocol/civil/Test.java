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

package com.googlecode.vicovre.media.protocol.civil;

import java.io.IOException;
import java.util.List;
import java.util.Vector;

import javax.media.Effect;
import javax.media.MediaLocator;
import javax.media.control.FormatControl;
import javax.media.format.YUVFormat;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import com.googlecode.vicovre.media.Misc;
import com.googlecode.vicovre.media.renderer.RGBRenderer;
import com.lti.civil.CaptureDeviceInfo;
import com.lti.civil.CaptureStream;
import com.lti.civil.CaptureSystem;
import com.lti.civil.CaptureSystemFactory;
import com.lti.civil.DefaultCaptureSystemFactorySingleton;
import com.lti.civil.VideoFormat;
import com.lti.civil.swing.CaptureDevice;
import com.lti.civil.utility.VideoFormatNames;

public class Test {

    private class Format {
        private VideoFormat format = null;

        private Format(VideoFormat format) {
            this.format = format;
        }

        public String toString() {
            return format.getWidth() + " x " + format.getHeight() + " (" + VideoFormatNames.formatTypeToString(format.getFormatType()) + ")";
        }
    }

    public static void main(String[] args) throws Exception {
        Misc.configureCodecs("/knownCodecs.xml");

        CaptureSystemFactory factory =
            DefaultCaptureSystemFactorySingleton.instance();
        CaptureSystem system = factory.createCaptureSystem();
        system.init();
        final List<CaptureDeviceInfo> list = system.getCaptureDeviceInfoList();
        Vector<CaptureDevice> choices = new Vector<CaptureDevice>();
        for (int i = 0; i < list.size(); i++)
        {
            final CaptureDeviceInfo info = list.get(i);
            String[] outputs = info.getOutputNames();
            if (outputs.length <= 1) {
                choices.add(new CaptureDevice(info));
            } else {
                for (int j = 0; j < outputs.length; j++) {
                    choices.add(new CaptureDevice(info, j));
                }
            }
        }

        CaptureDevice device = (CaptureDevice) JOptionPane.showInputDialog(null,
                "Select Capture Device:", "Capture Device",
                JOptionPane.QUESTION_MESSAGE, null, choices.toArray(),
                choices.get(0));
        if (device == null) {
            System.exit(0);
        }

        CaptureDeviceInfo info = device.getInfo();
        int output = device.getOutput();
        int input = 0;
        if (info.getOutputNames().length >= 1) {
            String[] inputs = info.getInputNames(output);
            if (inputs.length > 0) {
                String inputName = (String) JOptionPane.showInputDialog(null,
                        "Select Input:", "Input",
                        JOptionPane.QUESTION_MESSAGE, null, inputs,
                        null);
                if (inputName == null) {
                    System.exit(0);
                }
                for (int i = 0; i < inputs.length; i++) {
                    if (inputs[i].equals(inputName)) {
                        input = i;
                        break;
                    }
                }
            }
        }

        DataSource dataSource = new DataSource();
        dataSource.setLocator(new MediaLocator(
                "civil:" + info.getDeviceID() + "?output="
                + output + "&input=" + input));
        dataSource.connect();
        FormatControl formatControl = (FormatControl) dataSource.getControl(FormatControl.class.getName());
        javax.media.Format[] formats = formatControl.getSupportedFormats();
        javax.media.Format format = (javax.media.Format)
            JOptionPane.showInputDialog(null,
                "Select Format:", "Format",
                JOptionPane.QUESTION_MESSAGE, null, formats, null);
        formatControl.setFormat(format);

        RGBRenderer renderer = new RGBRenderer(new Effect[]{});
        renderer.setDataSource(dataSource, 0);
        renderer.setInputFormat(dataSource.getStreams()[0].getFormat());
        JFrame frame = new JFrame();
        frame.getContentPane().setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.X_AXIS));
        frame.setSize(640, 480);
        frame.getContentPane().add(renderer.getComponent());
        renderer.start();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        javax.media.Format newFormat = (javax.media.Format)
            JOptionPane.showInputDialog(null,
                "Select Format:", "Format",
                JOptionPane.QUESTION_MESSAGE, null, formats, null);
        dataSource.stop();
        dataSource.disconnect();
        formatControl.setFormat(newFormat);
        dataSource.connect();
        dataSource.start();
    }
}
