package net.nightwhistler.pageturner.dto;

/**
 * Created with IntelliJ IDEA.
 * User: alex
 * Date: 9/1/13
 * Time: 9:02 PM
 * To change this template use File | Settings | File Templates.
 */
public class SearchResult {

    private String query;
    private String display;
    private int index;
    private int start;
    private int end;

    public SearchResult(String query, String display, int index, int offset, int end) {
        this.query = query;
        this.display = display;
        this.index = index;
        this.start = offset;
        this.end = end;
    }

    public String getDisplay() {
        return display;
    }

    public int getIndex() {
        return index;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public String getQuery() {
        return query;
    }

}
