package group11.tcss450.uw.edu.datalab;


import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

import group11.tcss450.uw.edu.datalab.data.ColorEntry;


/**
 * A simple {@link Fragment} subclass.
 */
public class DBFragment extends Fragment {
    LinearLayout mLayout;

    public DBFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mLayout = (LinearLayout) inflater.inflate(R.layout.fragment_db, container, false);
        return mLayout;
    }
    @Override
    public void onStart() {
        super.onStart();
        if (getArguments() != null) {
            ArrayList<ColorEntry> colors =
                    (ArrayList<ColorEntry>) getArguments().getSerializable(
                            getString(R.string.DB_NAME));
            for (ColorEntry c : colors) {
                TextView tv = new TextView(getContext());
                Calendar cal = new GregorianCalendar();
                cal.setTimeInMillis(c.getTimeInMillies());
                tv.setText(
                        new SimpleDateFormat(getString(R.string.date_format)).
                                format(cal.getTime())
                                + "\n\t" + colorToString(c.getColor()));
                mLayout.addView(tv);
            }
        }
    }

    private String colorToString(int color) {
        return " r:" +
                Color.red(color) + ", g:" +
                Color.green(color) + ", b:" +
                Color.blue(color) + ", a:" +
                Color.alpha(color);
    }

}
