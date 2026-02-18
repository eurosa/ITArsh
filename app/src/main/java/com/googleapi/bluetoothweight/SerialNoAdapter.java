package com.googleapi.bluetoothweight;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SerialNoAdapter extends ArrayAdapter<String> {

    private List<String> originalList;
    private List<String> filteredList;
    private Filter filter;

    public SerialNoAdapter(@NonNull Context context, @NonNull List<String> objects) {
        super(context, android.R.layout.simple_dropdown_item_1line, objects);
        this.originalList = new ArrayList<>(objects);
        this.filteredList = new ArrayList<>(objects);
    }

    @NonNull
    @Override
    public Filter getFilter() {
        if (filter == null) {
            filter = new SerialNoFilter();
        }
        return filter;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(
                    android.R.layout.simple_dropdown_item_1line, parent, false);
        }

        TextView textView = convertView.findViewById(android.R.id.text1);
        String item = getItem(position);
        textView.setText(item);
        textView.setTextSize(18);

        return convertView;
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(
                    android.R.layout.simple_dropdown_item_1line, parent, false);
        }

        TextView textView = convertView.findViewById(android.R.id.text1);
        String item = getItem(position);
        textView.setText(item);
        textView.setTextSize(18);
        textView.setPadding(20, 20, 20, 20);

        return convertView;
    }

    private class SerialNoFilter extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();

            if (constraint == null || constraint.length() == 0) {
                results.values = new ArrayList<>(originalList);
                results.count = originalList.size();
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();
                List<String> filtered = new ArrayList<>();

                for (String item : originalList) {
                    if (item.toLowerCase().contains(filterPattern)) {
                        filtered.add(item);
                    }
                }

                results.values = filtered;
                results.count = filtered.size();
            }

            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            filteredList.clear();
            filteredList.addAll((List<String>) results.values);
            notifyDataSetChanged();
        }
    }

    @Override
    public int getCount() {
        return filteredList.size();
    }

    @Nullable
    @Override
    public String getItem(int position) {
        return filteredList.get(position);
    }
}