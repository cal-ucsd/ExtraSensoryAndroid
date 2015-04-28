package edu.ucsd.calab.extrasensory.ui;

import android.location.Location;
import android.os.Bundle;
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

/*
        SeekBar maxStorageSeekBar = (SeekBar)findViewById(R.id.max_storage_seek_bar);
        maxStorageSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int newMaxStorageValue = Math.max(2,progress);
                ESSettings.setMaxStoredExamples(newMaxStorageValue);
                Log.d(LOG_TAG,"Max storage changed to " + newMaxStorageValue);
                displayMaxStorageValue(newMaxStorageValue);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
*/

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

        updateLatLongButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Update the coordinates of the location bubble:
                double newLatitude = Double.parseDouble(latitudeEdit.getText().toString());
                double newLongitude = Double.parseDouble(longitudeEdit.getText().toString());
                ESSettings.setLocationBubbleCenter(newLatitude,newLongitude);
            }
        });

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

        boolean useHttps = ESNetworkAccessor.getESNetworkAccessor().get_useHttps();
        RadioGroup useHttpsRG = (RadioGroup)findViewById(R.id.radio_group_use_https);
        if (useHttps) {
            useHttpsRG.check(R.id.radio_https_on);
        }
        else {
            useHttpsRG.check(R.id.radio_https_off);
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
