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

import android.content.Intent;
import com.actionbarsherlock.view.Window;
import net.nightwhistler.nucular.atom.Entry;
import net.nightwhistler.nucular.atom.Feed;
import roboguice.RoboGuice;
import net.nightwhistler.pageturner.Configuration;
import net.nightwhistler.pageturner.PageTurner;
import net.nightwhistler.pageturner.R;
import net.nightwhistler.pageturner.catalog.CatalogFragment;
import android.os.Bundle;
import android.view.KeyEvent;

import com.github.rtyley.android.sherlock.roboguice.activity.RoboSherlockFragmentActivity;

public class CatalogActivity extends RoboSherlockFragmentActivity {
	private CatalogFragment catalogFragment;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Configuration config = RoboGuice.getInjector(this).getInstance(Configuration.class); 
		PageTurner.changeLanguageSetting(this, config);
		setTheme( config.getTheme() );
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_catalog);
		catalogFragment = (CatalogFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_catalog);
	}

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        setSupportProgressBarIndeterminate(true);
        setSupportProgressBarIndeterminateVisibility(true);
    }

    public void loadFakeFeed(Entry entry) {

        Feed originalFeed = entry.getFeed();

        Feed fakeFeed = new Feed();
        fakeFeed.addEntry(entry);
        fakeFeed.setTitle(entry.getTitle());
        fakeFeed.setDetailFeed(true);
        fakeFeed.setURL(originalFeed.getURL());

        Intent intent = new Intent( this, CatalogBookDetailsActivity.class );
        intent.putExtra("fakeFeed", fakeFeed);

        startActivity(intent);
    }

    @Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		return catalogFragment.dispatchKeyEvent(event);
	}

	// TODO Refactor this. Let the platform push/pop fragments from the fragment stack.
	@Override
	public void onBackPressed() {
		catalogFragment.onBackPressed();
	}

    @Override
    public boolean onSearchRequested() {
        catalogFragment.onSearchRequested();
        return true;
    }
}
