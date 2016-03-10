package br.gov.sp.prefeitura.mobilab.mobiaudio.services;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.ViewGroup;

import java.util.List;

import br.gov.sp.prefeitura.mobilab.mobiaudio.models.PointOfInterest;

/**
 * Created by Guilherme Araújo
 * 2015 © MobiONE
 * http://www.mobioneapps.com
 */

public class GeofenceService extends Service implements LocationListener {

  public interface GeofenceListener {
    void onApproachPointOfInterest(PointOfInterest POI);
    void onDepartingFromPointOfInterest(PointOfInterest POI);
  }

  private final GeofenceBinder mBinder = new GeofenceBinder();

  private GeofenceListener mListener;

  private List<PointOfInterest> mPOIList;
  private PointOfInterest mCurrentPOI;
  private double mRadius;

  public GeofenceService() {

  }

  @Override
  public IBinder onBind(Intent intent) {
    return mBinder;
  }

  public void setRadius(double radius) {
    mRadius = radius;
  }

  public void setListener(GeofenceListener listener) {
    mListener = listener;
  }

  public void setPOIFeed(List<PointOfInterest> POIList) {
    mPOIList = POIList;
  }

  public void clearHistory() {
    mCurrentPOI = null;
  }

  @Override
  public void onLocationChanged(Location location) {
    Log.d("GeofenceService", String.format("New location: lat %f  long %f", location.getLatitude(),
        location.getLongitude()));

    if (mCurrentPOI != null) {
      if (location.distanceTo(mCurrentPOI.getLocation()) > mRadius) {
        mListener.onDepartingFromPointOfInterest(mCurrentPOI);
        mCurrentPOI = null;
      }
    }

    for (PointOfInterest POI : mPOIList) {
      if (POI.equals(mCurrentPOI)) {
        continue;
      }

      Log.d("GeofenceService", String.format("Distance from %s: %.3f m", POI.getName(),
          location.distanceTo(POI.getLocation())));
      if (location.distanceTo(POI.getLocation()) < mRadius) {
        mCurrentPOI = POI;
        mListener.onApproachPointOfInterest(POI);
        break;
      }
    }
  }

  @Override
  public void onStatusChanged(String provider, int status, Bundle extras) {

  }

  @Override
  public void onProviderEnabled(String provider) {

  }

  @Override
  public void onProviderDisabled(String provider) {

  }

  public class GeofenceBinder extends Binder {
    public GeofenceService getService() {
      return GeofenceService.this;
    }
  }
}
