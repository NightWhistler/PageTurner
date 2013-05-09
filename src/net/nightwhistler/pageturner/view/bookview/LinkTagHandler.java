package net.nightwhistler.pageturner.view.bookview;

import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.view.View;
import net.nightwhistler.htmlspanner.TagNodeHandler;
import org.htmlcleaner.TagNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Creates clickable links.
 *
 * @author work
 *
 */
public class LinkTagHandler extends TagNodeHandler {

    private List<String> externalProtocols;

    private LinkCallBack callBack;

    public LinkTagHandler(LinkCallBack callBack) {

        this.callBack = callBack;

        this.externalProtocols = new ArrayList<String>();
        externalProtocols.add("http://");
        externalProtocols.add("epub://");
        externalProtocols.add("https://");
        externalProtocols.add("http://");
        externalProtocols.add("ftp://");
        externalProtocols.add("mailto:");
    }

    @Override
    public void handleTagNode(TagNode node, SpannableStringBuilder builder,
                              int start, int end) {

        String href = node.getAttributeByName("href");

        if (href == null) {
            return;
        }

        final String linkHref = href;

        // First check if it should be a normal URL link
        for (String protocol : this.externalProtocols) {
            if (href.toLowerCase(Locale.US).startsWith(protocol)) {
                builder.setSpan(new URLSpan(href), start, end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                return;
            }
        }

        // If not, consider it an internal nav link.
        ClickableSpan span = new ClickableSpan() {

            @Override
            public void onClick(View widget) {
                callBack.linkClicked(linkHref);
            }
        };

        builder.setSpan(span, start, end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    public static interface LinkCallBack {
        void linkClicked( String href );
    }
}