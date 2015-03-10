package edu.ucsd.calab.extrasensory.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.AdapterView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.ucsd.calab.extrasensory.R;
import edu.ucsd.calab.extrasensory.data.ESContinuousActivity;

/**
 * Feedback view for the user to provide the ground truth labels of what they are doing/feeling.
 * Right before starting this view you should call setFeedbackParametersBeforeStartingFeedback()
 * to inform this class what kind of feedback you need.
 */
public class FeedbackActivity extends BaseActivity {

    private static final String LOG_TAG = "[FeedbackActivity]";

    public static final int FEEDBACK_TYPE_ACTIVE = 1;
    public static final int FEEDBACK_TYPE_HISTORY_CONTINUOUS_ACTIVITY = 2;

    Button button;
    /**
     * This parameter type is to be used to transfer parameters to the feedback view,
     * that indicate what kind of feedback to perform and pass relevant data.
     */
    public static final class FeedbackParameters {
        private int _feedbackType;
        private ESContinuousActivity _continuousActivityToEdit;

        /**
         * Default parameters - for doing active feedback
         */
        public FeedbackParameters() {
            _feedbackType = FEEDBACK_TYPE_ACTIVE;
            _continuousActivityToEdit = null;
        }

        /**
         * Parameters for doing feedback to edit the labels of a continuous activity
         * @param continuousActivityToEdit The continuous activity whose labels to edit
         */
        public FeedbackParameters(ESContinuousActivity continuousActivityToEdit) {
            _feedbackType = FEEDBACK_TYPE_HISTORY_CONTINUOUS_ACTIVITY;
            _continuousActivityToEdit = continuousActivityToEdit;
        }
    }

    private static FeedbackParameters _transientInputParameters = null;

    /**
     * Call this static function right before starting the feedback activity.
     * Set the parameters for the feedback.
     * @param inputParameters
     */
    public static void setFeedbackParametersBeforeStartingFeedback(FeedbackParameters inputParameters) {
        _transientInputParameters = inputParameters;
    }
    private FeedbackParameters _parameters = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);
        Log.d(LOG_TAG,"activity being created");

        final String[] values = new String[] { "MainActivity", "Secondary Activities", "Mood", "Valid for" };

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, values);
        ListView listView = (ListView) findViewById(R.id.listview_activity);
        listView.setAdapter(adapter);

       /* List<Map<String, String>> data = new ArrayList<Map<String, String>>();
        for (String act:values) {
            Map<String, String> datum = new HashMap<String, String>(2);
            datum.put("row header", act);
            datum.put("row detail", "item chosen by user");
            data.add(datum);
        }
        SimpleAdapter adapter = new SimpleAdapter(this, data,
                android.R.layout.simple_list_item_2,
                new String[] {"title", "date"},
                new int[] {android.R.id.text1,
                        android.R.id.text2});
        listView.setAdapter(adapter);
*/
        View sendButton = getLayoutInflater().inflate(R.layout.activity_feedback_button, null);
        listView.addFooterView(sendButton);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                                    long arg3) {
                // For Long Duration Toast
                Toast.makeText(getApplicationContext(), values[arg2], Toast.LENGTH_LONG).show();
            }
          /*  public void onItemClick(AdapterView<?> parent, View view,
                int position, long id) {
                // selected item
                String product = ((TextView) view).getText().toString();

                // Launching new Activity on selecting single List Item
                Intent i = new Intent(getApplicationContext(), SelectionFromListActivity.class);
                // sending data to new activity
                i.putExtra("product", product);
                startActivity(i);
            }*/
        });

        addListenerOnButton();
    }

    @Override
    protected void onDestroy() {
        Log.d(LOG_TAG,"activity being destroyed");
        super.onDestroy();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_feedback, menu);
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

    @Override
    protected void onStart() {
        super.onStart();

        // Check if anyone set the transient input parameters:
        if (_transientInputParameters == null) {
            // Then behave as active feedback:
            _parameters = new FeedbackParameters();
        }

        // Quickly, copy the given input parameters and save them to the current Feedback object:
        _parameters = _transientInputParameters;
        _transientInputParameters = null;

        presentFeedbackView();
    }

    private void presentFeedbackView() {

        switch (_parameters._feedbackType) {
            case FEEDBACK_TYPE_ACTIVE:
                Log.i(LOG_TAG,"Opening active feedback view");
                //TODO: present active feedback fields
                break;
            case FEEDBACK_TYPE_HISTORY_CONTINUOUS_ACTIVITY:
                Log.i(LOG_TAG,"Opening feedback view for continuous activity from history");
                Log.d(LOG_TAG,"Got continuous activity: " + _parameters._continuousActivityToEdit);
                //TODO: read the expected parameters of the given continuous activity
                //TODO: present pre-existing labels and the required form fields
                break;
            default:
                Log.e(LOG_TAG,"Got unsupported feedback type: " + _parameters._feedbackType);
                //TODO: how to handle such a case? leave blank/default page? present active feedback? present error message? close activity and go back to previous?
        }
    }

    public void addListenerOnButton() {
        final Context context = this;
        button = (Button) findViewById(R.id.sendfeedback);
        button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                Intent intent = new Intent(context, MainActivity.class);
                startActivity(intent);
            }
        });

    }
 /*   public void onItemClick(int mPosition)
    {
        ListModel tempValues = ( ListModel ) CustomListViewValuesArr.get(mPosition);

        // SHOW ALERT

        Toast.makeText(CustomListView,
                ""+tempValues.getCompanyName()
                        +"
                Image:"+tempValues.getImage()
            +"
        Url:"+tempValues.getUrl(),
        Toast.LENGTH_LONG)
        .show();
    }*/
}
