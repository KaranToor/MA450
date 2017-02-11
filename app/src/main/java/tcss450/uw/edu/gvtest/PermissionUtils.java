package tcss450.uw.edu.gvtest;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import java.util.ArrayList;
/**
 * A class which allows for checking and/or requesting the apps' required permissions.
 * @author MA450 Team 11
 * @version Winter 2017
 */
public class PermissionUtils {

    /**
     * Checks for the apps' required permissions, and requests any that are missing.
     *
     * @param  activity the current activity
     * @param  requestCode an integer constant indicating the requested permission
     * @param permissions a list of permissions given to the app
     * @return      a boolean indicating the status of the requested permission
     */
    public static boolean requestPermission(
            Activity activity, int requestCode, String... permissions) {
        boolean granted = true;
        ArrayList<String> permissionsNeeded = new ArrayList<>();

        for (String s : permissions) {
            int permissionCheck = ContextCompat.checkSelfPermission(activity, s);
            boolean hasPermission = (permissionCheck == PackageManager.PERMISSION_GRANTED);
            granted &= hasPermission;
            if (!hasPermission) {
                permissionsNeeded.add(s);
            }
        }

        if (granted) {
            return true;
        } else {
            ActivityCompat.requestPermissions(activity,
                    permissionsNeeded.toArray(new String[permissionsNeeded.size()]),
                    requestCode);
            return false;
        }
    }

    /**
     * Checks for a specific permissions' status.
     *
     * @param requestCode an integer constant indicating the requested permission
     * @param permissionCode an integer constant indicating a specific permission
     * @param grantResults an integer array containing the permission's status
     * @return      a boolean indicating the status of the requested permission
     */
    public static boolean permissionGranted(
            int requestCode, int permissionCode, int[] grantResults) {
        if (requestCode == permissionCode) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }
}

