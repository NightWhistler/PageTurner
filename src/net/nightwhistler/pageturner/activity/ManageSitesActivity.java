package net.nightwhistler.pageturner.activity;

import java.util.ArrayList;
import java.util.List;

import net.nightwhistler.pageturner.Configuration;
import net.nightwhistler.pageturner.CustomOPDSSite;
import net.nightwhistler.pageturner.PlatformUtil;
import net.nightwhistler.pageturner.R;
import roboguice.RoboGuice;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.github.rtyley.android.sherlock.roboguice.activity.RoboSherlockListActivity;
import com.google.inject.Inject;

public class ManageSitesActivity extends RoboSherlockListActivity {

	@Inject
	Configuration config;
	
	private CustomOPDSSiteAdapter adapter;
	
	private static enum ContextAction { EDIT, DELETE };
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setTheme( RoboGuice.getInjector(this).getInstance(Configuration.class).getTheme() );
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
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		
		builder.setTitle(R.string.edit_site);
		LayoutInflater inflater = PlatformUtil.getLayoutInflater(this);
		
		View layout = inflater.inflate(R.layout.edit_site, null);
		builder.setView(layout);
		
		final TextView siteName = (TextView) layout.findViewById(R.id.siteName);
		final TextView siteURL = (TextView) layout.findViewById(R.id.siteUrl);
		final TextView siteDesc = (TextView) layout.findViewById(R.id.siteDescription);
		
		siteName.setText(site.getName());
		siteURL.setText(site.getUrl());
		siteDesc.setText(site.getDescription());		
				
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
				
				adapter.add(site);
				storeSites();
				dialog.dismiss();
			}
		});
		
		builder.setNegativeButton(android.R.string.cancel, null );
		
	
		builder.show();
	}
	
	private void showAddSiteDialog() {
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		
		builder.setTitle(R.string.add_site);
		LayoutInflater inflater = PlatformUtil.getLayoutInflater(this);
		
		View layout = inflater.inflate(R.layout.edit_site, null);
		builder.setView(layout);
		
		final TextView siteName = (TextView) layout.findViewById(R.id.siteName);
		final TextView siteURL = (TextView) layout.findViewById(R.id.siteUrl);
		final TextView siteDesc = (TextView) layout.findViewById(R.id.siteDescription);
				
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
				
				CustomOPDSSite site = new CustomOPDSSite();
				site.setName(siteName.getText().toString());
				site.setDescription(siteDesc.getText().toString());
				site.setUrl(siteURL.getText().toString());
				
				adapter.add(site);
				storeSites();
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
