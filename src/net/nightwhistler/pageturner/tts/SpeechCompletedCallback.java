package net.nightwhistler.pageturner.tts;

import android.media.MediaPlayer;

/**
 * Created with IntelliJ IDEA.
 * User: alex
 * Date: 4/12/13
 * Time: 10:19 AM
 * To change this template use File | Settings | File Templates.
 */
public interface SpeechCompletedCallback {

    void speechCompleted( TTSPlaybackItem item, MediaPlayer mediaPlayer );

}
