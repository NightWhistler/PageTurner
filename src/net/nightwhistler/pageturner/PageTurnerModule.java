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

package net.nightwhistler.pageturner;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;

import net.nightwhistler.pageturner.library.LibraryService;
import net.nightwhistler.pageturner.library.SqlLiteLibraryService;
import net.nightwhistler.pageturner.sync.PageTurnerWebProgressService;
import net.nightwhistler.pageturner.sync.ProgressService;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

/**
 * This is the main Guice module for PageTurner.
 * 
 * This module determines the implementations used to
 * inject dependencies used in actually running the app.
 * 
 * @author Alex Kuiper
 *
 */
public class PageTurnerModule extends AbstractModule {

	@Override
	protected void configure() {
		
		bind( LibraryService.class ).to( SqlLiteLibraryService.class );
		bind( ProgressService.class ).to( PageTurnerWebProgressService.class );
		
	}
	
	/**
	 * Binds the HttpClient interface to the DefaultHttpClient implementation.
	 * 
	 * In testing we'll use a stub.
	 * 
	 * @return
	 */
	@Provides
	public HttpClient getHttpClient() {
		HttpParams httpParams = new BasicHttpParams();
		DefaultHttpClient client = new DefaultHttpClient(httpParams);
		
		return client;
	}
}
