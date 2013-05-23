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

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import net.nightwhistler.pageturner.library.LibraryService;
import net.nightwhistler.pageturner.library.SqlLiteLibraryService;
import net.nightwhistler.pageturner.ssl.EasySSLSocketFactory;
import net.nightwhistler.pageturner.sync.PageTurnerWebProgressService;
import net.nightwhistler.pageturner.sync.ProgressService;
import net.nightwhistler.pageturner.tts.TTSPlaybackQueue;
import net.nightwhistler.pageturner.view.bookview.TextLoader;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;

import java.net.MalformedURLException;
import java.net.URL;

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

        bind(TTSPlaybackQueue.class).in(Singleton.class);
        bind(TextLoader.class).in(Singleton.class);
	}
	
	/**
	 * Binds the HttpClient interface to the DefaultHttpClient implementation.
	 * 
	 * In testing we'll use a stub.
	 * 
	 * @return
	 */
	@Provides
	@Inject
	public HttpClient getHttpClient(Configuration config) {
		HttpParams httpParams = new BasicHttpParams();
		DefaultHttpClient client;

        if ( config.isAcceptSelfSignedCertificates() ) {
            client = new SSLHttpClient(httpParams);
        } else {
            client = new DefaultHttpClient(httpParams);
        }

		for ( CustomOPDSSite site: config.getCustomOPDSSites() ) {
			if ( site.getUserName() != null && site.getUserName().length() > 0 ) {
				try {
					URL url = new URL(site.getUrl());
					client.getCredentialsProvider().setCredentials(
						new AuthScope(url.getHost(), url.getPort()),
						new UsernamePasswordCredentials(site.getUserName(), site.getPassword()));
				} catch (MalformedURLException mal ) {
					//skip to the next
				}				
			}
		}		
		
		return client;
	}


    public class SSLHttpClient extends DefaultHttpClient {


        public SSLHttpClient(HttpParams params) {
            super(params);
        }

        @Override protected ClientConnectionManager createClientConnectionManager() {
            SchemeRegistry registry = new SchemeRegistry();
            registry.register(
                    new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
            registry.register(new Scheme("https", new EasySSLSocketFactory(), 443));
            return new SingleClientConnManager(getParams(), registry);
        }

    }
	
}
