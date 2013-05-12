package net.nightwhistler.pageturner.scheduling;

import android.os.AsyncTask;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
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

    private LinkedList<QueuedTask<?,?,?>> taskQueue = new LinkedList<QueuedTask<?, ?, ?>>();
    private TaskQueueListener listener;

    public <A,B,C> void executeTask( QueueableAsyncTask<A,B,C> task, A... parameters ) {

        task.setCallback(this);

        this.taskQueue.add(new QueuedTask<A, B, C>(task, parameters));

        Log.d("TaskQueue", "Scheduled task of type " + task.getClass().getSimpleName()
                + " total tasks scheduled now: " + this.taskQueue.size() );

        if ( this.taskQueue.size() == 1 ) {
            Log.d("TaskQueue",  "Starting task, since task queue is 1.");
            this.taskQueue.peek().execute();
        }
    }

    /**
     * Cancels the currently running task and queues this task ahead of all others.
     *
     * @param task
     * @param parameters
     * @param <A>
     * @param <B>
     * @param <C>
     */
    public <A,B,C> void jumpQueueExecuteTask( QueueableAsyncTask<A,B,C> task, A... parameters ) {

        Log.d("TaskQueue", "Queue-jump requested for " + task.getClass().getSimpleName() );

        if ( this.taskQueue.isEmpty() ) {
            Log.d("TaskQueue", "Delegating to simple schedule since the queue is empty.");
            executeTask(task, parameters);
        } else {

            QueuedTask top = taskQueue.remove();
            Log.d("TaskQueue", "Cancelling task of type " + top.getTask().getClass().getSimpleName() );
            top.cancel();

            task.setCallback(this);
            taskQueue.add( 0, new QueuedTask<A, B, C>(task, parameters));

            Log.d("TaskQueue", "Starting task of type " + task.getClass().getSimpleName()
                    + " with queue " + getQueueAsString() );

            taskQueue.peek().execute();
        }

    }

    public void clear() {

        Log.d("TaskQueue", "Clearing task queue.");

        if ( ! this.taskQueue.isEmpty() ) {
            QueuedTask front = taskQueue.peek();
            Log.d("TaskQueue", "Canceling task of type: " + front.getClass().getSimpleName() );

            front.cancel();
            this.taskQueue.clear();
        } else {
            Log.d("TaskQueue", "Nothing to do, since queue was already empty.");
        }
    }

    public void setTaskQueueListener( TaskQueueListener listener ) {
        this.listener = listener;
    }

    private String getQueueAsString() {
        StringBuilder builder = new StringBuilder("[");

        for ( int i=0; i < this.taskQueue.size(); i++ ) {
            builder.append( this.taskQueue.get(i).getTask().getClass().getSimpleName() );
            if ( i < this.taskQueue.size() -1 ) {
                builder.append(", ");
            }
        }

        builder.append("]");

        return builder.toString();
    }

    @Override
    public void taskCompleted(QueueableAsyncTask<?, ?, ?> task, boolean wasCancelled) {

        if ( ! wasCancelled ) {
            QueuedTask queuedTask = this.taskQueue.remove();

            if ( queuedTask.getTask() != task ) {

                String errorMsg = "Tasks out of sync! Expected "+
                        queuedTask.getTask() + " but got " + task.getClass().getSimpleName() +
                        " with queue: " + getQueueAsString();
                Log.e("TaskQueue", errorMsg );

                throw new RuntimeException(errorMsg);
            }

            Log.d( "TaskQueue", "Completion of task of type " + task.getClass().getSimpleName()
                    + " total tasks scheduled now: " + this.taskQueue.size() + " with queue: " + getQueueAsString() );

            if ( ! this.taskQueue.isEmpty() ) {
                this.taskQueue.peek().execute();
            }
        } else {
            Log.d("TaskQueue", "Got taskCompleted for task " + task.getClass().getSimpleName() + " which was cancelled.");
        }

        if ( this.taskQueue.isEmpty() ) {
            this.listener.queueEmpty();
        }

    }
}
