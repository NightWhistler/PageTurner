package net.nightwhistler.pageturner.view;

import android.graphics.Color;

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

        private int color;

        public HighLight( int index, int start, int end, int color ) {
            this.start = start;
            this.end =  end;
            this.index = index;
            this.color = color;
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

        public int getColor() {
            return color;
        }

        public void setColor( int color ) {
            this.color = color;
        }
    }

    private Map<String, List<HighLight>> highLights = new HashMap<String, List<HighLight>>();

    public void registerHighlight( String bookFile, int index, int start, int end ) {
        if ( ! highLights.containsKey(bookFile) ) {
            highLights.put( bookFile, new ArrayList<HighLight>() );
        }

        highLights.get(bookFile).add( new HighLight(index, start, end, Color.YELLOW));
    }

    public List<HighLight> getHighLights(String bookFile) {
        if ( ! highLights.containsKey(bookFile)) {
            return  new ArrayList<HighLight>();
        }

        return highLights.get( bookFile );
    }



}
