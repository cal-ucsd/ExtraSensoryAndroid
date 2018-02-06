package edu.ucsd.calab.extrasensory.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.security.InvalidParameterException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import edu.ucsd.calab.extrasensory.ESApplication;
import edu.ucsd.calab.extrasensory.R;
import edu.ucsd.calab.extrasensory.data.ESActivity;
import edu.ucsd.calab.extrasensory.data.ESContinuousActivity;
import edu.ucsd.calab.extrasensory.data.ESDatabaseAccessor;
import edu.ucsd.calab.extrasensory.data.ESLabelStrings;
import edu.ucsd.calab.extrasensory.data.ESTimestamp;

/**
 * Fragment to display the history of activities (one day at a time),
 * and to enable the user to edit the context-labels of today and yesterday.
 * ========================================
 * The ExtraSensory App
 * @author Yonatan Vaizman yvaizman@ucsd.edu
 * Please see ExtraSensory App website for details and citation requirements:
 * http://extrasensory.ucsd.edu/ExtraSensoryApp
 * ========================================
 */
public class HistoryFragment extends BaseTabFragment {

    private static final String LOG_TAG = "[ESHistoryFragment]";
    private static final String INVALID_MERGE_ZONE_ALERT_TEXT = "The marked events contain more than one labeled event. Can't merge them to a single event.";
    private static final String ALERT_BUTTON_TEXT_OK = "O.K.";
    private static final int FEEDBACK_FROM_HISTORY_REQUEST_CODE = 3;

