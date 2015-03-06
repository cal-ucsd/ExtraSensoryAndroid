package edu.ucsd.calab.extrasensory.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import edu.ucsd.calab.extrasensory.ESApplication;
import edu.ucsd.calab.extrasensory.R;
import edu.ucsd.calab.extrasensory.data.ESContinuousActivity;
import edu.ucsd.calab.extrasensory.data.ESDatabaseAccessor;
import edu.ucsd.calab.extrasensory.data.ESTimestamp;
import edu.ucsd.calab.extrasensory.sensors.ESSensorManager;

/**
 * Fragment to display the history of activities (one day at a time)
 */
public class HistoryFragment extends BaseTabFragment {

    private static final String LOG_TAG = "[ESHistoryFragment]";

    public HistoryFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ESDatabaseAccessor.getESDatabaseAccessor().clearOrphanRecords(new ESTimestamp(0));
        calculateAndPresentDaysHistory();
    }

    /**
     * Calculate the history of a single day and present it as a list of continuous activities
     */
    private void calculateAndPresentDaysHistory() {
        //getting today's activities
        ESTimestamp startTime = ESTimestamp.getStartOfTodayTimestamp();

       /* for debug and check timestamp
        DateFormat df = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
        System.out.println("startDATE IS " + df.format(d));
        */

        ESTimestamp endTime = new ESTimestamp(startTime,1);
        SimpleDateFormat dateFormat = new SimpleDateFormat("EE MMM dd", Locale.US);

        Log.d(LOG_TAG,"getting activities from " + startTime.infoString() + " to " + endTime.infoString());

        ESContinuousActivity [] activityList = ESDatabaseAccessor.getESDatabaseAccessor().getContinuousActivitiesFromTimeRange(startTime, endTime);
        Log.d(LOG_TAG,"==== Got " + activityList.length + " cont activities: ");
        for (int i= 0; i < activityList.length; i++) {
            Log.d(LOG_TAG, activityList[i].toString());
        }

        HistoryAdapter histAdapter = new HistoryAdapter(getActivity().getBaseContext(), R.layout.rowlayout, activityList);
        // Get the list view and set it using this adapter
        ListView listView = (ListView) getView().findViewById(R.id.listview);

        LayoutInflater inflater = (LayoutInflater) getView().getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View header = inflater.inflate(R.layout.header_rowlayout, null);

        //Set day title
        TextView headerLabel = (TextView)header.findViewById(R.id.txtHeader);

        headerLabel.setText("Today- " + dateFormat.format(startTime.getDateOfTimestamp()));

        listView.addHeaderView(header);
        listView.setAdapter(histAdapter);
    }

    @Override
    protected void reactToRecordsUpdatedEvent() {
        super.reactToRecordsUpdatedEvent();
        Log.d(LOG_TAG,"reacting to records-update");
        calculateAndPresentDaysHistory();
    }

 /*   @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        // do something with the data
    }*/

    /**
     * Created by Jennifer on 2/18/2015.
     */

    private static class HistoryAdapter extends ArrayAdapter {

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

}
