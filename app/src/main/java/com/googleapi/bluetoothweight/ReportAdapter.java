package com.googleapi.bluetoothweight;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ViewHolder> {

    private List<WeighmentEntry> entryList;

    public ReportAdapter(List<WeighmentEntry> entryList) {
        this.entryList = entryList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_report, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WeighmentEntry entry = entryList.get(position);

        holder.txtSerial.setText("Slip #: " + entry.getSerialNo());
        holder.txtVehicleNo.setText("Vehicle: " + entry.getVehicleNo());
        holder.txtVehicleType.setText("Type: " + entry.getVehicleType());
        holder.txtMaterial.setText("Material: " + entry.getMaterial());
        holder.txtParty.setText("Party: " + entry.getParty());
        holder.txtNetWeight.setText("Net: " + entry.getNet() + " kg");
        holder.txtDate.setText(entry.getTimestamp().substring(0, 10));
    }

    @Override
    public int getItemCount() {
        return entryList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtSerial, txtVehicleNo, txtVehicleType, txtMaterial, txtParty, txtNetWeight, txtDate;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtSerial = itemView.findViewById(R.id.txtSerial);
            txtVehicleNo = itemView.findViewById(R.id.txtVehicleNo);
            txtVehicleType = itemView.findViewById(R.id.txtVehicleType);
            txtMaterial = itemView.findViewById(R.id.txtMaterial);
            txtParty = itemView.findViewById(R.id.txtParty);
            txtNetWeight = itemView.findViewById(R.id.txtNetWeight);
            txtDate = itemView.findViewById(R.id.txtDate);
        }
    }
}