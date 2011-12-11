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

import org.htmlcleaner.TagNode;

import android.text.SpannableStringBuilder;

/**
 * A TagNodeHandler handles a specific type of tag (a, img, p, etc),
 * and adds the correct spans to a SpannableStringBuilder.
 * 
 * For example: the TagNodeHandler for i (italic) tags
 * would do
 * 
 * <tt>
 * public void handleTagNode( TagNode node, SpannableStringBuilder builder, 
 * 		int start, int end ) {
 * 		builder.setSpan(new StyleSpan(Typeface.ITALIC), 
 * 			start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
 * }
 * </tt>
 * 
 * @author Alex Kuiper
 *
 */
public abstract class TagNodeHandler {
	
	/**
	 * Called before the children of this node are handled, allowing for
	 * text to be inserted before the childrens' text.
	 * 
	 * Default implementation is a no-op.
	 * 
	 * @param node
	 * @param builder
	 */
	public void beforeChildren( TagNode node, SpannableStringBuilder builder ) {
		
	}

	/**
	 * Handle the given node and add spans if needed.
	 * 
	 * @param node the node to handle
	 * @param builder the current stringbuilder
	 * @param start start position of inner text of this node
	 * @param end end position of inner text of this node.
	 */
	public abstract void handleTagNode( TagNode node, SpannableStringBuilder builder, int start, int end );
}
