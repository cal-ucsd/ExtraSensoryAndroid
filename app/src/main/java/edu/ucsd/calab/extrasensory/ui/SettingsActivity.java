package edu.ucsd.calab.extrasensory.ui;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Locale;

import edu.ucsd.calab.extrasensory.R;
import edu.ucsd.calab.extrasensory.data.ESSettings;
import edu.ucsd.calab.extrasensory.network.ESNetworkAccessor;
import edu.ucsd.calab.extrasensory.sensors.ESSensorManager;

public class SettingsActivity extends BaseActivity {

    private static final String LOG_TAG = "[SettingsActivity]";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        setContentView(R.layout.activity_setting);

        // Notification interval:
        final SeekBar notificationIntervalSeekBar = (SeekBar)findViewById(R.id.notification_interval_seek_bar);
        notificationIntervalSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int newIntervalValueMinutes = Math.max(2,progress);
                int newIntervalValueSeconds = 60 * newIntervalValueMinutes;
                ESSettings.setNotificationIntervalInSeconds(newIntervalValueSeconds);
                Log.d(LOG_TAG,"Notification interval changed to " + newIntervalValueSeconds);
                displayNotificationIntervalValue(newIntervalValueMinutes);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        // Use notifications:
        CheckBox useNearPastNotifications = (CheckBox)findViewById(R.id.checkbox_near_past_notifications);
        useNearPastNotifications.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                ESSettings.setUseNearPastNotifications(isChecked);
                notificationIntervalSeekBar.setEnabled(ESSettings.useAnyTypeOfNotifications());
            }
        });
        CheckBox useNearFutureNotifications = (CheckBox)findViewById(R.id.checkbox_near_future_notifications);
        useNearFutureNotifications.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                ESSettings.setUseNearFutureNotifications(isChecked);
                notificationIntervalSeekBar.setEnabled(ESSettings.useAnyTypeOfNotifications());
            }
        });

