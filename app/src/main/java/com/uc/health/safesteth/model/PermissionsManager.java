package com.uc.health.safesteth.model;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.location.LocationManager;

public class PermissionsManager {

    private static final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    /**
     * Method to execute verifications of permissions
     *
     * @param activity activity that are calling the method
     * @return a value that represents what permission need to be given
     */
    public static int validateBluetooth(Activity activity) {
        // -1 - Device does not support Bluetooth
        // 1 - Bluetooth is disabled
        // 2 - Locations Service are disabled
        // 0 - default
        if (mBluetoothAdapter == null)
            return -1;
        else if (!mBluetoothAdapter.isEnabled())
            return 1;
        else {
            if (!isLocationsServicesEnabled(activity))
                return 2;

            return 0;
        }
    }

    /**
     * Method to verify the state of the bluetooth manager (true - Bluetooth enabled)
     *
     * @return state of the bluetooth manager
     */
    public static boolean isBluetoothEnabled() {
        return mBluetoothAdapter.isEnabled();
    }

    /**
     * Method to verify the state of the locations services (true - Locations enabled)
     *
     * @param activity activity that are calling the method
     * @return state of the locations services
     */
    public static boolean isLocationsServicesEnabled(Activity activity) {
        final LocationManager manager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
        return manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }
}
