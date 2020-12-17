package com.uc.healthlab.safesteth;

import android.media.AudioFormat;

/**
 * @author João R. B. Santos
 * @since 1.0
 */
public final class AudioConstants {

    /* File Path of Stethoscope Connected audio warning */
    public static final String WARNING_STETHOSCOPE_CONNECTED = "stethoscope_connected.mp3";
    /* File Path of Stethoscope Disconnected audio warning */
    public static final String WARNING_STETHOSCOPE_DISCONNECTED = "stethoscope_disconnected.mp3";
    /* File Path of Stethoscope Out of Range audio warning */
    public static final String WARNING_STETHOSCOPE_OUT_OF_RANGE = "out_of_range.mp3";
    /* File Path of Stethoscope Error audio warning */
    public static final String WARNING_STETHOSCOPE_ERROR = "stethoscope_error.mp3";
    /* File Path of Error on Stethoscope Streaming audio warning */
    public static final String WARNING_ERROR_STREAMING = "error_streaming.mp3";
    /* File Path of Stethoscope's Battery Low audio warning */
    public static final String WARNING_BATTERY_LOW = "stethoscope_battery_low.mp3";

    /**
     * Sampling frequency to produce the exact original waveform
     * Note: Usually should be double the original frequency of the signal
     */
    public static final int DEFAULT_SAMPLE_RATE = 4000; // per second

    /**
     * Output channel for the streaming audio content
     */
    public static final int DEFAULT_CHANNELS = AudioFormat.CHANNEL_OUT_MONO; // mono (1-channel) or stereo (2-channels)

    /**
     * Encoding of the streaming audio
     */
    public static final int DEFAULT_AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
}