/*
        RadioGroup useNotificationsRG = (RadioGroup)findViewById(R.id.radio_group_use_notifications);
        useNotificationsRG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.radio_use_notifications_on:
                        ESSettings.setUseNotifications(true);
                        notificationIntervalSeekBar.setEnabled(true);
                        break;
                    case R.id.radio_use_notifications_off:
                        ESSettings.setUseNotifications(false);
                        notificationIntervalSeekBar.setEnabled(false);
                        break;
                    default:
                        Log.e(LOG_TAG,"got unexpected id for radio group of use-notifications");
                }
            }
        });
*/


        // Num examples stored before sending:
        SeekBar numExamplesStoreBeforeSendSeekBar = (SeekBar)findViewById(R.id.num_examples_store_before_send_seek_bar);
        numExamplesStoreBeforeSendSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                ESSettings.setNumExamplesStoreBeforeSend(progress);
                Log.i(LOG_TAG,"Changed num examples stored before sending to " + progress);
                displayNumExamplesStoreBeforeSend(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        // Secure communication:
        RadioGroup useHttpsRG = (RadioGroup)findViewById(R.id.radio_group_use_https);
        useHttpsRG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.radio_https_on:
                        ESNetworkAccessor.getESNetworkAccessor().set_useHttps(true);
                        break;
                    case R.id.radio_https_off:
                        ESNetworkAccessor.getESNetworkAccessor().set_useHttps(false);
                        break;
                    default:
                        Log.e(LOG_TAG,"got unexpected id for radio group of Https");
                }
            }
        });


        // Cellular communication:
        RadioGroup allowCellularRG = (RadioGroup)findViewById(R.id.radio_group_allow_cellular);
        allowCellularRG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.radio_allow_cellular_on:
                        ESSettings.setAllowCellular(true);
                        break;
                    case R.id.radio_allow_cellular_off:
                        ESSettings.setAllowCellular(false);
                        break;
                    default:
                        Log.e(LOG_TAG,"got unexpected id for radio group of cellular communication");
                }
            }
        });

        // Location bubble:
        RadioGroup locationUsedRG = (RadioGroup)findViewById(R.id.radio_group_location_bubble);
        final EditText latitudeEdit = (EditText)findViewById(R.id.edit_location_bubble_latitude);
        final EditText longitudeEdit = (EditText)findViewById(R.id.edit_location_bubble_longitude);
        final Button updateLatLongButton = (Button)findViewById(R.id.button_location_bubble_update);

        locationUsedRG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.radio_location_bubble_off:
                        ESSettings.setLocationBubbleUsed(false);
                        latitudeEdit.setEnabled(false);
                        longitudeEdit.setEnabled(false);
                        updateLatLongButton.setEnabled(false);
                        break;
                    case R.id.radio_location_bubble_on:
                        ESSettings.setLocationBubbleUsed(true);
                        latitudeEdit.setEnabled(true);
                        longitudeEdit.setEnabled(true);
                        updateLatLongButton.setEnabled(true);
                        break;
                    default:
                        Log.e(LOG_TAG,"got unexpected id for radio group");
                }
            }
        });

        latitudeEdit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                // Update the coordinates of the location bubble:
                double newLatitude = Double.parseDouble(latitudeEdit.getText().toString());
                double newLongitude = Double.parseDouble(longitudeEdit.getText().toString());
                ESSettings.setLocationBubbleCenter(newLatitude,newLongitude);
            }
        });
        longitudeEdit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                // Update the coordinates of the location bubble:
                double newLatitude = Double.parseDouble(latitudeEdit.getText().toString());
                double newLongitude = Double.parseDouble(longitudeEdit.getText().toString());
                ESSettings.setLocationBubbleCenter(newLatitude,newLongitude);
            }
        });
        updateLatLongButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Update the coordinates of the location bubble:
                double newLatitude = Double.parseDouble(latitudeEdit.getText().toString());
                double newLongitude = Double.parseDouble(longitudeEdit.getText().toString());
                ESSettings.setLocationBubbleCenter(newLatitude,newLongitude);
            }
        });


        // Save prediction files:
        RadioGroup savePredFilesRG = (RadioGroup)findViewById(R.id.radio_group_save_pred_files);
        savePredFilesRG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.radio_save_pred_files_on:
                        ESSettings.setSavePredictionFiles(true);
                        break;
                    case R.id.radio_save_pred_files_off:
                        ESSettings.setSavePredictionFiles(false);
                        break;
                    default:
                        Log.e(LOG_TAG,"got unexpected id for radio group of save-prediction-files");
                }
            }
        });

        // Save user-labels files:
        RadioGroup saveUserLabelsFilesRG = (RadioGroup)findViewById(R.id.radio_group_save_user_labels_files);
        saveUserLabelsFilesRG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.radio_save_user_labels_files_on:
                        ESSettings.setSaveUserLabelsFiles(true);
                        break;
                    case R.id.radio_save_user_labels_files_off:
                        ESSettings.setSaveUserLabelsFiles(false);
                        break;
                    default:
                        Log.e(LOG_TAG,"got unexpected id for radio group of save-prediction-files");
                }
            }
        });


        // Classifier settings:
        RadioGroup classifierSettingsRG = (RadioGroup)findViewById(R.id.radio_group_classifier_setting);
        final EditText classifierTypeEdit = (EditText)findViewById(R.id.edit_classifier_type);
        final EditText classifierNameEdit = (EditText)findViewById(R.id.edit_classifier_name);
        final Button updateClassifierButton = (Button)findViewById(R.id.button_classifier_update);

        classifierSettingsRG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.radio_classifier_setting_off:
//                        ESSettings.setLocationBubbleUsed(false);
                        classifierTypeEdit.setEnabled(false);
                        classifierNameEdit.setEnabled(false);
                        updateClassifierButton.setEnabled(false);
                        break;
                    case R.id.radio_classifier_setting_on:
