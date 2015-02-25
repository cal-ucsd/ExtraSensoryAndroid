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
import android.widget.TabHost;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import edu.ucsd.calab.extrasensory.ESApplication;
import edu.ucsd.calab.extrasensory.R;
import edu.ucsd.calab.extrasensory.sensors.ESSensorManager;


public class MainActivity extends BaseActivity {

    private static final String LOG_TAG = "[MainActivity]";

    private FragmentTabHost _fragmentTabHost;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(LOG_TAG,"Creating main activity");
        setContentView(R.layout.activity_main);

        _fragmentTabHost = (FragmentTabHost)findViewById(android.R.id.tabhost);
        _fragmentTabHost.setup(getApplicationContext(), getSupportFragmentManager(), android.R.id.tabcontent);

        // Add the tabs:
        _fragmentTabHost.addTab(_fragmentTabHost.newTabSpec(getString(R.string.tab_home_tag)).setIndicator(getString(R.string.tab_home_indicator)),
                HomeFragment.class, null);
        _fragmentTabHost.addTab(_fragmentTabHost.newTabSpec(getString(R.string.tab_history_tag)).setIndicator(getString(R.string.tab_history_indicator)),
                HistoryFragment.class, null);
        _fragmentTabHost.addTab(_fragmentTabHost.newTabSpec(getString(R.string.tab_summary_tag)).setIndicator(getString(R.string.tab_summary_indicator)),
                SummaryFragment.class,null);

        // Set the tab host to respond to tab presses:
        _fragmentTabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {

                if (getString(R.string.tab_home_tag).equals(tabId)) {
                    Log.i(LOG_TAG, "User switched to Home tab");
                    HomeFragment homeFragment = (HomeFragment) getSupportFragmentManager().findFragmentByTag(tabId);
                    homeFragment.onResume();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkGooglePlay();
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


}
