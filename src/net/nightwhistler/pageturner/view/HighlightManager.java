package net.nightwhistler.pageturner.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: alex
 * Date: 6/8/13
 * Time: 8:33 PM
 * To change this template use File | Settings | File Templates.
 */
public class HighlightManager {

    public static class HighLight {

        private int index;
        private int start;
        private int end;

        public HighLight( int index, int start, int end ) {
            this.start = start;
            this.end =  end;
            this.index = index;
        }

        public int getIndex() {
            return index;
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return  end;
        }


    }

    private Map<String, List<HighLight>> highLights = new HashMap<String, List<HighLight>>();

    public void registerHighlight( String bookFile, int index, int start, int end ) {
        if ( ! highLights.containsKey(bookFile) ) {
            highLights.put( bookFile, new ArrayList<HighLight>() );
        }

        highLights.get(bookFile).add( new HighLight(index, start, end));
    }

    public List<HighLight> getHighLights(String bookFile) {
        if ( ! highLights.containsKey(bookFile)) {
            return  new ArrayList<HighLight>();
        }

        return highLights.get( bookFile );
    }



}
