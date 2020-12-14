package com.uc.health.safesteth.model;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.mmm.healthcare.scope.Stethoscope;
import com.uc.health.safesteth.TTSInputStream;

import java.io.IOException;

import static com.uc.health.safesteth.AudioConstants.DEFAULT_AUDIO_FORMAT;
import static com.uc.health.safesteth.AudioConstants.DEFAULT_CHANNELS;
import static com.uc.health.safesteth.AudioConstants.DEFAULT_SAMPLE_RATE;
import static com.uc.health.safesteth.Constants.MESSAGES_KEY;
import static com.uc.health.safesteth.Constants.STATE_ERROR;

/**
 * Service to manage streaming data using the received data from the stethoscope
 *
 * @author João R. B. Santos
 * @since 1.0
 */
public class StreamingService {


    /**
     * Buffer size to the streaming data buffer
     * Bit Rate = bitspersample (16-bit or 24-bit) * samplespersec (44.1KHz-48KHz) * no. of channels
     */
    private final int bufferSize = 128;
    /**
     * Buffer that handles the streaming data from the stethoscope
     */
    private final byte[] buffer = new byte[bufferSize];
    private final Stethoscope mStethoscope;
    private final Handler mHandler;
    /**
     * Flag that controls streaming by pressing M button on stethoscope
     */
    private boolean streamingFlag = false;
    /**
     * AudioTrack to manage the streaming audio
     */
    private AudioTrack audioTrack;
    /**
     * Thread that handles the received data from the stethoscope and writes to the streaming buffer
     */
    private ReadAndStreamThread mReadThread;

    /**
     * Constructor default
     */
    public StreamingService(Stethoscope mStethoscope, Handler mHandler) {
        this.mStethoscope = mStethoscope;
        this.mHandler = mHandler;
    }

    public synchronized void startStreaming() throws Exception {
        // Check if the Audio Track is initialized
        if (audioTrack == null)
            initializeAudioOutputTrack();

        // Check if stethoscope is still connected
        if (!mStethoscope.isConnected())
            throw new Exception();

        // Verify if there is no thread running in background
        if (mReadThread != null)
            mReadThread = null;

        // Set the streaming flag to true
        this.streamingFlag = !this.streamingFlag;

        // Start the AudioTrack streaming
        audioTrack.play();

        // Open the stethoscope input stream
        mStethoscope.startAudioInput();
        mStethoscope.startAudioOutput();

        // Start the thread to connect with the given device
        mReadThread = new ReadAndStreamThread(mStethoscope);
        mReadThread.start();

    } // end startStreaming

    public synchronized void stopStreaming() {
        // Set the streaming flag to false
        this.streamingFlag = !this.streamingFlag;
    } // end stopStreaming

    private void initializeAudioOutputTrack() throws Exception {
        try {
            // Build the Audio Track according to specifications
            this.audioTrack = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build())
                    .setAudioFormat(new AudioFormat.Builder()
                            .setEncoding(DEFAULT_AUDIO_FORMAT)
                            .setSampleRate(DEFAULT_SAMPLE_RATE)
                            .setChannelMask(DEFAULT_CHANNELS)
                            .build())
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build();

            this.audioTrack.setBufferSizeInFrames(bufferSize * 2);
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }
    }

    /**
     * Get the state of the streaming
     * Doesn't need to be synchronized because it doesn't affect the thread's processes
     *
     * @return state of streaming
     */
    public synchronized boolean isStreamingFlag() {
        return streamingFlag;
    } // end isStreamingFlag

    /**
     * Thread that manages the data received from the stethoscope and writes them on the streaming
     * buffer
     */
    private class ReadAndStreamThread extends Thread {

        /**
         * Extension of Data Input Stream to normalise the number of bytes read per second
         * Note: The stethoscope sends an inconstant number of bytes per second
         */
        private final TTSInputStream ttsInputStream;

        public ReadAndStreamThread(Stethoscope mStethoscope) {
            // Get the stethoscope input stream and instantiate a new normalised stream
            this.ttsInputStream = new TTSInputStream(mStethoscope.getAudioInputStream());
        } // end Constructor

        @Override
        public void run() {
            // Define Thread priority to urgent in order to the work become prioritized
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

            try {
                while (streamingFlag) {

                    while (audioTrack != null && ttsInputStream.readFullyUntilEof(buffer) > 0) {
                        audioTrack.write(buffer, 0, buffer.length);
                    } // end while

                } // end while
            } catch (IOException e) {
                stopStreaming();

                Message msg = new Message();
                Bundle bundle = new Bundle();
                bundle.putInt(MESSAGES_KEY, STATE_ERROR);
                msg.setData(bundle);
                mHandler.sendMessage(msg);
            }

            if(mStethoscope != null)
                mStethoscope.stopAudioInputAndOutput();

            // Stop AudioTrack streaming and release the memory used
            if (audioTrack != null) {
                audioTrack.stop();
                audioTrack.release();
                audioTrack = null;
            }

        } // end run

    } // end Read Thread

} // end StreamingService