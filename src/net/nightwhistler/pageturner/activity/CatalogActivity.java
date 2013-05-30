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
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import com.actionbarsherlock.view.Window;
import com.github.rtyley.android.sherlock.roboguice.activity.RoboSherlockFragmentActivity;
import net.nightwhistler.nucular.atom.Feed;
import net.nightwhistler.pageturner.Configuration;
import net.nightwhistler.pageturner.PageTurner;
import net.nightwhistler.pageturner.R;
import net.nightwhistler.pageturner.catalog.BookDetailsFragment;
import net.nightwhistler.pageturner.catalog.CatalogFragment;
import net.nightwhistler.pageturner.catalog.CatalogParent;
import roboguice.RoboGuice;
import roboguice.inject.InjectFragment;

import javax.annotation.Nullable;

public class CatalogActivity extends RoboSherlockFragmentActivity implements CatalogParent {

    @InjectFragment(R.id.fragment_catalog)
    private CatalogFragment catalogFragment;

    @Nullable
    @InjectFragment(R.id.fragment_book_details)
    private BookDetailsFragment detailsFragment;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Configuration config = RoboGuice.getInjector(this).getInstance(Configuration.class); 
		PageTurner.changeLanguageSetting(this, config);
		setTheme( config.getTheme() );
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_catalog);

        hideDetailsView();
    }

    private void hideDetailsView() {
        if ( detailsFragment != null ) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.hide(detailsFragment);
            ft.commitAllowingStateLoss();
        }
    }

    private boolean isTwoPaneView() {
        return  getResources().getConfiguration().orientation
                == android.content.res.Configuration.ORIENTATION_LANDSCAPE
                && detailsFragment != null;
    }

    @Override
    public void onFeedReplaced(Feed feed) {

        if ( isTwoPaneView() && feed.getSize() == 1
                && feed.getEntries().get(0).getEpubLink() != null ) {
            loadFakeFeed(feed);
        } else {
            hideDetailsView();
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        setSupportProgressBarIndeterminate(true);
        setSupportProgressBarIndeterminateVisibility(true);
    }

    @Override
    public void loadFakeFeed(Feed fakeFeed) {

        if ( isTwoPaneView() ) {

            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
            ft.show(detailsFragment);
            ft.commit();

            detailsFragment.setNewFeed(fakeFeed, null);
        } else {
            Intent intent = new Intent( this, CatalogBookDetailsActivity.class );
            intent.putExtra("fakeFeed", fakeFeed);

            startActivity(intent);
        }
    }

    @Override
    public void loadFeedFromUrl(String url) {
        catalogFragment.loadURL(url);
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
