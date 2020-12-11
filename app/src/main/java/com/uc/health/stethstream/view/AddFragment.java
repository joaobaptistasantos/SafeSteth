package com.uc.health.stethstream.view;

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

import com.mmm.healthcare.scope.ConfigurationFactory;
import com.mmm.healthcare.scope.IBluetoothManager;
import com.mmm.healthcare.scope.Stethoscope;
import com.uc.health.stethstream.R;
import com.uc.health.stethstream.model.DataHolder;
import com.uc.health.stethstream.model.PermissionsManager;

import java.io.IOException;
import java.util.Vector;

import static com.uc.health.stethstream.Constants.PAIR_DEVICES;

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
     * Connects to the stethoscope the user selected. Also generates a report,
     * adds event listeners, and sets the stethoscope's display.
     */
    private class ConnectTask extends AsyncTask<Vector<Stethoscope>, Void, Void> {
        @Override
        protected Void doInBackground(Vector<Stethoscope>... params) {
            if (params.length == 0)
                return null;

            for (Stethoscope stethoscope : params[0]) {

                if (isCancelled())
                    break;

                if (stethoscope.isConnected()) {
                    stethoscope.disconnect();
                    stethoscope.stopAudioInputAndOutput();
                    break;
                }

                try {
                    // Connect to stethoscope
                    stethoscope.connect();
                } catch (IOException e) {
                    return null;
                }

                // Save on the Singleton of the application
                DataHolder.getInstance().setmStethoscope(stethoscope);

                // Finish the pairing activity
                // Setup the new fragment
                // Create transition to this fragment and add him to the stack so we can come back
                try {
                    getActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fl_main, new ConnectedFragment())
                            .addToBackStack("ConnectedFragment")
                            .commit();
                } catch (NullPointerException ex) {
                    return null;
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (DataHolder.getInstance().getmStethoscope() == null)
                getBluetoothDevices();
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }
}