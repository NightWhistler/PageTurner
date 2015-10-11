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
package net.nightwhistler.pageturner.catalog;

import android.content.Context;
import android.os.AsyncTask;
import com.google.inject.Inject;
import jedi.option.None;
import jedi.option.Option;
import net.nightwhistler.pageturner.Configuration;
import net.nightwhistler.pageturner.library.LibraryService;
import net.nightwhistler.pageturner.scheduling.QueueableAsyncTask;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.epub.EpubReader;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;

import static jedi.functional.FunctionalPrimitives.isEmpty;
import static jedi.option.Options.none;

public class DownloadFileTask extends QueueableAsyncTask<String, Long, Void> {

    private File destFile;

    private Exception failure;

    private DownloadFileCallback callBack;

    @Inject
    private Context context;

    @Inject
    private Configuration config;

    @Inject
    private LibraryService libraryService;

    @Inject
    private HttpClient httpClient;

    private static final Logger LOG = LoggerFactory.getLogger("DownloadFileTask");

    public interface DownloadFileCallback {

        void onDownloadStart();

        void downloadSuccess(File destinationFile);
        void downloadFailed();

        void progressUpdate( long progress, long total, int percentage );
    }

    DownloadFileTask(){}

    public void setCallBack(DownloadFileCallback callBack) {
        this.callBack = callBack;
    }

    @Override
    public void doOnPreExecute() {
        callBack.onDownloadStart();
    }

    @Override
    public Option<Void> doInBackground(String... params) {

        try {

            String url = params[0];
            LOG.debug("Downloading: " + url);

            String fileName = url.substring(url.lastIndexOf('/') + 1);
            fileName = fileName.replaceAll("\\?|&|=", "_");

            HttpGet get = new HttpGet(url);
            get.setHeader("User-Agent", config.getUserAgent() );
            HttpResponse response = httpClient.execute(get);

            if (response.getStatusLine().getStatusCode() == 200) {

                Option<File> destFolderOption = config.getDownloadsFolder();

                if ( isEmpty(destFolderOption) ) {
                    throw new IllegalStateException("Could not get download folder!");
                }

                File destFolder = destFolderOption.unsafeGet();

                if (!destFolder.exists()) {
                    destFolder.mkdirs();
                }

                /**
                 * Make sure we always store downloaded files as .epub,
                 * so they show up in scans later on.
                 */
                if ( ! fileName.endsWith(".epub") ) {
                    fileName = fileName + ".epub";
                }

                // Default Charset for android is UTF-8*
                String charsetName = Charset.defaultCharset().name();

                if (!Charset.isSupported(charsetName)) {
                    LOG.warn("{} is not a supported Charset. Will fall back to UTF-8", charsetName);
                    charsetName = "UTF-8";
                }

                try {
                    destFile = new File(destFolder, URLDecoder.decode(fileName,charsetName));
                } catch (UnsupportedEncodingException e) {
                    // Won't ever reach here
                    throw new AssertionError(e);
                }


                if (destFile.exists()) {
                    destFile.delete();
                }

                // lenghtOfFile is used for calculating download progress
                long lenghtOfFile = response.getEntity().getContentLength();

                // this is where the file will be seen after the download
                FileOutputStream f = new FileOutputStream(destFile);

                try {
                    // file input is from the url
                    InputStream in = response.getEntity().getContent();

                    // here's the download code
                    byte[] buffer = new byte[1024];
                    int len1 = 0;
                    long total = 0;

                    while ((len1 = in.read(buffer)) > 0 && ! isCancelled() ) {

                        // Make sure the user can cancel the download.
                        if (isCancelled()) {
                            return new None();
                        }

                        total += len1;
                        publishProgress(total, lenghtOfFile, (long) ((total * 100) / lenghtOfFile));
                        f.write(buffer, 0, len1);
                    }
                } finally {
                    f.close();
                }

                if ( ! isCancelled() ) {
                    //FIXME: This doesn't belong here really...
                    Book book = new EpubReader().readEpubLazy( destFile.getAbsolutePath(), "UTF-8" );
                    libraryService.storeBook(destFile.getAbsolutePath(), book, false, config.getCopyToLibraryOnScan() );
                }

            } else {
                this.failure = new RuntimeException(response
                    .getStatusLine().getReasonPhrase());
                LOG.error("Download failed: "
                    + response.getStatusLine().getReasonPhrase());
            }

        } catch (Exception e) {
            LOG.error("Download failed.", e);
            this.failure = e;
        }

        return none();
    }

    @Override
    public void doOnProgressUpdate(Long... values) {
        callBack.progressUpdate(values[0], values[1], values[2].intValue() );
    }

    @Override
    public void doOnPostExecute(Option<Void> unused) {
        if (!isCancelled() && failure == null) {
            callBack.downloadSuccess(destFile);
        } else if (failure != null) {
            callBack.downloadFailed();
        }
    }
}
