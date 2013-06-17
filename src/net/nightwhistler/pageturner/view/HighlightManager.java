package net.nightwhistler.pageturner.view;

import android.graphics.Color;
import com.google.inject.Inject;
import net.nightwhistler.pageturner.Configuration;
import net.nightwhistler.pageturner.dto.HighLight;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: alex
 * Date: 6/8/13
 * Time: 8:33 PM
 * To change this template use File | Settings | File Templates.
 */
public class HighlightManager {

    private List<HighLight> currentHighlights = new ArrayList<HighLight>();
    private String currentFileName;

    @Inject
    private Configuration config;

    private void updateBookFile( String fileName ) {

        if ( fileName == null ) {
            this.currentHighlights = new ArrayList<HighLight>();
        } else if ( ! fileName.equals(currentFileName) ) {
            saveHighLights();
            this.currentFileName = fileName;
            this.currentHighlights = config.getHightLights(fileName);
        }

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
        return Collections.unmodifiableList( currentHighlights );
    }

    public synchronized void saveHighLights() {
        if ( currentFileName != null && currentHighlights != null ) {
            config.storeHighlights(currentFileName, currentHighlights);
        }
    }


}
