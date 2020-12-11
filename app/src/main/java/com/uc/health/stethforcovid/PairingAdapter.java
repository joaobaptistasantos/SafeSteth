package com.uc.health.stethforcovid;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mmm.healthcare.scope.Stethoscope;

import java.util.ArrayList;

public class PairingAdapter extends RecyclerView.Adapter<PairingAdapter.PairingViewHolder> {

    private final Context context;
    private final ArrayList<Stethoscope> devicesList;

    private PairingAdapter.onItemClickListener listener;
    private int lastItemClicked = -1;

    public PairingAdapter(Context context, ArrayList<Stethoscope> devicesList) {
        this.context = context;
        this.devicesList = devicesList;
    }

    public void setOnItemClickListener(PairingAdapter.onItemClickListener listener) {
        this.listener = listener;
    }

    public int getLastItemClicked() {
        return lastItemClicked;
    }

    public void setLastItemClicked(int lastItemClicked) {
        this.lastItemClicked = lastItemClicked;
    }

    @Override
    public PairingAdapter.PairingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bluetooth_device, parent, false);
        return new PairingViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull final PairingViewHolder holder, final int position) {
        final Stethoscope currentItem = devicesList.get(position);

        // Change device name if needed
        holder.tvDeviceName.setText(currentItem.getName() == null ? "Unkown" : currentItem.getName());

        // Change button's text and clickcable flag
        holder.btPairDevice.setText(context.getString(lastItemClicked == position ? R.string.state_pairing : R.string.state_to_pair));

        holder.btPairDevice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // If the item clicked was the previous item clicked, nothing has to be done
                if (lastItemClicked == position)
                    return;

                // Change button's text and clickcable flag
                holder.btPairDevice.setText(context.getString(R.string.state_pairing));
                holder.btPairDevice.setClickable(false);
                // Alert the listener that the item has been clicked
                listener.onItemClickListener(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return devicesList.size();
    }

    public interface onItemClickListener {
        void onItemClickListener(int position);
    }

    public static class PairingViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvDeviceName;
        private final Button btPairDevice;

        public PairingViewHolder(@NonNull View itemView) {
            super(itemView);

            // References to view elements
            tvDeviceName = itemView.findViewById(R.id.tv_pairing_device_name);
            btPairDevice = itemView.findViewById(R.id.bt_pairing_device);
        }
    }
}
