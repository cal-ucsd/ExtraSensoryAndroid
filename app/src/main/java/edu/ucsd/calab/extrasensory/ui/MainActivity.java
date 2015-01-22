package edu.ucsd.calab.extrasensory.ui;

import android.content.Intent;
import android.support.v4.app.FragmentTabHost;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ToggleButton;

import edu.ucsd.calab.extrasensory.ESApplication;
import edu.ucsd.calab.extrasensory.R;


public class MainActivity extends ActionBarActivity {

    private static final String LOG_TAG = "[MainActivity]";

    private ESApplication getESApplication() {
        return (ESApplication)getApplication();
    }

    private FragmentTabHost fragmentTabHost;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.out.println("Creating main activity");
        setContentView(R.layout.activity_main);

        fragmentTabHost = (FragmentTabHost)findViewById(android.R.id.tabhost);
        fragmentTabHost.setup(getApplicationContext(),getSupportFragmentManager(),android.R.id.tabcontent);

        // Add the tabs:
        fragmentTabHost.addTab(fragmentTabHost.newTabSpec("home").setIndicator("Home"),
                HomeFragment.class, null);
        fragmentTabHost.addTab(fragmentTabHost.newTabSpec("history").setIndicator("History"),
                HistoryFragment.class, null);
        fragmentTabHost.addTab(fragmentTabHost.newTabSpec("summary").setIndicator("Summary"),
                SummaryFragment.class,null);

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        Intent intent;
        switch (id) {
            case R.id.action_settings:
                intent = new Intent(getApplicationContext(),SettingsActivity.class);
                startActivity(intent);
                break;
            case R.id.action_active_feedback:
                intent = new Intent(getApplicationContext(),FeedbackActivity.class);
                startActivity(intent);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    public void dataCollectionChanged(View view) {
        ToggleButton button = (ToggleButton)view;
        Log.v(LOG_TAG,"data collection toggle button pressed.");
        if (button.isChecked()) {
            getESApplication().startRecordingSchedule();
        }
        else {
            getESApplication().stopCurrentRecordingAndRecordingSchedule();
        }
    }
}
