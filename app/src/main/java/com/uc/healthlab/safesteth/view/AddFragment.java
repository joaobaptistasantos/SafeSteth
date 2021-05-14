package com.uc.healthlab.safesteth.view;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.mmm.healthcare.scope.ConfigurationFactory;
import com.mmm.healthcare.scope.IBluetoothManager;
import com.mmm.healthcare.scope.Stethoscope;
import com.mmm.healthcare.scope.StethoscopeException;
import com.uc.healthlab.safesteth.R;
import com.uc.healthlab.safesteth.model.DataHolder;
import com.uc.healthlab.safesteth.model.PermissionsManager;

import java.io.IOException;
import java.util.Vector;

import static com.uc.healthlab.safesteth.Constants.CONNECT_TASK_CANCELED;
import static com.uc.healthlab.safesteth.Constants.CONNECT_TASK_ERROR;
import static com.uc.healthlab.safesteth.Constants.CONNECT_TASK_NO_DEVICES;
import static com.uc.healthlab.safesteth.Constants.CONNECT_TASK_SUCCESSFUL;
import static com.uc.healthlab.safesteth.Constants.PAIR_DEVICES;

/**
 * @author João R. B. Santos
 * @since 1.0
 */
public class AddFragment extends Fragment {
    // View elements
    private LinearLayout llAddStethoscope;
    // Logic elements
    private ConnectTask connectTask;
    private IBluetoothManager bluetoothManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_add, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        llAddStethoscope = view.findViewById(R.id.ll_add_stethoscope);
        llAddStethoscope.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // If bluetooth is not enabled
                if (!PermissionsManager.isBluetoothEnabled())
                    return;

                if (connectTask != null) {
                    connectTask.cancel(true);
                    connectTask = null;
                }

                // Go to Pairing menu
                Intent intent = new Intent(getContext(), PairingActivity.class);
                getActivity().startActivityForResult(intent, PAIR_DEVICES);
            }
        });

        if (DataHolder.getInstance().getmStethoscope() != null)
            DataHolder.getInstance().setmStethoscope(null);

        getBluetoothDevices();
    }

    @Override
    public void onResume() {
        super.onResume();
        getBluetoothDevices();
    }

    @Override
    public void onPause() {
        if (connectTask != null && !connectTask.isCancelled())
            connectTask.cancel(true);

        connectTask = null;

        super.onPause();
    }

    public void getBluetoothDevices() {
        // If there's a task trying to connect, cancel them
        if (connectTask != null)
            connectTask.cancel(true);

        // Pull request for new connection
        if (bluetoothManager == null)
            bluetoothManager = ConfigurationFactory.getBluetoothManager();

        // Start Async task to connect to the selected stethoscope
        connectTask = new ConnectTask();
        connectTask.execute(bluetoothManager.getPairedDevices());

    }

    /**
     * AsyncTask to do pull pairing in background. The device should be on an infinite loop trying
     * to connect with possible stethoscopes detected by the Bluetooth Manager. This will keep the
     * doctor away from interacting with the interface and the connection could be established
     * without more steps.
     * <p>
     * Note: Sometimes this connection is not established at the first time due to broken pipes
     * detected on tests. This should happen towards abrupt finishes.
     * <p>
     * Return:
     * CONNECT_TASK_CANCELED when the AsyncTask has been canceled but already began
     * CONNECT_TASK_NO_DEVICES when empty parameters arrive
     * CONNECT_TASK_ERROR when the connection was not made successfully
     * CONNECT_TASK_SUCCESSFUL when the connection as been successfully established
     */
    private class ConnectTask extends AsyncTask<Vector<Stethoscope>, Void, Integer> {
        @Override
        protected Integer doInBackground(Vector<Stethoscope>... params) {
            /* AsyncTask has received no parameters, return CONNECT_TASK_NO_DEVICES */
            if (params.length == 0)
                return CONNECT_TASK_NO_DEVICES;

            /* Iterate over all stethoscopes available on Bluetooth Manager */
            for (Stethoscope stethoscope : params[0]) {

                /* AsyncTask has been canceled, return CONNECT_TASK_CANCELED */
                if (isCancelled())
                    return CONNECT_TASK_CANCELED;

                /* Stethoscope is already connected, disconnect him in order to connect him with
                 * this device  */
                if (stethoscope.isConnected())
                    stethoscope.disconnect();

                try {
                    /* Connect with the stethoscope */
                    stethoscope.connect();
                } catch (IOException | StethoscopeException e) {
                    /* Continue in order to try pair with other stethoscope */
                    continue;
                }

                /* Save the stethoscope's instance on Data Holder */
                DataHolder.getInstance().setmStethoscope(stethoscope);

                /* Connection was successfully made, return CONNECT_TASK_SUCCESSFUL code */
                return CONNECT_TASK_SUCCESSFUL;
            }

            /* Connected task was not completed successfully, return CONNECT_TASK_ERROR */
            return CONNECT_TASK_ERROR;
        }

        @Override
        protected void onPostExecute(Integer result) {
            /* If result is null, try to reconnect */
            if (result == null) {
                getBluetoothDevices();
            }
            /* If the Async Task was successfully ended, go to Connection Fragment */
            else if (result == CONNECT_TASK_SUCCESSFUL) {
                try {
                    // Move to Connected Fragment
                    NavController navController = NavHostFragment.findNavController(AddFragment.this);
                    navController.navigate(R.id.action_addFragment_to_connectedFragment);
                } catch (NullPointerException ex) {
                    /* If there is some problems during transition, try to connect again */
                    getBluetoothDevices();
                }
            }
            /* Otherwise, and if the Async Task was not canceled, keep trying to connect with
             * previously paired or discovered stethoscopes */
            else if (result != CONNECT_TASK_CANCELED)
                getBluetoothDevices();
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