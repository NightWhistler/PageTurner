package net.nightwhistler.pageturner.scheduling;

import com.xtremelabs.robolectric.RobolectricTestRunner;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.mockito.Mockito.*;

/**
 * Created with IntelliJ IDEA.
 * User: alex
 * Date: 5/11/13
 * Time: 4:12 PM
 * To change this template use File | Settings | File Templates.
 */
@RunWith(RobolectricTestRunner.class)
public class TaskQueueTest {

    private TaskQueue taskQueue;

    private TaskQueue.TaskQueueListener listener;

    @Before
    public void init() {
        this.taskQueue = new TaskQueue();
        this.listener =  mock(TaskQueue.TaskQueueListener.class);
        this.taskQueue.setTaskQueueListener(listener);

    }

    @Test
    public void testSingleTask() {

        String[] params = { "param1" };

        QueueableAsyncTask<String, String, String>
                mockTaskA = mock(QueueableAsyncTask.class);

        taskQueue.executeTask(mockTaskA, params);

        verify(mockTaskA).execute(params);

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

        verify(mockTaskA).execute(params);

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

        verify(mockTaskA).execute();
        verify(mockTaskB, never() ).execute();
        verify(mockTaskC, never() ).execute();

        taskQueue.taskCompleted(mockTaskA, false);
        verifyZeroInteractions(listener);

        verify( mockTaskB ).execute();
        verify(mockTaskC, never() ).execute();

        //Should not be executed again
        verify(mockTaskA, times(1)).execute();

        taskQueue.taskCompleted(mockTaskB, false);
        verify(mockTaskC).execute();
        verify(mockTaskA, times(1)).execute();
        verify(mockTaskB, times(1)).execute();

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

        verify(mockTaskA).execute();
        verify(mockTaskB, never() ).execute();
        verify(mockTaskC, never() ).execute();

        taskQueue.clear();
        verify( mockTaskA ).requestCancellation();

        taskQueue.taskCompleted(mockTaskA, true);

        verify(mockTaskB, never()).execute();
        verify(mockTaskC, never()).execute();

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

        verify(mockTaskA).execute();
        verify(mockTaskB, never() ).execute();
        verify(mockTaskC, never() ).execute();

        taskQueue.jumpQueueExecuteTask(mockTaskD);

        verify(mockTaskA).requestCancellation();
        verify(mockTaskD).execute();

        taskQueue.taskCompleted(mockTaskA, true);

        taskQueue.taskCompleted(mockTaskD, false);
        verify( mockTaskB ).execute();

        taskQueue.taskCompleted(mockTaskB, false);

        verify(mockTaskC).execute();
        taskQueue.taskCompleted(mockTaskC, false);

        verify(listener).queueEmpty();
    }



}
