package edu.ucsd.calab.extrasensory.ui;

import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import edu.ucsd.calab.extrasensory.R;
import edu.ucsd.calab.extrasensory.data.ESSettings;
import edu.ucsd.calab.extrasensory.network.ESNetworkAccessor;

public class SettingsActivity extends BaseActivity {

    private static final String LOG_TAG = "[SettingsActivity]";

    private static final float EXAMPLE_SIZE_IN_MEGABYTES = 0.24f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        // Notification interval:
        SeekBar notificationIntervalSeekBar = (SeekBar)findViewById(R.id.notification_interval_seek_bar);
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


        // Home sensing:
        RadioGroup useHomeSensingRG = (RadioGroup)findViewById(R.id.radio_group_home_sensing);
        useHomeSensingRG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.radio_home_sensing_on:
                        ESSettings.setHomeSensingUsed(true);
                        break;
                    case R.id.radio_home_sensing_off:
                        ESSettings.setHomeSensingUsed(false);
                        break;
                    default:
                        Log.e(LOG_TAG,"got unexpected id for radio group of home sensing");
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
                        Log.e(LOG_TAG,"got unexpected id for radio group of home sensing");
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

        // Home sensing:
        boolean useHomeSensing = ESSettings.isHomeSensingRelevant();
        RadioGroup useHomeSensingRG = (RadioGroup)findViewById(R.id.radio_group_home_sensing);
        if (useHomeSensing) {
            useHomeSensingRG.check(R.id.radio_home_sensing_on);
        }
        else {
            useHomeSensingRG.check(R.id.radio_home_sensing_off);
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
    }

    private void displayNotificationIntervalValue(int intervalMinutes) {
        TextView intervalValueView = (TextView)findViewById(R.id.notification_interval_value);
        String intervalString = String.format("%d min",intervalMinutes);
        intervalValueView.setText(intervalString);
    }

    private void displayNumExamplesStoreBeforeSend(int numExamplesStoreBeforeSend) {
        TextView textView = (TextView)findViewById(R.id.num_examples_store_before_send_value);
        String numStr = String.format("%d",numExamplesStoreBeforeSend);
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
}
