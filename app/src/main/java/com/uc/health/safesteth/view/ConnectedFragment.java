package com.uc.health.safesteth.view;

import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.mmm.healthcare.scope.BitmapFactory;
import com.mmm.healthcare.scope.Errors;
import com.mmm.healthcare.scope.IStethoscopeListener;
import com.mmm.healthcare.scope.Stethoscope;
import com.mmm.healthcare.scope.StethoscopeException;
import com.uc.health.safesteth.IOnBackPressed;
import com.uc.health.safesteth.R;
import com.uc.health.safesteth.model.DataHolder;
import com.uc.health.safesteth.model.MediaPlayerWrapper;
import com.uc.health.safesteth.model.StreamingService;

import java.io.IOException;

import static com.uc.health.safesteth.AudioConstants.WARNING_BATTERY_LOW;
import static com.uc.health.safesteth.AudioConstants.WARNING_ERROR_STREAMING;
import static com.uc.health.safesteth.AudioConstants.WARNING_STETHOSCOPE_CONNECTED;
import static com.uc.health.safesteth.AudioConstants.WARNING_STETHOSCOPE_DISCONNECTED;
import static com.uc.health.safesteth.AudioConstants.WARNING_STETHOSCOPE_ERROR;
import static com.uc.health.safesteth.AudioConstants.WARNING_STETHOSCOPE_OUT_OF_RANGE;
import static com.uc.health.safesteth.Constants.DISPLAY_IMAGE_LIVE;
import static com.uc.health.safesteth.Constants.DISPLAY_IMAGE_OFF;
import static com.uc.health.safesteth.Constants.MESSAGES_KEY;
import static com.uc.health.safesteth.Constants.STATE_ERROR;

/**
 * @author João R. B. Santos
 * @since 1.0
 */
public class ConnectedFragment extends Fragment implements IOnBackPressed {

    /**
     * Service to stream stethoscope's data
     */
    private StreamingService streamingService;

    // Logic items
    private ConnectTask connectTask;
    private AssetManager assetManager;
    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            int error = msg.getData().getInt(MESSAGES_KEY);

