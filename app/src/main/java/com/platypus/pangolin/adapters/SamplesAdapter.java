package com.platypus.pangolin.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.platypus.pangolin.R;
import com.platypus.pangolin.models.LocalizedSample;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class SamplesAdapter extends RecyclerView.Adapter<SamplesAdapter.ViewHolder> {
    ArrayList<LocalizedSample> samples;
    private final RecycleViewInterface recycleViewInterface;

    public SamplesAdapter(ArrayList<LocalizedSample> samples, RecycleViewInterface r){
        this.samples = samples;
        this.recycleViewInterface = r;
    }

    public void setSamples(ArrayList<LocalizedSample> samples) {
        this.samples = samples;
    }

    public ArrayList<LocalizedSample> getSamples() {
        return samples;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView timestampTextView;
        private final TextView signalConditionTextView;

        public ViewHolder(View view, RecycleViewInterface recycleViewInterface) {
            super(view);
            // Define click listener for the ViewHolder's View
            timestampTextView = (TextView) view.findViewById(R.id.timestamp_textview);
            signalConditionTextView = (TextView) view.findViewById(R.id.signal_condition_textview);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (recycleViewInterface == null)
                        return;

                    int pos = getAdapterPosition();

                    if (pos == RecyclerView.NO_POSITION)
                        return;

                    recycleViewInterface.onItemClick(pos);
                }
            });
        }

        public TextView getTimestampTextView() {
            return timestampTextView;
        }

        public TextView getSignalConditionTextView() {
            return signalConditionTextView;
        }
    }

    @NonNull
    @Override
    public SamplesAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.sample_view, parent, false);
        System.out.println("Called onCreateViewHolder");

        return new ViewHolder(view, recycleViewInterface);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TextView timestampTV = holder.getTimestampTextView();
        TextView conditionTV = holder.getSignalConditionTextView();
        LocalizedSample ls = samples.get(position);
        timestampTV.setText(formatTimestamp(ls.getTimestamp()));
        int condition = ls.getBasicSample().getCondition();

        if (condition == 0) {
            conditionTV.setText("Poor");
            conditionTV.setTextColor(Color.RED);
        }
        else if  (condition == 1) {
            conditionTV.setText("Average");
            conditionTV.setTextColor(Color.rgb(255,140,0));
        }
        else {
            conditionTV.setText("Excellent");
            conditionTV.setTextColor(Color.GREEN);
        }

    }

    @Override
    public int getItemCount() {
        return samples.size();
    }

    private String formatTimestamp(String timestamp){
        String outputFormat = "hh:mm dd-MM-yyyy a";
        SimpleDateFormat inputDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = null;
        try {
            date = inputDateFormat.parse(timestamp);
            SimpleDateFormat outputDateFormat = new SimpleDateFormat(outputFormat, Locale.ITALIAN);
            return outputDateFormat.format(date);
        } catch (ParseException e) {
            return timestamp;
        }
    }


}
