package com.github.privacystreams.utils.permission;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.util.Pair;

import com.github.privacystreams.core.Function;
import com.github.privacystreams.core.UQI;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Permission-related utilities in PrivacyStreams.
 */

public class PermissionUtils {
    /**
     * Check if the permission are granted in current context
     * @param context the context instance
     * @param requiredPermission the permissions to check
     * @return true if the permission is granted
     */
    public static boolean checkPermission(Context context, String requiredPermission) {
        Set<String> permissions = new HashSet<>();
        permissions.add(requiredPermission);
        return getDeniedPermissions(context, permissions).isEmpty();
    }

    /**
     * Check if the permissions are granted in current context
     * @param context the context instance
     * @param requiredPermissions the list of permissions to check
     * @return true if all permissions are granted
     */
    public static boolean checkPermissions(Context context, Set<String> requiredPermissions) {
        return getDeniedPermissions(context, requiredPermissions).isEmpty();
    }

    /**
     * Get a list of denied permissions
     * @param context the context instance
     * @param requiredPermissions the list of permissions to check
     * @return the denied permissions
     */
    public static Set<String> getDeniedPermissions(Context context, Set<String> requiredPermissions) {
        Set<String> deniedPermissions = new HashSet<>();
        if (requiredPermissions != null) {
            for (String p : requiredPermissions) {
                if (ContextCompat.checkSelfPermission(context, p) != PackageManager.PERMISSION_GRANTED) {
                    deniedPermissions.add(p);
                }
            }
        }
        return deniedPermissions;
    }

    /**
     * try request permission and evaluate UQI
     * @param uqi UQI instance
     */
    public static void requestPermissionAndEvaluate(UQI uqi, Function<Void, Void> query) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // For Android version M and above, there is chance to request permissions at runtime
            Pair<UQI, Function<Void, Void>> uqiQuery = new Pair<>(uqi, query);
            int requestCode = uqiQuery.hashCode();
            pendingUQIQueries.put(requestCode, uqiQuery);
            Intent permissionRequest = new Intent(uqi.getContext(), PermissionActivity.class);
            permissionRequest.putExtra(PermissionActivity.REQUEST_CODE, requestCode);
            permissionRequest.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            uqi.getContext().startActivity(permissionRequest);
        }
        else {
            // For Android M-, we cannot request permissions at runtime
            uqi.evaluate(query, false);
        }
    }
    static Map<Integer, Pair<UQI, Function<Void, Void>>> pendingUQIQueries = new HashMap<>();

}
