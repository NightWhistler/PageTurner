package net.nightwhistler.pageturner.testutils;

import jedi.option.Option;
import net.nightwhistler.pageturner.scheduling.QueueableAsyncTask;
import net.nightwhistler.pageturner.scheduling.TaskQueue;

/**
 * Created by alex on 10/25/14.
 */
public class SynchronousTaskQueue extends TaskQueue {

    private TaskQueueListener listener;

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public <A, B, C> void executeTask(QueueableAsyncTask<A, B, C> task, A... parameters) {
        task.doOnPreExecute();
        Option<C> result = task.doInBackground( parameters );
        task.doOnPostExecute( result );
        if ( listener != null ) {
            listener.queueEmpty();
        }
    }

    @Override
    public <A, B, C> void jumpQueueExecuteTask(QueueableAsyncTask<A, B, C> task, A... parameters) {
        executeTask( task, parameters );
    }

    @Override
    public void clear() {
        //no-op
    }

    @Override
    public void setTaskQueueListener(TaskQueueListener listener) {
        this.listener = listener;
    }

    @Override
    public void taskCompleted(QueueableAsyncTask<?, ?, ?> task, boolean wasCancelled) {
        //no op
    }
}