            if (error == STATE_ERROR) {
                handleDisconnection();
            }
        }
    };
    private boolean isOutRange = false;
    // View Elements
    private LinearLayout llStartStreaming;
    private ImageView ivBatteryLevel;
    private ImageView ivStartStreaming;
    private TextView tvStartStreaming;
    private TextView tvFilterType;
    private TextView tvSoundAmplification;
    private TextView tvDeviceName;
    private ImageView ivOff;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* Get the Assets Manager */
        assetManager = getContext().getAssets();

        /* Add listener interface to the selected stethoscope */
        addStethoscopeListener();

        /* Set OFF's image on display*/
        setDisplayImage(DISPLAY_IMAGE_OFF);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_connected, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Play audio to warning the user
        MediaPlayerWrapper.queueAudioFile(assetManager, WARNING_STETHOSCOPE_CONNECTED);

        // References to view elements
        llStartStreaming = view.findViewById(R.id.ll_start_streaming);
        ivBatteryLevel = view.findViewById(R.id.iv_battery);
        tvFilterType = view.findViewById(R.id.tv_filter_description);
        tvDeviceName = view.findViewById(R.id.tv_pairing_device_name);
        ivStartStreaming = view.findViewById(R.id.iv_start_streaming);
        tvStartStreaming = view.findViewById(R.id.tv_start_streaming);
        tvSoundAmplification = view.findViewById(R.id.tv_amplification_description);
        ivOff = view.findViewById(R.id.iv_turn_off_pairing);

        llStartStreaming.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleStreaming();
            }
        });

        ivOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleDisconnection();
            }
        });

        try {
            Stethoscope initializedStethoscope = DataHolder.getInstance().getmStethoscope();

            tvDeviceName.setText(initializedStethoscope.getName());
            tvSoundAmplification.setText(String.format("%d", initializedStethoscope.getSoundAmplificationLevel() + 1));

            switch (initializedStethoscope.getBatteryLevel()){
                case 3:
                    ivBatteryLevel.setImageDrawable(getActivity().getDrawable(R.drawable.ic_battery_max));
                    break;
                case 2:
                    ivBatteryLevel.setImageDrawable(getActivity().getDrawable(R.drawable.ic_battery_medium));
                    break;
                default:
                    ivBatteryLevel.setImageDrawable(getActivity().getDrawable(R.drawable.ic_battery_low));
                    // Play audio warning to user
                    MediaPlayerWrapper.queueAudioFile(assetManager, WARNING_BATTERY_LOW);
                    break;
            }

            switch (initializedStethoscope.getFilter()){
                case Bell:
                    tvFilterType.setText(getString(R.string.filter_bell));
                    break;
                case Diaphragm:
                    tvFilterType.setText(getString(R.string.filter_diaphragm));
                    break;
                case ExtendedRange:
                    tvFilterType.setText(getString(R.string.filter_extended));
                    break;
            }

        } catch (NullPointerException | StethoscopeException e) {
            handleDisconnection();
        }
    }

    @Override
    public void onDetach() {
        streamingService = null;
        connectTask = null;

        super.onDetach();
    }

    /**
     * Method that set a image on stethoscope's display
     * @param imagePath path of the image to be set
     */
    private void setDisplayImage(String imagePath){
        try {
            /* Set the display image on the stethoscope */
            DataHolder.getInstance().getmStethoscope().setDisplay(BitmapFactory.createBitmap(imagePath));
        } catch (IllegalArgumentException | IOException e) {
            /* Make a Toast appears to warn the user to an error occured setting the display image */
            Toast.makeText(getContext(),
                    R.string.display_image_error,
                    Toast.LENGTH_SHORT)
                    .show();
        }
    }

    private  void handleStreaming() {
        // Setup the streaming service
        if (streamingService == null)
            streamingService = new StreamingService(DataHolder.getInstance().getmStethoscope(), mHandler);

        /*
        If M button is pressed for the first time, streaming starts
        Otherwise, streaming stops
         */
        if (!streamingService.isStreamingFlag()) {

            try {
                // Start the streaming service
                streamingService.startStreaming();

                ivStartStreaming.setImageDrawable(getActivity().getDrawable(R.drawable.ic_stop_streaming));
                tvStartStreaming.setText(getString(R.string.stop_streaming_label));

                /* Set LIVE's image on display*/
                setDisplayImage(DISPLAY_IMAGE_LIVE);

            } catch (Exception e) {
                MediaPlayerWrapper.queueAudioFile(assetManager, WARNING_ERROR_STREAMING);
            }
        } else {

            ivStartStreaming.setImageDrawable(getActivity().getDrawable(R.drawable.ic_stethoscope_streaming));
            tvStartStreaming.setText(getString(R.string.start_streaming_label));

            /* Set OFF's image on display*/
            setDisplayImage(DISPLAY_IMAGE_OFF);

            // Stop the streaming service
            streamingService.stopStreaming();
        }
    }

    private void handleDisconnection() {
        // Close the streaming service if exists
        if (streamingService != null) {
            streamingService.stopStreaming();
            streamingService = null;
        }

        // Release Connect Task if operating
        if (connectTask != null) {
            connectTask.cancel(true);
            connectTask = null;
        }

        // Release the stethoscope instance if there's one
        if (DataHolder.getInstance().getmStethoscope() != null) {
            DataHolder.getInstance().getmStethoscope().disconnect();
            DataHolder.getInstance().setmStethoscope(null);
        }

        // Play audio warning to user
        MediaPlayerWrapper.queueAudioFile(assetManager, WARNING_STETHOSCOPE_DISCONNECTED);

        getActivity().getSupportFragmentManager().popBackStack();
    }

    /**
     * Adds listeners to the stethoscope.
     */
    private void addStethoscopeListener() {
        DataHolder.getInstance().getmStethoscope().addStethoscopeListener(new IStethoscopeListener() {

            @Override
            public void plusButtonDown(boolean isLongButtonClick) {
                // Update the sound amplification value on UI
                tvSoundAmplification.setText(String.format("%d", DataHolder.getInstance().getmStethoscope().getSoundAmplificationLevel() + 1));
            }

            @Override
            public void minusButtonDown(boolean isLongButtonClick) {
                // Update the sound amplification value on UI
                tvSoundAmplification.setText(String.format("%d", DataHolder.getInstance().getmStethoscope().getSoundAmplificationLevel() + 1));
            }

            @Override
            public void mButtonDown(boolean isLongButtonClick) {
                // Handle streaming state (open or close it)
                handleStreaming();
            }

            @Override
            public void filterButtonDown(boolean isLongButtonClick) {
                switch (DataHolder.getInstance().getmStethoscope().getFilter()){
                    case Bell:
                        tvFilterType.setText(getString(R.string.filter_bell));
                        break;
                    case Diaphragm:
                        tvFilterType.setText(getString(R.string.filter_diaphragm));
                        break;
                    case ExtendedRange:
                        tvFilterType.setText(getString(R.string.filter_extended));
                        break;
                }
            }

            @Override
            public void error(Errors error, String message) {
                // Play audio warning to user
                MediaPlayerWrapper.queueAudioFile(assetManager, WARNING_STETHOSCOPE_ERROR);

                handleDisconnection();
            }

            @Override
            public void lowBatteryLevel() {
                // Play audio warning to user
                MediaPlayerWrapper.queueAudioFile(assetManager, WARNING_BATTERY_LOW);
            }

            @Override
            public void disconnected() {
                if (isOutRange)
                    handleDisconnection();
            }

            @Override
            public void mButtonUp() {
                // Not used.
            }

            @Override
            public void plusButtonUp() {
                // Not used.
            }

            @Override
            public void minusButtonUp() {
                // Not used.
            }

            @Override
            public void filterButtonUp() {
                // Not used.
            }

            @Override
            public void onAndOffButtonDown(boolean isLongButtonClick) {
                if (isLongButtonClick)
                    handleDisconnection();
            }

            @Override
            public void onAndOffButtonUp() {
                // Not used.
            }

            @Override
            public void endOfOutputStream() {
                // Not used.
            }

            @Override
            public void endOfInputStream() {
                // Not used.
            }

            @Override
            public void outOfRange(boolean isOutOfRange) {
                isOutRange = isOutOfRange;
                // true - Stethoscope is out of range
                // false - Stethoscope came back in to range.
                if (isOutOfRange) {

                    // Play audio warning to user
                    MediaPlayerWrapper.queueAudioFile(assetManager, WARNING_STETHOSCOPE_OUT_OF_RANGE);

                    // Stop the streaming service an release there memory
                    if (streamingService != null) {
                        streamingService.stopStreaming();
                        streamingService = null;
                    }

                    // Update UI
                    ivStartStreaming.setImageDrawable(getActivity().getDrawable(R.drawable.ic_stethoscope_streaming));
                    tvStartStreaming.setText(getString(R.string.start_streaming_label));

                    // Pull request for new connection
                    new CountDownTimer(10000, 2000) {
                        public void onTick(long millisUntilDone) {
                            Stethoscope previousStethoscope = DataHolder.getInstance().getmStethoscope();

                            if (!previousStethoscope.isConnected()) {
                                // If there's a task trying to connect, cancel them
                                if (connectTask != null)
                                    connectTask.cancel(true);

                                // Start Async task to connect to the selected stethoscope
                                connectTask = new ConnectTask();
                                connectTask.execute(previousStethoscope);
                            }
                        }

                        public void onFinish() {
                            if (DataHolder.getInstance().getmStethoscope() == null || !DataHolder.getInstance().getmStethoscope().isConnected())
                                handleDisconnection();
                        }
                    }.start();
                } else {
                    if (DataHolder.getInstance().getmStethoscope() == null || !DataHolder.getInstance().getmStethoscope().isConnected())
                        handleDisconnection();
                }

            }

            @Override
            public void underrunOrOverrunError(boolean isUnderrun) {
                // Not used.
            }
        });
    }

    @Override
    public boolean onBackPressed() {
        handleDisconnection();
        return true;
    }

    /**
     * Connects to the stethoscope the user selected. Also generates a report,
     * adds event listeners, and sets the stethoscope's display.
     */
    private class ConnectTask extends AsyncTask<Stethoscope, Void, Void> {
        @Override
        protected Void doInBackground(Stethoscope... params) {
            Stethoscope stethoscope = params[0];

            if (stethoscope == null || isCancelled())
                return null;

            // Connect to stethoscope
            if (!stethoscope.isConnected()) {
                try {
                    // Connect to stethoscope
                    stethoscope.connect();
                } catch (IOException e) {
                    return null;
                }
            }

            // Play audio warning to user
            MediaPlayerWrapper.queueAudioFile(assetManager, WARNING_STETHOSCOPE_CONNECTED);

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }
}