package net.nightwhistler.pageturner.dto;

/**
 * Entry in a Table of Contents
 */
public class TocEntry {

    private String title;
    private String href;

    public TocEntry(String title, String href) {
        this.title = title;
        this.href = href;
    }

    public String getHref() {
        return href;
    }

    public String getTitle() {
        return title;
    }

}
