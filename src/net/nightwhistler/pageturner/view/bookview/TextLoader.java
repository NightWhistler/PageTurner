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
import com.google.inject.Inject;
import net.nightwhistler.htmlspanner.FontFamily;
import net.nightwhistler.htmlspanner.HtmlSpanner;
import net.nightwhistler.htmlspanner.TagNodeHandler;
import net.nightwhistler.pageturner.Configuration;
import net.nightwhistler.pageturner.view.FastBitmapDrawable;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.MediaType;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.epub.EpubReader;
import nl.siegmann.epublib.service.MediatypeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * Singleton storage for opened book and rendered text.
 *
 * Optimization in case of rotation of the screen.
 */
public class TextLoader implements LinkTagHandler.LinkCallBack {

    /**
     * We start clearing the cache if memory usage exceeds 75%.
     */
    private static final double CASH_CLEAR_THRESHOLD = 0.75;

    private String currentFile;
    private Book currentBook;
    private Map<String, Spannable> renderedText = new HashMap<String, Spannable>();

    private Map<String, FastBitmapDrawable> imageCache = new HashMap<String, FastBitmapDrawable>();

    private Map<String, Map<String, Integer>> anchors = new HashMap<String, Map<String, Integer>>();
    private List<AnchorHandler> anchorHandlers = new ArrayList<AnchorHandler>();

    private static final Logger LOG = LoggerFactory.getLogger("TextLoader");

    private HtmlSpanner htmlSpanner;

    private LinkTagHandler.LinkCallBack linkCallBack;

    @Inject
    public void setHtmlSpanner(HtmlSpanner spanner) {
        this.htmlSpanner = spanner;

        spanner.registerHandler("a", registerAnchorHandler(new LinkTagHandler(this)));

        spanner.registerHandler("h1",
                registerAnchorHandler(spanner.getHandlerFor("h1")));
        spanner.registerHandler("h2",
                registerAnchorHandler(spanner.getHandlerFor("h2")));
        spanner.registerHandler("h3",
                registerAnchorHandler(spanner.getHandlerFor("h3")));
        spanner.registerHandler("h4",
                registerAnchorHandler(spanner.getHandlerFor("h4")));
        spanner.registerHandler("h5",
                registerAnchorHandler(spanner.getHandlerFor("h5")));
        spanner.registerHandler("h6",
                registerAnchorHandler(spanner.getHandlerFor("h6")));

        spanner.registerHandler("p",
                registerAnchorHandler(spanner.getHandlerFor("p")));

    }

    private AnchorHandler registerAnchorHandler( TagNodeHandler wrapThis ) {
        AnchorHandler handler = new AnchorHandler(wrapThis);
        anchorHandlers.add(handler);
        return handler;
    }

    @Override
    public void linkClicked(String href) {
        if ( linkCallBack != null ) {
            linkCallBack.linkClicked(href);
        }
    }

    public void setLinkCallBack( LinkTagHandler.LinkCallBack callBack ) {
        this.linkCallBack = callBack;
    }

    public void registerTagNodeHandler( String tag, TagNodeHandler handler ) {
        this.htmlSpanner.registerHandler(tag, handler);
    }

    public Book initBook(String fileName) throws IOException {

        if (fileName == null) {
            throw new IOException("No file-name specified.");
        }

        if ( fileName.equals(currentFile) ) {
            LOG.debug("Returning cached Book for fileName " + currentFile );
            return currentBook;
        }

        closeCurrentBook();

        this.anchors = new HashMap<String, Map<String, Integer>>();

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

    public Integer getAnchor( String href, String anchor ) {
        if ( this.anchors.containsKey(href) ) {
            Map<String, Integer> nestedMap = this.anchors.get( href );
            return nestedMap.get(anchor);
        }

        return null;
    }

    public void setFontFamily(FontFamily family) {
        this.htmlSpanner.setDefaultFont(family);
    }

    public void setSerifFontFamily(FontFamily family) {
        this.htmlSpanner.setSerifFont(family);
    }

    public void setSansSerifFontFamily(FontFamily family) {
        this.htmlSpanner.setSansSerifFont(family);
    }

    public void setStripWhiteSpace(boolean stripWhiteSpace) {
        this.htmlSpanner.setStripExtraWhiteSpace(stripWhiteSpace);
    }

    public FastBitmapDrawable getCachedImage( String href ) {
        return imageCache.get( href );
    }

    public boolean hasCachedImage( String href ) {
        return imageCache.containsKey(href);
    }

    public void storeImageInChache( String href, FastBitmapDrawable drawable ) {
        this.imageCache.put(href, drawable);
    }

    private void registerNewAnchor(String href, String anchor, int position ) {
        if ( ! anchors.containsKey(href)) {
            anchors.put(href, new HashMap<String, Integer>());
        }

        anchors.get(href).put(anchor, position);
    }

    public Spannable getText( final Resource resource, boolean allowCaching ) throws IOException {

        if ( renderedText.containsKey(resource.getHref()) ) {
            LOG.debug("Returning cached text for href " + resource.getHref() );
            return renderedText.get(resource.getHref());
        }

        AnchorHandler.AnchorCallback callback = new AnchorHandler.AnchorCallback() {
            @Override
            public void registerAnchor(String anchor, int position) {
                registerNewAnchor(resource.getHref(), anchor, position);
            }
        };

        for ( AnchorHandler handler: this.anchorHandlers ) {
            handler.setCallback(callback);
        }

        double memoryUsage = Configuration.getMemoryUsage();
        double bitmapUsage = Configuration.getBitmapMemoryUsage();

        LOG.debug("Current memory usage is " +  (int) (memoryUsage * 100) + "%" );
        LOG.debug("Current bitmap memory usage is " +  (int) (bitmapUsage * 100) + "%" );

        //If memory usage gets over the threshold, try to free up memory
        if ( memoryUsage > CASH_CLEAR_THRESHOLD || bitmapUsage > CASH_CLEAR_THRESHOLD ) {
            clearCachedText();
        }

        Spannable result = htmlSpanner.fromHtml(resource.getReader());

        if ( allowCaching ) {
            renderedText.put(resource.getHref(), result);
        }

        return result;
    }

    private void clearCachedText() {
        clearImageCache();
        anchors.clear();

        renderedText.clear();
    }

    public void closeCurrentBook() {

        if ( currentBook != null ) {
            for ( Resource res: currentBook.getResources().getAll() ) {
                res.setData(null); //Release the byte[] data.
            }
        }

        currentBook = null;
        currentFile = null;
        renderedText.clear();
        clearImageCache();
        anchors.clear();
    }

    public void clearImageCache() {
        for (Map.Entry<String, FastBitmapDrawable> draw : imageCache.entrySet()) {
            draw.getValue().destroy();
        }

        imageCache.clear();
    }




}
