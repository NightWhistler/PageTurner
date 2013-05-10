package net.nightwhistler.pageturner.scheduling;

import android.os.AsyncTask;

/**
 * Created with IntelliJ IDEA.
 * User: alex
 * Date: 5/10/13
 * Time: 7:39 AM
 * To change this template use File | Settings | File Templates.
 */
public abstract class QueueableAsyncTask<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> {

    public static interface QueueCallback {
        void taskCompleted( QueueableAsyncTask<?,?,?> task, boolean wasCancelled );
    }

    private QueueCallback callback;

    @Override
    protected final void onPostExecute(Result result) {
        if ( callback != null ) {
            callback.taskCompleted( this, isCancelled() );
        }

        doOnPostExecute(result);
    }

    public void setCallback( QueueCallback callback ) {
        this.callback = callback;
    }

    protected abstract void doOnPostExecute(Result result);
}
