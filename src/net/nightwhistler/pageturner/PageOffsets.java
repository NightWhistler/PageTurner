/*
 * Copyright (C) 2012 Alex Kuiper
 * 
 * This file is part of PageTurner
 *
 * PageTurner is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PageTurner is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PageTurner.  If not, see <http://www.gnu.org/licenses/>.*
 */
package net.nightwhistler.pageturner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * This class allows page-offsets to be read from and stored as JSON.
 * 
 * Page-offsets are only valid under the circumstances they were 
 * calculated with: if text-size, page-margins, etc. change, they
 * must be re-calculated. This class allows checks for this. 
 * 
 * @author Alex Kuiper
 *
 */
public class PageOffsets {

    public static int ALGORITHM_VERSION = 2;

	private int fontSize;
	private String fontFamily;
	
	private int vMargin;
	private int hMargin;
	
	private int lineSpacing;
	
	private boolean fullScreen;

    private int algorithmVersion;

	private List<List<Integer>> offsets;
	
	private static enum Fields { fontSize, fontFamily, vMargin, hMargin, lineSpacing, fullScreen, offsets, algorithmVersion };
	
	private PageOffsets() {}
	
	public boolean isValid( Configuration config ) {
		
		return
				this.fontFamily.equals( config.getDefaultFontFamily().getName() )
				&& this.fontSize == config.getTextSize()
				&& this.vMargin == config.getVerticalMargin()
				&& this.hMargin == config.getHorizontalMargin()
				&& this.lineSpacing == config.getLineSpacing()
				&& this.fullScreen == config.isFullScreenEnabled()
                && this.algorithmVersion == ALGORITHM_VERSION;
	}
	
	public List<List<Integer>> getOffsets() {
		return offsets;
	}
	
	public static PageOffsets fromValues( Configuration config, List<List<Integer>> offsets ) {
		PageOffsets result = new PageOffsets();
		result.fontFamily = config.getDefaultFontFamily().getName();
		result.fontSize = config.getTextSize();
		result.hMargin = config.getHorizontalMargin();
		result.vMargin = config.getVerticalMargin();
		result.lineSpacing = config.getLineSpacing();
		result.fullScreen = config.isFullScreenEnabled();
        result.algorithmVersion = ALGORITHM_VERSION;

		result.offsets = offsets;
		
		return result;
	}
	
	
	public static PageOffsets fromJSON( String json ) {
		try {
			
			JSONObject offsetsObject = new JSONObject( json );
			PageOffsets result = new PageOffsets();
			
			result.fontFamily = offsetsObject.getString(Fields.fontFamily.name());
			result.fontSize = offsetsObject.getInt(Fields.fontSize.name());
			result.vMargin = offsetsObject.getInt(Fields.vMargin.name());
			result.hMargin = offsetsObject.getInt(Fields.hMargin.name());
			result.lineSpacing = offsetsObject.getInt(Fields.lineSpacing.name());
			result.fullScreen = offsetsObject.getBoolean(Fields.fullScreen.name() );
            result.algorithmVersion = offsetsObject.optInt(Fields.algorithmVersion.name(), -1);
			
			result.offsets = readOffsets(offsetsObject.getJSONArray(Fields.offsets.name()));

			return result;

		} catch (JSONException j) {
			return null;
		}
	}
	
	public String toJSON() {
		try {
			JSONObject jsonObject = new JSONObject();
			
			jsonObject.put(Fields.fontFamily.name(), this.fontFamily );
			jsonObject.put(Fields.fontSize.name(), this.fontSize );
			jsonObject.put(Fields.vMargin.name(), this.vMargin );
			jsonObject.put(Fields.hMargin.name(), this.hMargin );
			jsonObject.put(Fields.lineSpacing.name(), this.lineSpacing );
			jsonObject.put(Fields.fullScreen.name(), this.fullScreen );
            jsonObject.put(Fields.algorithmVersion.name(), this.algorithmVersion );
			
			jsonObject.put(Fields.offsets.name(), new JSONArray( this.offsets ) );
			
			return jsonObject.toString();
		} catch (JSONException e) {
			return null;
		}
	}
	
	private static List<List<Integer>> readOffsets( JSONArray jsonArray ) throws JSONException {
		
		List<List<Integer>> result = new ArrayList<List<Integer>>();
		
		for (int i = 0; i < jsonArray.length(); i++) {
			List<Integer> sublist = new ArrayList<Integer>();

			JSONArray subArray = new JSONArray(jsonArray.getString(i));

			for (int j = 0; j < subArray.length(); j++) {
				int val = subArray.getInt(j);
				sublist.add(val);
			}

			result.add(sublist);
		}
		
		return result;
	}
	
}
