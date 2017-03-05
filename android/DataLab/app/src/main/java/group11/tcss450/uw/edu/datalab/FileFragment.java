package group11.tcss450.uw.edu.datalab;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;


/**
 * A simple {@link Fragment} subclass.
 */
public class FileFragment extends Fragment {


    public FileFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        LinearLayout view = (LinearLayout) inflater.inflate(
                R.layout.fragment_file, container, false);
        try {
            InputStream inputStream = getActivity().openFileInput(
                    getString(R.string.COLOR_SELECTIONS));
            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();
                while ((receiveString = bufferedReader.readLine()) != null) {
                    TextView tv = new TextView(getActivity());
                    tv.setText(receiveString);
                    view.addView(tv);
                }
//                this.getActivity().supportInvalidateOptionsMenu();
                inputStream.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return view;
    }

}
