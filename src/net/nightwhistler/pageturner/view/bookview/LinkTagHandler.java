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
import android.text.SpannableStringBuilder;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.view.View;
import net.nightwhistler.htmlspanner.SpanStack;
import net.nightwhistler.htmlspanner.TagNodeHandler;
import org.htmlcleaner.TagNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Creates clickable links.
 *
 * @author Alex Kuiper
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
                              int start, int end, SpanStack spanStack) {

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

        spanStack.pushSpan(span, start, end);
    }

    public static interface LinkCallBack {
        void linkClicked( String href );
    }
}