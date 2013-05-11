package net.nightwhistler.pageturner.scheduling;

import android.os.AsyncTask;

/**
 * Subclass of AsyncTask which notifies the scheduler when it's done.
 *
  * @param <Params>
 * @param <Progress>
 * @param <Result>
 */
public abstract class QueueableAsyncTask<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> {

    public static interface QueueCallback {
        void taskCompleted( QueueableAsyncTask<?,?,?> task, boolean wasCancelled );
    }

    private QueueCallback callback;

    /**
     * Overridden and made final to implement notification.
     *
     * Subclasses should override doOnPostExecute() instead.
     *
     * @param result
     */
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

    /**
     * Gets executed on the UI thread.
     *
     * Override this to implement your on post-processing operations.
     *
     * @param result
     */
    protected void doOnPostExecute(Result result) { }
}
