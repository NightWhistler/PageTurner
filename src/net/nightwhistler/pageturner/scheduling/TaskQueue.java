package net.nightwhistler.pageturner.scheduling;

import android.os.AsyncTask;
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

    public synchronized <A,B,C> void executeTask( QueueableAsyncTask<A,B,C> task, A... parameters ) {

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
    public synchronized <A,B,C> void jumpQueueExecuteTask( QueueableAsyncTask<A,B,C> task, A... parameters ) {

        if ( this.taskQueue.isEmpty() ) {
            executeTask(task, parameters);
        } else {

            taskQueue.remove().cancel();

            task.setCallback(this);
            taskQueue.add( 0, new QueuedTask<A, B, C>(task, parameters));
            taskQueue.peek().execute();
        }

    }

    public synchronized void clear() {

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

    @Override
    public synchronized void taskCompleted(QueueableAsyncTask<?, ?, ?> task, boolean wasCancelled) {

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
