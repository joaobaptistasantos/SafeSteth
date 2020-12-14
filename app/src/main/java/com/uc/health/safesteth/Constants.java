package com.uc.health.safesteth;

/**
 * @author João R. B. Santos
 * @since 1.0
 */
public final class Constants {

    /**
     * Request code to request enable bluetooth connections.
     */
    public static final int REQUEST_ENABLE_BT = 1;
    public static final int REQUEST_DISCOVERABLE_BT = 2;
    public static final int REQUEST_ACCESS_COARSE_LOCATION = 3;
    public static final int PAIR_DEVICES = 4;
    public static final int PAIR_DEVICES_SUCCESSFUL = 5;

    public static final String MESSAGES_KEY = "message";
    public static final int STATE_ERROR = -1;  // error on connection

    /* Return code to when the Connected Async Task is canceled and is still running */
    public static final int CONNECT_TASK_CANCELED = -2;
    /* Return code to when the Connected Async Task detects some error */
    public static final int CONNECT_TASK_ERROR = -1;
    /* Return code to when the Connected Async Task receives no devices as parameter */
    public static final int CONNECT_TASK_NO_DEVICES = 0;
    /* Return code to when the Connected Async Task have made the connection successfully */
    public static final int CONNECT_TASK_SUCCESSFUL = 1;

    /* File Path of LIVE display image */
    public static final String DISPLAY_IMAGE_LIVE = "live.bmp";
    /* File Path of OFF display image */
    public static final String DISPLAY_IMAGE_OFF = "off.bmp";
}
