package net.nightwhistler.pageturner;

import java.util.List;

import roboguice.application.RoboApplication;

import com.google.inject.Module;

public class PageTurner extends RoboApplication {

	protected void addApplicationModules(List<Module> modules) {
		modules.add(new PageTurnerModule());		
    }
	
}
