package net.nightwhistler.pageturner.activity;

import net.nightwhistler.pageturner.R;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import android.widget.ListView;

public class CatalogStartScreenTest {

	/**
	 * In this test we succesfully get the main
	 * items list from the site, and display it.
	 */
	@Test
	@Ignore
	public void showStartScreenSuccess() {
		
		CatalogActivity catalog = new CatalogActivity();
		catalog.onCreate(null);
		
		ListView listView = (ListView) catalog.findViewById( R.id.catalogList );
		
		Assert.assertNotNull(listView);
		
		
	}
	
}
