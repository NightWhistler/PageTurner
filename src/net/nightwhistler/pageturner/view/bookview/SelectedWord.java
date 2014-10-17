package net.nightwhistler.pageturner.view.bookview;

/**
 * Created by alex on 10/14/14.
 */
public class SelectedWord {

    private int startOffset;
    private int endOffset;
    private CharSequence text;

    public SelectedWord( int startOffset, int endOffset, CharSequence text ) {
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.text = text;
    }

    public int getStartOffset() {
        return startOffset;
    }

    public int getEndOffset() {
        return endOffset;
    }

    public CharSequence getText() {

        if ( text == null ) {
            return "";
        }

        return text;
    }
}