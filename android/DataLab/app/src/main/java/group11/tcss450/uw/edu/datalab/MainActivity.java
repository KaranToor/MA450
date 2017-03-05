package group11.tcss450.uw.edu.datalab;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;

import group11.tcss450.uw.edu.datalab.data.ColorDB;

public class MainActivity extends AppCompatActivity implements  ColorFragment.OnFragmentInteractionListener{
    private SharedPreferences mPrefs;
    ColorFragment cf;
    ColorDB mCourseDB;
    Menu mMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        mCourseDB.delete();
        setSupportActionBar(toolbar);
        mPrefs = getSharedPreferences(getString(R.string.SHARED_PREFS), Context.MODE_PRIVATE);
        findViewById(R.id.content_main).
                setBackgroundColor(mPrefs.getInt(getString(R.string.COLOR), Color.RED));
        int pos = mPrefs.getInt(getString(R.string.POSITION), 0);
        if(savedInstanceState == null) {
            if (findViewById(R.id.content_main) != null) {
                cf = new ColorFragment();
                if (pos != 0) {
                    Bundle args = new Bundle();
                    args.putInt(getString(R.string.POSITION), pos);
                    cf.setArguments(args);
                }
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.content_main, cf)
                        .commit();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        mMenu = menu;
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        cf.getArguments().putInt(getString(R.string.POSITION),
                mPrefs.getInt(getString(R.string.POSITION), 0));
        //noinspection SimplifiableIfStatement
        if (id == R.id.file_menu_item) {
            FragmentTransaction transaction = getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.content_main, new FileFragment(), "FILE")
                    .addToBackStack(null);
            // Commit the transaction

            transaction.commit();
//            onPrepareOptionsMenu(mMenu);
            mMenu.findItem(R.id.delete_menu).setVisible(true);
        }else if (id == R.id.db_menu_item) {
            if (mCourseDB == null) {
//                mCourseDB.getmSQLiteDatabase().execSQL(getString(R.string.CREATE_COLORS_SQL));
                mCourseDB = new ColorDB(this);
            }
//            cf.getArguments().putInt(getString(R.string.POSITION),
//                    mPrefs.getInt(getString(R.string.POSITION), 0));
            DBFragment dbf = new DBFragment();
            Bundle args = new Bundle();
            args.putSerializable(getString(R.string.DB_NAME),
                    (Serializable) mCourseDB.getColors());
            dbf.setArguments(args);
            FragmentTransaction transaction = getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.content_main, dbf, "DB")
                    .addToBackStack(null);
            // Commit the transaction
            transaction.commit();
            mMenu.findItem(R.id.db_delete_menu).setVisible(true);
        } else if (id == R.id.delete_menu){
            try {
                FileOutputStream fileOutputStream = openFileOutput(getString(R.string.COLOR_SELECTIONS), 0);
                try {
                    FileWriter fileWriter = new FileWriter(fileOutputStream.getFD());
                    fileWriter.write("");
                    fileWriter.flush();
                    fileWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    fileOutputStream.getFD().sync();
                    fileOutputStream.close();
//                    getSupportFragmentManager().popBackStack();
                    FileFragment myFragment = (FileFragment) getSupportFragmentManager().findFragmentByTag("FILE");
                    if (myFragment != null){
                        ((LinearLayout)myFragment.getView()).removeAllViews();
                    }
                    item.setVisible(false);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (id == R.id.db_delete_menu){
            DBFragment myFragment = (DBFragment) getSupportFragmentManager().findFragmentByTag("DB");
            if (myFragment != null){
                ((LinearLayout)myFragment.getView()).removeAllViews();
                mCourseDB.delete();
            }
            item.setVisible(false);
        }
//        onPrepareOptionsMenu(mMenu);
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu (Menu menu) {
        try {
            InputStream inputStream = openFileInput(
                    getString(R.string.COLOR_SELECTIONS));
            if (inputStream.read() == -1) {
                //menu.getItem(1).setEnabled(false);
                // You can also use something like:
                 menu.findItem(R.id.file_menu_item).setEnabled(false);
            } else {
                menu.findItem(R.id.file_menu_item).setEnabled(true);
            }
            FileFragment myFragment = (FileFragment) getSupportFragmentManager().findFragmentByTag("FILE");
            if (myFragment != null && myFragment.isVisible()) {
                // add your code here
                menu.findItem(R.id.delete_menu).setVisible(true);
            } else {
                menu.findItem(R.id.delete_menu).setVisible(false);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return true;
    }

    @Override
    public void onFragmentInteraction(int color, int position) {
        this.findViewById(R.id.content_main).setBackgroundColor(color);
        saveToSharedPrefs(color, position);
        saveToFile(color, position);
        saveToSqlite(color);
    }

    private void saveToSharedPrefs(int color, int pos) {
        mPrefs.edit().putInt(getString(R.string.COLOR), color).apply();
        mPrefs.edit().putInt(getString(R.string.POSITION), pos).apply();
    }

    private void saveToFile(int color, int pos) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(
                    openFileOutput(getString(R.string.COLOR_SELECTIONS)
                            , Context.MODE_APPEND));
            outputStreamWriter.append("color = r:" );
            outputStreamWriter.append(Color.red(color) + ", g:");
            outputStreamWriter.append(Color.green(color) + ", b:");
            outputStreamWriter.append(Color.blue(color) + ", a:");
            outputStreamWriter.append(Color.alpha(color) + "\n");
            outputStreamWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void saveToSqlite(int color) {
        if (mCourseDB == null) {
            mCourseDB = new ColorDB(this);
        }
        mCourseDB.insertColor(System.currentTimeMillis(), color);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        onPrepareOptionsMenu(mMenu);
    }
}
