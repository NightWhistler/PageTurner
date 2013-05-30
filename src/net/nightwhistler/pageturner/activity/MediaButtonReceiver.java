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
package net.nightwhistler.pageturner.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class which converts media-button events into PageTurner-specific events.
 * 
 * Since a BroadCastReceiver for MediaButton events has to be specified
 * by class-name, the created instance will have no access to the activity.
 * 
 * To work around this problem, this class will re-broadcast any media
 * events as pageturner.media.key events, which can then be picked up
 * by an internal class inside an Activity or Fragment.
 * 
 * @author Alex Kuiper
 *
 */
public class MediaButtonReceiver extends BroadcastReceiver {

	private static final Logger LOG = LoggerFactory
			.getLogger("MediaButtonReveiver");
	
	public static final String INTENT_PAGETURNER_MEDIA = "pageturner.media.key";

	@Override
	public void onReceive(Context context, Intent intent) {

		String intentAction = intent.getAction();
		if (Intent.ACTION_MEDIA_BUTTON.equals(intentAction)) {

			LOG.info("Received media button, re-broadcasting as PageTurnerMediaKey");

			KeyEvent event = (KeyEvent) intent
					.getParcelableExtra(Intent.EXTRA_KEY_EVENT);

            if ( event != null ) {
			    Intent myIntent = new Intent(INTENT_PAGETURNER_MEDIA);
			    myIntent.putExtra("action", event.getAction());
			    myIntent.putExtra("keyCode", event.getKeyCode());

			    context.sendBroadcast(myIntent);
            }
		}

		if (isOrderedBroadcast()) {
			abortBroadcast();
		}
	}
}
