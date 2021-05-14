package com.uc.healthlab.safesteth.model;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.MediaPlayer;

import java.io.IOException;

public class MediaPlayerWrapper {
    static MediaPlayer _player;

    public static void play(AssetFileDescriptor afd) throws IOException {
        stop();
        _player = new MediaPlayer();
        _player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
        _player.prepare();
        _player.start();
    }

    public static void stop() {
        if (_player != null) {
            _player.reset();
            _player.release();
            _player = null;
        }
    }

    public static MediaPlayer get_player() {
        return _player;
    }

    public static void queueAudioFile(AssetManager assetManager, String filename) {
        try {
            final AssetFileDescriptor afd = assetManager.openFd(filename);

            if (_player != null && MediaPlayerWrapper.get_player().isPlaying()) {
                MediaPlayerWrapper.get_player().setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        try {
                            MediaPlayerWrapper.play(afd);
                        } catch (IOException ioException) {
                            ioException.printStackTrace();
                        }
                    }
                });
            } else {
                MediaPlayerWrapper.play(afd);

            }
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }
}