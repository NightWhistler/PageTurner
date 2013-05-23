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

package net.nightwhistler.pageturner.prefs;

import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.widget.Toast;
import net.nightwhistler.pageturner.R;

public class LanguageSwitchPreference extends ListPreference {
	
	private Context context;	

	private String oldValue;

	public LanguageSwitchPreference(Context context, AttributeSet attributes) {
		super(context, attributes);
		this.context = context;				
	}
	
	@Override
	public void setValue(String value) {
		super.setValue(value);
		
		if (oldValue != null && !value.equalsIgnoreCase(oldValue) ) {
			Toast.makeText(context, R.string.language_switch_message, Toast.LENGTH_LONG).show();			
		}
		
		this.oldValue = value;
	}
	
}
