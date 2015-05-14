package edu.ucsd.calab.extrasensory.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import edu.ucsd.calab.extrasensory.R;
import edu.ucsd.calab.extrasensory.data.ESActivity;
import edu.ucsd.calab.extrasensory.data.ESContinuousActivity;
import edu.ucsd.calab.extrasensory.data.ESDatabaseAccessor;
import edu.ucsd.calab.extrasensory.data.ESLabelStrings;
import edu.ucsd.calab.extrasensory.data.ESTimestamp;

/**
 * Fragment to display the history of activities (one day at a time)
 */
public class HistoryFragment extends BaseTabFragment {

    private static final String LOG_TAG = "[ESHistoryFragment]";

    private int _dayRelativeToToday = 0;

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
    public void onStart() {
        super.onStart();
        ESDatabaseAccessor.getESDatabaseAccessor().clearOrphanRecords(new ESTimestamp(0));
        _dayRelativeToToday = 0;
        calculateAndPresentDaysHistory();
    }

    private boolean allowedToEditDaysActivities() {
        // (allowed to edit only from today and yesterday):
        return _dayRelativeToToday >= -1;
    }

    /**
     * Calculate the history of a single day and present it as a list of continuous activities
     */
    private void calculateAndPresentDaysHistory() {
        //getting today's activities
        ESTimestamp todayStartTime = ESTimestamp.getStartOfTodayTimestamp();
        ESTimestamp focusDayStartTime = new ESTimestamp(todayStartTime,_dayRelativeToToday);
        ESTimestamp focusDayEndTime = new ESTimestamp(focusDayStartTime, 1);

        SimpleDateFormat dateFormat = new SimpleDateFormat("EE MMM dd", Locale.US);
        String headerText = dateFormat.format(focusDayStartTime.getDateOfTimestamp());
        if (_dayRelativeToToday == 0) {
            // Then it's today:
            headerText = "Today- " + headerText;
        }
        if (!allowedToEditDaysActivities()) {
            headerText += " (view only)";
        }

        Log.d(LOG_TAG, "getting activities from " + focusDayStartTime.infoString() + " to " + focusDayEndTime.infoString());

        ESContinuousActivity[] activityArray = ESDatabaseAccessor.getESDatabaseAccessor().
                getContinuousActivitiesFromTimeRange(focusDayStartTime, focusDayEndTime);
        presentSpecificHistoryContent(headerText,activityArray);
    }

