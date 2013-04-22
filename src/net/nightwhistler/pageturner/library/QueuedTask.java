package net.nightwhistler.pageturner.library;

import android.os.AsyncTask;
import net.nightwhistler.pageturner.PlatformUtil;

/**
 * Created with IntelliJ IDEA.
 * User: alex
 * Date: 4/22/13
 * Time: 9:39 AM
 * To change this template use File | Settings | File Templates.
 */
public class QueuedTask<A, B, C> {

    private AsyncTask<A, B, C> task;
    private A[] parameters;

    public QueuedTask(AsyncTask<A,B,C> task, A[] params ) {
        this.task = task;
        this.parameters = params;
    }


    public void execute() {
        PlatformUtil.executeTask(task, parameters);
    }

    public void cancel() {
        this.task.cancel(true);
    }

    public AsyncTask<A,B,C> getTask() {
        return task;
    }
}



