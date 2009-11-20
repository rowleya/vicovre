package com.googlecode.vicovre.web.play.metadata;

import java.util.Comparator;

public class ThumbSorter implements Comparator<Thumbnail> {

    /**
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare(Thumbnail arg0, Thumbnail arg1) {
        return (int) (arg0.getStart() - arg1.getStart());
    }

}