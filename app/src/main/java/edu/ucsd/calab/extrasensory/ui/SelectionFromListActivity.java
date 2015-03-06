package edu.ucsd.calab.extrasensory.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.HashSet;

import edu.ucsd.calab.extrasensory.ESApplication;
import edu.ucsd.calab.extrasensory.R;
import edu.ucsd.calab.extrasensory.data.ESLabelStrings;

/**
 * This class manages selecting labels from a list.
 * It provides additional features, as index by subjects, search and label suggestions.
 *
 * To use this activity:
 * 1) You must prepare a proper intent, containing the extra string key LIST_TYPE_KEY,
 * with int value of one of the LIST_TYPE_... constants defined here.
 *
 * 2) In addition, the intent can contain more extra keys:
 * PRESELECTED_LABELS_KEY - with value of string[] of the currently selected labels.
 *
 * 3) Start this activity with startActivityForResult() function, to signal that you are expecting a result.
 *
 * 4) When the user is done with this activity, it sets the result and finishes.
 * Then you can catch the result by implementing onActivityResult() and checking the requestCode is the one you started the activity with.
 * In onActivityResult() you'll get the "response" intent with the results from this activity.
 * Specifically, the result intent should contain a key SELECTED_LABELS_OUTPUT_KEY with a value of string[] with the selected labels.
 */
public class SelectionFromListActivity extends BaseActivity {

    private static final String LOG_TAG = "[SelectionFromListActivity]";

    public static final String LIST_TYPE_KEY = "edu.ucsd.calab.extrasensory.key.list_type";
    public static final String PRESELECTED_LABELS_KEY = "edu.ucsd.calab.extrasensory.key.preselected_labels";
    public static final String FREQUENTLY_USED_LABELS_KEY = "edu.ucsd.calab.extrasensory.key.frequently_used_labels";

    public static final String SELECTED_LABELS_OUTPUT_KEY = "edu.ucsd.calab.extrasensory.key.selected_labels";

    private static final int LIST_TYPE_MISSING = -1;
    public static final int LIST_TYPE_MAIN_ACTIVITY = 1;
    public static final int LIST_TYPE_SECONDARY_ACTIVITIES = 2;
    public static final int LIST_TYPE_MOODS = 3;


    //    private ArrayList<String> _sectionsHeaders;
//    private ArrayList<ArrayList<String>> _sectionsLists;
    private String[] _labelChoices;
    private HashSet<String> _selectedLabels;
    private boolean _allowMultiSelection = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selection_from_list);

        Intent inputParameters = getIntent();
        if (!inputParameters.hasExtra(LIST_TYPE_KEY)) {
            Log.e(LOG_TAG,"Selection from list was started without specifying type of list");
            finish();
            return;
        }

        // Set the view according to the parameters:
        int listType = inputParameters.getIntExtra(LIST_TYPE_KEY,LIST_TYPE_MISSING);
        switch (listType) {
            case LIST_TYPE_MAIN_ACTIVITY:
                _labelChoices = ESLabelStrings.getMainActivities();
                _allowMultiSelection = false;
                break;
            case LIST_TYPE_SECONDARY_ACTIVITIES:
                _labelChoices = ESLabelStrings.getSecondaryActivities();
                _allowMultiSelection = true;
                break;
            case LIST_TYPE_MOODS:
                _labelChoices = ESLabelStrings.getMoods();
                _allowMultiSelection = true;
                break;
            default:
                Log.e(LOG_TAG,"Unsupported list type received: " + listType);
                finish();
                return;
        }

        if (inputParameters.hasExtra(PRESELECTED_LABELS_KEY)) {
            String[] preselected = inputParameters.getStringArrayExtra(PRESELECTED_LABELS_KEY);
            _selectedLabels = new HashSet<>(preselected.length);
            for (int i=0; i < preselected.length; i ++) {
                _selectedLabels.add(preselected[i]);
            }
        }
        else {
            _selectedLabels = new HashSet<>(10);
        }


        refreshListContent();
    }

    private void refreshListContent() {
        ListView choicesListView = (ListView)findViewById(R.id.listview_selection_choices_list);
        ChoiceItem[] items = new ChoiceItem[_labelChoices.length+1];
        items[0] = new ChoiceItem("header text",true);
        for (int i=0; i<_labelChoices.length; i++ ) {
            items[i+1] = new ChoiceItem(_labelChoices[i]);
        }
        ChoicesListAdapter choicesListAdapter = new ChoicesListAdapter(items,this);
        choicesListView.setAdapter(choicesListAdapter);
        //TODO: present the list of choices, where the items that are in _selectedLabels appear with some mark (e.g. checkmark)
        //TODO: later add here also the sections of labels and index
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_selection_from_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Call this function when user presses "done" button.
     * This function will return the selected items list to whoever called this activity.
     */
    private void returnSelectedLabels() {
        Intent selectedLabelsIntent = new Intent();
        String[] returnedSelectedLabels = new String[_selectedLabels.size()];
        returnedSelectedLabels = _selectedLabels.toArray(returnedSelectedLabels);
        selectedLabelsIntent.putExtra(SELECTED_LABELS_OUTPUT_KEY,returnedSelectedLabels);

        setResult(Activity.RESULT_OK,selectedLabelsIntent);
        finish();
    }



    private static class ChoiceItem {
        public String _label;
        public boolean _isSectionHeader;
        public ChoiceItem(String label,boolean isSectionHeader) {
            _label = label;
            _isSectionHeader = isSectionHeader;
        }
        public ChoiceItem(String label) {
            this(label,false);
        }

        @Override
        public String toString() {
            return _label;
        }
    }

    private static class ChoicesListAdapter extends ArrayAdapter<ChoiceItem> {

        private SelectionFromListActivity _handler;

        public ChoicesListAdapter(ChoiceItem[] objects,SelectionFromListActivity handler) {
            super(ESApplication.getTheAppContext(), R.layout.row_in_selection_from_list, R.id.text_label_name_in_selection_choice, objects);
            _handler = handler;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View rowView =  super.getView(position,convertView,parent);
            ChoiceItem item = getItem(position);
            if (item._isSectionHeader) {
                rowView.setBackgroundColor(Color.BLUE);
                return rowView;
            }

            if (_handler._selectedLabels.contains(item._label)) {

            }

            return rowView;
        }
    }
}
