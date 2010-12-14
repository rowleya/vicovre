package com.googlecode.vicovre.gwt.client.layout;

import java.util.Vector;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.event.dom.client.HasChangeHandlers;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DisclosurePanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.googlecode.vicovre.gwt.client.layoutcreator.LayoutCreatorPopup;
import com.googlecode.vicovre.gwt.utils.client.MessageResponse;
import com.googlecode.vicovre.gwt.utils.client.MessageResponseHandler;

public class LayoutSelectionPanel extends VerticalPanel implements ClickHandler,
        MouseOverHandler, MouseOutHandler, MessageResponseHandler,
        HasChangeHandlers {

    private static final int LAYOUT_WIDTH = 150;

    private LayoutPreview selection = null;

    private HorizontalPanel currentCustomLayoutPanel = null;

    private VerticalPanel customLayoutPanel = new VerticalPanel();

    private int customLayoutCount = 0;

    private int noLayoutsPerWidth = 0;

    private String url = null;

    private Vector<LayoutPreview> previews = new Vector<LayoutPreview>();

    public LayoutSelectionPanel(Layout[] predefinedLayouts,
            Layout[] customLayouts, String url) {
        this.url = url;
        this.customLayoutCount = customLayouts.length;

        noLayoutsPerWidth = Window.getClientWidth() / LAYOUT_WIDTH;
        int maxLayouts = Math.max(predefinedLayouts.length,
                customLayouts.length);
        if (noLayoutsPerWidth > maxLayouts) {
            noLayoutsPerWidth = maxLayouts;
        }

        add(new Label("Select a layout for the video:"));

        Label predefLabel = new Label("Predefined Layouts");
        DOM.setStyleAttribute(predefLabel.getElement(), "fontWeight", "bold");
        add(predefLabel);
        setHorizontalAlignment(VerticalPanel.ALIGN_LEFT);
        addLayouts(predefinedLayouts, this);

        customLayoutPanel.setHorizontalAlignment(VerticalPanel.ALIGN_LEFT);
        DisclosurePanel customLayoutDisclosurePanel =
            new DisclosurePanel("Custom Layouts");
        DOM.setStyleAttribute(
                customLayoutDisclosurePanel.getHeader().getElement(),
                "fontWeight", "bold");
        customLayoutDisclosurePanel.setContent(customLayoutPanel);
        add(customLayoutDisclosurePanel);
        currentCustomLayoutPanel = addLayouts(customLayouts, customLayoutPanel);

        Button addNewButton = new Button("Create New Layout");
        add(addNewButton);
        addNewButton.addClickHandler(this);
    }

    private HorizontalPanel createNextPanel() {
        HorizontalPanel panel = new HorizontalPanel();
        panel.setHorizontalAlignment(HorizontalPanel.ALIGN_CENTER);
        return panel;
    }

    private LayoutPreview createPreview(Layout layout) {
        LayoutPreview preview = new LayoutPreview(layout, LAYOUT_WIDTH - 5);
        preview.addClickHandler(this);
        preview.addMouseOverHandler(this);
        preview.addMouseOutHandler(this);
        previews.add(preview);
        return preview;
    }

    private HorizontalPanel addLayouts(Layout[] layouts, VerticalPanel panel) {
        HorizontalPanel layoutPanel = null;
        int count = 0;
        for (Layout layout : layouts) {
            if ((count % noLayoutsPerWidth) == 0) {
                layoutPanel = createNextPanel();
                panel.add(layoutPanel);
            }
            LayoutPreview preview = createPreview(layout);
            layoutPanel.add(preview);
            count += 1;
        }
        return layoutPanel;
    }

    public LayoutPreview addCustomLayout(Layout layout) {
        if ((customLayoutCount % noLayoutsPerWidth) == 0) {
            currentCustomLayoutPanel = createNextPanel();
            customLayoutPanel.add(currentCustomLayoutPanel);
        }
        LayoutPreview preview = createPreview(layout);
        currentCustomLayoutPanel.add(preview);
        customLayoutCount += 1;
        return preview;
    }

    public void onClick(ClickEvent event) {
        Object source = event.getSource();
        if (source instanceof LayoutPreview) {
            LayoutPreview preview = (LayoutPreview) source;
            if (selection != null) {
                selection.setSelected(false);
            }
            selection = preview;
            selection.setSelected(true);
            NativeEvent changeEvent = Document.get().createChangeEvent();
            DomEvent.fireNativeEvent(changeEvent, this);
        } else {
            LayoutCreatorPopup popup = new LayoutCreatorPopup(url, this);
            popup.center();
        }
    }

    public void onMouseOver(MouseOverEvent event) {
        Object source = event.getSource();
        if (source instanceof LayoutPreview) {
            LayoutPreview preview = (LayoutPreview) source;
            preview.setHighlight(true);
        }
    }

    public void onMouseOut(MouseOutEvent event) {
        Object source = event.getSource();
        if (source instanceof LayoutPreview) {
            LayoutPreview preview = (LayoutPreview) source;
            preview.setHighlight(false);
        }
    }

    public void handleResponse(MessageResponse response) {
        if (response.getResponseCode() == MessageResponse.OK) {
            LayoutCreatorPopup popup = (LayoutCreatorPopup)
                response.getSource();
            Layout layout = popup.getLayout();
            LayoutPreview preview = addCustomLayout(layout);
            if (selection != null) {
                selection.setSelected(false);
            }
            selection = preview;
            selection.setSelected(true);
            NativeEvent changeEvent = Document.get().createChangeEvent();
            DomEvent.fireNativeEvent(changeEvent, this);
        }
    }

    public void setLayout(String layout) {
        if (selection != null) {
            selection.setSelected(false);
            selection = null;
        }
        if (layout != null) {
            for (LayoutPreview preview : previews) {
                if (preview.getLayout().getName().equals(layout)) {
                    selection = preview;
                    selection.setSelected(true);
                    return;
                }
            }
        }
    }

    public Layout getSelection() {
        if (selection != null) {
            return selection.getLayout();
        }
        return null;
    }

    public HandlerRegistration addChangeHandler(ChangeHandler changeHandler) {
        return addDomHandler(changeHandler, ChangeEvent.getType());
    }
}
