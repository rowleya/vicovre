/**
 * Copyright (c) 2008-2009, University of Bristol
 * Copyright (c) 2008-2009, University of Manchester
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

import java.awt.Dialog;
import java.awt.Dimension;
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
import javax.swing.JProgressBar;

/**
 * A dialog for indicating progress
 * @author Andrew G D Rowley
 * @version 1.0
 */
public class ProgressDialog extends JDialog implements ActionListener {

    private static final int DIALOG_WIDTH = 200;

    private static final int DIALOG_HEIGHT = 40;

    private static final int DIALOG_HEIGHT_CANCEL = 80;

    private static final int PROGRESS_HEIGHT = 20;

    private static final int BORDER_WIDTH = 5;

    private JLabel label = null;

    private JProgressBar progress = null;

    private JButton cancelButton = new JButton("Cancel");

    private boolean cancelled = false;

    /**
     * Creates a new Progress Dialog
     *
     * @param parent The parent of the dialog
     * @param title The title of the dialog
     * @param modal True if modal, false if modeless
     * @param indeterminate True if the progress length is undetermined
     */
    public ProgressDialog(Frame parent, String title, boolean modal,
            boolean indeterminate, boolean cancellable) {
        super(parent, title, modal);
        init(indeterminate, cancellable);
    }

    /**
     * Creates a new Progress Dialog
     *
     * @param parent The parent of the dialog
     * @param title The title of the dialog
     * @param modal True if modal, false if modeless
     * @param indeterminate True if the progress length is undetermined
     */
    public ProgressDialog(Dialog parent, String title, boolean modal,
            boolean indeterminate, boolean cancellable) {
        super(parent, title, modal);
        init(indeterminate, cancellable);
    }


    /**
     * Creates a new Progress Dialog
     *
     * @param title The title of the dialog
     * @param modal True if modal, false if modeless
     * @param indeterminate True if the progress length is undetermined
     */
    public ProgressDialog(String title, boolean modal,
            boolean indeterminate, boolean cancellable) {
        super((Frame) null, title, modal);
        init(indeterminate, cancellable);
    }

    private void init(boolean indeterminate, boolean cancellable) {
        JPanel content = new JPanel();
        label = new JLabel();
        label.setAlignmentX(CENTER_ALIGNMENT);
        progress = new JProgressBar();
        progress.setIndeterminate(indeterminate);
        progress.setPreferredSize(new Dimension(DIALOG_WIDTH, PROGRESS_HEIGHT));
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(
                BORDER_WIDTH, BORDER_WIDTH, BORDER_WIDTH, BORDER_WIDTH));
        content.add(label);
        content.add(Box.createVerticalGlue());
        content.add(progress);
        add(content);
        setUndecorated(true);
        if (!cancellable) {
            setSize(new Dimension(DIALOG_WIDTH, DIALOG_HEIGHT));
        } else {
            cancelButton.setAlignmentX(CENTER_ALIGNMENT);
            content.add(Box.createVerticalGlue());
            content.add(cancelButton);
            cancelButton.addActionListener(this);
            setSize(new Dimension(DIALOG_WIDTH, DIALOG_HEIGHT_CANCEL));
        }
        setLocationRelativeTo(null);
    }

    /**
     * Sets the message displayed in the dialog
     * @param message The message to set
     */
    public void setMessage(String message) {
        label.setText(message);
    }

    /**
     * Sets the bounds of the progress
     * @param lower The lower bound
     * @param upper The upper bound
     */
    public void setBounds(int lower, int upper) {
        progress.setIndeterminate(false);
        progress.setMinimum(lower);
        progress.setMaximum(upper);
    }

    /**
     * Sets the progress
     * @param value The new progress value
     */
    public void setProgress(int value) {
        progress.setValue(value);
    }

    /**
     * Makes the progress bar indeterminate
     */
    public void setIndeterminate() {
        progress.setIndeterminate(true);
    }

    public void actionPerformed(ActionEvent e) {
        cancelled = true;
        setVisible(false);
    }

    public boolean wasCancelled() {
        return cancelled;
    }
}
