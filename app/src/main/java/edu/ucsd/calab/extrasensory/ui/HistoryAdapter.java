package edu.ucsd.calab.extrasensory.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.Date;
import java.text.SimpleDateFormat;

import edu.ucsd.calab.extrasensory.R;
import edu.ucsd.calab.extrasensory.data.ESContinuousActivity;

/**
 * Created by Jennifer on 2/18/2015.
 */

public class HistoryAdapter extends ArrayAdapter{

    // This variable gives context as to what is calling the inflater and where it needs
    // to inflate the view
    private final Context context;
    //linked list of ESContinuousActivities that need to be shown
    private final ESContinuousActivity[] values;
    int layoutResourceId;

    /**
     * Constructor for History Adapter
     * @param context context from activity
     * @param layoutResourceId The xml rowlayout
     * @param values The ESContinuousActivity[] with the values we want to display
     */
    public HistoryAdapter(Context context, int layoutResourceId, ESContinuousActivity[] values) {
        super(context, R.layout.rowlayout, values);
        this.layoutResourceId = layoutResourceId;
        this.context = context;
        this.values = values;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        ESContinuousActivityHolder holder = null;

        if (row == null){
            //Here we're creating the inflater that will be used to push in the custom view
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            // This is the View that will be returned for every row --note that it inflates it with rowlayout
            row = inflater.inflate(layoutResourceId, parent, false);

            //linking labels to xml view
            holder = new ESContinuousActivityHolder();
            holder.time = (TextView)row.findViewById(R.id.firstLine);
            holder.mainActivity = (TextView)row.findViewById(R.id.secondLine);
            row.setTag(holder);

            String activityLabel = "";
            String timeLabel = "";
            String endTimeLabel = "";
            Date date;

            //get one activity from the array
            ESContinuousActivity activity = values[position];

            if(activity.getMainActivityUserCorrection() != null){
                activityLabel = activity.getMainActivityUserCorrection();
            }
            else{
                activityLabel = activity.getMainActivityServerPrediction() + "?";
            }
                //setting time label
            date = activity.getStartTimestamp().getDateOfTimestamp();
            timeLabel = new SimpleDateFormat("hh:mm a").format(date);
            date = activity.getEndTimestamp().getDateOfTimestamp();
            endTimeLabel = new SimpleDateFormat("hh:mm a").format(date);
            timeLabel = timeLabel + " - " + endTimeLabel;

            // System.out.println("adapter adding activity: " + activityLabel);

            //setting activity label
            holder.mainActivity.setText(activityLabel);
            holder.time.setText(timeLabel);
        }
        return row;
    }

    static class ESContinuousActivityHolder
    {
        TextView time;
        TextView mainActivity;
    }
}

