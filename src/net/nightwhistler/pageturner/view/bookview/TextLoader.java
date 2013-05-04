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

package net.nightwhistler.pageturner.view.bookview;

import android.text.Spannable;
import android.text.Spanned;
import net.nightwhistler.htmlspanner.HtmlSpanner;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.MediaType;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.epub.EpubReader;
import nl.siegmann.epublib.service.MediatypeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Singleton storage for opened book and rendered text.
 *
 * Optimization in case of rotation of the screen.
 */
public class TextLoader {

    private String currentFile;
    private Book currentBook;
    private Map<String, Spannable> renderedText = new HashMap<String, Spannable>();

    private static final Logger LOG = LoggerFactory.getLogger("TextLoader");

    public Book initBook(String fileName) throws IOException {

        if (fileName == null) {
            throw new IOException("No file-name specified.");
        }

        if ( fileName.equals(currentFile) ) {
            LOG.debug("Returning cached Book for fileName " + currentFile );
            return currentBook;
        }

        closeCurrentBook();

        // read epub file
        EpubReader epubReader = new EpubReader();

        MediaType[] lazyTypes = {
                MediatypeService.CSS, // We don't support CSS yet

                MediatypeService.GIF, MediatypeService.JPG,
                MediatypeService.PNG,
                MediatypeService.SVG, // Handled by the ResourceLoader

                MediatypeService.OPENTYPE,
                MediatypeService.TTF, // We don't support custom fonts
                // either
                MediatypeService.XPGT,

                MediatypeService.MP3,
                MediatypeService.MP4, // And no audio either
                MediatypeService.OGG,
                MediatypeService.SMIL, MediatypeService.XPGT,
                MediatypeService.PLS };

        Book newBook = epubReader.readEpubLazy(fileName, "UTF-8",
                Arrays.asList(lazyTypes));

        this.currentBook = newBook;
        this.currentFile = fileName;

        return newBook;

    }

    public Spannable getText( Resource resource, HtmlSpanner spanner, boolean allowCaching ) throws IOException {

        if ( renderedText.containsKey(resource.getHref()) ) {
            LOG.debug("Returning cached text for href " + resource.getHref() );
            return renderedText.get(resource.getHref());
        }

        Spannable result = spanner.fromHtml(resource.getReader());

        if ( allowCaching ) {
            renderedText.put(resource.getHref(), result);
        }

        return result;
    }

    private void closeCurrentBook() {

        if ( currentBook != null ) {
            for ( Resource res: currentBook.getResources().getAll() ) {
                res.setData(null); //Release the byte[] data.
            }
        }

        currentBook = null;
        currentFile = null;
        renderedText.clear();
    }

}
