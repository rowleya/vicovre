package com.googlecode.vicovre.web.play.metadata;

public class MetadataAnnotation {

    private double start = 0;

    private double end = 0;

    private String nodeType = null;

    private String text = null;

    private String colour = null;
    private String name = null;
    private String url = null;
    private String email = null;

    public MetadataAnnotation(double start, double end, String nodeType,
            String text, String colour) {
        this.colour = "0x000000";
        this.start = start;
        this.end = end;
        this.nodeType = nodeType;
        this.text = text;
        if (colour != null) {
            this.colour = colour.replace("#", "0x");
        }
    }

    public MetadataAnnotation(double start, double end, String nodeType,
            String text, String colour, String name, String url,
            String email) {
        this.colour = "0x000000";
        this.start = start;
        this.end = end;
        this.nodeType = nodeType;
        this.text = text;
        if (colour != null) {
            this.colour = colour.replace("#", "0x");
        }
        this.name = name;
        this.url = url;
        this.email = email;
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
     * Returns the text
     *
     * @return the text
     */
    public String getText() {
        return text;
    }

    /**
     * Returns the nodeType
     *
     * @return the nodeType
     */
    public String getNodeType() {
        return nodeType;
    }

    /**
     * Returns the start
     *
     * @return the start
     */
    public double getStart() {
        return start;
    }

    /**
     * Returns the colour
     *
     * @return the colour
     */
    public String getColour() {
        return colour;
    }

    /**
     * Gets the text
     *
     * @return The type
     */
    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public String getEmail() {
        return email;
    }

    public boolean equals(Object o) {
        if (!(o instanceof MetadataAnnotation)) {
            return false;
        }
        MetadataAnnotation a = (MetadataAnnotation) o;
        if (!a.nodeType.equals(nodeType)) {
            return false;
        }
        if (!a.text.equals(text)) {
            return false;
        }
        if (a.start != start) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        String hash = nodeType + text + start;
        return hash.hashCode();
    }

    public boolean update(double startTime, String text) {
        if ((start <= startTime) && (end >= startTime)) {
            this.text = text;
            return true;
        }
        return false;
    }
}