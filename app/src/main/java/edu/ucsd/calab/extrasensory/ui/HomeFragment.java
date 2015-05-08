package edu.ucsd.calab.extrasensory.ui;


import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;

import edu.ucsd.calab.extrasensory.ESApplication;
import edu.ucsd.calab.extrasensory.R;
import edu.ucsd.calab.extrasensory.network.ESNetworkAccessor;
import edu.ucsd.calab.extrasensory.sensors.AudioProcessing.MFCC;

/**
 * A simple {@link Fragment} subclass.
 */
public class HomeFragment extends BaseTabFragment {

    private static final String LOG_TAG = "[ES-HomeFragment]";

    private ESApplication getESApplication()  {
        MainActivity mainActivity = (MainActivity) getActivity();
        return (ESApplication)mainActivity.getApplication();
    }

    public HomeFragment() {
        // Required empty public constructor
    }

    private RadioGroup _dataCollectionRadioGroup = null;
    private TextView _storedExamplesCount = null;
    private TextView _feedbackQueueCount = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View homeView = inflater.inflate(R.layout.fragment_home, container, false);
        _dataCollectionRadioGroup = (RadioGroup)homeView.findViewById(R.id.radio_group_data_collection);
        _dataCollectionRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.radio_data_collection_off:
                        Log.i(LOG_TAG,"User turned data collection off");
                        getESApplication().set_userSelectedDataCollectionOn(false);
                        break;
                    case R.id.radio_data_collection_on:
                        Log.i(LOG_TAG,"User turned data collection on");
                        getESApplication().set_userSelectedDataCollectionOn(true);
                        break;
                    default:
                        Log.e(LOG_TAG,"Unrecognized action for data collection radio button group: " + checkedId);
                        // Do nothing
                }
            }
        });

        _storedExamplesCount = (TextView)homeView.findViewById(R.id.text_zip_file_count);
        presentNumStoredExamples();

        _feedbackQueueCount = (TextView)homeView.findViewById(R.id.text_feedback_count);
        presentFeedbackQueueCount();



        return homeView;
    }


    private void presentNumStoredExamples() {
        int num = ESNetworkAccessor.getESNetworkAccessor().uploadQueueSize();
        _storedExamplesCount.setText("" + num);
    }

    private void presentFeedbackQueueCount() {
        int num = ESNetworkAccessor.getESNetworkAccessor().feedbackQueueSize();
        _feedbackQueueCount.setText("" + num);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (_dataCollectionRadioGroup == null) {
            Log.e(LOG_TAG,"radio group of data collection is null");
        }
        if (getESApplication().is_userSelectedDataCollectionOn()) {
            _dataCollectionRadioGroup.check(R.id.radio_data_collection_on);
        }
        else {
            _dataCollectionRadioGroup.check(R.id.radio_data_collection_off);
        }
    }

    @Override
    protected void reactToRecordsUpdatedEvent() {
        super.reactToRecordsUpdatedEvent();
        Log.d(LOG_TAG,"reacting to records-update");
        //TODO: redraw the relevant image to the latest activity
    }

    @Override
    protected void reactToNetworkQueueSizeChangedEvent() {
        super.reactToNetworkQueueSizeChangedEvent();
        presentNumStoredExamples();
    }

    @Override
    protected void reactToFeedbackQueueSizeChangedEvent() {
        super.reactToFeedbackQueueSizeChangedEvent();
        presentFeedbackQueueCount();
    }




}
