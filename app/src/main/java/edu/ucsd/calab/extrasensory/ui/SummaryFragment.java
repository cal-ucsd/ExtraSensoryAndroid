package edu.ucsd.calab.extrasensory.ui;


import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ActionMenuView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import edu.ucsd.calab.extrasensory.ESApplication;
import edu.ucsd.calab.extrasensory.R;
import edu.ucsd.calab.extrasensory.data.ESDatabaseAccessor;
import edu.ucsd.calab.extrasensory.data.ESLabelStrings;

/**
 * A simple {@link Fragment} subclass.
 */
public class SummaryFragment extends BaseTabFragment {

    private static final String LOG_TAG = "[ES-SummaryFragment]";

    private ListView _listView;

    public SummaryFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_summary, container, false);
        _listView = (ListView)view.findViewById(R.id.listview_summary_items);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        calculateAndPresentSummary();
    }

    @Override
    protected void reactToRecordsUpdatedEvent() {
        super.reactToRecordsUpdatedEvent();
        Log.d(LOG_TAG, "reacting to records-update");
        calculateAndPresentSummary();
    }

    private void calculateAndPresentSummary() {
        String[] labels = ESLabelStrings.getMainActivities();
        Map<String,Integer> labelCounts = ESDatabaseAccessor.getESDatabaseAccessor().getLabelCounts(null, ESDatabaseAccessor.ESLabelType.ES_LABEL_TYPE_MAIN);
        int maxCount = 0;

        List<SummaryItem> items = new ArrayList<>(labels.length);
        for (int i = 0; i < labels.length; i ++) {
            String label = labels[i];
            int count = labelCounts.containsKey(label) ? labelCounts.get(label) : 0;
            int color = ESLabelStrings.getColorForMainActivity(label);
            items.add(new SummaryItem(label,count,color));

            if (count > maxCount) {
                maxCount = count;
            }
        }

        if (_listView.getAdapter() == null) {
            _listView.setAdapter(new SummaryListAdapter(getActivity(),items,maxCount));
        }
        else {
            ((SummaryListAdapter)_listView.getAdapter()).refreshList(items, maxCount);
        }
    }

    private static class SummaryItem {
        private String _label;
        private int _count;
        private int _color;
        public SummaryItem(String label,int count,int color) {
            _label = label;
            _count = count;
            _color = color;
        }
    }

    private static class SummaryListAdapter extends ArrayAdapter<SummaryItem> {

        private List<SummaryItem> _items;
        private int _maxCount;

        /**
         * Constructor
         *
         * @param context The current context.
         * @param objects The objects to represent in the ListView.
         * @param maxCount The maximal count among the items.
         */
        public SummaryListAdapter(Context context, List<SummaryItem> objects,int maxCount) {
            super(context, R.layout.row_in_summary_list, R.id.text_activity_name_in_summary_row, objects);
            _items = objects;
            _maxCount = maxCount;
        }

        public void refreshList(List<SummaryItem> items,int maxCount) {
            _items = items;
            _maxCount = maxCount;
            notifyDataSetChanged();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View rowView = super.getView(position, convertView, parent);
            SummaryItem item = _items.get(position);

            TextView colorLegend = (TextView) rowView.findViewById(R.id.text_color_legend_for_row_in_summary);
            colorLegend.setBackgroundColor(item._color);

            TextView labelName = (TextView) rowView.findViewById(R.id.text_activity_name_in_summary_row);
            labelName.setText(item._label);

            TextView countText = (TextView) rowView.findViewById(R.id.text_activity_count_in_summary_row);
            countText.setText("" + item._count);

            TextView colorBar = (TextView) rowView.findViewById(R.id.text_colored_bar_in_summary_row);
            colorBar.setBackgroundColor(item._color);
            float fractionOfMax = (float)item._count / (float)_maxCount;
            android.widget.LinearLayout.LayoutParams newParams = new LinearLayout.LayoutParams(0,colorBar.getLayoutParams().height,fractionOfMax);
            colorBar.setLayoutParams(newParams);

            return rowView;
        }



    }
}
