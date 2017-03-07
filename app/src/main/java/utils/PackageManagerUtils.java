package utils;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.support.annotation.NonNull;

import com.google.common.io.BaseEncoding;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Provides utility logic for getting the app's SHA1 signature. Used with restricted API keys.
 *
 * @author Google Cloud Vision Android sample
 * @version Winter 2017
 */
public class PackageManagerUtils {

    /**
     * Gets the SHA1 signature, hex encoded for inclusion with Google Cloud Platform API requests
     *
     * @param thePackageName Identifies the APK whose signature should be extracted.
     * @return a lowercase, hex-encoded
     */
    public static String getSignature(@NonNull PackageManager thePM, @NonNull String thePackageName) {
        try {
            PackageInfo packageInfo = thePM.getPackageInfo(thePackageName, PackageManager.GET_SIGNATURES);
            if (packageInfo == null
                    || packageInfo.signatures == null
                    || packageInfo.signatures.length == 0
                    || packageInfo.signatures[0] == null) {
                return null;
            }
            return signatureDigest(packageInfo.signatures[0]);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    /**
     * Returns the signature as a string.
     *
     * @param theSig The signature to process.
     * @return The String of a signature.
     */
    private static String signatureDigest(Signature theSig) {
        byte[] signature = theSig.toByteArray();
        try {
            MessageDigest md = MessageDigest.getInstance("SHA1");
            byte[] digest = md.digest(signature);
            return BaseEncoding.base16().lowerCase().encode(digest);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
}