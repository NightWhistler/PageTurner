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
import com.actionbarsherlock.view.Window;
import com.github.rtyley.android.sherlock.roboguice.activity.RoboSherlockFragmentActivity;
import net.nightwhistler.pageturner.Configuration;
import net.nightwhistler.pageturner.PageTurner;
import net.nightwhistler.pageturner.R;
import roboguice.RoboGuice;
import roboguice.inject.InjectFragment;

public class LibraryActivity extends PageTurnerActivity {

    @InjectFragment(R.id.fragment_library)
    private LibraryFragment libraryFragment;

    @Override
    protected int getMainLayoutResource() {
        return R.layout.activity_library;
    }

	@Override
	public void onBackPressed() {
		libraryFragment.onBackPressed();
	}

    @Override
    public boolean onSearchRequested() {
        libraryFragment.onSearchRequested();
        return true;
    }
}