    private void presentSpecificHistoryContent(String headerText,ESContinuousActivity[] activityArray) {

        //Set day title
        View header = getView().findViewById(R.id.history_header);
        TextView headerLabel = (TextView) header.findViewById(R.id.text_history_header_title);
        headerLabel.setText(headerText);

        // Adjust the day-navigation buttons:
        Button prevButton = (Button) header.findViewById(R.id.button_previous_day_in_history_header);
        prevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                _dayRelativeToToday --;
                calculateAndPresentDaysHistory();
            }
        });
        Button nextButton = (Button) header.findViewById(R.id.button_next_day_in_history_header);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                _dayRelativeToToday ++;
                calculateAndPresentDaysHistory();
            }
        });

        Log.d(LOG_TAG,"==== Got " + activityArray.length + " cont activities: ");
        for (int i= 0; i < activityArray.length; i++) {
            Log.d(LOG_TAG, activityArray[i].toString());
        }
        ArrayList<ESContinuousActivity> activityList = getArrayList(activityArray);

        // Get the list view and set it using this adapter
        ListView listView = (ListView) getView().findViewById(R.id.listview_history_items);
        if (listView.getAdapter() == null) {
            HistoryAdapter histAdapter = new HistoryAdapter(getActivity().getBaseContext(), R.layout.history_rowlayout, activityList,this);
            listView.setAdapter(histAdapter);
        }
        else {
            ((HistoryAdapter)listView.getAdapter()).resetItems(activityList);
        }

    }

    private ArrayList<ESContinuousActivity> getArrayList(ESContinuousActivity[] items) {
        if (items == null) {
            return new ArrayList<>(0);
        }
        ArrayList<ESContinuousActivity> arrayList = new ArrayList<>(items.length);
        for (int i=0; i<items.length; i++) {
            arrayList.add(items[i]);
        }
        return arrayList;
    }

    @Override
    protected void reactToRecordsUpdatedEvent() {
        super.reactToRecordsUpdatedEvent();
        Log.d(LOG_TAG,"reacting to records-update");
        calculateAndPresentDaysHistory();
    }

    private void rowClicked(ESContinuousActivity continuousActivity) {
        Intent intent = new Intent(getActivity(),FeedbackActivity.class);
        FeedbackActivity.setFeedbackParametersBeforeStartingFeedback(new FeedbackActivity.FeedbackParameters(continuousActivity));
        startActivity(intent);
    }

    private void rowSwipedRight(ESContinuousActivity continuousActivity) {
        //TODO: mark for merge
    }

    private void rowSwipedLeft(ESContinuousActivity continuousActivity) {
        Date startTime = continuousActivity.getStartTimestamp().getDateOfTimestamp();
        Date endTime = continuousActivity.getEndTimestamp().getDateOfTimestamp();
        SimpleDateFormat dateFormat = new SimpleDateFormat("EE hh:mm", Locale.US);
        String headerText = String.format("%s - %s",dateFormat.format(startTime),dateFormat.format(endTime));

        // Split to atomic (minute) activities:
        ESContinuousActivity[] splitActivities = ESDatabaseAccessor.getESDatabaseAccessor().splitToSeparateContinuousActivities(continuousActivity);

        presentSpecificHistoryContent(headerText,splitActivities);
    }

    /**
     * Created by Jennifer on 2/18/2015.
     */

    private static class HistoryAdapter extends ArrayAdapter {

        private ArrayList<ESContinuousActivity> _items;
        private HistoryFragment _handler;

        /**
         * Constructor for History Adapter
         * @param context context from activity
         * @param layoutResourceId The xml rowlayout
         * @param items The list of ESContinuousActivity with the values we want to display
         * @param handler The HistoryFragment that uses this adapter
         */
        public HistoryAdapter(Context context, int layoutResourceId, ArrayList<ESContinuousActivity> items,HistoryFragment handler) {
            super(context,layoutResourceId,R.id.text_main_activity_in_history_row,items);
            this._items = items;
            this._handler = handler;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ESContinuousActivityHolder holder = null;
            View row = super.getView(position,convertView,parent);

            //linking labels to xml view
            holder = new ESContinuousActivityHolder();
            holder.time = (TextView)row.findViewById(R.id.text_time_in_history_row);
            holder.mainActivity = (TextView)row.findViewById(R.id.text_main_activity_in_history_row);
            row.setTag(holder);

            String activityLabel = "";
            String mainActivityForColor = "";
            String timeLabel = "";
            String endTimeLabel = "";
            Date date;

            //get one activity from the array
            final ESContinuousActivity continuousActivity = _items.get(position);

            if(continuousActivity.getMainActivityUserCorrection() != null){
                activityLabel = continuousActivity.getMainActivityUserCorrection();
                mainActivityForColor = activityLabel;
            }
            else{
                mainActivityForColor = continuousActivity.getMainActivityServerPrediction();
                activityLabel = mainActivityForColor + "?";
            }
            //setting time label
            date = continuousActivity.getStartTimestamp().getDateOfTimestamp();
            timeLabel = new SimpleDateFormat("hh:mm a").format(date);
            date = continuousActivity.getEndTimestamp().getDateOfTimestamp();
            endTimeLabel = new SimpleDateFormat("hh:mm a").format(date);
            if (!endTimeLabel.equals(timeLabel)) {
                timeLabel = timeLabel + " - " + endTimeLabel;
            }

            //setting activity label
            holder.mainActivity.setText(activityLabel);
            holder.time.setText(timeLabel);

            // Setting the details label:
            TextView detailsText = (TextView)row.findViewById(R.id.text_details_in_history_row);
            detailsText.setText(getDetailsString(continuousActivity));

            row.setBackgroundColor(ESLabelStrings.getColorForMainActivity(mainActivityForColor));

            // If allowed to edit activities, define the listener for click and swipes:
            if (_handler.allowedToEditDaysActivities()) {
                // Setting the click listener:
                row.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.i(LOG_TAG, "row clicked");
                        _handler.rowClicked(continuousActivity);
                    }
                });

                // Setting the gesture detections:
                row.setOnTouchListener(new OnSwipeTouchListener(getContext()) {
                    @Override
                    public boolean onSwipeRight() {
                        Log.i(LOG_TAG, "Swiped row to the right");
                        _handler.rowSwipedRight(continuousActivity);
                        return true;
                    }

                    @Override
                    public boolean onSwipeLeft() {
                        Log.i(LOG_TAG, "Swiped row to the left");
                        _handler.rowSwipedLeft(continuousActivity);
                        return true;
                    }
                });
            }

            return row;
        }

        private String getDetailsString(ESContinuousActivity continuousActivity) {
            String details = "";

            String[] moods = continuousActivity.getMoods();
            if (moods != null && moods.length > 0) {
                details += moods[0];
                for (int i=1; i<moods.length; i++) {
                    details += ", " + moods[i];
                }
            }

            String[] sec = continuousActivity.getSecondaryActivities();
            if (sec != null && sec.length > 0) {
                details += " (" + sec[0];
                for (int i=1; i<sec.length; i++) {
                    details += ", " + sec[i];
                }
                details += ")";
            }

            return details;
        }

        public void resetItems(ArrayList<ESContinuousActivity> items) {
            this._items.clear();
            this._items.addAll(items);
            notifyDataSetChanged();
        }

        static class ESContinuousActivityHolder
        {
            TextView time;
            TextView mainActivity;
        }
    }

}
