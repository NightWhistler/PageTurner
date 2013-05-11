package net.nightwhistler.pageturner.catalog;

import net.nightwhistler.nucular.atom.Entry;
import net.nightwhistler.pageturner.scheduling.QueueableAsyncTask;

/**
 * Created with IntelliJ IDEA.
 * User: alex
 * Date: 5/10/13
 * Time: 8:45 AM
 * To change this template use File | Settings | File Templates.
 */
public class LoadThumbnailTask extends QueueableAsyncTask<Entry, Void, Void> {

    @Override
    protected Void doInBackground(Entry... entries) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected void doOnPostExecute(Void aVoid) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
