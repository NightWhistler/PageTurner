package net.nightwhistler.pageturner.view.bookview;

import android.text.style.BackgroundColorSpan;
import net.nightwhistler.pageturner.view.HighlightManager;

/**
 * Created with IntelliJ IDEA.
 * User: alex
 * Date: 6/17/13
 * Time: 8:46 AM
 * To change this template use File | Settings | File Templates.
 */
public class HighlightSpan extends BackgroundColorSpan {

    private HighlightManager.HighLight highLight;

    public HighlightSpan( HighlightManager.HighLight highLight ) {
        super( highLight.getColor() );
        this.highLight = highLight;
    }

    public HighlightManager.HighLight getHighLight() {
        return this.highLight;
    }


}
