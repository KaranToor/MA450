package group11.tcss450.uw.edu.datalab.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;

import java.util.ArrayList;
import java.util.List;

import group11.tcss450.uw.edu.datalab.R;


public class ColorDB {

    public static final int DB_VERSION = 1;
    private final String DB_NAME;
    private final String COLORS_TABLE;
    private final String[] COLUMN_NAMES;

    private ColorDBHelper mColorDBHelper;

    public SQLiteDatabase getmSQLiteDatabase() {
        return mSQLiteDatabase;
    }

    public void delete(){

        mSQLiteDatabase.execSQL(mcontext.getString(R.string.DROP_COLORS_SQL));
        mSQLiteDatabase.execSQL(mcontext.getString(R.string.CREATE_COLORS_SQL));
    }
    private SQLiteDatabase mSQLiteDatabase;
    Context mcontext;

    public ColorDB(Context context) {

        COLUMN_NAMES = context.getResources().getStringArray(R.array.DB_COLUMN_NAMES);
        DB_NAME = context.getString(R.string.DB_NAME);
        COLORS_TABLE = context.getString(R.string.TABLE_NAME);
        mcontext = context;
        mColorDBHelper = new ColorDBHelper(
                context, DB_NAME, null, DB_VERSION);
        mSQLiteDatabase = mColorDBHelper.getWritableDatabase();
    }

    public boolean insertColor(Long time, int color) {
        ContentValues contentValues = new ContentValues();

        contentValues.put(COLUMN_NAMES[0], time);
        contentValues.put(COLUMN_NAMES[1], Color.red(color));
        contentValues.put(COLUMN_NAMES[2], Color.green(color));
        contentValues.put(COLUMN_NAMES[3], Color.blue(color));

        long rowId = mSQLiteDatabase.insert(COLORS_TABLE, null, contentValues);
        return rowId != -1;
    }

    public void closeDB() {
        mSQLiteDatabase.close();
    }


    /**
     * Returns the list of ColorEntry objects from the local Colors table.
     * @return list
     */
    public List<ColorEntry> getColors() {


        Cursor c = mSQLiteDatabase.query(
                COLORS_TABLE,  // The table to query
                COLUMN_NAMES,                               // The COLUMN_NAMES to return
                null,                                // The COLUMN_NAMES for the WHERE clause
                null,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                null                                 // The sort order
        );
        c.moveToFirst();
        List<ColorEntry> list = new ArrayList<ColorEntry>();
        for (int i=0; i<c.getCount(); i++) {
            Long id = c.getLong(0);
            int red = c.getInt(1);
            int green = c.getInt(2);
            int blue = c.getInt(3);
            list.add(new ColorEntry(id, Color.argb(255, red, green, blue)));
            c.moveToNext();
        }

        return list;
    }

    class ColorDBHelper extends SQLiteOpenHelper {

        private final String CREATE_COLOR_SQL;

        private final String DROP_COLOR_SQL;

        public ColorDBHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
            super(context, name, factory, version);
            CREATE_COLOR_SQL = context.getString(R.string.CREATE_COLORS_SQL);
            DROP_COLOR_SQL = context.getString(R.string.DROP_COLORS_SQL);

        }

        @Override
        public void onCreate(SQLiteDatabase sqLiteDatabase) {
            sqLiteDatabase.execSQL(CREATE_COLOR_SQL);
        }

        @Override
        public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
            sqLiteDatabase.execSQL(DROP_COLOR_SQL);
            onCreate(sqLiteDatabase);
        }
    }

}
