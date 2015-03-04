package edu.ucsd.calab.extrasensory.ui;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import edu.ucsd.calab.extrasensory.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class SummaryFragment extends BaseTabFragment {

    private static final String LOG_TAG = "[ES-SummaryFragment]";

    public SummaryFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_summary, container, false);
    }

    @Override
    protected void reactToRecordsUpdatedEvent() {
        super.reactToRecordsUpdatedEvent();
        Log.d(LOG_TAG, "reacting to records-update");
        //TODO: update the pie chart / list of statistics
    }

}
