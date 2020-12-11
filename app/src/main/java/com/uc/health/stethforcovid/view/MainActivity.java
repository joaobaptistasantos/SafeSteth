package com.uc.health.stethforcovid.view;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.provider.Settings;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.mmm.healthcare.scope.ConfigurationFactory;
import com.uc.health.stethforcovid.R;
import com.uc.health.stethforcovid.IOnBackPressed;
import com.uc.health.stethforcovid.model.PermissionsManager;

import static com.uc.health.stethforcovid.Constants.PAIR_DEVICES;
import static com.uc.health.stethforcovid.Constants.PAIR_DEVICES_SUCCESSFUL;
import static com.uc.health.stethforcovid.Constants.REQUEST_ACCESS_COARSE_LOCATION;
import static com.uc.health.stethforcovid.Constants.REQUEST_DISCOVERABLE_BT;
import static com.uc.health.stethforcovid.Constants.REQUEST_ENABLE_BT;

public class MainActivity extends AppCompatActivity {

    /**
     * BroadcastReceiver to detect changes on bluetooth connection
     */
    private final BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                switch (state) {
                    // Bluetooth become from enabled to disabled
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        startBluetoothIntent();
                        break;
                    // Bluetooth are on, validate if locations services are enabled
                    case BluetoothAdapter.STATE_TURNING_ON:
                        // Check if Locations Service are enabled
                        if (!PermissionsManager.isLocationsServicesEnabled(MainActivity.this))
                            buildAlertNoGps();
                        break;
                }

            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Register the activity to allow license check.
        ConfigurationFactory.setContext(this);

        // Check for validations
        switch (PermissionsManager.validateBluetooth(this)) {
            case -1:
                buildAlertBluetoothNotSupported();
                break;
            case 1:
                startBluetoothIntent();
                break;
            case 2:
                buildAlertNoGps();
                break;
        }

        // Register the Bluetooth Receiver to handle changes on Bluetooth Configurations
        IntentFilter intentBluetoothFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mBluetoothReceiver, intentBluetoothFilter);

        // Create a new fragment to be presented
        Fragment fragment = new AddFragment();

        // Create transition to this fragment and add him to the stack so we can come back
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fl_main, fragment)
                .commit();
    }

    @Override
    public void onBackPressed() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fl_main);

        if (!(fragment instanceof IOnBackPressed) || !((IOnBackPressed) fragment).onBackPressed()) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        // Unregister the bluetooth broadcast receiver
        unregisterReceiver(mBluetoothReceiver);

        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            startBluetoothIntent();
        } else if (requestCode == REQUEST_DISCOVERABLE_BT
                && BluetoothAdapter.getDefaultAdapter().getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            startBluetoothDiscoverabilityIntent();
        } else if (requestCode == REQUEST_ACCESS_COARSE_LOCATION && resultCode == Activity.RESULT_CANCELED) {
            startLocationIntent();
        } else if (requestCode == PAIR_DEVICES && resultCode == PAIR_DEVICES_SUCCESSFUL) {
            // Setup the new fragment
            Fragment fragment = new ConnectedFragment();

            // Create transition to this fragment and add him to the stack so we can come back
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fl_main, fragment)
                    .addToBackStack("ConnectedFragment")
                    .commit();
        }
    }

    /**
     * Method to build an Alert Dialog to warn the user that he has the location services disabled
     */
    private void buildAlertNoGps() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.alert_gps_not_detected_title)
                .setMessage(R.string.alert_gps_not_detected_content)
                .setCancelable(false)
                .setPositiveButton(R.string.alert_gps_not_detected_ok, new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .show();
    }

    /**
     * Method to build an Alert Dialog to warn the user that he has the bluetooth disabled
     */
    private void buildAlertBluetoothNotSupported() {
        new AlertDialog.Builder(getApplicationContext())
                .setTitle(R.string.alert_bluetooth_not_supported_title)
                .setCancelable(false)
                .setMessage(R.string.alert_bluetooth_not_supported_content)
                .setPositiveButton(R.string.alert_bluetooth_not_supported_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .show();

    }

    /**
     * Method to start Bluetooth Discoverability Intent in order to make user grant this permission
     * so the device can be establish a bluetooth connection with the stethoscope
     */
    private void startBluetoothIntent() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }

    /**
     * Method to start Bluetooth Discoverability Intent in order to make user grant this permission
     * so the device can be discovered by the stethoscope for pairing
     */
    private void startBluetoothDiscoverabilityIntent() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        startActivityForResult(enableBtIntent, REQUEST_DISCOVERABLE_BT);
    }

    /**
     * Method to start Location Intent in order to make user grant this permission so the device
     * can locate the stethoscope (needed with newer versions of Android)
     */
    private void startLocationIntent() {
        Intent enableBtIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivityForResult(enableBtIntent, REQUEST_ACCESS_COARSE_LOCATION);
    }
}