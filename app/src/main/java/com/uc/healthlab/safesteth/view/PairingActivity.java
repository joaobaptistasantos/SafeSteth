package com.uc.healthlab.safesteth.view;

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
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mmm.healthcare.scope.ConfigurationFactory;
import com.mmm.healthcare.scope.IBluetoothManager;
import com.mmm.healthcare.scope.Stethoscope;
import com.mmm.healthcare.scope.StethoscopeException;
import com.uc.healthlab.safesteth.PairingAdapter;
import com.uc.healthlab.safesteth.R;
import com.uc.healthlab.safesteth.model.DataHolder;

import java.io.IOException;
import java.util.ArrayList;

import static com.uc.healthlab.safesteth.Constants.CONNECT_TASK_CANCELED;
import static com.uc.healthlab.safesteth.Constants.CONNECT_TASK_ERROR;
import static com.uc.healthlab.safesteth.Constants.CONNECT_TASK_NO_DEVICES;
import static com.uc.healthlab.safesteth.Constants.CONNECT_TASK_SUCCESSFUL;
import static com.uc.healthlab.safesteth.Constants.PAIR_DEVICES_SUCCESSFUL;
import static com.uc.healthlab.safesteth.Constants.REQUEST_DISCOVERABLE_BT;
import static com.uc.healthlab.safesteth.Constants.REQUEST_ENABLE_BT;

/**
 * @author João R. B. Santos
 * @since 1.0
 */
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
        //tvScan = view.findViewById(R.id.tvScan);

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
                    // Change the previous item clicked
                    rvAvailableDevicesAdapter.notifyItemChanged(rvAvailableDevicesAdapter.getLastItemClicked());
                    // Set this one to the last item clicked
                    rvAvailableDevicesAdapter.setLastItemClicked(position);
                    // Cancel the last connection attempt
                    connectTask.cancel(true);
                }

                // Start Async task to connect to the selected stethoscope
                getBluetoothDevices(position);
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
     * AsyncTask to do pull pairing in background. The device should be on an infinite loop trying
     * to connect with the selected stethoscope. This will keep the doctor away from interacting
     * with the interface and the connection could be established without more steps.
     * <p>
     * Note: Sometimes this connection is not established at the first time due to broken pipes
     * detected on tests. This should happen towards abrupt finishes.
     * <p>
     * Return:
     * CONNECT_TASK_CANCELED when the AsyncTask has been canceled but already began
     * CONNECT_TASK_NO_DEVICES when empty parameters arrive
     * CONNECT_TASK_ERROR when the selected stethoscope is null
     * CONNECT_TASK_SUCCESSFUL when the connection as been successfully established
     * position when it is needed to try a reconnection with the device
     */
    private class ConnectTask extends AsyncTask<Integer, Void, Integer> {
        @Override
        protected Integer doInBackground(Integer... params) {
            /* AsyncTask has been canceled, return CONNECT_TASK_CANCELED */
            if (isCancelled())
                return CONNECT_TASK_CANCELED;

            /* AsyncTask has received no parameters, return CONNECT_TASK_NO_DEVICES */
            if (params.length == 0)
                return CONNECT_TASK_NO_DEVICES;

            /* Get the selected stethoscope */
            final int position = params[0];
            final Stethoscope selectedStethoscope = availableDevices.get(position);

            /* Stethoscope obtained is null, return CONNECT_TASK_ERROR */
            if (selectedStethoscope == null)
                return CONNECT_TASK_ERROR;

            /* Stethoscope is already connected, disconnect him in order to connect him with
             * this device  */
            if (selectedStethoscope.isConnected())
                selectedStethoscope.disconnect();

            try {
                /* Connect with the stethoscope */
                selectedStethoscope.connect();
            } catch (IOException | StethoscopeException e) {
                /* Return the position, if IOException or StethoscopeException occurs, in order to
                 * try to reconnect */
                return position;
            }

            /* Save the stethoscope's instance on Data Holder */
            DataHolder.getInstance().setmStethoscope(selectedStethoscope);

            /* Finish the activity with PAIR_DEVICES_SUCCESSFUL code */
            Intent intent = getIntent();
            setResult(PAIR_DEVICES_SUCCESSFUL, intent);
            finish();

            /* Return CONNECT_TASK_SUCCESSFUL code */
            return CONNECT_TASK_SUCCESSFUL;
        }

        @Override
        protected void onPostExecute(Integer result) {
            /* If the Async Task was not successfully ended or canceled, it will keep trying to
             * connect with the selected device */
            if (result != CONNECT_TASK_SUCCESSFUL && result != CONNECT_TASK_CANCELED)
                getBluetoothDevices(result);
        }

        @Override
        protected void onPreExecute() {
            // Not used.
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            // Not used.
        }
    }
}