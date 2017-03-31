package com.github.privacystreams.core;

import android.content.Context;

import com.github.privacystreams.core.exceptions.PSException;
import com.github.privacystreams.core.providers.MStreamProvider;
import com.github.privacystreams.core.providers.SStreamProvider;
import com.github.privacystreams.core.purposes.Purpose;
import com.github.privacystreams.utils.Logging;
import com.github.privacystreams.utils.permission.PermissionUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * The unified query interface for all kinds of personal data.
 * You will need to construct an UQI with <code>UQI uqi = new UQI(context);</code>
 * Then, to get multi-item stream data, call <code>uqi.getData</code> ({@link #getData(MStreamProvider, Purpose)});
 * To get single-item data, call <code>uqi.getData</code> ({@link #getData(SStreamProvider, Purpose)}).
 */

public class UQI {
    private Map<Function<Void, ?>, Purpose> provider2Purpose;
    private Set<Function<Void, Void>> queries;

    private Purpose getPurposeOfQuery(Function<Void, Void> query) {
        return this.provider2Purpose.get(query.getHead());
    }

    private transient Context context;
    public Context getContext() {
        return this.context;
    }
    public void setContext(Context context) { this.context = context; }

    private transient Gson gson;
    public Gson getGson() {
        return this.gson;
    }

    private transient PSException exception;
    public PSException getException() {
        return exception;
    }

    public UQI(Context context) {
        this.context = context;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.provider2Purpose = new HashMap<>();
        this.queries = new HashSet<>();
    }

    /**
     * Get a MStream from a provider with a purpose.
     * For example, using <code>uqi.getData(Contact.getAll(), Purpose.FEATURE("..."))</code> will return a stream of contacts.
     * @param mStreamProvider the function to provide the personal data stream, e.g. GeoLocation.asUpdates().
     * @param purpose the purpose of personal data use, e.g. Purpose.ADS("xxx").
     * @return a multi-item stream
     */
    public MStream getData(MStreamProvider mStreamProvider, Purpose purpose) {
        this.provider2Purpose.put(mStreamProvider, purpose);
        return new MStream(this, mStreamProvider);
    }

    /**
     * Get an SStream from a provider with a purpose
     * For example, using <code>uqi.getData(GeoLocation.asLastKnown(), Purpose.FEATURE("..."))</code> will return a stream that contains one location item.
     * @param sStreamProvider the function to provide the personal data item, e.g. Location.asLastKnown(), Audio.record(100).
     * @param purpose the purpose of personal data use, e.g. Purpose.ADS("xxx").
     * @return a single-item stream
     */
    public SStream getData(SStreamProvider sStreamProvider, Purpose purpose) {
        this.provider2Purpose.put(sStreamProvider, purpose);
        return new SStream(this, sStreamProvider);
    }

    /**
     * Stop all query in this UQI.
     */
    public void stopAll() {
        Logging.debug("Trying to stop all PrivacyStreams Queries.");

        this.exception = PSException.INTERRUPTED("Stopped by app.");
        for (Function<Void, Void> query : queries) {
            query.cancel(this);
        }
    }

    /**
     * Evaluate current UQI.
     *
     * @param query the query to evaluate.
     * @param retry whether to try again if the permission is denied.
     */
    public void evaluate(Function<Void, Void> query, boolean retry) {
        Logging.debug("Trying to evaluate PrivacyStreams Query.");
        Logging.debug("Purpose: " + this.getPurposeOfQuery(query));
        Logging.debug("Query: " + query);
        Logging.debug("Required Permissions: " + query.getRequiredPermissions());

        this.queries.add(query);

        if (PermissionUtils.checkPermissions(this.context, query.getRequiredPermissions())) {
            Logging.debug("Evaluating...");
            query.apply(this, null);
        }
        else if (retry) {
            // If retry is true, try to request permissions
            Logging.debug("Permission denied, retrying...");
            PermissionUtils.requestPermissionAndEvaluate(this, query);
        }
        else {
            // If retry is false, cancel all functions.
            Logging.debug("Permission denied, cancelling...");
            Set<String> deniedPermissions = PermissionUtils.getDeniedPermissions(this.context, query.getRequiredPermissions());
            this.exception = PSException.PERMISSION_DENIED(deniedPermissions.toArray(new String[]{}));
            query.cancel(this);
//            this.context = null; // remove context
        }
    }
}
