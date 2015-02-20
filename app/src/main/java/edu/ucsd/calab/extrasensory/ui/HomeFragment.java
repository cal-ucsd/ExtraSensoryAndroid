package edu.ucsd.calab.extrasensory.ui;


import android.content.DialogInterface;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import edu.ucsd.calab.extrasensory.ESApplication;
import edu.ucsd.calab.extrasensory.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class HomeFragment extends Fragment {

    private static final String LOG_TAG = "[ES-HomeFragment]";

    private ESApplication getESApplication()  {
        MainActivity mainActivity = (MainActivity) getActivity();
        return (ESApplication)mainActivity.getApplication();
    }

    public HomeFragment() {
        // Required empty public constructor
    }

    private RadioGroup _dataCollectionRadioGroup = null;

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
        return homeView;
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

}
