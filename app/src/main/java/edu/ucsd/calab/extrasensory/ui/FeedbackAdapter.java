package edu.ucsd.calab.extrasensory.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import edu.ucsd.calab.extrasensory.R;

/**
 * Created by Jennifer on 2/28/2015.
 */
public class FeedbackAdapter extends ArrayAdapter<String>{
    // This variable gives context as to what is calling the inflater and where it needs
    // to inflate the view
    private final Context context;
    //linked list of ESContinuousActivities that need to be shown
    private final String[] values;
    /**
     * Constructor for Feedback Adapter
     * @param context context from activity
     * @param values The String[] with the values we want to display
     */
    public FeedbackAdapter(Context context, String[] values) {
        super(context, R.layout.activity_rowlayout, values);
        this.context = context;
        this.values = values;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.activity_rowlayout, parent, false);
        TextView textView = (TextView) rowView.findViewById(R.id.firstLine);
        textView.setText(values[position]);

        return rowView;
    }
}
