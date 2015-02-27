package edu.ucsd.calab.extrasensory.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import edu.ucsd.calab.extrasensory.R;
import edu.ucsd.calab.extrasensory.data.ESContinuousActivity;
import edu.ucsd.calab.extrasensory.data.ESDatabaseAccessor;
import edu.ucsd.calab.extrasensory.data.ESTimestamp;

/**
 * A simple {@link Fragment} subclass.
 */
public class HistoryFragment extends Fragment {

    private static final String LOG_TAG = "[ESHistoryFragment]";

    public HistoryFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //getting today's activities
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY,00);
        cal.set(Calendar.MINUTE,00);
        cal.set(Calendar.SECOND,0);
        cal.set(Calendar.MILLISECOND,0);
        Date d = cal.getTime();

        ESTimestamp startTime = new ESTimestamp(d);

       /* for debug and check timestamp
        DateFormat df = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
        System.out.println("startDATE IS " + df.format(d));
        */
        cal.set(Calendar.HOUR_OF_DAY,23);
        cal.set(Calendar.MINUTE,59);
        cal.set(Calendar.SECOND,59);
        cal.set(Calendar.MILLISECOND,0);
        d = cal.getTime();

        ESTimestamp endTime = new ESTimestamp(d);

        ESContinuousActivity [] activityList = ESDatabaseAccessor.getESDatabaseAccessor().getContinuousActivitiesFromTimeRange(startTime, endTime);
        Log.d(LOG_TAG,"==== Got " + activityList.length + " cont activities: ");
        for (int i= 0; i < activityList.length; i++) {
            Log.d(LOG_TAG, activityList[i].toString());
        }

        HistoryAdapter histAdapter = new HistoryAdapter(getActivity().getBaseContext(), R.layout.rowlayout, activityList);
        // Get the list view and set it using this adapter
        ListView listView = (ListView) getView().findViewById(R.id.listview);

        LayoutInflater inflater = (LayoutInflater) getView().getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View header = inflater.inflate(R.layout.header_rowlayout, null);

        //Set day title
        TextView headerLabel = (TextView)header.findViewById(R.id.txtHeader);

        SimpleDateFormat today = new SimpleDateFormat("EE MMM dd");
        headerLabel.setText("Today- " + today.format(d));

        listView.addHeaderView(header);
        listView.setAdapter(histAdapter);
    }

 /*   @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        // do something with the data
    }*/
}
