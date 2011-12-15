package net.nightwhistler.pageturner.view;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import nl.siegmann.epublib.util.IOUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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
	
	private static final Logger LOG = LoggerFactory.getLogger( ResourceLoader.class );
	
	public ResourceLoader(String fileName) {
		this.fileName = fileName;
	}
	
	public static interface ResourceCallback {
		void onLoadResource( String href, byte[] data );
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
		
		ZipInputStream in = new ZipInputStream(new FileInputStream(this.fileName));
		
		for(ZipEntry zipEntry = in.getNextEntry(); zipEntry != null; zipEntry = in.getNextEntry()) {
			if(zipEntry.isDirectory()) {
				continue;
			}
			
			String href = zipEntry.getName();
			
			LOG.info("Got resource " + href );
			
			List<ResourceCallback> filteredCallbacks = findCallbacksFor(href);
			
			if ( ! filteredCallbacks.isEmpty() ) {
			
				byte[] data = IOUtil.toByteArray(in);
			
				for ( ResourceCallback callBack: filteredCallbacks ) {
					callBack.onLoadResource(href, data);
				}
			}
		}
		
		in.close();
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