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

package net.nightwhistler.pageturner.library;

import net.nightwhistler.pageturner.Configuration;
import net.nightwhistler.pageturner.scheduling.QueueableAsyncTask;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Task which deletes all files in the library that no longer exist.
 */
public class CleanFilesTask extends QueueableAsyncTask<Void, Void, Void> {

    private Configuration config;

    private LibraryService libraryService;

    private ImportCallback callback;

    private int deletedFiles = 0;

    public CleanFilesTask(ImportCallback callback, LibraryService service, Configuration config) {
        this.callback = callback;
        this.libraryService = service;
        this.config = config;
    }

    @Override
    protected Void doInBackground(Void... voids) {

        QueryResult<LibraryBook> allBooks = libraryService.findAllByTitle(null);
        List<String> filesToDelete = new ArrayList<String>();

        for ( int i=0; i < allBooks.getSize() && ! isCancelled(); i++ ) {
            LibraryBook book = allBooks.getItemAt(i);
            File file = new File(book.getFileName());

            if ( ! file.exists() ) {
                filesToDelete.add(book.getFileName());
            }
        }

        allBooks.close();

        for ( String fileName: filesToDelete ) {
            if ( ! isCancelled() ) {
                libraryService.deleteBook(fileName);
                deletedFiles++;
            }
        }

        return null;
    }

    @Override
    protected void doOnPostExecute(Void aVoid) {
        callback.booksDeleted(deletedFiles);
    }
}
