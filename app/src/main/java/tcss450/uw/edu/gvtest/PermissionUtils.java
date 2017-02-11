package tcss450.uw.edu.gvtest;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import java.util.ArrayList;

/**
 * A class which allows for checking and/or requesting the apps' required permissions.
 *
 * @author Google Cloud Vision Android sample
 * @version Winter 2017
 */
public class PermissionUtils {

    /**
     * Checks for the apps' required permissions, and requests any that are missing.
     *
     * @param theActivity    the current activity
     * @param theRequestCode an integer constant indicating the requested permission
     * @param thePermissions a list of permissions given to the app
     * @return a boolean indicating the status of the requested permission
     */
    public static boolean requestPermission(
            Activity theActivity, int theRequestCode, String... thePermissions) {
        boolean granted = true;
        ArrayList<String> permissionsNeeded = new ArrayList<>();

        for (String s : thePermissions) {
            int permissionCheck = ContextCompat.checkSelfPermission(theActivity, s);
            boolean hasPermission = (permissionCheck == PackageManager.PERMISSION_GRANTED);
            granted &= hasPermission;
            if (!hasPermission) {
                permissionsNeeded.add(s);
            }
        }

        if (granted) {
            return true;
        } else {
            ActivityCompat.requestPermissions(theActivity,
                    permissionsNeeded.toArray(new String[permissionsNeeded.size()]),
                    theRequestCode);
            return false;
        }
    }

    /**
     * Checks for a specific permissions' status.
     *
     * @param theRequestCode    an integer constant indicating the requested permission
     * @param thePermissionCode an integer constant indicating a specific permission
     * @param theGrantResults   an integer array containing the permission's status
     * @return a boolean indicating the status of the requested permission
     */
    public static boolean permissionGranted(
            int theRequestCode, int thePermissionCode, int[] theGrantResults) {
        if (theRequestCode == thePermissionCode) {
            if (theGrantResults.length > 0 && theGrantResults[0] == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }
}

