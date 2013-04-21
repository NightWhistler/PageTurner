package net.nightwhistler.pageturner.catalog;

import android.os.AsyncTask;
import com.google.inject.Inject;
import net.nightwhistler.pageturner.Configuration;
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

    @Inject
    private Configuration config;

    @Inject
    private LibraryService libraryService;

    @Override
    protected Void doInBackground(Void... voids) {

        QueryResult<LibraryBook> allBooks = libraryService.findAllByTitle(null);
        List<String> filesToDelete = new ArrayList<String>();

        for ( int i=0; i < allBooks.getSize(); i++ ) {
            LibraryBook book = allBooks.getItemAt(i);
            File file = new File(book.getFileName());

            if ( ! file.exists() ) {
                filesToDelete.add(book.getFileName());
            }
        }

        allBooks.close();

        for ( String fileName: filesToDelete ) {
            libraryService.deleteBook(fileName);
        }

        return null;
    }
}
