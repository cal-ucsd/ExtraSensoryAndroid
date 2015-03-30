package edu.ucsd.calab.extrasensory.ui;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import edu.ucsd.calab.extrasensory.R;
import edu.ucsd.calab.extrasensory.data.ESSettings;

public class SettingsActivity extends BaseActivity {

    private static float EXAMPLE_SIZE_IN_MEGABYTES = 0.24f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        setDisplayedContent();
    }

    private void setDisplayedContent() {
        // Set the max storage value:
        int maxStorage = ESSettings.maxStoredExamples();
        TextView maxStorageValue = (TextView)findViewById(R.id.max_storage_value);
        float hours = ((float)maxStorage) / 60f;
        float megaBytes = ((float)maxStorage) * EXAMPLE_SIZE_IN_MEGABYTES;
        String storageString = String.format("%d (~%.2f hr, ~%.2f MB)",maxStorage,hours,megaBytes);
        maxStorageValue.setText(storageString);

        // Set the UUID:
        TextView uuid_text = (TextView)findViewById(R.id.uuid_content);
        uuid_text.setText(ESSettings.uuid());
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
