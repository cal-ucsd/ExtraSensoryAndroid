package edu.ucsd.calab.extrasensory.ui;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;

import edu.ucsd.calab.extrasensory.ESApplication;
import edu.ucsd.calab.extrasensory.R;
import edu.ucsd.calab.extrasensory.network.ESNetworkAccessor;
import edu.ucsd.calab.extrasensory.sensors.AudioProcessing.MFCC;
import edu.ucsd.calab.extrasensory.sensors.WatchProcessing.ESWatchProcessor;

/**
 * This class is for the "home page", which acts as a dashboard for the user to know what is going on and to help debug problems.
 *
 * ========================================
 * The ExtraSensory App
 * @author Yonatan Vaizman yvaizman@ucsd.edu
 * Please see ExtraSensory App website for details and citation requirements:
 * http://extrasensory.ucsd.edu/ExtraSensoryApp
 * ========================================
 */
public class HomeFragment extends BaseTabFragment {

    private static final String LOG_TAG = "[ES-HomeFragment]";
    private static final String NO_AVAILABLE_NETWORK_FOR_SENDING = "There's no available network now to send the data to the server.";
    private static final String ALERT_BUTTON_TEXT_OK = "o.k.";

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
//    private ImageView _watchIcon = null;
    private ImageButton _watchIconButton = null;
    private Button _sendStoredExamplesButton = null;

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

        _watchIconButton = (ImageButton)homeView.findViewById(R.id.imagebutton_watch_icon);
        presentWatchIcon();
        _watchIconButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ESWatchProcessor.getTheWatchProcessor().isWatchConnected()) {
                    ESWatchProcessor.getTheWatchProcessor().launchWatchApp();
                }
            }
        });

        _sendStoredExamplesButton = (Button)homeView.findViewById(R.id.button_send_stored_examples);
        _sendStoredExamplesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!ESNetworkAccessor.getESNetworkAccessor().canWeUseNetworkNow()) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setIcon(R.drawable.ic_launcher).setMessage(NO_AVAILABLE_NETWORK_FOR_SENDING);
                    builder.setPositiveButton(ALERT_BUTTON_TEXT_OK,new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    builder.create().show();

                    return;
                }
                // If we can use network to send the data, go for it:
                ESNetworkAccessor.getESNetworkAccessor().uploadWhatYouHave();
            }
        });

        return homeView;
    }

    private void presentWatchIcon() {
        if (ESWatchProcessor.getTheWatchProcessor().isWatchConnected()) {
            _watchIconButton.setImageResource(R.drawable.watch_on);
            _watchIconButton.setEnabled(true);
        }
        else {
            _watchIconButton.setImageResource(R.drawable.watch_off);
            _watchIconButton.setEnabled(false);
        }
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

        presentWatchIcon();
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
