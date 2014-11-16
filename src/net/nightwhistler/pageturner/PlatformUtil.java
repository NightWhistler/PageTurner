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

import android.annotation.TargetApi;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Looper;
import android.view.LayoutInflater;

import java.util.List;

public class PlatformUtil {

	public static LayoutInflater getLayoutInflater( Context context ) {
		if ( Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB ) {
			return (LayoutInflater) context.getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		} else {
			return (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}
	}

    public static boolean isRunningOnUiThread() { return Looper.getMainLooper().getThread() == Thread.currentThread(); }

    public static boolean isAtLeast( int versionCode ) {
        return Build.VERSION.SDK_INT >= versionCode;
    }

    public static void verifyNotOnUiThread() {
        if ( isRunningOnUiThread() ) {
            throw new IllegalStateException("This method should not be called from the UI Thread!");
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static <A, B, C> void executeTask( AsyncTask<A, B, C> task, A... params ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
        }
        else {
            task.execute(params);
        }
    }

    public static boolean isIntentAvailable(Context context, Intent intent) {

        if ( context == null ) {
            return false;
        }

        final PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> list = packageManager.queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }

    public static void copyTextToClipboard(Context context, String text) {

        if ( android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES. HONEYCOMB ) {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = ClipData.newPlainText("PageTurner copied text", text);
            clipboard.setPrimaryClip(clip);
        }
    }


}
