/*
 * Copyright (C) 2012 Alex Kuiper
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

package net.nightwhistler.pageturner;

import net.nightwhistler.pageturner.library.LibraryService;
import net.nightwhistler.pageturner.library.SqlLiteLibraryService;
import net.nightwhistler.pageturner.sync.PageTurnerWebProgressService;
import net.nightwhistler.pageturner.sync.ProgressService;
import roboguice.config.AbstractAndroidModule;

public class PageTurnerModule extends AbstractAndroidModule {

	@Override
	protected void configure() {
		
		bind( LibraryService.class ).to( SqlLiteLibraryService.class );
		bind( ProgressService.class ).to( PageTurnerWebProgressService.class );
		
	}
}
