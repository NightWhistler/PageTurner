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
