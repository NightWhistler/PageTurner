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

import roboguice.RoboGuice;
import net.nightwhistler.pageturner.Configuration;
import net.nightwhistler.pageturner.Configuration.ColourProfile;
import net.nightwhistler.pageturner.R;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.github.rtyley.android.sherlock.roboguice.activity.RoboSherlockFragmentActivity;

public class ReadingActivity extends RoboSherlockFragmentActivity {
	private ReadingFragment readingFragment;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {

        Configuration config = RoboGuice.getInjector(this).getInstance(Configuration.class);
        int theme = config.getTheme();

        if ( config.isFullScreenEnabled() ) {
            if (config.getColourProfile() == ColourProfile.NIGHT) {
			    theme = R.style.DarkFullScreen;
		    } else {
			    theme = R.style.LightFullScreen;
		    }
        }

		setTheme( theme );
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_reading);
		readingFragment = (ReadingFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_reading);
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		readingFragment.onWindowFocusChanged(hasFocus);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return readingFragment.onTouchEvent(event);
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		return readingFragment.dispatchKeyEvent(event);
	}
}
