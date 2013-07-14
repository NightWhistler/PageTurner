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


import android.os.Bundle;

import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import android.widget.AdapterView;
import com.google.inject.Inject;
import net.nightwhistler.pageturner.Configuration;
import net.nightwhistler.pageturner.R;
import roboguice.inject.InjectFragment;

public class ReadingActivity extends PageTurnerActivity {

    @InjectFragment(R.id.fragment_reading)
    private ReadingFragment readingFragment;

    @Inject
    private Configuration config;

    @Override
    protected int getMainLayoutResource() {
        return R.layout.activity_reading;
    }

    @Override
    public void onDrawerClosed(View view) {
        getSupportActionBar().setTitle(R.string.app_name);
        super.onDrawerClosed(view);
    }

    protected String[] getMenuItems( Configuration config ) {
        if ( config.isFullScreenEnabled() ) {
            return array("", config.getLastReadTitle(), getString(R.string.library), getString(R.string.download));
        } else {
            return array(config.getLastReadTitle(), getString(R.string.library), getString(R.string.download));
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

        if ( config.isFullScreenEnabled() ) {
            super.onItemClick(adapterView, view, i-1, l);
        } else {
            super.onItemClick(adapterView, view, i, l);
        }

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
}
