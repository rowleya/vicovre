package com.googlecode.vicovre.gwt.recorder.client;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.googlecode.vicovre.gwt.client.layout.Layout;
import com.googlecode.vicovre.gwt.client.layout.LayoutSelectionPanel;
import com.googlecode.vicovre.gwt.utils.client.MessageResponse;
import com.googlecode.vicovre.gwt.utils.client.MessageResponseHandler;
import com.googlecode.vicovre.gwt.utils.client.ModalPopup;

public class LayoutSelectionPopup extends ModalPopup<VerticalPanel>
        implements ClickHandler {

    private LayoutSelectionPanel layoutSelection = null;

    private Button okButton = new Button("OK");

    private Button cancelButton = new Button("Cancel");

    private MessageResponseHandler handler = null;

    public LayoutSelectionPopup(Layout[] layouts, Layout[] customLayouts,
            String url, MessageResponseHandler handler) {
        super(new VerticalPanel());
        VerticalPanel panel = getWidget();

        this.handler = handler;
        layoutSelection = new LayoutSelectionPanel(layouts, customLayouts, url);

        HorizontalPanel buttonPanel = new HorizontalPanel();
        buttonPanel.setWidth("100%");
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        buttonPanel.setCellHorizontalAlignment(okButton,
                HorizontalPanel.ALIGN_LEFT);
        buttonPanel.setCellHorizontalAlignment(cancelButton,
                HorizontalPanel.ALIGN_RIGHT);

        panel.add(new Label("Select a layout:"));
        panel.add(layoutSelection);
        panel.add(buttonPanel);

        okButton.addClickHandler(this);
        cancelButton.addClickHandler(this);
    }

    public void onClick(ClickEvent event) {
        hide();
        if (event.getSource() == okButton) {
            handler.handleResponse(new MessageResponse(
                    MessageResponse.OK, this));
        } else {
            handler.handleResponse(new MessageResponse(
                    MessageResponse.CANCEL, this));
        }
    }

    public Layout getLayout() {
        return layoutSelection.getSelection();
    }

    public void setLayout(Layout layout) {
        if (layout != null) {
            layoutSelection.setLayout(layout.getName());
        } else {
            layoutSelection.setLayout(null);
        }
    }

}
