package com.github.privacystreams.location;

import android.Manifest;
import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresPermission;
import android.util.Log;

import com.github.privacystreams.core.UQI;
import com.github.privacystreams.core.providers.MStreamProvider;
import com.github.privacystreams.utils.Assertions;
import com.github.privacystreams.utils.Logging;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;


/**
 * Provide location updates with Google API.
 */
class GoogleLocationProvider extends MStreamProvider implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener  {

    private static final String LOG_TAG = "[GoogleLocationProvider]";

    private final long interval;
    private final String level;

    protected GoogleLocationProvider(long interval, String level) {
        this.interval = interval;
        this.level = Assertions.notNull("level", level);

        this.addParameters(interval, level);
        if (GeoLocation.Levels.METER.equals(level)) {
            this.addRequiredPermissions(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        else {
            this.addRequiredPermissions(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
    }

    private transient GoogleApiClient mGoogleApiClient = null;

    @Override
    protected void provide() {
        mGoogleApiClient = new GoogleApiClient.Builder(getContext())
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        if (!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }
    }

    //to get the location change
    @Override
    public void onLocationChanged(Location location) {
        this.output(new GeoLocation(location));
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        startLocationUpdate();
    }

    @Override
    protected void onCancelled(UQI uqi) {
        super.onCancelled(uqi);
        stopLocationUpdate();
    }

    @Override
    public void onConnectionSuspended(int i) {
        if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_NETWORK_LOST) {
            Logging.debug(LOG_TAG + "Connection lost. Cause: Network Lost.");
        } else if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_SERVICE_DISCONNECTED) {
            Logging.debug(LOG_TAG + "Connection lost. Cause: Service Disconnected");
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Logging.warn(LOG_TAG + "Not connected with GoogleApiClient");
        this.finish();
    }


    private void startLocationUpdate() {
        long fastInterval = interval / 2;

        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(interval);
        mLocationRequest.setFastestInterval(fastInterval);

        if (GeoLocation.Levels.METER.equals(this.level))
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        else if (GeoLocation.Levels.BUILDING.equals(this.level))
            mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        else
            mLocationRequest.setPriority(LocationRequest.PRIORITY_LOW_POWER);

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    private void stopLocationUpdate() {
        if (mGoogleApiClient != null)
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        this.finish();
    }
}
