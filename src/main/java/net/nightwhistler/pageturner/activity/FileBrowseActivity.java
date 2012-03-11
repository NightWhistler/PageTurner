package net.nightwhistler.pageturner.activity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import net.nightwhistler.pageturner.R;
import roboguice.activity.RoboListActivity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class FileBrowseActivity extends RoboListActivity {
	
	private FileAdapter adapter;
	
	private File currentFolder;

	@Override
	protected void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);
		
		Uri data = getIntent().getData();		
		
		if ( data != null ) {
			File file = new File(data.getPath());
			
			if ( file.exists() && file.isDirectory() ) {
				this.adapter = new FileAdapter();
				adapter.setFolder(file);
			}
		}		
		
		if ( adapter == null) {
			this.adapter = new FileAdapter();
			adapter.setFolder(new File("/sdcard"));
		}
		
		setListAdapter(adapter);		
		registerForContextMenu(getListView());
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		File f = this.adapter.getItem(position);
		this.adapter.setFolder(f);
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		
		AdapterView.AdapterContextMenuInfo info;		
		info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		
		this.currentFolder = adapter.getItem(info.position);
		
		menu.add("Select this folder");
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		Intent intent = getIntent();
		intent.setData( Uri.fromFile(currentFolder) );
		setResult(RESULT_OK, intent);
		finish();
		return true;
	}	
	
	private class FileAdapter extends BaseAdapter {
		
		private File currentFolder;
		private List<File> items = new ArrayList<File>();
		
		public void setFolder( File folder ) {
			
			this.currentFolder = folder;
			items = new ArrayList<File>();
			
			if ( folder.getParentFile() != null ) {
				items.add( folder.getParentFile() );
			}
			
			for ( String child: folder.list() ) {
				File childFile = new File( folder, child );
				if ( childFile.isDirectory() ) {
					items.add(childFile);
				}
			}
			
			notifyDataSetChanged();
		}
		
		@Override
		public int getCount() {
			return items.size();
		}
		
		@Override
		public File getItem(int position) {
			return items.get(position);
		}
		
		@Override
		public long getItemId(int position) {
			return position;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View rowView;		
			final File file = getItem(position);

			LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
						
			if ( convertView == null ) {				
				rowView = inflater.inflate(R.layout.folder_line, parent, false);
			} else {
				rowView = convertView;
			}
			
			TextView label = (TextView) rowView.findViewById(R.id.folderName);

			if ( position == 0 && currentFolder.getParentFile() != null ) {
				label.setText("..");
			} else {
				label.setText(file.getName());
			}
			
			return rowView;
		}
		
		
	}
	
}
