package net.nightwhistler.pageturner.view.bookview;

import android.text.Spannable;
import jedi.option.Option;
import net.nightwhistler.pageturner.epub.PageTurnerSpine;
import net.nightwhistler.pageturner.scheduling.QueueableAsyncTask;
import nl.siegmann.epublib.domain.Resource;

import static jedi.functional.FunctionalPrimitives.isEmpty;

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

        Option<Resource> resource = spine.getNextResource();

        resource.forEach( res -> {
            Option<Spannable> cachedText = textLoader.getCachedTextForResource( res );

            if ( isEmpty(cachedText) ) {
                try {
                    textLoader.getText( res, PreLoadTask.this::isCancelled );
                } catch ( Exception | OutOfMemoryError e ) {
                    //Ignore
                }
            }
        });

        return null;
    }

}