//                        ESSettings.setLocationBubbleUsed(true);
                        classifierTypeEdit.setEnabled(true);
                        classifierNameEdit.setEnabled(true);
                        updateClassifierButton.setEnabled(true);
                        break;
                    default:
                        Log.e(LOG_TAG,"got unexpected id for radio group");
                }
            }
        });

        classifierTypeEdit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                // Do nothing
            }
        });
        classifierNameEdit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                // Do nothing
            }
        });
        updateClassifierButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Update the classifier settings:
                ESSettings.setClassifierSettings(classifierTypeEdit.getText().toString(),classifierNameEdit.getText().toString());
            }
        });


        setDisplayedContent();
    }

    private void setDisplayedContent() {
/*
        // Set the max storage value:
        int maxStorage = ESSettings.maxStoredExamples();
        displayMaxStorageValue(maxStorage);
        SeekBar maxStorageSeekBar = (SeekBar)findViewById(R.id.max_storage_seek_bar);
        maxStorageSeekBar.setProgress(maxStorage);
*/

        // Set the notification interval value:
        int notificationIntervalInSeconds = ESSettings.notificationIntervalInSeconds();
        int intervalMinutes = notificationIntervalInSeconds / 60;
        displayNotificationIntervalValue(intervalMinutes);
        SeekBar intervalSeekBar = (SeekBar)findViewById(R.id.notification_interval_seek_bar);
        intervalSeekBar.setProgress(intervalMinutes);

        // Set should use notifications or not:
        CheckBox useNearPastNotificationsCB = (CheckBox)findViewById(R.id.checkbox_near_past_notifications);
        useNearPastNotificationsCB.setChecked(ESSettings.useNearPastNotifications());
        CheckBox useNearFutureNotificationsCB = (CheckBox)findViewById(R.id.checkbox_near_future_notifications);
        useNearFutureNotificationsCB.setChecked(ESSettings.useNearFutureNotifications());

        intervalSeekBar.setEnabled(ESSettings.useAnyTypeOfNotifications());


        // Set the number of examples stored before sending:
        int numExamplesStoreBeforeSend = ESSettings.numExamplesStoreBeforeSend();
        displayNumExamplesStoreBeforeSend(numExamplesStoreBeforeSend);
        SeekBar numExSeekBar = (SeekBar)findViewById(R.id.num_examples_store_before_send_seek_bar);
        numExSeekBar.setProgress(numExamplesStoreBeforeSend);

        // Secure communication:
        boolean useHttps = ESNetworkAccessor.getESNetworkAccessor().get_useHttps();
        RadioGroup useHttpsRG = (RadioGroup)findViewById(R.id.radio_group_use_https);
        if (useHttps) {
            useHttpsRG.check(R.id.radio_https_on);
        }
        else {
            useHttpsRG.check(R.id.radio_https_off);
        }

        // Cellular communication:
        boolean allowCellular = ESSettings.isCellularAllowed();
        RadioGroup allowCellularRG = (RadioGroup)findViewById(R.id.radio_group_allow_cellular);
        if (allowCellular) {
            allowCellularRG.check(R.id.radio_allow_cellular_on);
        }
        else {
            allowCellularRG.check(R.id.radio_allow_cellular_off);
        }

        // Location bubble:
        Location bubbleCenter = ESSettings.locationBubbleCenter();
        EditText latitudeEdit = (EditText)findViewById(R.id.edit_location_bubble_latitude);
        EditText longitudeEdit = (EditText)findViewById(R.id.edit_location_bubble_longitude);
        Button updateLatLongButton = (Button)findViewById(R.id.button_location_bubble_update);

        if (bubbleCenter != null) {
            latitudeEdit.setText("" + bubbleCenter.getLatitude());
            longitudeEdit.setText("" + bubbleCenter.getLongitude());
        }

        boolean useLocationBubble = ESSettings.shouldUseLocationBubble();
        RadioGroup useLocationRG = (RadioGroup)findViewById(R.id.radio_group_location_bubble);
        if (useLocationBubble) {
            useLocationRG.check(R.id.radio_location_bubble_on);
            latitudeEdit.setEnabled(true);
            longitudeEdit.setEnabled(true);
            updateLatLongButton.setEnabled(true);
        }
        else {
            useLocationRG.check(R.id.radio_location_bubble_off);
            latitudeEdit.setEnabled(false);
            longitudeEdit.setEnabled(false);
            updateLatLongButton.setEnabled(false);
        }

        // Set the UUID:
        TextView uuid_text = (TextView)findViewById(R.id.uuid_content);
        uuid_text.setText(ESSettings.uuid());

        // should save prediction files:
        RadioGroup savePredFilesRG = (RadioGroup)findViewById(R.id.radio_group_save_pred_files);
        if (ESSettings.savePredictionFiles()) {
            savePredFilesRG.check(R.id.radio_save_pred_files_on);
        }
        else {
            savePredFilesRG.check(R.id.radio_save_pred_files_off);
        }

        // should save user-labels files:
        RadioGroup saveUserLabelsFilesRG = (RadioGroup)findViewById(R.id.radio_group_save_user_labels_files);
        if (ESSettings.saveUserLabelsFiles()) {
            saveUserLabelsFilesRG.check(R.id.radio_save_user_labels_files_on);
        }
        else {
            saveUserLabelsFilesRG.check(R.id.radio_save_user_labels_files_off);
        }

        // Classifier settings:
        EditText classifierTypeEdit = (EditText)findViewById(R.id.edit_classifier_type);
        EditText classifierNameEdit = (EditText)findViewById(R.id.edit_classifier_name);
        Button updateClassifierButton = (Button)findViewById(R.id.button_classifier_update);

        classifierTypeEdit.setText(ESSettings.classifierType());
        classifierNameEdit.setText(ESSettings.classifierName());

        RadioGroup classifierSettingsRG = (RadioGroup)findViewById(R.id.radio_group_classifier_setting);
        classifierSettingsRG.check(R.id.radio_classifier_setting_off);
        classifierTypeEdit.setEnabled(false);
        classifierNameEdit.setEnabled(false);
        updateClassifierButton.setEnabled(false);

        // Which sensors to use:
        CheckBox recordAudioCheckBox = (CheckBox)findViewById(R.id.checkbox_record_audio);
        recordAudioCheckBox.setChecked(ESSettings.shouldRecordAudio());

        CheckBox recordLocationCheckBox = (CheckBox)findViewById(R.id.checkbox_record_location);
        recordLocationCheckBox.setChecked(ESSettings.shouldRecordLocation());

        CheckBox recordWatchCheckBox = (CheckBox)findViewById(R.id.checkbox_record_watch);
        recordWatchCheckBox.setChecked(ESSettings.shouldRecordWatch());

        displaySensorsChecks(true);
        displaySensorsChecks(false);
    }

    private void displaySensorsChecks(boolean hf1lf0) {

        ArrayList<Integer> registeredSensorTypes;
        ArrayList<Integer> sensorTypesToRecord;
        int listviewID;
        if (hf1lf0) {
            registeredSensorTypes = ESSensorManager.getESSensorManager().getRegisteredHighFreqSensorTypes();
            sensorTypesToRecord = ESSettings.highFreqSensorTypesToRecord();
            listviewID = R.id.listview_hf_sensors;
        }
        else {
            registeredSensorTypes = ESSensorManager.getESSensorManager().getRegisteredLowFreqSensorTypes();
            sensorTypesToRecord = ESSettings.lowFreqSensorTypesToRecord();
            listviewID = R.id.listview_lf_sensors;
        }

        SensorCheckAdapter adapter = new SensorCheckAdapter(
                getBaseContext(),
                hf1lf0,registeredSensorTypes,sensorTypesToRecord);
        ListView listView = (ListView)findViewById(listviewID);
        listView.setAdapter(adapter);
        setListViewHeightBasedOnChildren(listView);
    }


    public static void setListViewHeightBasedOnChildren(ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null)
            return;

        int desiredWidth = View.MeasureSpec.makeMeasureSpec(listView.getWidth(), View.MeasureSpec.UNSPECIFIED);
        int totalHeight = 0;
        View view = null;
        for (int i = 0; i < listAdapter.getCount(); i++) {
            view = listAdapter.getView(i, view, listView);
            if (i == 0)
                view.setLayoutParams(new ViewGroup.LayoutParams(desiredWidth, ViewGroup.LayoutParams.WRAP_CONTENT));

            view.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED);
            totalHeight += view.getMeasuredHeight();
        }
        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
    }

    private void displayNotificationIntervalValue(int intervalMinutes) {
        TextView intervalValueView = (TextView)findViewById(R.id.notification_interval_value);
        String intervalString = String.format(Locale.US,"%d min",intervalMinutes);
        intervalValueView.setText(intervalString);
    }

    private void displayNumExamplesStoreBeforeSend(int numExamplesStoreBeforeSend) {
        TextView textView = (TextView)findViewById(R.id.num_examples_store_before_send_value);
        String numStr = String.format(Locale.US,"%d",numExamplesStoreBeforeSend);
        textView.setText(numStr);
    }

