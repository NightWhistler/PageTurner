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

import java.util.List;
import java.util.Locale;

import net.nightwhistler.htmlspanner.FontFamily;
import roboguice.inject.ContextSingleton;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;

import com.google.inject.Inject;

/**
 * Application configuration class which provides a friendly API to the various
 * settings available.
 * 
 * @author Alex Kuiper
 * 
 */
@ContextSingleton
public class Configuration {

	private SharedPreferences settings;
	private Context context;

	private FontFamily cachedFamily;

	public static enum ScrollStyle {
		ROLLING_BLIND, PAGE_TIMER
	}

	public static enum AnimationStyle {
		CURL, SLIDE, NONE
	}

	public static enum OrientationLock {
		PORTRAIT, LANDSCAPE, REVERSE_PORTRAIT, REVERSE_LANDSCAPE, NO_LOCK
	}

	public static enum ColourProfile {
		DAY, NIGHT
	}

	public static enum LibraryView {
		BOOKCASE, LIST
	}

	public static enum CoverLabelOption {
		ALWAYS, NEVER, WITHOUT_COVER
	}

	public static enum LibrarySelection {
		BY_LAST_READ, LAST_ADDED, UNREAD, BY_TITLE, BY_AUTHOR;
	}

	public static final String KEY_POS = "offset:";
	public static final String KEY_IDX = "index:";
	public static final String KEY_NAV_TAP_V = "nav_tap_v";
	public static final String KEY_NAV_TAP_H = "nav_tap_h";
	public static final String KEY_NAV_SWIPE_H = "nav_swipe_h";
	public static final String KEY_NAV_SWIPE_V = "nav_swipe_v";
	public static final String KEY_NAV_VOL = "nav_vol";

	public static final String KEY_EMAIL = "email";
	public static final String KEY_FULL_SCREEN = "full_screen";
	public static final String KEY_COPY_TO_LIB = "copy_to_library";
	public static final String KEY_STRIP_WHITESPACE = "strip_whitespace";
	public static final String KEY_SCROLLING = "scrolling";

	public static final String KEY_LAST_FILE = "last_file";
	public static final String KEY_DEVICE_NAME = "device_name";
	public static final String KEY_TEXT_SIZE = "itext_size";

	public static final String KEY_MARGIN_H = "margin_h";
	public static final String KEY_MARGIN_V = "margin_v";

	public static final String KEY_LINE_SPACING = "line_spacing";

	public static final String KEY_NIGHT_MODE = "night_mode";
	public static final String KEY_SCREEN_ORIENTATION = "screen_orientation";
	public static final String KEY_FONT_FACE = "font_face";

	public static final String PREFIX_DAY = "day";
	public static final String PREFIX_NIGHT = "night";

	public static final String KEY_BRIGHTNESS = "bright";
	public static final String KEY_BACKGROUND = "bg";
	public static final String KEY_LINK = "link";
	public static final String KEY_TEXT = "text";

	public static final String KEY_BRIGHTNESS_CTRL = "set_brightness";
	public static final String KEY_SCROLL_STYLE = "scroll_style";
	public static final String KEY_SCROLL_SPEED = "scroll_speed";

	public static final String KEY_H_ANIMATION = "h_animation";
	public static final String KEY_V_ANIMATION = "v_animation";

	public static final String KEY_LIB_VIEW = "library_view";
	public static final String KEY_LIB_SEL = "library_selection";

	public static final String ACCESS_KEY = "access_key";
	public static final String CALIBRE_SERVER = "calibre_server";
	public static final String CALIBRE_USER = "calibre_user";
	public static final String CALIBRE_PASSWORD = "calibre_password";

	public static final String KEY_COVER_LABELS = "cover_labels";

	public static final String KEY_KEEP_SCREEN_ON = "keep_screen_on";

	public static final String KEY_OFFSETS = "offsets";
	
	private static final String KEY_SHOW_PAGENUM = "show_pagenum";

	@Inject
	public Configuration(Context context) {
		this.settings = PreferenceManager.getDefaultSharedPreferences(context);
		this.context = context;
	}

