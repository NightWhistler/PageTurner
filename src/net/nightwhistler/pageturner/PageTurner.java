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

import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;

import android.app.Application;

@ReportsCrashes(formKey="dC1sc0tQUVpzdlRRMDNhMFJtbW1UaWc6MQ")
public class PageTurner extends Application {
	
	@Override
	public void onCreate() {
		ACRA.init(this);
		if(Configuration.IS_EINK_DEVICE) { // e-ink looks better with dark-on-light (esp. Nook Touch where theming breaks light-on-dark
			setTheme(R.style.Theme_Sherlock_Light);
		}
		super.onCreate();
	}
	
}
