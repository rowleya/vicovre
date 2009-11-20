package com.googlecode.vicovre.web.play.metadata;

public class Thumbnail {

    private double start = 0;

    private double end = 0;

    private String filename = null;

    public Thumbnail(double start, double end, String filename) {
        this.start = start;
        this.end = end;
        this.filename = filename;
    }

    public void setEnd(double end) {
        this.end = end;
    }

    /**
     * Returns the end
     *
     * @return the end
     */
    public double getEnd() {
        return end;
    }

    /**
     * Returns the filename
     *
     * @return the filename
     */
    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    /**
     * Returns the start
     *
     * @return the start
     */
    public double getStart() {
        return start;
    }

    public boolean equals(Object o) {
        if (!(o instanceof Thumbnail)) {
            return false;
        }

        Thumbnail t = (Thumbnail) o;
        if (!t.filename.equals(filename)) {
            return false;
        }
        if (t.start != start) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        String hash = filename + start;
        return hash.hashCode();
    }

}