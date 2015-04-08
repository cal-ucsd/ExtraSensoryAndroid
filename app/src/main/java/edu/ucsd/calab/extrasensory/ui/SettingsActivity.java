package edu.ucsd.calab.extrasensory.ui;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.LinkedHashMap;

import edu.ucsd.calab.extrasensory.R;
import edu.ucsd.calab.extrasensory.data.ESSettings;

public class SettingsActivity extends BaseActivity {

    private static final String LOG_TAG = "[SettingsActivity]";

    private static final float EXAMPLE_SIZE_IN_MEGABYTES = 0.24f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
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


        setDisplayedContent();
    }

    private void setDisplayedContent() {
        // Set the max storage value:
        int maxStorage = ESSettings.maxStoredExamples();
        displayMaxStorageValue(maxStorage);
        SeekBar maxStorageSeekBar = (SeekBar)findViewById(R.id.max_storage_seek_bar);
        maxStorageSeekBar.setProgress(maxStorage);

        // Set the notification interval value:
        int notificationIntervalInSeconds = ESSettings.notificationIntervalInSeconds();
        int intervalMinutes = notificationIntervalInSeconds / 60;
        displayNotificationIntervalValue(intervalMinutes);
        SeekBar intervalSeekBar = (SeekBar)findViewById(R.id.notification_interval_seek_bar);
        intervalSeekBar.setProgress(intervalMinutes);

        // Set the UUID:
        TextView uuid_text = (TextView)findViewById(R.id.uuid_content);
        uuid_text.setText(ESSettings.uuid());
    }

    private void displayNotificationIntervalValue(int intervalMinutes) {
        TextView intervalValueView = (TextView)findViewById(R.id.notification_interval_value);
        String intervalString = String.format("%d min",intervalMinutes);
        intervalValueView.setText(intervalString);
    }

    private void displayMaxStorageValue(int maxStorage) {
        TextView maxStorageValue = (TextView)findViewById(R.id.max_storage_value);
        float hours = ((float)maxStorage) / 60f;
        float megaBytes = ((float)maxStorage) * EXAMPLE_SIZE_IN_MEGABYTES;
        String storageString = String.format("%d (~%.2f hr, ~%.2f MB)",maxStorage,hours,megaBytes);
        maxStorageValue.setText(storageString);
    }

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