/*
    private void displayMaxStorageValue(int maxStorage) {
        TextView maxStorageValue = (TextView)findViewById(R.id.max_storage_value);
        float hours = ((float)maxStorage) / 60f;
        float megaBytes = ((float)maxStorage) * EXAMPLE_SIZE_IN_MEGABYTES;
        String storageString = String.format("%d (~%.2f hr, ~%.2f MB)",maxStorage,hours,megaBytes);
        maxStorageValue.setText(storageString);
    }
*/

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_setting, menu);
        _optionsMenu = menu;
        checkRecordingStateAndSetRedLight();

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

    void onCheckboxClicked(View view) {
        // Is the view now checked?
        boolean checked = ((CheckBox) view).isChecked();

        // Check which checkbox was clicked
        switch (view.getId()) {
            case R.id.checkbox_record_audio:
                ESSettings.setShouldRecordAudio(checked);
                break;
            case R.id.checkbox_record_location:
                ESSettings.setShouldRecordLocation(checked);
                break;
            case R.id.checkbox_record_watch:
                ESSettings.setShouldRecordWatch(checked);
                break;
            default:
                Log.e(LOG_TAG,"onCheckboxClicked was called with unsupported view: " + view.toString());
         }
    }

    private static class SensorCheckAdapter extends BaseAdapter {
        private boolean _hf1lf0;
        private ArrayList<Integer> _registeredSensors;
        private ArrayList<Integer> _sensorsToRecord;
        private LayoutInflater _inflater;

        SensorCheckAdapter(Context context,boolean hf1lf0,ArrayList<Integer> registeredSensors,ArrayList<Integer> sensorsToRecord) {
            this._hf1lf0 = hf1lf0;
            this._registeredSensors = registeredSensors;
            this._sensorsToRecord = sensorsToRecord;
            _inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return _registeredSensors.size();
        }

        /**
         * Get the data item associated with the specified position in the data set.
         *
         * @param position Position of the item whose data we want within the adapter's
         *                 data set.
         * @return The data at the specified position.
         */
        @Override
        public Object getItem(int position) {
            return _registeredSensors.get(position);
        }

        /**
         * Get the row id associated with the specified position in the list.
         *
         * @param position The position of the item within the adapter's data set whose row id we want.
         * @return The id of the item at the specified position.
         */
        @Override
        public long getItemId(int position) {
            return (long)(_registeredSensors.get(position));
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row;
            if (convertView != null) {
                row = convertView;
            }
            else {
                row = _inflater.inflate(R.layout.row_settings_should_record_sensor_or_not, null, false);
            }
            CheckBox checkBox = (CheckBox)row.findViewById(R.id.checkbox_should_record_sensor);

            Integer sensorTypeInteger = _registeredSensors.get(position);
            int sensorType = sensorTypeInteger;
            String sensorNiceName = ESSensorManager.getESSensorManager().getSensorNiceName(sensorType);
            checkBox.setText(sensorNiceName);
            checkBox.setTag(sensorTypeInteger);
            boolean shouldRecord = _sensorsToRecord.contains(sensorType);
            checkBox.setChecked(shouldRecord);

            checkBox.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
                /**
                 * Called when the checked state of a compound button has changed.
                 *
                 * @param buttonView The compound button view whose state has changed.
                 * @param isChecked  The new checked state of buttonView.
                 */
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    Integer sensorTypeInteger = (Integer)buttonView.getTag();
                    ESSettings.setShouldRecordSensor(sensorTypeInteger,isChecked,_hf1lf0);
                }
            });
            return row;
        }

    }
}