	public boolean isVerticalTappingEnabled() {
		return settings.getBoolean(KEY_NAV_TAP_V, true);
	}

	public boolean isHorizontalTappingEnabled() {
		return settings.getBoolean(KEY_NAV_TAP_H, true);
	}

	public boolean isHorizontalSwipeEnabled() {
		return settings.getBoolean(KEY_NAV_SWIPE_H, true);
	}

	public boolean isVerticalSwipeEnabled() {
		return settings.getBoolean(KEY_NAV_SWIPE_V, true)
				&& !isScrollingEnabled();
	}

	public int getLastPosition(String fileName) {

		String bookHash = Integer.toHexString(fileName.hashCode());

		int pos = settings.getInt(KEY_POS + bookHash, -1);

		if (pos != -1) {
			return pos;
		}

		// Fall-back for older settings.
		return settings.getInt(KEY_POS + fileName, -1);
	}

	public void setPageOffsets(String fileName, List<List<Integer>> offsets) {
		String bookHash = Integer.toHexString(fileName.hashCode());

		PageOffsets offsetsObject = PageOffsets.fromValues(this, offsets);
		String json = offsetsObject.toJSON();
		updateValue(KEY_OFFSETS + bookHash, json );
	}

	public List<List<Integer>> getPageOffsets(String fileName) {
		String bookHash = Integer.toHexString(fileName.hashCode());
		String data = settings.getString(KEY_OFFSETS + bookHash, "");

		PageOffsets offsets = PageOffsets.fromJSON(data);
		
		if ( offsets == null || ! offsets.isValid(this) ) {
			return null;
		}
		
		return offsets.getOffsets();
	}

	public void setLastPosition(String fileName, int position) {
		String bookHash = Integer.toHexString(fileName.hashCode());
		updateValue(KEY_POS + bookHash, position);
	}

	public int getLastIndex(String fileName) {
		String bookHash = Integer.toHexString(fileName.hashCode());

		int pos = settings.getInt(KEY_IDX + bookHash, -1);

		if (pos != -1) {
			return pos;
		}

		// Fall-back for older settings.
		return settings.getInt(KEY_IDX + fileName, -1);
	}

	public void setLastIndex(String fileName, int index) {
		String bookHash = Integer.toHexString(fileName.hashCode());
		updateValue(KEY_IDX + bookHash, index);
	}

	public boolean isVolumeKeyNavEnabled() {
		return settings.getBoolean(KEY_NAV_VOL, false);
	}

	public String getSynchronizationEmail() {
		return settings.getString(KEY_EMAIL, "").trim();
	}

	public boolean isShowPageNumbers() {
		return settings.getBoolean(KEY_SHOW_PAGENUM, false);
	}
	
	public String getSynchronizationAccessKey() {
		return settings.getString(ACCESS_KEY, "").trim();
	}

	public boolean isSyncEnabled() {
		String email = getSynchronizationEmail();
		String accessKey = getSynchronizationAccessKey();

		return email.length() > 0 && accessKey.length() > 0;
	}

	public boolean isFullScreenEnabled() {
		return settings.getBoolean(KEY_FULL_SCREEN, false);
	}

	public boolean isCopyToLibrayEnabled() {
		return settings.getBoolean(KEY_COPY_TO_LIB, true);
	}

	public boolean isStripWhiteSpaceEnabled() {
		return settings.getBoolean(KEY_STRIP_WHITESPACE, false);
	}

	public boolean isScrollingEnabled() {
		return settings.getBoolean(KEY_SCROLLING, false);
	}

	public String getLastOpenedFile() {
		return settings.getString(KEY_LAST_FILE, "");
	}

	public void setLastOpenedFile(String fileName) {
		updateValue(KEY_LAST_FILE, fileName);
	}

	public String getDeviceName() {
		return settings.getString(KEY_DEVICE_NAME, Build.MODEL);
	}

	public int getTextSize() {
		return settings.getInt(KEY_TEXT_SIZE, 16);
	}

	public void setTextSize(int textSize) {
		updateValue(KEY_TEXT_SIZE, textSize);
	}

	public int getHorizontalMargin() {
		return settings.getInt(KEY_MARGIN_H, 15);
	}

