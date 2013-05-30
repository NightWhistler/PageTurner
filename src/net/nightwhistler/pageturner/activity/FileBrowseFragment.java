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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.github.rtyley.android.sherlock.roboguice.fragment.RoboSherlockListFragment;
import com.google.inject.Inject;
import net.nightwhistler.pageturner.Configuration;
import net.nightwhistler.pageturner.R;

import java.io.File;
import java.util.*;

public class FileBrowseFragment extends RoboSherlockListFragment {

	private FileAdapter adapter;	
	
	@Inject
	private Configuration config;

	@Override
	public void onCreate(Bundle savedInstanceState) {	
		super.onCreate(savedInstanceState);
		
		Uri data = getActivity().getIntent().getData();		
		
		File file = null;
		
		if ( data != null && data.getPath() != null ) {
			file = new File(data.getPath());
		}
		
		if (file == null || ! (file.exists() && file.isDirectory()) ) {
			file = new File(config.getStorageBase());
		}
		
		if (file == null || ! (file.exists() && file.isDirectory()) ) {
			file = new File("/");
		}
		
		this.adapter = new FileAdapter();
		adapter.setFolder(file);
		getActivity().setTitle(adapter.getCurrentFolder());
	}
	

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		setListAdapter(adapter);
	}



	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		File f = this.adapter.getItem(position);
		if ( f.exists() && f.isDirectory() ) {
			this.adapter.setFolder(f);
			getActivity().setTitle(adapter.getCurrentFolder());
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
					if ( childFile.isDirectory() || childFile.getName().toLowerCase(Locale.US).endsWith(".epub")) {
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

            LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
						
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
						Intent intent = getActivity().getIntent();
						intent.setData( Uri.fromFile(file) );
						getActivity().setResult(Activity.RESULT_OK, intent);
						getActivity().finish();
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
