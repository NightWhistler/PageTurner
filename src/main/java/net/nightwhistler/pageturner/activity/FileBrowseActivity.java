package net.nightwhistler.pageturner.activity;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class FileBrowseActivity extends RoboListActivity {
	
	private FileAdapter adapter;	

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
		
		setTitle(adapter.getCurrentFolder());
		
		setListAdapter(adapter);		
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		File f = this.adapter.getItem(position);
		if ( f.exists() && f.isDirectory() ) {
			this.adapter.setFolder(f);
			setTitle(adapter.getCurrentFolder());
		}
	}	
	
	private class FileAdapter extends BaseAdapter {
		
		private File currentFolder;
		private List<File> items = new ArrayList<File>();
		
		public void setFolder( File folder ) {
			
			this.currentFolder = folder;
			items = new ArrayList<File>();
			File[] listing = folder.listFiles();
			
			if ( listing != null ) {
				for ( File childFile: listing ) {					
					if ( childFile.isDirectory() || childFile.getName().toLowerCase().endsWith(".epub")) {
						items.add(childFile);
					}
				}
			}
			
			Collections.sort(items, new FileSorter() );
			
			if ( folder.getParentFile() != null ) {
				items.add(0, folder.getParentFile() );
			}
			
			notifyDataSetChanged();
		}
		
		public String getCurrentFolder() {
			return this.currentFolder.getAbsolutePath();
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
		public View getView(final int position, View convertView, ViewGroup parent) {
			View rowView;		
			final File file = getItem(position);

			LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
						
			if ( convertView == null ) {				
				rowView = inflater.inflate(R.layout.folder_line, parent, false);
			} else {
				rowView = convertView;
			}
			
			ImageView img = (ImageView) rowView.findViewById(R.id.folderIcon);
			CheckBox selectBox = (CheckBox) rowView.findViewById(R.id.selectBox);
			
			if ( file.isDirectory() ) {
				img.setImageDrawable(getResources().getDrawable(R.drawable.folder));
				selectBox.setVisibility(View.VISIBLE);
			} else {
				img.setImageDrawable(getResources().getDrawable(R.drawable.book));
				selectBox.setVisibility(View.GONE);
			}
			
			selectBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if ( isChecked ) {
						Intent intent = getIntent();
						intent.setData( Uri.fromFile(file) );
						setResult(RESULT_OK, intent);
						finish();
					}
				}
			});
			selectBox.setFocusable(false);
			
			TextView label = (TextView) rowView.findViewById(R.id.folderName);

			if ( position == 0 && currentFolder.getParentFile() != null ) {
				label.setText("..");
			} else {
				label.setText(file.getName());
			}
			
			return rowView;
		}
		
		
	}
	
	private class FileSorter implements Comparator<File> {
		@Override
		public int compare(File lhs, File rhs) {			
			
			if ( (lhs.isDirectory() && rhs.isDirectory()) ||
					(!lhs.isDirectory() && !rhs.isDirectory())) {
				return lhs.getName().compareTo(rhs.getName());
			}
			
			if ( lhs.isDirectory() ) {
				return -1;
			} else {
				return 1;
			}
						
		}
	}
	
}
