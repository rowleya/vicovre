package com.googlecode.vicovre.web.play.metadata;

import java.util.List;
import java.util.Vector;

import com.googlecode.vicovre.recordings.ReplayLayoutPosition;
import com.googlecode.vicovre.recordings.Stream;
import com.googlecode.vicovre.repositories.layout.LayoutPosition;
import com.googlecode.vicovre.repositories.layout.LayoutRepository;

public class MetadataLayoutPosition {

    private MetadataStream stream = null;
    private String name = null;
    private String type = null;
    private int width = 0;
    private int height = 0;
    private int x = 0;
    private int y = 0;
    private double opacity = 0;
    private boolean changes = false;
    private Vector<MetadataStream> audioStreams = new Vector<MetadataStream>();

    public MetadataLayoutPosition(LayoutRepository layoutRepository,
            String layoutName, ReplayLayoutPosition replayLayoutPosition,
            long realStart, long realEnd, List<Stream> audio) {
        name = replayLayoutPosition.getName();
        Stream replayStream = replayLayoutPosition.getStream();
        if (replayStream != null) {
            stream = new MetadataStream(replayStream, realStart, realEnd);
            type = stream.getType();
        }
        LayoutPosition layoutPosition = layoutRepository.findLayout(
                layoutName).findStreamPosition(name);
        if (layoutPosition != null) {
            x = layoutPosition.getX();
            y = layoutPosition.getY();
            opacity = layoutPosition.getOpacity();
            width = layoutPosition.getWidth();
            height = layoutPosition.getHeight();
            changes = layoutPosition.hasChanges();
            if (layoutPosition.hasAudio()) {
                for (Stream stream : audio) {
                    audioStreams.add(
                            new MetadataStream(stream, realStart, realEnd));
                }
            }
        }
    }

    public MetadataLayoutPosition(LayoutPosition layoutPosition) {
        name = layoutPosition.getName();
        type = layoutPosition.getName();
        x = layoutPosition.getX();
        y = layoutPosition.getY();
        opacity = layoutPosition.getOpacity();
        width = layoutPosition.getWidth();
        height = layoutPosition.getHeight();
    }

    public MetadataStream getStream() {
        return stream;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public double getOpacity() {
        return opacity;
    }

    public boolean isChanges() {
        return changes;
    }

    public Vector<MetadataStream> getAudioStreams() {
        return audioStreams;
    }
}