    private ESContinuousActivity[] _activityArray = null;
    private String _headerText = null;
    private int _dayRelativeToToday = 0;
    private boolean _presentingSplitContinuousActivity = false;
    private boolean _justGotBackFromFeedback = false;
    private ESTimestamp _markZoneStartTimestamp = null;
    private ESTimestamp _markZoneEndTimestamp = null;
    private void clearMergeMarkZone() {
        _markZoneStartTimestamp = null;
        _markZoneEndTimestamp = null;
    }

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
        if (_justGotBackFromFeedback) {
            // Don't change the day.
            _justGotBackFromFeedback = false;
        }
        else {
            // Change the day to "today"
            _dayRelativeToToday = 0;
        }
        _presentingSplitContinuousActivity = false;
        clearMergeMarkZone();
        setTimeUnitSelectorContent();
        calculateAndPresentDaysHistory();
    }

    @Override
    public void onActivityResult (int requestCode, int resultCode, Intent data) {
        if (requestCode == FEEDBACK_FROM_HISTORY_REQUEST_CODE) {
            // Remain in the same day
            _justGotBackFromFeedback = true;
        }
    }

    private boolean allowedToEditDaysActivities() {
        // (allowed to edit only from today and yesterday):
        return _dayRelativeToToday >= -1;
    }

    /**
     * Calculate the history of a single day and present it as a list of continuous activities
     */
    private synchronized void calculateAndPresentDaysHistory() {
        _presentingSplitContinuousActivity = false;

        //getting today's activities
        ESTimestamp todayStartTime = ESTimestamp.getStartOfTodayTimestamp();
        ESTimestamp focusDayStartTime = new ESTimestamp(todayStartTime,_dayRelativeToToday);
        ESTimestamp focusDayEndTime = new ESTimestamp(focusDayStartTime, 1);

        SimpleDateFormat dateFormat = new SimpleDateFormat("EE MMM dd", Locale.US);
        _headerText = dateFormat.format(focusDayStartTime.getDateOfTimestamp());
        if (_dayRelativeToToday == 0) {
            // Then it's today:
            _headerText = "Today- " + _headerText;
        }
        if (!allowedToEditDaysActivities()) {
            _headerText += " (view only)";
        }

        Log.d(LOG_TAG, "getting activities from " + focusDayStartTime.infoString() + " to " + focusDayEndTime.infoString());

        boolean addGapDummies = true;
        _activityArray = ESDatabaseAccessor.getESDatabaseAccessor().
                getContinuousActivitiesFromTimeRange(focusDayStartTime, focusDayEndTime, addGapDummies);
        presentHistoryContent();
    }

    private static final String[] TIME_UNIT_LABELS = new String[]{"1 minute","5 minutes","10 minutes"};
    private static final int[] TIME_UNIT_VALS_MINUTES = new int[]{1,5,10};
    private void setTimeUnitSelectorContent() {
        Spinner timeUnitSelector = (Spinner)(getView().findViewById(R.id.spinner_time_unit_in_history));
        List<String> timeUnitStrings = new ArrayList(TIME_UNIT_LABELS.length);
        for (String timeUnitLabel : TIME_UNIT_LABELS) {
            timeUnitStrings.add(timeUnitLabel);
        }
        ArrayAdapter<String> timeUnitAdapter = new ArrayAdapter<String>(ESApplication.getTheAppContext(),
                android.R.layout.simple_spinner_item, timeUnitStrings);
        timeUnitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        timeUnitSelector.setAdapter(timeUnitAdapter);
        timeUnitSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int timeUnitMinutes = TIME_UNIT_VALS_MINUTES[position];
                ESContinuousActivity.basicTimeUnitMinutes = timeUnitMinutes;
                calculateAndPresentDaysHistory();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    private void presentHistoryContent() {

        //Set day title
        View header = getView().findViewById(R.id.history_header);
        TextView headerLabel = (TextView) header.findViewById(R.id.text_history_header_title);
        headerLabel.setText(_headerText);

        // Time-unit list:

        // Adjust the day-navigation buttons:
        Button prevButton = (Button) header.findViewById(R.id.button_previous_day_in_history_header);
        prevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!_presentingSplitContinuousActivity) {
                    _dayRelativeToToday--;
                }
                clearMergeMarkZone();
                calculateAndPresentDaysHistory();
            }
        });
        Button nextButton = (Button) header.findViewById(R.id.button_next_day_in_history_header);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!_presentingSplitContinuousActivity) {
                    _dayRelativeToToday++;
                }
                clearMergeMarkZone();
                calculateAndPresentDaysHistory();
            }
        });

        Log.d(LOG_TAG,"==== Got " + _activityArray.length + " cont activities: ");
        for (int i= 0; i < _activityArray.length; i++) {
            Log.d(LOG_TAG, _activityArray[i].toString());
        }
        ArrayList<ESContinuousActivity> activityList = getArrayList(_activityArray);

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
        Log.v(LOG_TAG,"reacting to records-update");
        if (_presentingSplitContinuousActivity) {
            Log.v(LOG_TAG,"Since presenting split continuous activity, not refreshing the history page.");
            return;
        }
        calculateAndPresentDaysHistory();
    }

    private boolean isActivityInTheMergeMarkZone(ESContinuousActivity continuousActivity) {
        if (_markZoneStartTimestamp == null || _markZoneEndTimestamp == null) {
            return false;
        }

        ESTimestamp timestamp = continuousActivity.getStartTimestamp();
        if (timestamp.isEarlierThan(_markZoneStartTimestamp)) {
            return false;
        }

        if (timestamp.isLaterThan(_markZoneEndTimestamp)) {
            return false;
        }

        return true;
    }

    private synchronized boolean isMarkZoneValidForMerging() {
        // Just as sanity check, make sure there is a mark zone:
        if (_markZoneStartTimestamp == null || _markZoneEndTimestamp == null) {
            return false;
        }

        boolean foundUserProvidedLabels = false;
        for (ESContinuousActivity continuousActivity : _activityArray) {
            if (continuousActivity.isUnrecordedGap()) {
                // Ignore this dummy-activity:
                continue;
            }
            if (continuousActivity.getStartTimestamp().isEarlierThan(_markZoneStartTimestamp)) {
                continue;
            }
            if (continuousActivity.getStartTimestamp().isLaterThan(_markZoneEndTimestamp)) {
                // Then there's no use continuing on the list, we passed the mark zone safely:
                break;
            }

            if (continuousActivity.hasUserProvidedLabels()) {
                // did we already find another activity with user-labels?
                if (foundUserProvidedLabels) {
                    // Then this zone is not valid. We have 2 different activities with user labels:
                    return false;
                }
                foundUserProvidedLabels = true;
            }
        }
        // If reached here safely, we haven't found 2 different activities with user labels
        return true;
    }

    private void alertUserOfInvalidMergeMarkZone() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setIcon(R.drawable.ic_launcher).setMessage(INVALID_MERGE_ZONE_ALERT_TEXT);
        builder.setPositiveButton(ALERT_BUTTON_TEXT_OK,new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                clearMergeMarkZone();
                presentHistoryContent();
            }
        });
        builder.create().show();
    }

    private void rowClicked(ESContinuousActivity continuousActivity) {
        ESContinuousActivity continuousActivityForFeedback = continuousActivity;

        // See if this row is in the mark zone:
        if (isActivityInTheMergeMarkZone(continuousActivity)) {
            // Then the feedback should be done over the whole time range of the marked zone
            // First check the validity of the time zone for merging:
            if (!isMarkZoneValidForMerging()) {
                // Then the mark zone is not valid. We should alert the user and clear the zone:
                Log.i(LOG_TAG,"Clicked a row in an invalid merge zone.");
                alertUserOfInvalidMergeMarkZone();
                return;
            }

            // Mark zone is safe for merging,
            // so we should go to feedback for a single continuous activity for the whole marked time range:
            continuousActivityForFeedback = ESDatabaseAccessor.getESDatabaseAccessor().
                    getSingleContinuousActivityFromTimeRange(_markZoneStartTimestamp, _markZoneEndTimestamp);
        }

        Intent intent = new Intent(getActivity(),FeedbackActivity.class);
        FeedbackActivity.setFeedbackParametersBeforeStartingFeedback(new FeedbackActivity.FeedbackParameters(continuousActivityForFeedback));
        startActivityForResult(intent,FEEDBACK_FROM_HISTORY_REQUEST_CODE);
        //startActivity(intent);
    }

    private void rowSwipedRight(ESContinuousActivity continuousActivity) {
        // Are we presenting a split continuous activity?
        if (_presentingSplitContinuousActivity) {
            // Then ignore this gesture:
            Log.v(LOG_TAG,"[swipe-right] In split-activities mode. Ignoring swipe to the right.");
            return;
        }

        // Is this continuous activity already in the mark zone?
        if (isActivityInTheMergeMarkZone(continuousActivity)) {
            // Then this gesture should cause clearing the mark zone:
            Log.v(LOG_TAG, "[swipe-right] Row already in marked zone. Clearing mark zone.");
            clearMergeMarkZone();
            presentHistoryContent();
            return;
        }

        ESTimestamp swipedStartTimestamp = continuousActivity.getStartTimestamp();
        ESTimestamp swipedEndTimestamp = continuousActivity.getEndTimestamp();

        // Is there no mark zone currently?
        if (_markZoneStartTimestamp == null || _markZoneEndTimestamp == null) {
            // Then this marked activity is starting a new mark zone:
            Log.v(LOG_TAG,"[swipe-right] No mark zone. Starting new mark zone with this one row.");
            _markZoneStartTimestamp = swipedStartTimestamp;
            _markZoneEndTimestamp = swipedEndTimestamp;
        }
        else if (swipedStartTimestamp.isEarlierThan(_markZoneStartTimestamp)) {
            Log.v(LOG_TAG,"[swipe-right] Row earlier than mark zone. Expanding zone to earlier.");
            _markZoneStartTimestamp = swipedStartTimestamp;
        }
        else if (swipedEndTimestamp.isLaterThan(_markZoneEndTimestamp)) {
            Log.v(LOG_TAG,"[swipe-right] Row later than mark zone. Expanding zone to later.");
            _markZoneEndTimestamp = swipedEndTimestamp;
        }
        else {
            // We should have covered all the cases.
            Log.e(LOG_TAG,"Swipe right failed to fit any case.");
        }

        presentHistoryContent();
    }

    private synchronized void rowSwipedLeft(ESContinuousActivity continuousActivity) {
        // Split the chosen continuous activity and present it as separate minute activities:
        _presentingSplitContinuousActivity = true;

        Date startTime = continuousActivity.getStartTimestamp().getDateOfTimestamp();
        Date endTime = continuousActivity.getEndTimestamp().getDateOfTimestamp();
        SimpleDateFormat dateFormat = new SimpleDateFormat("EE hh:mm", Locale.US);
        _headerText = String.format("%s - %s",dateFormat.format(startTime),dateFormat.format(endTime));

        // Split to atomic (minute) activities:
        ESContinuousActivity[] splitActivities = ESDatabaseAccessor.getESDatabaseAccessor().splitToSeparateContinuousActivities(continuousActivity);
        _activityArray = splitActivities;
        clearMergeMarkZone();
        presentHistoryContent();
    }

    /**
     * Created by Jennifer on 2/18/2015.
     */

    private static class HistoryAdapter extends ArrayAdapter {

        private static final int ITEM_TYPE_DUMMY = 0;
        private static final int ITEM_TYPE_ACTUAL = 1;

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
        public int getViewTypeCount() { return 2; }

        @Override
        public int getItemViewType(int position) {
            return _items.get(position).isUnrecordedGap() ? ITEM_TYPE_DUMMY : ITEM_TYPE_ACTUAL;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            int rowType = getItemViewType(position);
            ESContinuousActivityHolder holder = null;

            View row;
            if (convertView == null) {
                holder = new ESContinuousActivityHolder();
                switch (rowType) {
                    case ITEM_TYPE_DUMMY:
                        row = LayoutInflater.from(getContext()).inflate(R.layout.history_gap_rowlayout,null);
                        holder.gapTime = (TextView)row.findViewById(R.id.text_gap_in_history_dummy_row);
                        break;
                    case ITEM_TYPE_ACTUAL:
                        row = super.getView(position,convertView,parent);
                        //row = LayoutInflater.from(getContext()).inflate(R.layout.history_rowlayout,null);
                        holder.time = (TextView)row.findViewById(R.id.text_time_in_history_row);
                        holder.mainActivity = (TextView)row.findViewById(R.id.text_main_activity_in_history_row);
                        holder.details = (TextView)row.findViewById(R.id.text_details_in_history_row);
                        break;
                    default:
                        throw new InvalidParameterException("Got unsupported history row type: " + rowType);
                }
                row.setTag(holder);
            }
            else {
                row = convertView;
                holder = (ESContinuousActivityHolder)row.getTag();
            }

            //get one activity from the array
            final ESContinuousActivity continuousActivity = _items.get(position);

            // Set the values for the row:
            if (rowType == ITEM_TYPE_DUMMY) {
                int gapSeconds = continuousActivity.gapDurationSeconds();
                int gapMinutes = gapSeconds / 60;
                int gapHours = gapSeconds / 3600;
                String gapDurStr = (gapHours >= 1) ? "" + gapHours + " hours" : "" + gapMinutes + " minutes";
                String gapStr = "Gap ~" + gapDurStr;

                holder.gapTime.setText(gapStr);
                return row;
            }

            // Assume now we have a regular actual continuous activity row:
            String activityLabel = "";
            String mainActivityForColor = "";
            String timeLabel = "";
            String endTimeLabel = "";
            Date date;

            if(continuousActivity.getMainActivityUserCorrection() != null){
                activityLabel = continuousActivity.getMainActivityUserCorrection();
                mainActivityForColor = activityLabel;
            }
            else{
                mainActivityForColor = continuousActivity.getMainActivityServerPrediction();
                if (mainActivityForColor == null) {
                    activityLabel = "in process...";
                }
                else {
                    activityLabel = mainActivityForColor + "?";
                }
            }

            // Add location coordinates (each instance may have a representative location point):
//            activityLabel += " loc: " + ESLabelStrings.makeCSV(continuousActivity.getLocationLatLongFromFirstInstance());

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
            holder.details.setText(getDetailsString(continuousActivity));

            row.setBackgroundColor(ESLabelStrings.getColorForMainActivity(mainActivityForColor));
            
            // Is this row marked for merging?
            ImageView chckmarkView = (ImageView)row.findViewById(R.id.image_mark_for_merge_in_history);
            if (_handler.isActivityInTheMergeMarkZone(continuousActivity)) {
                chckmarkView.setImageResource(R.drawable.checkmark_in_circle);
            }
            else {
                chckmarkView.setImageBitmap(null);
            }

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
            else {
                // Make sure this row has no response to click or swipes:
                row.setOnClickListener(null);
                row.setOnTouchListener(null);
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

            String[] sec;
            String delim = ",";
            String suffix = "";
            if (continuousActivity.hasUserProvidedLabels()) {
                sec = continuousActivity.getSecondaryActivities();
            }
            else {
                sec = continuousActivity.getSecondaryActivitiesOrServerGuesses();
                delim = "";
                suffix = "?";
            }
            if (sec != null && sec.length > 0) {
                details += " (" + sec[0] + suffix;
                for (int i=1; i<sec.length; i++) {
                    details += delim + " " + sec[i] + suffix;
                }
                details += ")";
            }

            if (details.length() > 100) {
                details = details.substring(0,100) + "...";
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
            TextView details;
            TextView gapTime;

            public String toString() {
                return "time: " + time + ". main: " + mainActivity + ". details: " + details + ". gap: " + gapTime;
            }
        }
    }

}
