package group11.tcss450.uw.edu.datalab.data;

import java.io.Serializable;

/**
 * Encapsulates a tuple from the Color table.
 */
public class ColorEntry implements Serializable {

    private final long mTimeInMillies;
    private final int mColor;

    public ColorEntry(long timeInMillies, int color) {
        mTimeInMillies = timeInMillies;
        mColor = color;
    }

    public long getTimeInMillies() {
        return mTimeInMillies;
    }

    public int getColor() {
        return mColor;
    }

}