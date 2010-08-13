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

package com.googlecode.vicovre.gwt.download.client;

import com.allen_sauer.gwt.dnd.client.DragContext;
import com.allen_sauer.gwt.dnd.client.drop.SimpleDropController;
import com.google.gwt.user.client.ui.VerticalPanel;

public class VideoDropController extends SimpleDropController {

    private VerticalPanel target = null;

    private VideoStreamSelectionPage page = null;

    private String position = null;

    private String baseUrl = null;

    private String folder = null;

    private String recordingId = null;

    private int width = 0;

    private int height = 0;

    public VideoDropController(VideoStreamSelectionPage page, String position,
            VerticalPanel dropTarget, String baseUrl,
            String folder, String recordingId, int width, int height) {
        super(dropTarget);
        this.page = page;
        this.position = position;
        this.target = dropTarget;
        this.baseUrl = baseUrl;
        this.folder = folder;
        this.recordingId = recordingId;
        this.width = width;
        this.height = height;
    }

    public void onDrop(DragContext context) {
        super.onDrop(context);
        VideoPreviewPanel panel = (VideoPreviewPanel) context.draggable;
        VideoPreviewPanel clone = new VideoPreviewPanel(baseUrl, folder,
                recordingId, panel.getStreamId(), width, height);
        target.clear();
        target.add(clone);
        page.setPositon(position, panel.getStreamId());
    }

}
