/**
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
 * 
 * @author Joel Pedraza (saik0)
 */

package net.nightwhistler.pageturner.utils;

import com.github.rtyley.android.sherlock.roboguice.fragment.RoboSherlockDialogFragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public final class DialogFragmentUtils {
	private DialogFragmentUtils() {
		// This class cannot be instantiated
	}

	/**
	 * Create a new instance of a DialogFragment to show a {@link android.app.Dialog}
	 * 
	 * @param dialog The dialog to show in the DialogFragment
	 * @return a DialogFragment
	 */
	public static DialogFragment fromDialog(final Dialog dialog) {
		return new RoboSherlockDialogFragment() {
			@Override
			public Dialog onCreateDialog(Bundle savedInstanceState) {
				return dialog;
			}
		};
	}

	/**
	 * Create a new instance of a DialogFragment from an {@link android.app.AlertDialog.Builder}
	 * 
	 * @param builder The builder to use when creating the Dialog
	 * @return a DialogFragment
	 */
	public static DialogFragment fromBuilder(final AlertDialog.Builder builder) {
		return fromDialog(builder.create());
	}
}
