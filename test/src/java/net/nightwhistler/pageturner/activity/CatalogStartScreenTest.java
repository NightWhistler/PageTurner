package net.nightwhistler.pageturner.activity;

import net.nightwhistler.pageturner.R;

import net.nightwhistler.pageturner.catalog.CatalogFragment;

import org.robolectric.RobolectricTestRunner;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.widget.ListView;
import org.robolectric.annotation.Config;

@Config(emulateSdk = 18)
@RunWith(RobolectricTestRunner.class)
public class CatalogStartScreenTest {

	/**
	 * In this test we successfully get the main
	 * items list from the site, and display it.
	 */
	@Test
	public void showStartScreenSuccess() {
	    
        CatalogFragment fragment = new CatalogFragment();
		
		
	}
	
}
