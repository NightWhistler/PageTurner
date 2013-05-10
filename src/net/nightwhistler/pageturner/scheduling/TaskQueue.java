package net.nightwhistler.pageturner.scheduling;

import android.os.AsyncTask;
import android.util.Log;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Generic task scheduling queue.
 *
 * Allows for consistent execution and cancelling of tasks
 * across Android versions.
 *
 * @author Alex Kuiper
 */
public class TaskQueue implements QueueableAsyncTask.QueueCallback {

    public static interface TaskQueueListener {
        void queueEmpty();
    }

    private Queue<QueuedTask<?,?,?>> taskQueue = new LinkedList<QueuedTask<?, ?, ?>>();
    private TaskQueueListener listener;

    public <A,B,C> void executeTask( QueueableAsyncTask<A,B,C> task, A... parameters ) {

        task.setCallback(this);

        this.taskQueue.add(new QueuedTask<A, B, C>(task, parameters));

        if ( this.taskQueue.size() == 1 ) {
            this.taskQueue.peek().execute();
        }

        Log.d("TaskQueue", "Scheduled task of type " + task.getClass().getSimpleName()
                + " total tasks scheduled now: " + this.taskQueue.size() );
    }

    public void clear() {

        Log.d("TaskQueue", "Clearing task queue.");

        if ( ! this.taskQueue.isEmpty() ) {
            taskQueue.peek().cancel();
            this.taskQueue.clear();
        }
    }

    public void setTaskQueueListener( TaskQueueListener listener ) {
        this.listener = listener;
    }

    @Override
    public void taskCompleted(QueueableAsyncTask<?, ?, ?> task, boolean wasCancelled) {

        if ( ! wasCancelled ) {
            QueuedTask queuedTask = this.taskQueue.remove();

            if ( queuedTask.getTask() != task ) {
                throw new RuntimeException("Tasks out of sync! Expected "+
                        queuedTask.getTask() + " but got " + task );
            }

           Log.d( "TaskQueue", "Completion of task of type " + task.getClass().getSimpleName()
                    + " total tasks scheduled now: " + this.taskQueue.size() );

            if ( ! this.taskQueue.isEmpty() ) {
                this.taskQueue.peek().execute();
            }
        }

        if ( this.taskQueue.isEmpty() ) {
            this.listener.queueEmpty();
        }

    }
}
