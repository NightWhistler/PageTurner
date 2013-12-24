package net.nightwhistler.pageturner.view.bookview;

import net.nightwhistler.htmlspanner.HtmlSpanner;
import net.nightwhistler.pageturner.scheduling.QueueableAsyncTask;

/**
 * Created with IntelliJ IDEA.
 * User: alex
 * Date: 12/24/13
 * Time: 1:17 PM
 * To change this template use File | Settings | File Templates.
 */
public class QueueableAsyncTaskCancellationCallback implements HtmlSpanner.CancellationCallback {

    private QueueableAsyncTask<?,?,?> task;

    public QueueableAsyncTaskCancellationCallback( QueueableAsyncTask<?,?,?> task ) {
        this.task = task;
    }

    @Override
    public boolean isCancelled() {
        return task.isCancelRequested();
    }
}
