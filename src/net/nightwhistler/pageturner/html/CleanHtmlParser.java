/*
 * Copyright (C) 2011 Alex Kuiper
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

package net.nightwhistler.pageturner.html;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlcleaner.ContentNode;
import org.htmlcleaner.TagNode;

import android.graphics.Typeface;
import android.text.Layout.Alignment;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.AlignmentSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.SubscriptSpan;
import android.text.style.SuperscriptSpan;
import android.text.style.TypefaceSpan;

public class CleanHtmlParser {
	
	private Map<String, TagNodeHandler> handlers;	
	
	private boolean stripExtraWhiteSpace = false;
	
	private static int MARGIN_INDENT = 30;	
	
	private static Pattern SPECIAL_CHAR = Pattern.compile( "(\t| +|&[a-z]*;|&#[0-9]*;|\n)" );

	private static Map<String, String> REPLACEMENTS = new HashMap<String, String>();

	static {		
		
		REPLACEMENTS.put("", " ");
		REPLACEMENTS.put("\n", " ");		
		REPLACEMENTS.put("&nbsp;", " ");
		REPLACEMENTS.put("&amp;", "&");
		REPLACEMENTS.put("&quot;", "\"");
		REPLACEMENTS.put("&cent;", "¢" );
		REPLACEMENTS.put("&lt;", "<" );
		REPLACEMENTS.put("&gt;", ">" );
		REPLACEMENTS.put("&sect;", "§" );

	}
		
	private static String getEditedText(String aText){
		StringBuffer result = new StringBuffer();
		Matcher matcher = SPECIAL_CHAR.matcher(aText);

		while ( matcher.find() ) {
			matcher.appendReplacement(result, getReplacement(matcher));
		}
		matcher.appendTail(result);
		return result.toString();
	}

	private static String getReplacement(Matcher aMatcher){
		
		String match = aMatcher.group(0).trim();
		String result = REPLACEMENTS.get( match );

		if ( result != null ) {
			return result;
		} else if ( match.startsWith("&#") ) {
			//Translate to unicode character.
			try {
				Integer code = Integer.parseInt(match.substring(2, match.length()-1));
				return "" + (char) code.intValue();
			} catch (NumberFormatException nfe) {
				return "";
			}
		} else {
			return "";
		}
	}


	public CleanHtmlParser() {
		this.handlers = new HashMap<String, TagNodeHandler>();
		registerBuiltInHandlers();
	}
	
	public void setStripExtraWhiteSpace(boolean stripExtraWhiteSpace) {
		this.stripExtraWhiteSpace = stripExtraWhiteSpace;
	}
	
	public void registerHandler( String tagName, TagNodeHandler handler ) {
		this.handlers.put(tagName, handler);
	}

	public Spanned fromTagNode( TagNode node ) {
		SpannableStringBuilder result = new SpannableStringBuilder();
		handleContent(result, node, null);
		
		return result;		
	}
	
	private void appendNewLine( SpannableStringBuilder builder ) {
		
		int len = builder.length();
		
		if ( stripExtraWhiteSpace ) {
			//Should never have more than 2 \n characters in a row.
			if ( len > 2 && builder.charAt(len -1) == '\n' && builder.charAt(len - 2) == '\n' ) {
				return; 
			}
		}
		
		builder.append("\n");		
	}
	
	private void handleContent( SpannableStringBuilder builder, Object node, TagNode parent ) {		
		if ( node instanceof ContentNode ) {			
			
			ContentNode contentNode = (ContentNode) node;
			
			if ( builder.length() > 0 ) {
				char lastChar = builder.charAt( builder.length() - 1 );
				if ( lastChar != ' ' && lastChar != '\n' ) {
					builder.append(' ');
				}
			}
			
			String text;
			if ( hasPreParent(parent) ) {
				text = contentNode.getContent().toString();
			} else {
				text = getEditedText( contentNode.getContent().toString() ).trim();
			}
			
			builder.append( text );			
						
		} else if ( node instanceof TagNode ) { 
			applySpan(builder, (TagNode) node); 
		}		
	}
	
	private boolean hasPreParent( TagNode tagNode ) {
		if ( tagNode == null ) {
			return false;
		}
		
		return tagNode.getName().equals("pre")
			|| hasPreParent(tagNode.getParent());
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
		
		TagNodeHandler handler = this.handlers.get(node.getName());
		
		int lengthBefore = builder.length();
		
		if ( handler != null ) {
			handler.beforeChildren(node, builder);
		}
		
		for ( Object childNode: node.getChildren() ) {
			handleContent(builder, childNode, node );
		}
		
		int lengthAfter = builder.length();
		
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
		
		TagNodeHandler marginHandler = new TagNodeHandler() {
			
			@Override
			public void beforeChildren(TagNode node,
					SpannableStringBuilder builder) {
				
				if (builder.length() > 0 && builder.charAt(builder.length() -1) != '\n' ) {
					appendNewLine(builder);					
				}
			}
			
			public void handleTagNode(TagNode node, SpannableStringBuilder builder,
					int start, int end) {				
				
				builder.setSpan(new LeadingMarginSpan.Standard(MARGIN_INDENT), 
						start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				appendNewLine(builder);
				appendNewLine(builder);
			}
		};
		
		registerHandler("blockquote", marginHandler);
		registerHandler("ul", marginHandler);
		registerHandler("ol", marginHandler);
		
		
		TagNodeHandler brHandler = new TagNodeHandler() {
			public void handleTagNode(TagNode node, SpannableStringBuilder builder,
					int start, int end) {
				appendNewLine(builder);
			}
		};
		
		registerHandler("br", brHandler);
	
		TagNodeHandler pHandler = new TagNodeHandler() {
			public void handleTagNode(TagNode node, SpannableStringBuilder builder,
					int start, int end) {
				appendNewLine(builder);
				appendNewLine(builder);
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
		
		//PRE is a special case: here we apply the style, but it's also handled special in
		//contentNode processing.		
		TagNodeHandler preHandler = new TagNodeHandler() {
			public void handleTagNode(TagNode node, SpannableStringBuilder builder,
					int start, int end) {
				
				builder.setSpan( new TypefaceSpan("monospace"), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				appendNewLine(builder);
				appendNewLine(builder);
			}
		};
		
		registerHandler("pre", preHandler);	
		
		
		
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
		
		registerHandler("li", new ListItemHandler() );		
	}
	
	private class HeaderHandler extends TagNodeHandler {
		
		private float size;
		
		public HeaderHandler(float size) {
			this.size = size;
		}		
		
		@Override
		public void beforeChildren(TagNode node, SpannableStringBuilder builder) {
			if (builder.length() > 0 && builder.charAt(builder.length() -1) != '\n' ) {
				builder.append("\n");					
			}
		}
		
		@Override
		public void handleTagNode(TagNode node, SpannableStringBuilder builder,
				int start, int end) {
			
			builder.setSpan(new RelativeSizeSpan(size), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			builder.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			
			appendNewLine(builder);
			appendNewLine(builder);
		}
	}	
	
	private class ListItemHandler extends TagNodeHandler {
		
		private int getMyIndex(TagNode node) {
			
			if ( node.getParent() == null ) {
				return -1;
			}
			
			int i = 1;
			
			for ( Object child: node.getParent().getChildren() ) {
				if ( child == node ) {
					return i;
				} 				
				
				if (child instanceof TagNode ) {
					TagNode childNode = (TagNode) child;				
					if ( "li".equals(childNode.getName())) {
						i++;
					}
				}
			}
			
			return -1;
		}
		
		private String getParentName(TagNode node) {
			if ( node.getParent() == null ) {
				return null;
			}
			
			return node.getParent().getName();
		}
		
		@Override
		public void beforeChildren(TagNode node, SpannableStringBuilder builder) {
			if ( "ol".equals(getParentName(node))) {
				builder.append( "" + getMyIndex(node) + ". " );
			} else if ("ul".equals(getParentName(node))) {
				//Unicode bullet character.
				builder.append("\u2022  ");
			}
		}
		
		@Override
		public void handleTagNode(TagNode node, SpannableStringBuilder builder,
				int start, int end) {
			
			if (builder.length() > 0 && builder.charAt(builder.length() -1) != '\n' ) {
				builder.append("\n");
			}
			
		}
	}
}
