package net.nightwhistler.pageturner.catalog;

import android.annotation.TargetApi;
import android.os.Build;
import android.util.Base64;
import net.nightwhistler.nucular.atom.Entry;
import net.nightwhistler.nucular.atom.Feed;
import net.nightwhistler.nucular.atom.Link;
import net.nightwhistler.pageturner.scheduling.QueueableAsyncTask;

import static net.nightwhistler.pageturner.catalog.Catalog.getImageLink;

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
