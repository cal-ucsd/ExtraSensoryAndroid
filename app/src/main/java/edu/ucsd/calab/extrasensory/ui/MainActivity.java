package edu.ucsd.calab.extrasensory.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.FragmentTabHost;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ToggleButton;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import edu.ucsd.calab.extrasensory.ESApplication;
import edu.ucsd.calab.extrasensory.R;
import edu.ucsd.calab.extrasensory.sensors.ESSensorManager;


public class MainActivity extends ActionBarActivity {

    private static final String LOG_TAG = "[MainActivity]";

    private ESApplication getESApplication() {
        return (ESApplication)getApplication();
    }

    private FragmentTabHost fragmentTabHost;
    private Menu _optionsMenu = null;

    private BroadcastReceiver _broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                Log.e(LOG_TAG,"Broadcast receiver caught null intent");
                return;
            }
            if (ESSensorManager.BROADCAST_RECORDING_STATE_CHANGED.equals(intent.getAction())) {
                Log.v(LOG_TAG,"Caught recording state broadcast");
                checkRecordingStateAndSetRedLight();
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(LOG_TAG,"Creating main activity");
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
    protected void onResume() {
        super.onResume();
        checkGooglePlay();
        checkRecordingStateAndSetRedLight();
        LocalBroadcastManager.getInstance(this).registerReceiver(_broadcastReceiver,new IntentFilter(ESSensorManager.BROADCAST_RECORDING_STATE_CHANGED));
    }

    private void checkGooglePlay() {
        int googleServicesResult = GooglePlayServicesUtil.isGooglePlayServicesAvailable(ESApplication.getTheAppContext());
        if (googleServicesResult == ConnectionResult.SUCCESS) {
            Log.i(LOG_TAG, "We have google play services");
        }
        else {
            Log.i(LOG_TAG,"We don't have required google play services");
            final PendingIntent pendingIntent = GooglePlayServicesUtil.getErrorPendingIntent(googleServicesResult,this,0);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.need_google_play_services_title);
            builder.setMessage(R.string.need_google_play_services_message);
            builder.setPositiveButton(R.string.set_google_play_button, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    Log.v(LOG_TAG,"User clicked to go to Google Play Services to update required services");
                    try {
                        pendingIntent.send();
                    } catch (PendingIntent.CanceledException e) {
                        Log.e(LOG_TAG,"Failed redirecting user to Google Play");
                        e.printStackTrace();
                    }
                }
            });
            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    Log.i(LOG_TAG,"User clicked 'cancel' for Google Play");
                }
            });

            Dialog dialog = builder.create();
            dialog.show();
        }

    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(_broadcastReceiver);
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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

    private void checkRecordingStateAndSetRedLight() {
        if (_optionsMenu == null) {
            // Then there is no place to hide/show the red light:
            return;
        }

        MenuItem redLight = _optionsMenu.findItem(R.id.action_red_circle);
        if (ESSensorManager.getESSensorManager().is_recordingRightNow()) {
            redLight.setVisible(true);
            Log.i(LOG_TAG, "Recording now - turning on red light");
        }
        else {
            redLight.setVisible(false);
            Log.i(LOG_TAG,"Not recording - turning off red light");
        }
    }
}
