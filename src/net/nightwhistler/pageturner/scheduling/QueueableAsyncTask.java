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
import jedi.functional.Command;
import jedi.functional.Functor;
import jedi.option.Option;
import net.nightwhistler.ui.UiUtils;

import static java.lang.Integer.toHexString;
import static jedi.option.Options.none;

/**
 * Subclass of AsyncTask which notifies the scheduler when it's done.
 *
 * @param <Params>
 * @param <Progress>
 * @param <Result>
 */
public class QueueableAsyncTask<Params, Progress, Result> extends AsyncTask<Params, Progress, Option<Result>> {

    public static interface QueueCallback {
        void taskCompleted( QueueableAsyncTask<?,?,?> task, boolean wasCancelled );
    }

    private UiUtils.Action onPreExecutionOperation;
    private Command<Option<Result>> onPostExecuteOperation;
    private Command<Option<Result>> onCancelledOperation;
    private Command<Progress[]> onProgressUpdateOperation;

    private Functor<Params[], Option<Result>> doInBackgroundFunction;

    private QueueCallback callback;

    private boolean cancelRequested = false;

    @Override
    protected final void onPreExecute() {
        this.doOnPreExecute();
    }

    /**
     * Called before execution.
     *
     */
    public void doOnPreExecute() {
        if ( this.onPostExecuteOperation != null ) {
            this.onPreExecutionOperation.perform();
        }
    }

    @Override
    protected final void onProgressUpdate(Progress... values) {
        this.doOnProgressUpdate( values );
    }

    public void doOnProgressUpdate( Progress... values ) {
        if ( this.onProgressUpdateOperation != null ) {
            this.onProgressUpdateOperation.execute( values );
        }
    }

    /**
     * Overridden and made final to implement notification.
     *
     * Subclasses should override doOnPostExecute() instead.
     *
     * @param result
     */
    @Override
    protected final void onPostExecute(Option<Result> result) {
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

    public boolean isCancelRequested() {
        return cancelRequested;
    }

    @Override
    protected final void onCancelled(Option<Result> result) {
        if ( callback != null ) {
            callback.taskCompleted( this, this.cancelRequested );
        }

        doOnCancelled (result);
    }

    @Override
    protected final void onCancelled() {
        onCancelled(null);
    }

    public void doOnCancelled(Option<Result> result) {
        if ( this.onCancelledOperation != null ) {
            this.onCancelledOperation.execute(result);
        }
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
    public void doOnPostExecute(Option<Result> result) {
        if ( this.onPostExecuteOperation != null ) {
            this.onPostExecuteOperation.execute(result);
        }
    }

    @Override
    public Option<Result> doInBackground(Params... paramses) {
        if ( this.doInBackgroundFunction != null ) {
            return this.doInBackgroundFunction.execute( paramses );
        }

        return none();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " (" + toHexString( hashCode() ) + ")";
    }

    /**
     * Sets the operation to be performed when this task is cancelled.
     *
     * @param onCancelledOperation
     * @return this object
     */
    public QueueableAsyncTask setOnCancelled(Command<Option<Result>> onCancelledOperation) {
        this.onCancelledOperation = onCancelledOperation;
        return this;
    }

    public QueueableAsyncTask setOnPostExecute(Command<Option<Result>> onPostExecuteOperation) {
        this.onPostExecuteOperation = onPostExecuteOperation;
        return this;
    }

    public QueueableAsyncTask setDoInBackground(Functor<Params[], Option<Result>> doInBackgroundFunction) {
        this.doInBackgroundFunction = doInBackgroundFunction;
        return this;
    }

    public void setOnPreExecute(UiUtils.Action onPreExecutionOperation) {
        this.onPreExecutionOperation = onPreExecutionOperation;
    }

    public void setOnProgressUpdate(Command<Progress[]> onProgressUpdateOperation) {
        this.onProgressUpdateOperation = onProgressUpdateOperation;
    }
}
