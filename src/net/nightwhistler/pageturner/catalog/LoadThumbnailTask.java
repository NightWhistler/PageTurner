/*
 * Copyright (C) 2013 Alex Kuiper
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
package net.nightwhistler.pageturner.catalog;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.util.Log;
import com.google.inject.Inject;
import net.nightwhistler.nucular.atom.Link;
import net.nightwhistler.pageturner.scheduling.QueueableAsyncTask;
import net.nightwhistler.pageturner.view.FastBitmapDrawable;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class LoadThumbnailTask extends QueueableAsyncTask<Link, Void, Void> {

    private HttpClient httpClient;
    private LoadFeedCallback callBack;

    private String baseUrl;

    private Link imageLink;
    private Drawable drawable;

    @Inject
    public LoadThumbnailTask(HttpClient httpClient ) {
        this.httpClient = httpClient;
    }

    public void setLoadFeedCallback( LoadFeedCallback callBack ) {
        this.callBack = callBack;
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
    protected void onPreExecute() {
        this.callBack.onLoadingStart();
    }

    @Override
    protected Void doInBackground(Link... entries) {

        this.imageLink = entries[0];

        if ( imageLink == null ) {
            return null;
        }

        String href = imageLink.getHref();


        try {
            String target = new URL(new URL(baseUrl), href).toString();

            Log.i("LoadThumbnailTask", "Downloading image: " + target);

            HttpGet currentRequest = new HttpGet(target);
            HttpResponse resp = httpClient.execute(currentRequest);

            Bitmap bitmap = BitmapFactory.decodeStream(resp.getEntity().getContent());
            this.drawable = new FastBitmapDrawable( bitmap );

        } catch (Exception io) {
            //Ignore and exit.
        } catch ( OutOfMemoryError error ) {

        }

        return null;
    }

    @Override
    protected void doOnPostExecute(Void nothing) {
        callBack.notifyLinkUpdated(imageLink, drawable);
    }
}
