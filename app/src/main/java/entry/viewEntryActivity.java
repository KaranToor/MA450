package entry;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import tcss450.uw.edu.gvtest.R;

/**
 * @author MA450 Team 11
 * @version Winter 2017
 * Represents an already entered entry being viewed
 */
public class viewEntryActivity extends AppCompatActivity {
    /**
     * Initializes activity
     * @param theSavedInstanceState the instance state.
     */
    @Override
    protected void onCreate(Bundle theSavedInstanceState) {
        super.onCreate(theSavedInstanceState);
        setContentView(R.layout.activity_view_entry);
    }
}
