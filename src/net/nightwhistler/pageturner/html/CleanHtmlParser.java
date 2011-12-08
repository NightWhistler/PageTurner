/*
 * Copyright (C) 2011 Alex Kuiper
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.nightwhistler.pageturner.html;

import java.util.HashMap;
import java.util.Map;

import org.htmlcleaner.ContentNode;
import org.htmlcleaner.TagNode;

import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.Layout.Alignment;
import android.text.style.AlignmentSpan;
import android.text.style.QuoteSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.SubscriptSpan;
import android.text.style.SuperscriptSpan;
import android.text.style.TypefaceSpan;

public class CleanHtmlParser {
	
	private Map<String, TagNodeHandler> handlers;
	
	public CleanHtmlParser() {
		this.handlers = new HashMap<String, TagNodeHandler>();
		registerBuiltInHandlers();
	}
	
	public void registerHandler( String tagName, TagNodeHandler handler ) {
		this.handlers.put(tagName, handler);
	}

	public Spanned fromTagNode( TagNode node ) {
		SpannableStringBuilder result = new SpannableStringBuilder();
		handleContent(result, node);
		return result;		
	}
	
	private void handleContent( SpannableStringBuilder builder, Object node ) {		
		if ( node instanceof ContentNode ) {			
			
			if ( builder.length() > 0 ) {
				char lastChar = builder.charAt( builder.length() - 1 );
				if ( lastChar != ' ' && lastChar != '\n' ) {
					builder.append(' ');
				}
			}
			
			builder.append( ((ContentNode) node).getContent().toString().replaceAll("\n", " ").trim() );
		} else if ( node instanceof TagNode ) { 
			applySpan(builder, (TagNode) node); 
		}		
	}
	
	/**
	 * Gets the currently registered handler for this tag.
	 * 
	 * Used so it can be wrapped.
	 * 
	 * @param tagName
	 * @return
	 */
	public TagNodeHandler getHandlerFor( String tagName ) {
		return this.handlers.get(tagName);
	}	
	
	private void applySpan( SpannableStringBuilder builder, TagNode node ) {
		
		int lengthBefore = builder.length();
		
		for ( Object childNode: node.getChildren() ) {
			handleContent(builder, childNode );
		}
		
		int lengthAfter = builder.length();
		
		TagNodeHandler handler = this.handlers.get(node.getName());
		
		if ( handler != null ) {
			handler.handleTagNode(node, builder, lengthBefore, lengthAfter);
		}
	}
	
	private void registerBuiltInHandlers() {
		
		TagNodeHandler italicHandler = new TagNodeHandler() {
			public void handleTagNode(TagNode node, SpannableStringBuilder builder,
					int start, int end) {
				builder.setSpan(new StyleSpan(Typeface.ITALIC), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);				
			}
		};
		
		registerHandler("i", italicHandler);
		registerHandler("strong", italicHandler);
		registerHandler("cite", italicHandler);
		registerHandler("dfn", italicHandler);
		
		
		TagNodeHandler boldHandler = new TagNodeHandler() {
			public void handleTagNode(TagNode node, SpannableStringBuilder builder,
					int start, int end) {
				builder.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);				
			}
		};
		
		registerHandler("b", boldHandler);
		registerHandler("em", boldHandler);
		
		TagNodeHandler quoteHandler = new TagNodeHandler() {
			public void handleTagNode(TagNode node, SpannableStringBuilder builder,
					int start, int end) {
				builder.setSpan(new QuoteSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				builder.append("\n\n");
			}
		};
		
		registerHandler("blockquote", quoteHandler);
		
		TagNodeHandler brHandler = new TagNodeHandler() {
			public void handleTagNode(TagNode node, SpannableStringBuilder builder,
					int start, int end) {
				builder.append("\n");
			}
		};
		
		registerHandler("br", brHandler);
	
		TagNodeHandler pHandler = new TagNodeHandler() {
			public void handleTagNode(TagNode node, SpannableStringBuilder builder,
					int start, int end) {
				builder.append("\n\n");
			}
		};
		
		registerHandler("p", pHandler);
		registerHandler("div", pHandler);

		registerHandler("h1", new HeaderHandler(1.5f));
		registerHandler("h2", new HeaderHandler(1.4f));
		registerHandler("h3", new HeaderHandler(1.3f));
		registerHandler("h4", new HeaderHandler(1.2f));
		registerHandler("h5", new HeaderHandler(1.1f));
		registerHandler("h6", new HeaderHandler(1f));
		
		TagNodeHandler monSpaceHandler = new TagNodeHandler() {
			public void handleTagNode(TagNode node, SpannableStringBuilder builder,
					int start, int end) {
				
				builder.setSpan( new TypefaceSpan("monospace"), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
		};
		
		registerHandler("tt", monSpaceHandler);	
		
		TagNodeHandler bigHandler = new TagNodeHandler() {
			public void handleTagNode(TagNode node, SpannableStringBuilder builder,
					int start, int end) {
				
				builder.setSpan( new RelativeSizeSpan(1.25f), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
		};
		
		registerHandler("big", bigHandler);	
		
		TagNodeHandler smallHandler = new TagNodeHandler() {
			public void handleTagNode(TagNode node, SpannableStringBuilder builder,
					int start, int end) {
				
				builder.setSpan( new RelativeSizeSpan(0.8f), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
		};
		
		registerHandler("small", smallHandler);	
		
		TagNodeHandler subHandler = new TagNodeHandler() {
			public void handleTagNode(TagNode node, SpannableStringBuilder builder,
					int start, int end) {
				
				builder.setSpan( new SubscriptSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
		};
		
		registerHandler("sub", subHandler);	
		
		TagNodeHandler superHandler = new TagNodeHandler() {
			public void handleTagNode(TagNode node, SpannableStringBuilder builder,
					int start, int end) {
				
				builder.setSpan( new SuperscriptSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
		};
		
		registerHandler("sup", superHandler);	
		
		TagNodeHandler centerHandler = new TagNodeHandler() {
			
			@Override
			public void handleTagNode(TagNode node, SpannableStringBuilder builder,
					int start, int end) {
				builder.setSpan(new AlignmentSpan() {
					@Override
					public Alignment getAlignment() {
						return Alignment.ALIGN_CENTER;
					}
				}, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);				
			}
		};
		
		registerHandler("center", centerHandler);
	}
	
	private class HeaderHandler implements TagNodeHandler {
		
		private float size;
		
		public HeaderHandler(float size) {
			this.size = size;
		}		
		
		@Override
		public void handleTagNode(TagNode node, SpannableStringBuilder builder,
				int start, int end) {
			
			builder.setSpan(new RelativeSizeSpan(size), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			builder.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			
			builder.append("\n\n");
		}
	}
}
