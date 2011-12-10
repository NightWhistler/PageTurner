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
package net.nightwhistler.pageturner.view;

import android.text.Spanned;

public interface PageChangeStrategy {

	/**
	 * Loads the given section of text.
	 * 
	 * This will be a whole "file" from an epub.
	 * 
	 * @param text
	 */
	public void loadText( Spanned text );
	
	/**
	 * Returns the text-offset of the top-left character on the screen.
	 * 
	 * @return
	 */
	public int getPosition();
	
	/**
	 * Tells this strategy to move the window so the specified
	 * position ends up on the top line of the windows.
	 * 
	 * @param pos
	 */
	public void setPosition( int pos );	
	
	/**
	 * Move the view one page up.
	 */
	public void pageUp();
	
	/**
	 * Move the view one page down.
	 */
	public void pageDown();
	
	/** Simple way to differentiate without instanceof **/
	public boolean isScrolling();
	
	public void clearText();
	
	public void clearStoredPosition();
	
	public void updatePosition();
	
	public void reset();
	
}
