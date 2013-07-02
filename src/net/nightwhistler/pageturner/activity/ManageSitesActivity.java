/*
 * Copyright (C) 2013 Alex Kuiper
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
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.*;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.github.rtyley.android.sherlock.roboguice.activity.RoboSherlockListActivity;
import com.google.inject.Inject;
import net.nightwhistler.pageturner.*;
import roboguice.RoboGuice;

import java.util.ArrayList;
import java.util.List;

public class ManageSitesActivity extends RoboSherlockListActivity {

	@Inject
	Configuration config;
	
	private CustomOPDSSiteAdapter adapter;
	
	private static enum ContextAction { EDIT, DELETE };
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Configuration config = RoboGuice.getInjector(this).getInstance(Configuration.class); 
		PageTurner.changeLanguageSetting(this, config);
		setTheme( config.getTheme() );
		
		super.onCreate(savedInstanceState);		
	
		List<CustomOPDSSite> sites = config.getCustomOPDSSites();
			
		this.adapter = new CustomOPDSSiteAdapter(sites);
		setListAdapter(this.adapter);
		registerForContextMenu(getListView());
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.edit_sites_menu, menu);

		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(
			com.actionbarsherlock.view.MenuItem item) {
		showAddSiteDialog();
		return true;
	}
		
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		
		menu.add(Menu.NONE, ContextAction.EDIT.ordinal(), Menu.NONE, R.string.edit );
		menu.add(Menu.NONE, ContextAction.DELETE.ordinal(), Menu.NONE, R.string.delete ); 
		
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		showEditDialog( adapter.getItem(position) );
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		
		ContextAction action = ContextAction.values()[item.getItemId()];
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		CustomOPDSSite site = adapter.getItem(info.position);
		
		switch (action) {
		
		case EDIT:
			showEditDialog(site);
			break;
		case DELETE:
			 adapter.remove(adapter.getItem(info.position));
			 storeSites();	
		}		
	   
	    return true;
	}
	
	private void storeSites() {
		List<CustomOPDSSite> sites = new ArrayList<CustomOPDSSite>();
		for ( int i=0; i < adapter.getCount(); i++ ) {
			sites.add( adapter.getItem(i));
		}
		
		config.storeCustomOPDSSites(sites);
	}
	
	private void showEditDialog(final CustomOPDSSite site) {
		showSiteDialog(R.string.edit_site, site);		
	}
	
	private void showAddSiteDialog() {		
		showSiteDialog(R.string.add_site, null);
	}
	
	private void showSiteDialog(int titleResource, final CustomOPDSSite siteParam ) {
		
		final CustomOPDSSite site;
		
		if ( siteParam == null ) {
			site = new CustomOPDSSite();
		} else {
			site = siteParam;
		}
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		
		builder.setTitle(titleResource);
		LayoutInflater inflater = PlatformUtil.getLayoutInflater(this);
		
		View layout = inflater.inflate(R.layout.edit_site, null);
		builder.setView(layout);
		
		final TextView siteName = (TextView) layout.findViewById(R.id.siteName);
		final TextView siteURL = (TextView) layout.findViewById(R.id.siteUrl);
		final TextView siteDesc = (TextView) layout.findViewById(R.id.siteDescription);
		final TextView userName = (TextView) layout.findViewById(R.id.username);
		final TextView password = (TextView) layout.findViewById(R.id.password);
		
		siteName.setText(site.getName());
		siteURL.setText(site.getUrl());
		siteDesc.setText(site.getDescription());
		userName.setText(site.getUserName());
		password.setText(site.getPassword());
				
		builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				
				if ( siteName.getText().toString().trim().length() == 0 ) {
					Toast.makeText(ManageSitesActivity.this, R.string.msg_name_blank, Toast.LENGTH_SHORT).show();
					return;
				}
				
				if ( siteURL.getText().toString().trim().length() == 0 ) {
					Toast.makeText(ManageSitesActivity.this, R.string.msg_url_blank, Toast.LENGTH_SHORT).show();
					return;
				}				
				
				site.setName(siteName.getText().toString());
				site.setDescription(siteDesc.getText().toString());
				site.setUrl(siteURL.getText().toString());
				site.setUserName(userName.getText().toString());
				site.setPassword(password.getText().toString());
							
				if ( siteParam == null ) {
					adapter.add(site);
				}
				
				storeSites();
				adapter.notifyDataSetChanged();
				dialog.dismiss();
			}
		});
		
		builder.setNegativeButton(android.R.string.cancel, null );		
	
		builder.show();	
	}

	private class CustomOPDSSiteAdapter extends ArrayAdapter<CustomOPDSSite> {
		public CustomOPDSSiteAdapter(List<CustomOPDSSite> sites) {
			super(ManageSitesActivity.this, 0, sites);
		}		
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			
			View view;
			
			if ( convertView != null ) {
				view = convertView;
			} else {
				view = ( (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE) ).inflate(R.layout.manage_sites, null);
			}
			
			TextView siteName = (TextView) view.findViewById( R.id.siteName );
			TextView description = (TextView) view.findViewById( R.id.siteDescription );
			
			CustomOPDSSite site = this.getItem(position);
			siteName.setText( site.getName() );
			description.setText( site.getDescription() );
			
			return view;
		}
		
		
		
	}
	

}
