package net.nightwhistler.pageturner.dto;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: alex
 * Date: 6/17/13
 * Time: 11:54 AM
 * To change this template use File | Settings | File Templates.
 */
public class HighLight {

    private static enum Fields { displayText, textNote, index, start, end, color };

    private String textNote;

    private String displayText;

    private int index;
    private int start;
    private int end;

    private int color;

    public HighLight( String displayText, int index, int start, int end, int color ) {
        this.start = start;
        this.end =  end;
        this.index = index;
        this.color = color;
        this.displayText = displayText;
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

    public void setTextNote(String note) {
        this.textNote = note;
    }

    public String getTextNote() {
        return textNote;
    }

    public String getDisplayText() {
        return this.displayText;
    }

    public static String toJSON( List<HighLight> highLights ) {
        try {
            ArrayList<JSONObject> result = new ArrayList<JSONObject>();

            for ( HighLight highLight: highLights ) {
                JSONObject jsonObject = new JSONObject();

                jsonObject.put( Fields.index.name(), highLight.getIndex() );
                jsonObject.put( Fields.start.name(), highLight.getStart() );
                jsonObject.put( Fields.end.name(), highLight.getEnd() );
                jsonObject.put( Fields.color.name(), highLight.getColor() );
                jsonObject.put( Fields.displayText.name(), highLight.getDisplayText() );

                if ( highLight.getTextNote() != null ) {
                    jsonObject.put( Fields.textNote.name(), highLight.getTextNote() );
                }

                result.add( jsonObject );
            }

            return new JSONArray(result).toString();

        } catch (JSONException json) {
            throw new IllegalArgumentException( "Could not serialize to json", json );
        }
    }

    public static List<HighLight> fromJSON( String fileName, String jsonSource ) {

        try {
            JSONArray jsonArray = new JSONArray(jsonSource);
            List<HighLight> result = new ArrayList<>();

            for (int i = 0; i < jsonArray.length(); i++) {

                JSONObject json = jsonArray.getJSONObject(i);

                HighLight highLight = new HighLight(
                        json.getString(Fields.displayText.name()),
                        json.getInt(Fields.index.name()),
                        json.getInt(Fields.start.name()),
                        json.getInt(Fields.end.name()),
                        json.getInt(Fields.color.name()));

                highLight.setTextNote( json.optString( Fields.textNote.name() ));

                result.add(highLight);
            }

            return result;

        } catch ( JSONException json ) {
            throw new IllegalArgumentException( "Unreadable JSONArray", json );
        }
    }
}
