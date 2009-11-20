package com.googlecode.vicovre.web.play.metadata;

public class TextThumbnail extends Thumbnail {

    private String text = null;
    private String type = null;
    private String name = null;
    private String url = null;
    private String email = null;
    private String icon = null;

    public TextThumbnail(double start, double end, String filename,
            String text, String type) {
        super(start, end, filename);
        this.text = text;
        this.type = type;
    }

    public TextThumbnail(double start, double end, String icon,
            String text, String type, String name, String url, String email) {
        super(start, end, null);
        this.text = text;
        this.icon = icon;
        this.type = type;
        this.name = name;
        this.url = url;
        this.email = email;
    }

    /**
     * Gets the text
     *
     * @return The text
     */
    public String getText() {
        return text;
    }

    /**
     * Gets the text
     *
     * @return The type
     */
    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getIcon() {
        return icon;
    }

    public String getUrl() {
        return url;
    }

    public String getEmail() {
        return email;
    }

    public boolean equals(Object o) {
        if (!(o instanceof TextThumbnail)) {
            return false;
        }
        TextThumbnail t = (TextThumbnail) o;
        if (!t.type.equals(type)) {
            return false;
        }
        if (!t.text.equals(text)) {
            return false;
        }
        return super.equals(o);
    }

    public int hashCode() {
        String hash = type + text;
        return hash.hashCode();
    }
}
