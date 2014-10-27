package net.nightwhistler.pageturner.view.bookview;

import android.text.Spannable;
import jedi.option.None;
import jedi.option.Option;
import net.nightwhistler.pageturner.epub.PageTurnerSpine;
import net.nightwhistler.pageturner.scheduling.QueueableAsyncTask;
import nl.siegmann.epublib.domain.Resource;

import static jedi.functional.FunctionalPrimitives.isEmpty;
import static jedi.option.Options.none;

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
    public Option<Void> doInBackground(Void... voids) {
        doInBackground();

        return none();
    }

    private void doInBackground() {
        if ( spine == null ) {
            return;
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
    }
}


