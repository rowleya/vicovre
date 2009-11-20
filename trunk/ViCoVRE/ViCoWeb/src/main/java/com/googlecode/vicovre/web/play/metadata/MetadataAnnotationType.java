package com.googlecode.vicovre.web.play.metadata;

public class MetadataAnnotationType
        implements Comparable<MetadataAnnotationType> {

    private String type = null;

    private String icon = null;

    private String text = null;

    private long index = -1;

    public MetadataAnnotationType(String type, String icon, String text,
            long index) {
        this.type = type;
        this.icon = icon;
        this.text = text;
        this.index = index;
    }

    /**
     * Returns the type
     *
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * Returns the icon
     *
     * @return the icon
     */
    public String getIcon() {
        return icon;
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
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object o) {
        if (!(o instanceof MetadataAnnotationType)) {
            return false;
        }
        MetadataAnnotationType a = (MetadataAnnotationType) o;
        return a.type.equals(type);
    }

    /**
     *
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return type.hashCode();
    }

    public int compareTo(MetadataAnnotationType o) {
        if (index == -1) {
            return 1;
        }
        if (o.index == -1) {
            return -1;
        }
        long diff = index - o.index;
        return ((int) diff);
    }
}