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

/**
 * Fragment of text to be played back.
 */
public class TTSPlaybackItem {

    private CharSequence text;
    private MediaPlayer mediaPlayer;
    private int totalTextLength;
    private int offset;

    private boolean lastElementOfPage;

    private String fileName;

    public TTSPlaybackItem(CharSequence text, MediaPlayer mediaPlayer,
                           int totalTextLength, int offset, boolean lastElementOfPage, String fileName) {
        this.text = text;
        this.mediaPlayer = mediaPlayer;
        this.totalTextLength = totalTextLength;
        this.lastElementOfPage = lastElementOfPage;
        this.fileName = fileName;
        this.offset = offset;
    }

    public void setOnSpeechCompletedCallback( final SpeechCompletedCallback callback ) {
        this.mediaPlayer.setOnCompletionListener(
                mediaPlayer -> callback.speechCompleted(TTSPlaybackItem.this, mediaPlayer)
        );
    }

    public int getOffset() {
        return offset;
    }

    public CharSequence getText() {
        return text;
    }

    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    public int getTotalTextLength() {
        return totalTextLength;
    }

    public boolean isLastElementOfPage() {
        return lastElementOfPage;
    }

    public String getFileName() {
        return  fileName;
    }

}
