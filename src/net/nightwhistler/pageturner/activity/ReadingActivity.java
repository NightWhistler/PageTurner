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
package net.nightwhistler.pageturner.activity;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import android.widget.AdapterView;
import android.widget.ExpandableListView;
import com.google.inject.Inject;
import net.nightwhistler.pageturner.Configuration;
import net.nightwhistler.pageturner.R;
import net.nightwhistler.pageturner.TextUtil;
import net.nightwhistler.pageturner.dto.HighLight;
import net.nightwhistler.pageturner.dto.TocEntry;
import net.nightwhistler.pageturner.view.HighlightManager;
import roboguice.inject.InjectFragment;

import java.util.ArrayList;
import java.util.List;

public class ReadingActivity extends PageTurnerActivity {

    @InjectFragment(R.id.fragment_reading)
    private ReadingFragment readingFragment;

    @Inject
    private Configuration config;

    private int tocIndex = -1;
    private int highlightIndex = -1;


    @Override
    protected int getMainLayoutResource() {
        return R.layout.activity_reading;
    }

    @Override
    public void onDrawerClosed(View view) {
        getSupportActionBar().setTitle(R.string.app_name);
        super.onDrawerClosed(view);
    }

    @Override
    protected void initDrawerItems() {
        super.initDrawerItems();

        if ( this.readingFragment != null ) {
            List<TocEntry> tocList = this.readingFragment
                    .getTableOfContents();

            if (tocList != null && ! tocList.isEmpty()) {

                List<NavigationAdapter.NavigationChildItem> tocNames
                        = new ArrayList<NavigationAdapter.NavigationChildItem>();

                for ( TocEntry entry: tocList ) {
                    tocNames.add( new TocChildItem( entry ) );
                }

                getAdapter().setChildren(this.tocIndex, tocNames );
            }

            List<HighLight> highlights = this.readingFragment.getHighlights();

            if ( highlights != null && ! highlights.isEmpty() ) {
                List<NavigationAdapter.NavigationChildItem> highlightItems =
                        new ArrayList<NavigationAdapter.NavigationChildItem>();

                for ( HighLight highLight: highlights ) {
                    highlightItems.add( new HighlightChildItem(highLight) );
                }

                getAdapter().setChildren(this.highlightIndex, highlightItems );

            }
        }

    }

    protected String[] getMenuItems( Configuration config ) {

        List<String> menuItems = new ArrayList<String>();

        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && config.isFullScreenEnabled() ) {
            menuItems.add("");
        }

        menuItems.add( config.getLastReadTitle() );
        menuItems.add( getString(R.string.library));
        menuItems.add( getString(R.string.download));

        if ( this.readingFragment != null ) {

            List<TocEntry> tocList = this.readingFragment
                    .getTableOfContents();

            if ( tocList != null && ! tocList.isEmpty() ) {
                menuItems.add( getString(R.string.toc_label));
                this.tocIndex = menuItems.size() - 1;
            }

            List<HighLight> highLights = this.readingFragment.getHighlights();

            if ( highLights != null && ! highLights.isEmpty() ) {
                menuItems.add( getString(R.string.highlights));
                this.highlightIndex = menuItems.size() - 1;
            }

        }

        return menuItems.toArray(new String[menuItems.size()]);
    }

    @Override
    public boolean onGroupClick(ExpandableListView expandableListView, View view, int i, long l) {

        int correctedIndex = getCorrectIndex(i);

        if ( correctedIndex == 0 || i == tocIndex || i == highlightIndex ) {
            return false;
        }

        return super.onGroupClick(expandableListView, view, correctedIndex, l);
    }

    private int getCorrectIndex( int i ) {

        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && config.isFullScreenEnabled() ) {
            return i - 1;
        } else {
            return i;
        }
    }

    @Override
    public boolean onChildClick(ExpandableListView expandableListView, View view, int i, int i2, long l) {

        super.onChildClick(expandableListView, view, i, i2, l );

        NavigationAdapter.NavigationChildItem childItem = getAdapter().getChild( i, i2 );

        if ( childItem instanceof TocChildItem ) {
            this.readingFragment.navigateTo( ( (TocChildItem) childItem ).getTocEntry() );
        } else if ( childItem instanceof HighlightChildItem ) {
            this.readingFragment.navigateTo( ( (HighlightChildItem) childItem ).getHighLight() );
        }

        return false;
    }

    @Override
    protected int getTheme(Configuration config) {
        int theme = config.getTheme();

        if ( config.isFullScreenEnabled() ) {
            if (config.getColourProfile() == Configuration.ColourProfile.NIGHT) {
                theme = R.style.DarkFullScreen;
            } else {
                theme = R.style.LightFullScreen;
            }
        }

        return theme;
    }

    @Override
    protected void onCreatePageTurnerActivity(Bundle savedInstanceState) {

        Class<? extends PageTurnerActivity> lastActivityClass = config.getLastActivity();

        if ( !config.isAlwaysOpenLastBook() && lastActivityClass != null
                && lastActivityClass != ReadingActivity.class ) {
            Intent intent = new Intent(this, lastActivityClass);

            startActivity(intent);
            finish();
        }

    }

    @Override
    public boolean onSearchRequested() {
        readingFragment.onSearchRequested();
        return true;
    }

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		readingFragment.onWindowFocusChanged(hasFocus);
	}
	
	public void onMediaButtonEvent(View view) {
		this.readingFragment.onMediaButtonEvent(view.getId());
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return readingFragment.onTouchEvent(event);
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		return readingFragment.dispatchKeyEvent(event);
	}

    @Override
    protected void beforeLaunchActivity() {
        readingFragment.saveReadingPosition();
        readingFragment.getBookView().releaseResources();
    }

    private class TocChildItem implements NavigationAdapter.NavigationChildItem {

        private TocEntry tocEntry;

        public TocChildItem( TocEntry tocEntry ) {
            this.tocEntry = tocEntry;
        }

        @Override
        public String getTitle() {
            return this.tocEntry.getTitle();
        }

        @Override
        public String getSubtitle() {
            return "";
        }

        public TocEntry getTocEntry() {
            return tocEntry;
        }
    }

    private class HighlightChildItem implements NavigationAdapter.NavigationChildItem {

        private HighLight highLight;

        public HighlightChildItem( HighLight highLight ) {
            this.highLight = highLight;
        }

        @Override
        public String getSubtitle() {

            String text = highLight.getPercentage() + "%";

            if ( highLight.getPageNumber() != -1 ) {
                text = String.format( getString(R.string.page_number_of),
                        highLight.getPageNumber(), highLight.getTotalPages() )
                        + " (" + highLight.getPercentage() + "%)";
            }

            if ( highLight.getTextNote() != null && highLight.getTextNote().trim().length() > 0 ) {
                text += ": " + TextUtil.shortenText( highLight.getTextNote() );
            }

            return text;
        }

        @Override
        public String getTitle() {
            return highLight.getDisplayText();
        }

        public HighLight getHighLight() {
            return highLight;
        }

    }
}
