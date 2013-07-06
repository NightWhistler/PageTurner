package net.nightwhistler.pageturner.view.bookview;

import android.text.SpannableStringBuilder;
import net.nightwhistler.htmlspanner.SpanStack;
import net.nightwhistler.htmlspanner.TagNodeHandler;
import net.nightwhistler.htmlspanner.css.CompiledRule;
import org.htmlcleaner.TagNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: alex
 * Date: 6/22/13
 * Time: 12:50 PM
 * To change this template use File | Settings | File Templates.
 */
public class CSSLinkHandler extends TagNodeHandler {

    private static final Logger LOG =  LoggerFactory.getLogger("CSSLinkHandler");

    private TextLoader textLoader;

    public CSSLinkHandler( TextLoader textLoader ) {
        this.textLoader = textLoader;
    }

    public void handleTagNode(TagNode node, SpannableStringBuilder builder, int start, int end, SpanStack spanStack) {

        if ( getSpanner().isAllowStyling() ) {
            String type = node.getAttributeByName("type");
            String href = node.getAttributeByName("href");

            LOG.debug("Found link tag: type=" + type + " and href=" + href );

            if ( type == null || ! type.equals("text/css") ) {
                LOG.debug("Ignoring link of type " + type );
            }

            List<CompiledRule> rules = this.textLoader.getCSSRules(href);

            for ( CompiledRule rule: rules ) {
                spanStack.registerCompiledRule(rule);
            }
        }
    }

}
