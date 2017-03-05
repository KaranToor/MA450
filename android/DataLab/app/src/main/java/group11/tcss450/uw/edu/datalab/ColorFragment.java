package group11.tcss450.uw.edu.datalab;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ColorFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 */
public class ColorFragment extends Fragment implements AdapterView.OnItemSelectedListener{

    private OnFragmentInteractionListener mListener;
    public final String POS = "pos";
    Spinner spinner;
    int prev;

    public ColorFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_color, container, false);
        spinner = (Spinner) v.findViewById(R.id.spinner);


        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(),
                R.array.auto_complete_colors, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);
        if (getArguments() != null) {
            int pos = getArguments().getInt(POS);
            prev = pos;
            spinner.setSelection(pos);
        }
        return v;
    }

//    @Override
//    public void onStart() {
//        super.onStart();
//        if (getArguments() != null){
//            int pos = getArguments().getInt(POS);
//            spinner.setSelection(pos);
//        }
//    }
//    @Override
//    public void onResume() {
//        super.onResume();
//        if (getArguments() != null){
//            int pos = getArguments().getInt(POS);
//            spinner.setSelection(pos);
//        }
//    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (mListener != null && position != prev) {
            mListener.onFragmentInteraction(Color.parseColor(getResources().getStringArray(R.array.color_strings)[position]), position);
            prev = position;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(int color, int position);
    }
}
