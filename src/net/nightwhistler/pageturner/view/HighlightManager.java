package net.nightwhistler.pageturner.view;

import android.graphics.Color;
import android.util.Log;
import com.google.inject.Inject;
import net.nightwhistler.pageturner.Configuration;
import net.nightwhistler.pageturner.dto.HighLight;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Manages highlights for a book.
 *
 * @author Alex Kuiper
 */
public class HighlightManager {

    private List<HighLight> currentHighlights = new ArrayList<HighLight>();
    private String currentFileName;

    private static final HighlightComparator COMP = new HighlightComparator();

    @Inject
    private Configuration config;

    private void updateBookFile( String fileName ) {

        if ( currentFileName != null && !currentFileName.equals(fileName) ) {
            saveHighLights();
        }

        if ( fileName == null ) {
            this.currentHighlights = new ArrayList<HighLight>();
        } else if ( ! fileName.equals(currentFileName) ) {
            this.currentHighlights = config.getHightLights(fileName);
            sort( this.currentHighlights );
        }

        this.currentFileName = fileName;
    }

    public synchronized  void registerHighlight( String bookFile, String displayText, int index, int start, int end ) {

        updateBookFile(bookFile);

        currentHighlights.add(new HighLight(displayText, index, start, end, Color.YELLOW));
        saveHighLights();
    }

    public synchronized void removeHighLight( HighLight highLight ) {
        currentHighlights.remove(highLight);
        saveHighLights();
    }

    public synchronized List<HighLight> getHighLights(String bookFile) {
        updateBookFile(bookFile);

        return Collections.unmodifiableList( this.currentHighlights );
    }

    public synchronized void saveHighLights() {
        if ( currentFileName != null && currentHighlights != null ) {
            Log.d("HighlightManager", "Storing highlights for file " + currentFileName + ": "
                    + currentHighlights.size() + " items.");

            sort( this.currentHighlights );
            config.storeHighlights(currentFileName, currentHighlights);
        }
    }

    private static void sort( List<HighLight> highLights ) {
        Collections.sort(highLights, COMP);
    }

    private static class HighlightComparator implements Comparator<HighLight> {
        @Override
        public int compare(HighLight lhs, HighLight rhs) {

            Integer left;
            Integer right;

            if ( lhs.getIndex() != rhs.getIndex() ) {
                left = lhs.getIndex();
                right = rhs.getIndex();
            } else {
                left = lhs.getStart();
                right = rhs.getStart();
            }

            return left.compareTo( right );
        }
    }


}
