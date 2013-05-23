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

import android.annotation.TargetApi;
import android.os.Build;
import android.util.Base64;
import net.nightwhistler.nucular.atom.Link;
import net.nightwhistler.pageturner.scheduling.QueueableAsyncTask;

/**
 * Loads images for links that have the image-data embedded as Base64 data.
 */
@TargetApi(Build.VERSION_CODES.FROYO)
public class ParseBinDataTask extends QueueableAsyncTask<Link, Void, Void> {

    private LoadFeedCallback callBack;


    public void setLoadFeedCallback( LoadFeedCallback callBack ) {
        this.callBack = callBack;
    }

    @Override
    protected void onPreExecute() {
        this.callBack.onLoadingStart();
    }

    @Override
    protected Void doInBackground(Link... links) {

        Link imageLink = links[0];
        String href = imageLink.getHref();
        String dataString = href.substring(href.indexOf(',') + 1);

        imageLink.setBinData(Base64.decode(dataString,
                Base64.DEFAULT));

        return null;
    }

    @Override
    protected void doOnPostExecute(Void aVoid) {
        callBack.notifyLinkUpdated();
    }
}
