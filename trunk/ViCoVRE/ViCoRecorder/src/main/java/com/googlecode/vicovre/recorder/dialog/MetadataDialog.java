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


import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;

import com.googlecode.vicovre.recordings.Metadata;

public class MetadataDialog extends JDialog implements ActionListener {

    private static final int WIDTH = 600;

    private Metadata metadata = null;

    private HashMap<String, JTextComponent> fields =
        new HashMap<String, JTextComponent>();

    private HashMap<String, JComponent> components =
        new HashMap<String, JComponent>();

    private JPanel content = new JPanel();

    private JPanel elements = new JPanel();

    private JPanel buttons = new JPanel();

    private JButton okButton = new JButton("OK");

    private JButton cancelButton = new JButton("Cancel");

    private JButton addSimpleItem = new JButton("Add Simple Item");

    private JButton addMutlilineItem = new JButton("Add Multiline Item");

    private int elementsHeight = 0;

    private int maxLabelWidth = 0;

    private boolean cancelled = false;

    public MetadataDialog(JDialog parent, Metadata recMetadata) {
        super(parent, "Enter Metadata for the Recording", true);
        setResizable(false);
        metadata = recMetadata;
        if (metadata == null) {
            metadata = new Metadata("name", "");
        }

        buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
        buttons.add(okButton);
        buttons.add(Box.createHorizontalGlue());
        buttons.add(addSimpleItem);
        buttons.add(Box.createHorizontalGlue());
        buttons.add(addMutlilineItem);
        buttons.add(Box.createHorizontalGlue());
        buttons.add(cancelButton);

        okButton.addActionListener(this);
        cancelButton.addActionListener(this);
        addSimpleItem.addActionListener(this);
        addMutlilineItem.addActionListener(this);

        elements.setLayout(new BoxLayout(elements, BoxLayout.Y_AXIS));
        List<String> keys = metadata.getKeys();
        for (String key : keys) {
            addItem(key);
        }


        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        content.add(elements);
        content.add(buttons);
        elements.setAlignmentX(LEFT_ALIGNMENT);
        buttons.setAlignmentX(LEFT_ALIGNMENT);
        getContentPane().add(content);
        setupSize();
    }

    private void addItem(String key) {
        JTextComponent field = null;
        JComponent component = null;
        if (metadata.isMultiline(key)) {
            field = new JTextArea(metadata.getValue(key));
            JScrollPane scroll = new JScrollPane(field);
            scroll.setVerticalScrollBarPolicy(
                    JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            scroll.setHorizontalScrollBarPolicy(
                    JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
            scroll.setMaximumSize(new Dimension(maxLabelWidth, 70));
            component = scroll;
            elementsHeight += 70;
        } else {
            field = new JTextField(metadata.getValue(key));
            field.setMaximumSize(new Dimension(maxLabelWidth, 20));
            component = field;
            elementsHeight += 20;
        }

        fields.put(key, field);
        components.put(key, component);

        String name = Metadata.getDisplayName(key);
        JLabel label = new JLabel(name + ":");
        Dimension labelSize = label.getMaximumSize();
        maxLabelWidth = Math.max(maxLabelWidth, labelSize.width);
        JPanel itemPanel = new JPanel();
        itemPanel.setLayout(new BoxLayout(itemPanel, BoxLayout.X_AXIS));
        itemPanel.add(label);
        itemPanel.add(Box.createHorizontalGlue());
        itemPanel.add(component);
        itemPanel.setAlignmentX(LEFT_ALIGNMENT);
        label.setAlignmentY(TOP_ALIGNMENT);
        component.setAlignmentY(TOP_ALIGNMENT);
        elements.add(itemPanel);
        elements.add(Box.createVerticalStrut(5));
        elementsHeight += 5;
    }

    private void setupSize() {
        Insets insets = getParent().getInsets();
        int maxWidth = WIDTH - (maxLabelWidth + 15
                + insets.left + insets.right);

        for (String key : metadata.getKeys()) {
            JComponent field = components.get(key);
            Dimension maxSize = field.getMaximumSize();
            maxSize.width = maxWidth;
            field.setMaximumSize(maxSize);
            field.setPreferredSize(maxSize);
            field.setMinimumSize(maxSize);
            field.setSize(maxSize);
        }

        Dimension buttonsSize = buttons.getMaximumSize();
        setSize(WIDTH, elementsHeight + 10 + insets.top + insets.bottom
                + buttonsSize.height);
        setLocationRelativeTo(getParent());
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(cancelButton)) {
            cancelled = true;
            setVisible(false);
        } else if (e.getSource().equals(okButton)) {
            String key = metadata.getPrimaryKey();
            JTextComponent value = fields.get(key);
            if (value.getText().equals("")) {
                String displayName = Metadata.getDisplayName(key);
                JOptionPane.showMessageDialog(this,
                        displayName + " cannot be blank!",
                        "Error", JOptionPane.ERROR_MESSAGE);
            } else {
                cancelled = false;
                setVisible(false);
            }
        } else if (e.getSource().equals(addSimpleItem)
                || e.getSource().equals(addMutlilineItem)) {
            String name = JOptionPane.showInputDialog(this, "Item name:");
            if ((name != null) && !name.equals("")) {
                String key = Metadata.getKey(name);
                if (metadata.getValue(key) != null) {
                    JOptionPane.showMessageDialog(this,
                            "An item by this name already exists!", "Error",
                            JOptionPane.ERROR_MESSAGE);
                } else {
                    metadata.setValue(key, "", true, true,
                            e.getSource().equals(addMutlilineItem));
                    addItem(key);
                    setupSize();
                }
            }
        }
    }

    public boolean wasCancelled() {
        return cancelled;
    }

    public void setVisible(boolean visible) {
        if (visible) {
            cancelled = true;
        }
        super.setVisible(visible);
    }

    public Metadata getMetadata() {
        for (String key : metadata.getKeys()) {
            String value = fields.get(key).getText();
            metadata.setValue(key, value);
        }
        return metadata;
    }
}
