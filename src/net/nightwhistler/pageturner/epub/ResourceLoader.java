/*
 * Copyright (C) 2011 Alex Kuiper
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
package net.nightwhistler.pageturner.epub;

import nl.siegmann.epublib.domain.Resources;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;


/**
 * This is mostly a performance utility:
 * 
 * We don't want to load all the images from an
 * EPUB at once, but lazy-loading several images
 * and opening the file for each is too slow.
 * 
 * The image loader allows a class to register call-backs
 * so several images can be loaded in a single go.
 * 
 * @author Alex Kuiper
 *
 */
public class ResourceLoader  {
	
	private String fileName;
		
	public ResourceLoader(String fileName) {
		this.fileName = fileName;
	}
	
	public static interface ResourceCallback {
		void onLoadResource( String href, InputStream stream );
	}
	
	private class Holder {
		String href;
		ResourceCallback callback;
	}
	
	private List<Holder> callbacks = new ArrayList<ResourceLoader.Holder>();
	
	public void clear() {
		this.callbacks.clear();
	}
	
	public void load() throws IOException {

        ZipFile zipFile = null;

		try {
            zipFile = new ZipFile(this.fileName);

            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            while( entries.hasMoreElements() ) {
                ZipEntry zipEntry = entries.nextElement();

				if(zipEntry.isDirectory()) {
					continue;
				}

				String href = zipEntry.getName();

				List<ResourceCallback> filteredCallbacks = findCallbacksFor(href);

				if ( ! filteredCallbacks.isEmpty() ) {

					for ( ResourceCallback callBack: filteredCallbacks ) {
						callBack.onLoadResource(href, zipFile.getInputStream(zipEntry) );
					}
				}
			}
		} finally {
			if ( zipFile != null ) {
				zipFile.close();
			}
			
			this.callbacks.clear();
		}
	}
	
	private List<ResourceCallback> findCallbacksFor( String href ) {
		List<ResourceCallback> result = new ArrayList<ResourceLoader.ResourceCallback>();
		
		for ( Holder holder: this.callbacks ) {
			if ( href.endsWith(holder.href ) ) {
				result.add(holder.callback);
			}
		}
		
		return result;
	}
	
	public void registerCallback( String forHref, ResourceCallback callback ) {
			
		Holder holder = new Holder();
		holder.href = URLDecoder.decode(forHref);
		holder.callback = callback;
		
		callbacks.add(holder);		
	}
}