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

package com.googlecode.vicovre.streamer;

import info.clearthought.layout.TableLayout;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.googlecode.vicovre.utils.Config;

public class ProfileDialog extends JDialog implements ActionListener {

    private static final String NAME = "profileName";

    private static final String EMAIL = "profileEmail";

    private static final String PHONE = "profilePhone";

    private static final String LOCATION = "profileLocation";

    private String initialName = null;

    private String initialEmail = null;

    private String initialPhone = null;

    private String initialLocation = null;

    private JTextField name = new JTextField();

    private JTextField email = new JTextField();

    private JTextField phone = new JTextField();

    private JTextField location = new JTextField();

    private JButton okButton = new JButton("OK");

    private JButton cancelButton = new JButton("Cancel");

    private boolean cancelled = false;

    public ProfileDialog(Frame parent) {
        super(parent, "Details", true);
        setSize(600, 165);
        setLocationRelativeTo(parent);

        JPanel content = new JPanel();
        content.setLayout(new TableLayout(new double[]{70, TableLayout.FILL},
                new double[]{20, 5, 20, 5, 20, 5, 20, 5, 20}));
        content.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(content);

        content.add(new JLabel("Name:"), "0, 0");
        content.add(name, "1, 0");
        content.add(new JLabel("E-mail:"), "0, 2");
        content.add(email, "1, 2");
        content.add(new JLabel("Phone:"), "0, 4");
        content.add(phone, "1, 4");
        content.add(new JLabel("Location:"), "0, 6");
        content.add(location, "1, 6");

        JPanel buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
        buttons.add(okButton);
        buttons.add(Box.createHorizontalGlue());
        buttons.add(cancelButton);
        content.add(buttons, "0, 8, 1, 8");

        okButton.addActionListener(this);
        cancelButton.addActionListener(this);
    }

    public void init(Config config) {
        name.setText(config.getParameter(NAME, ""));
        email.setText(config.getParameter(EMAIL, ""));
        phone.setText(config.getParameter(PHONE, ""));
        location.setText(config.getParameter(LOCATION, ""));
    }

    public void storeConfiguration(Config config) {
        config.setParameter(NAME, name.getText());
        config.setParameter(EMAIL, email.getText());
        config.setParameter(PHONE, phone.getText());
        config.setParameter(LOCATION, location.getText());
    }

    public void setVisible(boolean visible) {
        if (visible) {
            initialName = name.getText();
            initialEmail = email.getText();
            initialPhone = phone.getText();
            initialLocation = location.getText();
            cancelled = false;
        }
        super.setVisible(visible);
    }

    public boolean wasCancelled() {
        return cancelled;
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(okButton)) {
            cancelled = false;
            setVisible(false);
        } else if (e.getSource().equals(cancelButton)) {
            cancelled = true;
            name.setText(initialName);
            email.setText(initialEmail);
            phone.setText(initialPhone);
            location.setText(initialLocation);
            setVisible(false);
        }
    }

    public String getName() {
        return name.getText();
    }

    public String getEmail() {
        return email.getText();
    }

    public String getPhone() {
        return phone.getText();
    }

    public String getLoc() {
        return location.getText();
    }

}
