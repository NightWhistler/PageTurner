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
