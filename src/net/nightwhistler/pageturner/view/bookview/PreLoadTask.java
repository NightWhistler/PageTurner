package net.nightwhistler.pageturner.view.bookview;

import android.text.Spannable;
import net.nightwhistler.pageturner.epub.PageTurnerSpine;
import net.nightwhistler.pageturner.scheduling.QueueableAsyncTask;
import nl.siegmann.epublib.domain.Resource;

/**
 * Created by alex on 10/14/14.
 */
public class PreLoadTask extends
        QueueableAsyncTask<Void, Void, Void> {

    private PageTurnerSpine spine;
    private TextLoader textLoader;

    public PreLoadTask( PageTurnerSpine spine, TextLoader textLoader ) {
        this.spine = spine;
        this.textLoader = textLoader;
    }

    @Override
    protected Void doInBackground(Void... voids) {

        if ( spine == null ) {
            return null;
        }

        Resource resource = spine.getNextResource();
        if ( resource == null ) {
            return null;
        }

        Spannable cachedText = textLoader.getCachedTextForResource( resource );

        if ( cachedText == null ) {
            try {
                textLoader.getText( resource, this::isCancelled );
            } catch ( Exception | OutOfMemoryError e ) {
                //Ignore
            }
        }

        return null;
    }

}


