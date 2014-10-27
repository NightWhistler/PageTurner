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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Base64;
import jedi.option.Option;
import net.nightwhistler.nucular.atom.Link;
import net.nightwhistler.pageturner.scheduling.QueueableAsyncTask;
import net.nightwhistler.pageturner.view.FastBitmapDrawable;

import static jedi.option.Options.some;

/**
 * Loads images for links that have the image-data embedded as Base64 data.
 */
@TargetApi(Build.VERSION_CODES.FROYO)
public class ParseBinDataTask extends QueueableAsyncTask<Link, Void, FastBitmapDrawable> {

    private LoadFeedCallback callBack;

    private Link imageLink;

    public void setLoadFeedCallback( LoadFeedCallback callBack ) {
        this.callBack = callBack;
    }

    @Override
    public void doOnPreExecute() {
        this.callBack.onLoadingStart();
    }

    @Override
    public Option<FastBitmapDrawable> doInBackground(Link... links) {

        this.imageLink = links[0];
        String href = imageLink.getHref();
        String dataString = href.substring(href.indexOf(',') + 1);

        byte[] data = Base64.decode(dataString, Base64.DEFAULT);

        Bitmap bitmap = BitmapFactory.decodeByteArray( data, 0, data.length );

        return some( new FastBitmapDrawable(bitmap) );
    }

    @Override
    public void doOnPostExecute(Option<FastBitmapDrawable> result) {
        result.forEach( r -> callBack.notifyLinkUpdated(imageLink, r) );
    }

}
