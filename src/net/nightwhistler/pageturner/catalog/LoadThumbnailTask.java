package net.nightwhistler.pageturner.catalog;

import android.util.Log;
import com.google.inject.Inject;
import net.nightwhistler.nucular.atom.Entry;
import net.nightwhistler.nucular.atom.Link;
import net.nightwhistler.pageturner.scheduling.QueueableAsyncTask;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import java.io.IOException;
/**
 * Created with IntelliJ IDEA.
 * User: alex
 * Date: 5/10/13
 * Time: 8:45 AM
 * To change this template use File | Settings | File Templates.
 */
public class LoadThumbnailTask extends QueueableAsyncTask<Link, Void, Void> {

    private HttpClient httpClient;
    private LoadFeedCallback callBack;

    private Map<String, byte[]> cache = new HashMap<String, byte[]>();
    private String baseUrl;

    @Inject
    public LoadThumbnailTask(HttpClient httpClient ) {
        this.httpClient = httpClient;
    }

    public void setLoadFeedCallback( LoadFeedCallback callBack ) {
        this.callBack = callBack;
    }

    public void setCache( Map<String, byte[]> cache ) {
        this.cache = cache;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public void requestCancellation() {
        Log.d("LoadThumbnailTask", "Got cancel request");
        super.requestCancellation();
    }

    @Override
    protected Void doInBackground(Link... entries) {

        Link imageLink = entries[0];

        if ( imageLink == null ) {
            return null;
        }

        String href = imageLink.getHref();

        if (cache != null && cache.containsKey(href)) {
            imageLink.setBinData(cache.get(href));
        } else {

            try {
                String target = new URL(new URL(baseUrl), href).toString();

                Log.i("LoadThumbnailTask", "Downloading image: " + target);

                HttpGet currentRequest = new HttpGet(target);
                HttpResponse resp = httpClient.execute(currentRequest);

                int lengthOfFile = (int) resp.getEntity().getContentLength();

                if ( lengthOfFile <= 0 ) {
                    return  null;
                }

                ByteArrayOutputStream out = new ByteArrayOutputStream(lengthOfFile);

                InputStream in = resp.getEntity().getContent();

                // here's the download code
                byte[] buffer = new byte[1024];
                int len1 = 0;
                long total = 0;

                while ((len1 = in.read(buffer)) > 0 && ! isCancelled() ) {

                    // Make sure the user can cancel the download.
                    if (isCancelled()) {
                        return null;
                    }

                    total += len1;

                    out.write(buffer, 0, len1);
                }

                if ( ! isCancelled() ) {
                    imageLink.setBinData(out.toByteArray());

                    if ( cache != null ) {
                        cache.put(href, imageLink.getBinData());
                    }
                }

            } catch (IOException io) {
                //Ignore and exit.
            }
        }

        return null;
    }

    @Override
    protected void doOnPostExecute(Void aVoid) {
        callBack.notifyLinkUpdated();
    }
}
