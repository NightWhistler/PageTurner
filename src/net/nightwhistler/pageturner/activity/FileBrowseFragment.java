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
		FileItem fileItem = this.adapter.getItem(position);

        if ( fileItem.importOnClick ) {
            returnFile(fileItem.file);
        } else if ( fileItem.file.isDirectory() && fileItem.file.exists() ) {
            this.adapter.setFolder(fileItem.file);
            getActivity().setTitle(adapter.getCurrentFolder());
        }

	}

    private void returnFile( File file ) {
        Intent intent = getActivity().getIntent();
        intent.setData( Uri.fromFile(file) );
        getActivity().setResult(Activity.RESULT_OK, intent);
        getActivity().finish();
    }

	private class FileAdapter extends BaseAdapter {
		
		private File currentFolder;
		private List<FileItem> items = new ArrayList<FileItem>();
		
		public void setFolder( File folder ) {
			
			this.currentFolder = folder;
			items = new ArrayList<FileItem>();
			File[] listing = folder.listFiles();
			
			if ( listing != null ) {
				for ( File childFile: listing ) {					
					if ( childFile.isDirectory() || childFile.getName().toLowerCase(Locale.US).endsWith(".epub")) {
						items.add(new FileItem(childFile.getName(), childFile, ! childFile.isDirectory() ));
					}
				}
			}
			
			Collections.sort(items, new FileSorter() );

            items.add( 0, new FileItem( "[" + getString(R.string.import_this) + "]", folder, true));

			if ( folder.getParentFile() != null ) {
				items.add(0, new FileItem( "[..]", folder.getParentFile(), false ));
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
		public FileItem getItem(int position) {
			return items.get(position);
		}
		
		@Override
		public long getItemId(int position) {
			return position;
		}
		
		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			View rowView;		
			final FileItem fileItem = getItem(position);

            LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
						
			if ( convertView == null ) {				
				rowView = inflater.inflate(R.layout.folder_line, parent, false);
			} else {
				rowView = convertView;
			}
			
			ImageView img = (ImageView) rowView.findViewById(R.id.folderIcon);
			CheckBox selectBox = (CheckBox) rowView.findViewById(R.id.selectBox);

            if ( fileItem.file.isDirectory() ) {
			    img.setImageDrawable(getResources().getDrawable(R.drawable.folder));
			    selectBox.setVisibility(View.VISIBLE);
            } else {
                img.setImageDrawable(getResources().getDrawable(R.drawable.file));
                selectBox.setVisibility(View.GONE);
            }

			selectBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if ( isChecked ) {
                        returnFile( fileItem.file );
					}
				}
			});
			selectBox.setFocusable(false);
			
			TextView label = (TextView) rowView.findViewById(R.id.folderName);

			label.setText( fileItem.label );
			
			return rowView;
		}
		
	}

    private static class FileItem {

        private CharSequence label;
        private File file;

        private boolean importOnClick;

        public FileItem( CharSequence label, File file, boolean importOnClick ) {
            this.label = label;
            this.file = file;
            this.importOnClick = importOnClick;
        }
    }
	
	private static class FileSorter implements Comparator<FileItem> {
		@Override
		public int compare(FileItem lhs, FileItem rhs) {
			
			if ( (lhs.file.isDirectory() && rhs.file.isDirectory()) ||
					(!lhs.file.isDirectory() && !rhs.file.isDirectory())) {
				return lhs.file.getName().compareTo(rhs.file.getName());
			}
			
			if ( lhs.file.isDirectory() ) {
				return -1;
			} else {
				return 1;
			}
						
		}
	}
	
}
