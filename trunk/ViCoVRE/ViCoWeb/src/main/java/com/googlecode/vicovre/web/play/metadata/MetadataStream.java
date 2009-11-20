package com.googlecode.vicovre.web.play.metadata;

import com.googlecode.vicovre.recordings.Stream;

public class MetadataStream {
        private long start = 0;
        private long end = 0;
        private String type;
        private String ssrc;
        private double duration;

        public MetadataStream(Stream stream, long realStart, long realEnd) {
            start = stream.getStartTime().getTime();
            if (start < realStart) {
                start = realStart;
            }
            end = stream.getEndTime().getTime();
            if (end > realEnd) {
                end = realEnd;
            }
            duration = ((double) (end - start)) / 1000;
            type = stream.getRtpType().getMediaType();
            ssrc = stream.getSsrc();
        }

        public long getStart() {
            return start;
        }

        public long getEnd() {
            return end;
        }

        public String getType() {
            return type;
        }

        public String getSsrc() {
            return ssrc;
        }

        public double getDuration() {
            return duration;
        }

    }