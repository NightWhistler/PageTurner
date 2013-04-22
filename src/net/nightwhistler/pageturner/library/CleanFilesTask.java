package net.nightwhistler.pageturner.library;

import android.os.AsyncTask;
import com.google.inject.Inject;
import net.nightwhistler.pageturner.Configuration;
import net.nightwhistler.pageturner.library.ImportCallback;
import net.nightwhistler.pageturner.library.LibraryBook;
import net.nightwhistler.pageturner.library.LibraryService;
import net.nightwhistler.pageturner.library.QueryResult;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Task which deletes all files in the library that no longer exist.
 */
public class CleanFilesTask extends AsyncTask<Void, Void, Void> {

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
    protected void onPostExecute(Void aVoid) {
        callback.taskCompleted(this, isCancelled());
        callback.booksDeleted(deletedFiles);
    }
}
