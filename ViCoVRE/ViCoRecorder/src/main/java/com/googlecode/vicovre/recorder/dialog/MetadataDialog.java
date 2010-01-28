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

package com.googlecode.vicovre.recorder.dialog;

import info.clearthought.layout.TableLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import com.googlecode.vicovre.recordings.RecordingMetadata;

public class MetadataDialog extends JDialog implements ActionListener {

    private JTextField name = new JTextField("");

    private JTextArea description = new JTextArea("");

    private JButton okButton = new JButton("OK");

    private JButton cancelButton = new JButton("Cancel");

    private boolean cancelled = false;

    public MetadataDialog(JDialog parent, RecordingMetadata metadata) {
        super(parent, "Enter Metadata for the Recording", true);
        setSize(600, 170);
        setResizable(false);
        setLocationRelativeTo(parent);
        if (metadata != null) {
            name.setText(metadata.getName());
            description.setText(metadata.getDescription());
        }

        JPanel content = new JPanel();
        content.setLayout(new TableLayout(new double[]{70, TableLayout.FILL},
                new double[]{20, 5, 70, 5, 20}));
        content.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(content);

        JScrollPane descriptionScroll = new JScrollPane(description);
        descriptionScroll.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        descriptionScroll.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

        content.add(new JLabel("Name:"), "0, 0");
        content.add(name, "1, 0");
        content.add(new JLabel("Description:"), "0, 2");
        content.add(descriptionScroll, "1, 2");

        description.setBorder(BorderFactory.createEtchedBorder());

        JPanel buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
        buttons.add(okButton);
        buttons.add(Box.createHorizontalGlue());
        buttons.add(cancelButton);
        content.add(buttons, "0, 4, 1, 4");

        okButton.addActionListener(this);
        cancelButton.addActionListener(this);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(cancelButton)) {
            cancelled = true;
        }
        setVisible(false);
    }

    public boolean wasCancelled() {
        return cancelled;
    }

    public String getName() {
        return name.getText();
    }

    public String getDescription() {
        return description.getText();
    }
}
