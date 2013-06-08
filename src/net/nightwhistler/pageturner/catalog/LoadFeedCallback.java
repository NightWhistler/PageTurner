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

package net.nightwhistler.pageturner.catalog;

import android.graphics.drawable.Drawable;
import net.nightwhistler.nucular.atom.Feed;
import net.nightwhistler.nucular.atom.Link;

public interface LoadFeedCallback {

    public static enum ResultType { REPLACE, APPEND }
	
	void setNewFeed( Feed feed, ResultType resultType );

	void errorLoadingFeed( String error );

    void emptyFeedLoaded(Feed feed);
		
	void notifyLinkUpdated(Link link, Drawable drawable);

    void onLoadingStart();

    void onLoadingDone();
}
