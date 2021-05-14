package com.uc.health.safesteth.view;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mmm.healthcare.scope.ConfigurationFactory;
import com.mmm.healthcare.scope.IBluetoothManager;
import com.mmm.healthcare.scope.Stethoscope;
import com.uc.health.safesteth.PairingAdapter;
import com.uc.health.safesteth.R;
import com.uc.health.safesteth.model.DataHolder;

import java.io.IOException;
import java.util.ArrayList;

import static com.uc.health.safesteth.Constants.PAIR_DEVICES_SUCCESSFUL;
import static com.uc.health.safesteth.Constants.REQUEST_DISCOVERABLE_BT;
import static com.uc.health.safesteth.Constants.REQUEST_ENABLE_BT;

public class PairingActivity extends AppCompatActivity {

    /**
     * Broadcast Receiver to detect changes on bluetooth's state
     */
    private final BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                if (state == BluetoothAdapter.STATE_TURNING_OFF)
                    startBluetoothIntent();
            }
        }
    };

    /**
     * Broadcast Receiver to detect changes on bluetooth discoverability's state
     */
    private final BroadcastReceiver mStateReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)) {

                int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);

                switch (mode) {
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                    case BluetoothAdapter.SCAN_MODE_NONE:
                        if (BluetoothAdapter.getDefaultAdapter().isEnabled())
                            startBluetoothDiscoverabilityIntent();
                        break;
                }
            }
        }
    };
    private final ArrayList<Stethoscope> availableDevices = new ArrayList<>();
    // View items
    private ImageView ibClose;
    private LinearLayout llCancel;
    // Recycler View Elements
    private RecyclerView rvAvailableDevices;
    private PairingAdapter rvAvailableDevicesAdapter;
    private RecyclerView.LayoutManager rvAvailableDeviceslayoutManager;
    // Logic items
    private ConnectTask connectTask;
    private IBluetoothManager bluetoothManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pairing);

        // References to View Elements
        rvAvailableDevices = findViewById(R.id.rvAvailableDevices);
        ibClose = findViewById(R.id.ibClose);
        llCancel = findViewById(R.id.llCancel);

        // Setup the Layout Manager and the Recycler View item's divider
        rvAvailableDeviceslayoutManager = new LinearLayoutManager(this);
        rvAvailableDevices.setLayoutManager(rvAvailableDeviceslayoutManager);
        rvAvailableDevices.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        // Setup the Recycler View's Adapter
        rvAvailableDevicesAdapter = new PairingAdapter(this, availableDevices);
        rvAvailableDevices.setAdapter(rvAvailableDevicesAdapter);

        rvAvailableDevicesAdapter.setOnItemClickListener(new PairingAdapter.onItemClickListener() {
            @Override
            public void onItemClickListener(int position) {
                // If there's a task trying to connect, cancel them
                if (connectTask != null) {
                    // Cancel the last connection attempt
                    connectTask.cancel(true);
                }

                // Start Async task to connect to the selected stethoscope
                getBluetoothDevices(position);
            }
        });

        llCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // If there's a task trying to connect, cancel them
                if (connectTask != null) {
                    // Get the previous position before move on
                    int previousPosition = rvAvailableDevicesAdapter.getLastItemClicked();
                    // Set -1 to no item being selected
                    rvAvailableDevicesAdapter.setLastItemClicked(-1);
                    // Update the previous item clicked
                    rvAvailableDevicesAdapter.notifyItemChanged(previousPosition);
                    // Cancel the last connection attempt
                    connectTask.cancel(true);
                }

                llCancel.setVisibility(View.GONE);
            }
        });

        ibClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Register the Bluetooth Receiver to handle changes on Bluetooth Configurations
        IntentFilter intentBluetoothFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        IntentFilter intentBluetoothStateFilter = new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        registerReceiver(mBluetoothReceiver, intentBluetoothFilter);
        registerReceiver(mStateReceiver, intentBluetoothStateFilter);

        if (DataHolder.getInstance().getmStethoscope() != null)
            DataHolder.getInstance().setmStethoscope(null);

        bluetoothManager = ConfigurationFactory.getBluetoothManager();

        // Setup recycler view with paired stethoscopes
        availableDevices.addAll(bluetoothManager.getPairedDevices());
        rvAvailableDevicesAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        // Unregister Receivers
        unregisterReceiver(mBluetoothReceiver);
        unregisterReceiver(mStateReceiver);

        // Cancel connections attempts if any
        if (connectTask != null)
            connectTask.cancel(true);

        connectTask = null;

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

        }
    }

    public void getBluetoothDevices(int position) {
        // If there's a task trying to connect, cancel them
        if (connectTask != null)
            connectTask.cancel(true);

        // Pull request for new connection
        if (bluetoothManager == null)
            bluetoothManager = ConfigurationFactory.getBluetoothManager();

        // Start Async task to connect to the selected stethoscope
        connectTask = new ConnectTask();
        connectTask.execute(position);
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
     * Method to build an Alert Dialog to warn the user that an error as occured during pairing
     */
    private void buildAlertPairingError() {
        Toast.makeText(getApplicationContext(),
                R.string.alert_pairing_error_content,
                Toast.LENGTH_SHORT)
                .show();
    }

    /**
     * Connects to the stethoscope the user selected. Also generates a report,
     * adds event listeners, and sets the stethoscope's display.
     */
    private class ConnectTask extends AsyncTask<Integer, Void, Integer> {
        @Override
        protected Integer doInBackground(Integer... params) {
            if (isCancelled())
                return null;

            if (params.length == 0)
                return null;

            final int position = params[0];
            Stethoscope selectedStethoscope = availableDevices.get(position);

            if (selectedStethoscope == null)
                return position;

            try {
                if (selectedStethoscope.isConnected()) {
                    selectedStethoscope.disconnect();
                    selectedStethoscope.stopAudioInputAndOutput();
                    return position;
                }

                // Connect to stethoscope
                try {
                    // Connect to stethoscope
                    selectedStethoscope.connect();
                } catch (IOException e) {
                    return position;
                }

                // Save on the Singleton of the application
                DataHolder.getInstance().setmStethoscope(selectedStethoscope);

                // Finish the pairing activity
                Intent intent = getIntent();
                setResult(PAIR_DEVICES_SUCCESSFUL, intent);
                finish();
            } catch (Exception e) {
                // If the connection fails, reset the adapter and warn the user
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        rvAvailableDevicesAdapter.setLastItemClicked(-1);
                        rvAvailableDevicesAdapter.notifyItemChanged(position);
                        buildAlertPairingError();
                    }
                });
            }

            return position;
        }

        @Override
        protected void onPostExecute(Integer result) {
            if (result != null && DataHolder.getInstance().getmStethoscope() == null)
                getBluetoothDevices(result);
        }

        @Override
        protected void onPreExecute() {
            llCancel.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }
}