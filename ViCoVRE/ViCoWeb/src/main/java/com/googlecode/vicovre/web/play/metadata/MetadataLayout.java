package com.googlecode.vicovre.web.play.metadata;

import java.util.Vector;

import com.googlecode.vicovre.recordings.ReplayLayout;
import com.googlecode.vicovre.recordings.ReplayLayoutPosition;
import com.googlecode.vicovre.recordings.Recording;
import com.googlecode.vicovre.recordings.Stream;
import com.googlecode.vicovre.repositories.layout.Layout;
import com.googlecode.vicovre.repositories.layout.LayoutRepository;
import com.googlecode.vicovre.repositories.layout.LayoutPosition;

public class MetadataLayout {
    private double timeStamp = 0;
    private String layoutName = null;
    private Vector<MetadataLayoutPosition> layoutPositions =
        new Vector<MetadataLayoutPosition>();

    public MetadataLayout(LayoutRepository layoutRepository,
            Recording recording, ReplayLayout replayLayout) {
        this.timeStamp = (double) (replayLayout.getTime()
                - recording.getStartTime().getTime()) / 1000;
        this.layoutName = replayLayout.getName();
        Layout layout = layoutRepository.findLayout(replayLayout.getName());
        for (LayoutPosition layoutPosition : layout.getStreamPositions()) {
            if (!layoutPosition.isAssignable()) {
                layoutPositions.add(
                    new MetadataLayoutPosition(layoutPosition));
            }
        }
        long realStart = recording.getStartTime().getTime()
            + replayLayout.getTime();
        long realEnd = recording.getStartTime().getTime()
            + replayLayout.getEndTime();
        for (ReplayLayoutPosition replayLayoutPosition
                : replayLayout.getLayoutPositions()) {
            layoutPositions.add(new MetadataLayoutPosition(layoutRepository,
                    layoutName, replayLayoutPosition, realStart, realEnd,
                    replayLayout.getAudioStreams()));
        }
    }

    public double getTimeStamp() {
        return timeStamp;
    }

    public String getLayoutName() {
        return layoutName;
    }

    public Vector<MetadataLayoutPosition> getLayoutPositions() {
        return layoutPositions;
    }
}