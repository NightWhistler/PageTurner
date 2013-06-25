package net.nightwhistler.pageturner.view.bookview;

import android.content.Context;
import android.graphics.Typeface;
import com.google.inject.Inject;
import net.nightwhistler.htmlspanner.FontFamily;
import net.nightwhistler.htmlspanner.SystemFontResolver;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.util.IOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created with IntelliJ IDEA.
 * User: alex
 * Date: 6/23/13
 * Time: 9:56 AM
 * To change this template use File | Settings | File Templates.
 */
public class EpubFontResolver extends SystemFontResolver {

    private Map<String, FontFamily> loadedTypeFaces = new HashMap<String, FontFamily>();
    private TextLoader textLoader;
    private Context context;

    private static final Logger LOG = LoggerFactory.getLogger("EpubFontResolver");

    @Inject
    public EpubFontResolver(TextLoader loader, Context context) {

        this.textLoader = loader;
        loader.setFontResolver(this);

        this.context = context;
    }

    @Override
    protected FontFamily resolveFont(String name) {

        LOG.debug("Trying lookup for font " + name );

        if ( loadedTypeFaces.containsKey(name) ) {
            return loadedTypeFaces.get(name);
        }

        LOG.debug("Font is not in cache, falling back to super.");

        return super.resolveFont(name);
    }

    public void loadEmbeddedFont( String name, String resourceHRef ) {

        LOG.debug("Attempting to load custom font from href " + resourceHRef );

        if ( loadedTypeFaces.containsKey(name) ) {
            LOG.debug("Already have font " + resourceHRef + ", aborting.");
            return;
        }

        Resource res = textLoader.getCurrentBook().getResources().getByHref(resourceHRef);

        if ( res == null ) {
            LOG.error("No resource found for href " + resourceHRef );
            return;
        }

        File tempFile = new File(context.getCacheDir(), UUID.randomUUID().toString() );

        try {
            IOUtil.copy( res.getInputStream(), new FileOutputStream(tempFile));
            res.close();

            Typeface typeface = Typeface.createFromFile(tempFile);

            FontFamily fontFamily = new FontFamily( name, typeface );

            LOG.debug("Loaded embedded font with name " + name );
            loadedTypeFaces.put(name, fontFamily);

        } catch ( IOException io ) {
            LOG.error("Could not load embedded font " + name, io );
        } finally {
            tempFile.delete();
        }
    }
}
