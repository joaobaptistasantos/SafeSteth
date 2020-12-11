package com.uc.health.stethstream;

public final class Constants {

    /**
     * Debug Tag
     */
    public static final String TAG = "DEBUG_LITTMANN";

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
}
