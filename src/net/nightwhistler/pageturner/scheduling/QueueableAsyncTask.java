package net.nightwhistler.pageturner.scheduling;

import android.os.AsyncTask;
import net.nightwhistler.pageturner.Configuration;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

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

    private boolean cancelRequested = false;

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
            callback.taskCompleted( this, this.cancelRequested );
        }

        doOnPostExecute(result);
    }

    /**
     * Called when a cancellation is requested.
     *
     * Default simply sets a flag and calls cancel()
     */
    public void requestCancellation() {
        this.cancelRequested = true;
        this.cancel(true);
    }

    @Override
    protected final void onCancelled(Result result) {
        if ( callback != null ) {
            callback.taskCompleted( this, this.cancelRequested );
        }

        doOnCancelled (result);
    }

    @Override
    protected final void onCancelled() {
        onCancelled(null);
    }

    public void doOnCancelled(Result result) {

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
