package net.nightwhistler.pageturner.scheduling;

import android.annotation.TargetApi;
import android.os.AsyncTask;
import org.junit.Ignore;
import org.robolectric.RobolectricTestRunner;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import java.util.concurrent.Executor;

import static org.mockito.Mockito.*;

/**
 * Created with IntelliJ IDEA.
 * User: alex
 * Date: 5/11/13
 * Time: 4:12 PM
 * To change this template use File | Settings | File Templates.
 */

@TargetApi(16)
@Config(emulateSdk = 16)
@RunWith(RobolectricTestRunner.class)
public class TaskQueueTest {

    private TaskQueue taskQueue;

    private TaskQueue.TaskQueueListener listener;

    @Before
    public void init() {
        this.taskQueue = new TaskQueue();
        this.listener = mock(TaskQueue.TaskQueueListener.class);
        this.taskQueue.setTaskQueueListener(listener);
    }

    @Test
    public void testSingleTask() {

        String[] params = { "param1" };

        QueueableAsyncTask<String, String, String>
                mockTaskA = mock(QueueableAsyncTask.class);

        taskQueue.executeTask(mockTaskA, params);

        verify(mockTaskA).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);

        //Mock completion of task A
        taskQueue.taskCompleted(mockTaskA, false);

        verify( listener ).queueEmpty();
    }

    @Test(expected = RuntimeException.class )
    public void testTasksOutOfSync() {

        String[] params = { "param1" };

        QueueableAsyncTask<String, String, String>
                mockTaskA = mock(QueueableAsyncTask.class);

        taskQueue.executeTask(mockTaskA, params);

        verify(mockTaskA).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);

        //Mock completion of a different task
        taskQueue.taskCompleted(mock(QueueableAsyncTask.class), false);

    }

    /**
     * Very basic test-case: we queue 3 tasks and
     * check that all 3 are executed in order.
     */
    @Test
    public void testHappyFlow() {

        QueueableAsyncTask<String, String, String>
                mockTaskA = mock(QueueableAsyncTask.class);
        QueueableAsyncTask<String, String, String>
                mockTaskB = mock(QueueableAsyncTask.class);
        QueueableAsyncTask<String, String, String>
                mockTaskC = mock(QueueableAsyncTask.class);

        taskQueue.executeTask(mockTaskA);
        taskQueue.executeTask(mockTaskB);
        taskQueue.executeTask(mockTaskC);

        verify(mockTaskA).executeOnExecutor(any(Executor.class));
        verify(mockTaskB, never() ).executeOnExecutor(any(Executor.class));
        verify(mockTaskC, never() ).executeOnExecutor(any(Executor.class));

        taskQueue.taskCompleted(mockTaskA, false);
        verifyZeroInteractions(listener);

        verify( mockTaskB ).executeOnExecutor(any(Executor.class));
        verify(mockTaskC, never() ).executeOnExecutor(any(Executor.class));

        //Should not be executed again
        verify(mockTaskA, times(1)).executeOnExecutor(any(Executor.class));

        taskQueue.taskCompleted(mockTaskB, false);
        verify(mockTaskC).executeOnExecutor(any(Executor.class));
        verify(mockTaskA, times(1)).executeOnExecutor(any(Executor.class));
        verify(mockTaskB, times(1)).executeOnExecutor(any(Executor.class));

        taskQueue.taskCompleted(mockTaskC, false);

        verify(listener).queueEmpty();
    }

    @Test
    public void testQueueClear() {
        QueueableAsyncTask<String, String, String>
                mockTaskA = mock(QueueableAsyncTask.class);
        QueueableAsyncTask<String, String, String>
                mockTaskB = mock(QueueableAsyncTask.class);
        QueueableAsyncTask<String, String, String>
                mockTaskC = mock(QueueableAsyncTask.class);

        taskQueue.executeTask(mockTaskA);
        taskQueue.executeTask(mockTaskB);
        taskQueue.executeTask(mockTaskC);

        verify(mockTaskA).executeOnExecutor( any(Executor.class) );
        verify(mockTaskB, never() ).executeOnExecutor(any(Executor.class));
        verify(mockTaskC, never()).executeOnExecutor(any(Executor.class));

        taskQueue.clear();
        verify( mockTaskA ).requestCancellation();

        taskQueue.taskCompleted(mockTaskA, true);

        verify(mockTaskB, never()).executeOnExecutor(any(Executor.class));
        verify(mockTaskC, never()).executeOnExecutor(any(Executor.class));

        //verifyZeroInteractions(listener);      ???

    }

    /**
     * Tests queue-jumping: the currently executing task
     * is cancelled and the new task is put ahead of the queue.
     *
     * The rest of the tasks execute as usual.
     */
    @Test
    public void testQueueJumping() {

        QueueableAsyncTask<String, String, String>
                mockTaskA = mock(QueueableAsyncTask.class);
        QueueableAsyncTask<String, String, String>
                mockTaskB = mock(QueueableAsyncTask.class);
        QueueableAsyncTask<String, String, String>
                mockTaskC = mock(QueueableAsyncTask.class);
        QueueableAsyncTask<String, String, String>
                mockTaskD = mock(QueueableAsyncTask.class);

        taskQueue.executeTask(mockTaskA);
        taskQueue.executeTask(mockTaskB);
        taskQueue.executeTask(mockTaskC);

        verify(mockTaskA).executeOnExecutor(any(Executor.class));
        verify(mockTaskB, never() ).executeOnExecutor(any(Executor.class));
        verify(mockTaskC, never() ).executeOnExecutor(any(Executor.class));

        taskQueue.jumpQueueExecuteTask(mockTaskD);

        verify(mockTaskA).requestCancellation();
        verify(mockTaskD).executeOnExecutor(any(Executor.class));

        taskQueue.taskCompleted(mockTaskA, true);

        taskQueue.taskCompleted(mockTaskD, false);
        verify( mockTaskB ).executeOnExecutor(any(Executor.class));

        taskQueue.taskCompleted(mockTaskB, false);

        verify(mockTaskC).executeOnExecutor(any(Executor.class));
        taskQueue.taskCompleted(mockTaskC, false);

        verify(listener).queueEmpty();
    }



}
