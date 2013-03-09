package net.nightwhistler.pageturner.catalog;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

import net.nightwhistler.nucular.atom.Entry;
import net.nightwhistler.nucular.atom.Feed;
import net.nightwhistler.nucular.atom.Link;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Catalog {

	/**
	 * Reserved ID to identify the feed entry where custom sites are added.
	 */
	public static final String CUSTOM_SITES_ID = "IdCustomSites";
	
	private static final Logger LOG = LoggerFactory.getLogger("Catalog");
	
	/**
	 * Selects the right image link for an entry, based on preference.
	 * 
	 * @param feed
	 * @param entry
	 * @return
	 */
	public static Link getImageLink(Feed feed, Entry entry) {
		Link[] linkOptions;

		if (isLeafEntry(feed)) {
			linkOptions = new Link[] { entry.getImageLink(), entry.getThumbnailLink() };
		} else {
			linkOptions = new Link[] { entry.getThumbnailLink(), entry.getImageLink() };						
		}
		
		Link imageLink = null;					
		for ( int i=0; imageLink == null && i < linkOptions.length; i++ ) {
			imageLink = linkOptions[i];
		}
		
		return imageLink;
	}
	
	public static void loadImageLink(Map<String, byte[]> cache, Link imageLink,
			String baseUrl) throws IOException {

		HttpParams httpParams = new BasicHttpParams();
		DefaultHttpClient client = new DefaultHttpClient(httpParams);
		//client.getCredentialsProvider().setCredentials(new AuthScope(null, -1),
				//new UsernamePasswordCredentials(user, password));

		if (imageLink != null) {
			String href = imageLink.getHref();

			if (cache.containsKey(href)) {
				imageLink.setBinData(cache.get(href));
			} else {

				String target = new URL(new URL(baseUrl), href).toString();

				LOG.info("Downloading image: " + target);

				HttpResponse resp = client.execute(new HttpGet(target));

				imageLink.setBinData(EntityUtils.toByteArray(resp.getEntity()));

				cache.put(href, imageLink.getBinData());
			}
		}
	}
	
	/**
	 * Checks if a feed is a leaf:
	 * 
	 * A feed is a leaf if it isn't our custom
	 * sites feed and only has 1 entry.
	 * 
	 * @param feed
	 * @return
	 */
	public static boolean isLeafEntry(Feed feed) {		
		
		return feed.getEntries().size() == 1
				&& ( feed.getId() == null
				|| ! feed.getId().equals(CUSTOM_SITES_ID));
	}
}
