package net.nightwhistler.pageturner.activity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;

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

			Intent myIntent = new Intent(INTENT_PAGETURNER_MEDIA);
			myIntent.putExtra("action", event.getAction());
			myIntent.putExtra("keyCode", event.getKeyCode());

			context.sendBroadcast(myIntent);
		}

		if (isOrderedBroadcast()) {
			abortBroadcast();
		}
	}
}