	public int getVerticalMargin() {
		return settings.getInt(KEY_MARGIN_V, 15);
	}

	public int getLineSpacing() {
		return settings.getInt(KEY_LINE_SPACING, 0);
	}

	public boolean isKeepScreenOn() {
		return settings.getBoolean(KEY_KEEP_SCREEN_ON, false);
	}

	public int getTheme() {
		if (getColourProfile() == ColourProfile.NIGHT) {
			return R.style.Theme_Sherlock;
		} else {
			return R.style.Theme_Sherlock_Light_DarkActionBar;
		}

	}

	public void setColourProfile(ColourProfile profile) {
		if (profile == ColourProfile.DAY) {
			updateValue(KEY_NIGHT_MODE, false);
		} else {
			updateValue(KEY_NIGHT_MODE, true);
		}
	}

	public CoverLabelOption getCoverLabelOption() {
		return CoverLabelOption.valueOf(settings.getString(KEY_COVER_LABELS,
				CoverLabelOption.ALWAYS.name().toLowerCase(Locale.US)));
	}

	public ColourProfile getColourProfile() {
		if (settings.getBoolean(KEY_NIGHT_MODE, false)) {
			return ColourProfile.NIGHT;
		} else {
			return ColourProfile.DAY;
		}
	}

	public OrientationLock getScreenOrientation() {
		String orientation = settings.getString(KEY_SCREEN_ORIENTATION,
				OrientationLock.NO_LOCK.name().toLowerCase(Locale.US));
		return OrientationLock.valueOf(orientation.toUpperCase(Locale.US));
	}

	private void updateValue(String key, Object value) {

		SharedPreferences.Editor editor = settings.edit();

		if (value instanceof String) {
			editor.putString(key, (String) value);
		} else if (value instanceof Integer) {
			editor.putInt(key, (Integer) value);
		} else if (value instanceof Boolean) {
			editor.putBoolean(key, (Boolean) value);
		} else {
			throw new IllegalArgumentException("Unsupported type: "
					+ value.getClass().getSimpleName());
		}

		editor.commit();
	}

	private FontFamily loadFamilyFromAssets(String key, String baseName) {
		Typeface basic = Typeface.createFromAsset(context.getAssets(), baseName
				+ ".otf");
		Typeface boldFace = Typeface.createFromAsset(context.getAssets(),
				baseName + "-Bold.otf");
		Typeface italicFace = Typeface.createFromAsset(context.getAssets(),
				baseName + "-Italic.otf");
		Typeface biFace = Typeface.createFromAsset(context.getAssets(),
				baseName + "-BoldItalic.otf");

		FontFamily fam = new FontFamily(key, basic);
		fam.setBoldTypeface(boldFace);
		fam.setItalicTypeface(italicFace);
		fam.setBoldItalicTypeface(biFace);

		return fam;
	}

	public FontFamily getFontFamily() {

		String fontFace = settings.getString(KEY_FONT_FACE, "gen_book_bas");

		if (cachedFamily != null && fontFace.equals(cachedFamily.getName())) {
			return cachedFamily;
		}

		if ("gen_book_bas".equals(fontFace)) {
			return this.cachedFamily = loadFamilyFromAssets(fontFace,
					"GentiumBookBasic");
		}
		if ("gen_bas".equals(fontFace)) {
			return this.cachedFamily = loadFamilyFromAssets(fontFace,
					"GentiumBasic");
		}

		Typeface face = Typeface.SANS_SERIF;

		if ("sans".equals(fontFace)) {
			face = Typeface.SANS_SERIF;
		} else if ("serif".equals(fontFace)) {
			face = Typeface.SERIF;
		} else if ("mono".equals(fontFace)) {
			face = Typeface.MONOSPACE;
		}

		return this.cachedFamily = new FontFamily(fontFace, face);
	}

	public int getBrightNess() {
		// Brightness 0 means black screen :)
		return Math.max(1, getProfileSetting(KEY_BRIGHTNESS, 50, 50));
	}

	public void setBrightness(int brightness) {
		if (getColourProfile() == ColourProfile.DAY) {
			updateValue("day_bright", brightness);
		} else {
			updateValue("night_bright", brightness);
		}
	}

