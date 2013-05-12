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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.nightwhistler.pageturner.Configuration;
import net.nightwhistler.pageturner.R;
import net.nightwhistler.pageturner.scheduling.QueueableAsyncTask;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.epub.EpubReader;
import nl.siegmann.epublib.service.MediatypeService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.AsyncTask;

public class ImportTask extends QueueableAsyncTask<File, Integer, Void> implements OnCancelListener {
	
	private Context context;
	private LibraryService libraryService;
	private ImportCallback callBack;	
	private Configuration config;
	
	private boolean copyToLibrary;
	
	private List<String> errors = new ArrayList<String>();
	
	private static final Logger LOG = LoggerFactory.getLogger(ImportTask.class);
	
	private static final int UPDATE_FOLDER = 1;
	private static final int UPDATE_IMPORT = 2;
	
	private int foldersScanned = 0;
	private int booksImported = 0;

    private boolean emptyLibrary;
    private boolean silent;
	
	private String importFailed = null;
	
	public ImportTask( Context context, LibraryService libraryService,
			ImportCallback callBack, Configuration config, boolean copyToLibrary,
            boolean silent) {

		this.context = context;
		this.libraryService = libraryService;
		this.callBack = callBack;
		this.copyToLibrary = copyToLibrary;
		this.config = config;
        this.silent = silent;
	}		
	
	@Override
	public void onCancel(DialogInterface dialog) {
		LOG.debug("User aborted import.");	
		this.cancel(true);
	}

    @Override
	protected Void doInBackground(File... params) {
		File parent = params[0];
		
		if ( ! parent.exists() ) {
			importFailed = String.format( context.getString(R.string.no_such_folder), parent.getPath());			
			return null;
		}

        this.emptyLibrary = this.libraryService.findAllByTitle(null).getSize() == 0;
		
		List<File> books = new ArrayList<File>();			
		findEpubsInFolder(parent, books);
		
		int total = books.size();
		int i = 0;			
        
		while ( i < books.size() && ! isCancelled() ) {
			
			File book = books.get(i);
			
			LOG.info("Importing: " + book.getAbsolutePath() );
			try {
                if ( importBook( book ) ) {
                    booksImported++;
                }
			} catch (OutOfMemoryError oom ) {
				errors.add(book.getName() + ": Out of memory.");
				return null;
			}
			
			i++;
			publishProgress(UPDATE_IMPORT, i, total);
		}
		
		return null;
	}
	
	private void findEpubsInFolder( File folder, List<File> items) {
		
		if ( folder == null ) {
			return;
		}			
		
		if ( isCancelled() ) {
			return;
		}		
		
		if ( folder.isDirectory() && folder.listFiles() != null) {
			
			for (File child: folder.listFiles() ) {
				findEpubsInFolder(child, items); 
			}
			
			foldersScanned++;
			publishProgress(UPDATE_FOLDER, foldersScanned);
			
		} else {
			
			String fileName = folder.getAbsolutePath();
			
			//Scan items 
			if ( fileName.endsWith(".epub") ) {
				items.add(folder);
			} else if ( fileName.startsWith(config.getLibraryFolder()) 
					|| fileName.startsWith(config.getDownloadsFolder() )) {
					
				if ( folder.getName().indexOf(".") == -1 ) {				
					//Older versions downloaded files without an extension				
					items.add(folder);
				}
			}
		}
	}

    private boolean importBook(File file) {

        if (! libraryService.hasBook(file.getName() ) ) {
            try {

                String fileName = file.getAbsolutePath();

                // read epub file
                EpubReader epubReader = new EpubReader();

                Book importedBook = epubReader.readEpubLazy(fileName, "UTF-8",
                        Arrays.asList(MediatypeService.mediatypes));

                libraryService.storeBook(fileName, importedBook, false, this.copyToLibrary);

                return true;

            } catch (Exception io ) {
                errors.add( file + ": " + io.getMessage() );
                LOG.error("Error while reading book: " + file, io);
            }
        }

        return false;
    }

    @Override
	protected void onProgressUpdate(Integer... values) {
		
		String message;
		
		if ( values[0] == UPDATE_IMPORT ) {
			message = String.format(context.getString(R.string.importing), values[1], values[2]);		
		} else {
			message = String.format(context.getString(R.string.scan_folders), values[1]);			
		}
		
		callBack.importStatusUpdate(message, silent);
	}

    @Override
    protected void doOnPostExecute(Void aVoid) {

        LOG.debug("Import task completed, imported " + booksImported  + " books.");

		if ( importFailed != null ) {
			callBack.importFailed(importFailed, silent);
		} else if ( ! isCancelled() ) {
			this.callBack.importComplete(booksImported, errors, emptyLibrary, silent);
		}
	}
}
