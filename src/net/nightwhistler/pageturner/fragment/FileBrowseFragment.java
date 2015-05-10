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
package net.nightwhistler.pageturner.fragment;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import com.google.inject.Inject;
import net.nightwhistler.pageturner.Configuration;
import net.nightwhistler.pageturner.activity.FileAdapter;
import net.nightwhistler.pageturner.activity.FileItem;
import roboguice.fragment.RoboListFragment;

import java.io.File;

public class FileBrowseFragment extends RoboListFragment {

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
			file = config.getStorageBase().unsafeGet();
		}
		
		if (file == null || ! (file.exists() && file.isDirectory()) ) {
			file = new File("/");
		}
		
		this.adapter = new FileAdapter(this.getActivity());
        this.adapter.setItemSelectionListener( this::returnFile );

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

        if ( fileItem.isImportOnClick() ) {
            returnFile(fileItem.getFile());
        } else if ( fileItem.getFile().isDirectory() && fileItem.getFile().exists() ) {
            this.adapter.setFolder(fileItem.getFile());
            getActivity().setTitle(adapter.getCurrentFolder());
        }

	}

    private void returnFile( File file ) {
        Intent intent = getActivity().getIntent();
        intent.setData( Uri.fromFile(file) );
        getActivity().setResult(Activity.RESULT_OK, intent);
        getActivity().finish();
    }

}
