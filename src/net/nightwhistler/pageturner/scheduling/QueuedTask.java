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
import net.nightwhistler.pageturner.PlatformUtil;

/**
 * Wraps a QueueableAsyncTask and its parameters, so that it can be executed later.
 *
 * It's essentially a simple Command Object for tasks.
 *
 * @param <A>
 * @param <B>
 * @param <C>
 */
public class QueuedTask<A, B, C> {

    private QueueableAsyncTask<A, B, C> task;
    private A[] parameters;

    public QueuedTask(QueueableAsyncTask<A,B,C> task, A[] params ) {
        this.task = task;
        this.parameters = params;
    }

    public void execute() {
        PlatformUtil.executeTask(task, parameters);
    }

    public void cancel() {
        this.task.requestCancellation();
    }

    public AsyncTask<A,B,C> getTask() {
        return task;
    }
}



