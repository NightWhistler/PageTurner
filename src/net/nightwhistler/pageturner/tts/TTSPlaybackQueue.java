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

package net.nightwhistler.pageturner.tts;

import android.media.MediaPlayer;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import java.io.File;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/*
Playback queue which is thread-safe, so it can be a singleton.

It only accepts items when it has first been activated.
 */
public class TTSPlaybackQueue {

    private boolean active;
    private Queue<TTSPlaybackItem> playbackItemQueue = new ConcurrentLinkedQueue<TTSPlaybackItem>();

    public synchronized boolean isActive() {
        return active;
    }

    public synchronized  void activate() {
        playbackItemQueue.clear();
        this.active = true;
    }

    public synchronized void deactivate() {

        for ( TTSPlaybackItem item: this.playbackItemQueue ) {
            MediaPlayer mediaPlayer = item.getMediaPlayer();
            mediaPlayer.stop();
            mediaPlayer.release();
            new File(item.getFileName()).delete();
        }

        this.active = false;
    }

    public void updateSpeechCompletedCallbacks(SpeechCompletedCallback callback) {
        for ( TTSPlaybackItem item: this.playbackItemQueue ) {
            item.setOnSpeechCompletedCallback(callback);
        }
    }

    public synchronized  TTSPlaybackItem peek() {
        return playbackItemQueue.peek();
    }

    public synchronized  void add( TTSPlaybackItem item ) {
        if ( active ) {
            this.playbackItemQueue.add(item);
        }
    }

    public synchronized  int size() {
        return playbackItemQueue.size();
    }

    public synchronized boolean isEmpty() {
        return this.playbackItemQueue.isEmpty();
    }

    public synchronized TTSPlaybackItem remove() {
        return this.playbackItemQueue.remove();
    }

}