	public int getBackgroundColor() {
		return getProfileSetting(KEY_BACKGROUND, Color.WHITE, Color.BLACK);
	}

	public int getTextColor() {
		return getProfileSetting(KEY_TEXT, Color.BLACK, Color.GRAY);
	}

	public int getLinkColor() {
		return getProfileSetting(KEY_LINK, Color.BLUE, Color.rgb(255, 165, 0));
	}

	private int getProfileSetting(String setting, int dayDefault,
			int nightDefault) {

		if (getColourProfile() == ColourProfile.NIGHT) {
			return settings.getInt(PREFIX_NIGHT + "_" + setting, nightDefault);
		} else {
			return settings.getInt(PREFIX_DAY + "_" + setting, dayDefault);
		}

	}

	public boolean isBrightnessControlEnabled() {
		return settings.getBoolean(KEY_BRIGHTNESS_CTRL, false);
	}

	public ScrollStyle getAutoScrollStyle() {
		String style = settings.getString(KEY_SCROLL_STYLE,
				ScrollStyle.ROLLING_BLIND.name().toLowerCase(Locale.US));
		if ("rolling_blind".equals(style)) {
			return ScrollStyle.ROLLING_BLIND;
		} else {
			return ScrollStyle.PAGE_TIMER;
		}
	}

	public int getScrollSpeed() {
		return settings.getInt(KEY_SCROLL_SPEED, 20);
	}

	public AnimationStyle getHorizontalAnim() {
		String animH = settings.getString(KEY_H_ANIMATION, AnimationStyle.CURL
				.name().toLowerCase(Locale.US));
		return AnimationStyle.valueOf(animH.toUpperCase(Locale.US));
	}

	public AnimationStyle getVerticalAnim() {
		String animV = settings.getString(KEY_V_ANIMATION, AnimationStyle.SLIDE
				.name().toLowerCase(Locale.US));
		return AnimationStyle.valueOf(animV.toUpperCase(Locale.US));
	}

	public LibraryView getLibraryView() {
		String libView = settings.getString(KEY_LIB_VIEW, LibraryView.BOOKCASE
				.name().toLowerCase(Locale.US));
		return LibraryView.valueOf(libView.toUpperCase(Locale.US));
	}

	public void setLibraryView(LibraryView viewStyle) {
		String libView = viewStyle.name().toLowerCase(Locale.US);
		updateValue(KEY_LIB_VIEW, libView);
	}

	public LibrarySelection getLastLibraryQuery() {
		String query = settings.getString(KEY_LIB_SEL,
				LibrarySelection.LAST_ADDED.name().toLowerCase(Locale.US));
		return LibrarySelection.valueOf(query.toUpperCase(Locale.US));
	}

	public void setLastLibraryQuery(LibrarySelection sel) {
		updateValue(KEY_LIB_SEL, sel.name().toLowerCase(Locale.US));
	}

	public String getCalibreServer() {
		return settings.getString(CALIBRE_SERVER, "");
	}

	public String getCalibreUser() {
		return settings.getString(CALIBRE_USER, "");
	}

	public String getCalibrePassword() {
		return settings.getString(CALIBRE_PASSWORD, "");
	}

	public String getStorageBase() {
		return Environment.getExternalStorageDirectory().getAbsolutePath();
	}

	public String getPageTurnerFolder() {
		return getStorageBase() + "/PageTurner";
	}

	public String getDownloadsFolder() {
		return getPageTurnerFolder() + "/Downloads";
	}

	public String getLibraryFolder() {
		return getPageTurnerFolder() + "/Books";
	}

	/*
	  Returns the bytes of available memory left on the heap. Not
	  totally sure if it works reliably.
	 */
	public long getAvailableBytesOfMemory()
	{
		Runtime runtime = Runtime.getRuntime();
		long maxHeapMemoryBytes = runtime.maxMemory();
		long allocatedMemoryBytes = runtime.totalMemory() - runtime.freeMemory();
		return (maxHeapMemoryBytes - allocatedMemoryBytes);
	}
}
