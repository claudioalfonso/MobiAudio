package br.gov.sp.prefeitura.mobilab.mobiaudio.services;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;

import com.github.mrengineer13.snackbar.SnackBar;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import br.gov.sp.prefeitura.mobilab.mobiaudio.R;

/**
 * Created by Guilherme Araújo
 * 2015 © MobiONE
 * http://www.mobioneapps.com
 */

public class TrackerService extends Service implements LocationListener {
  // Minimum distance (in meters) to trigger a location update
  private static final float UPDATE_DISTANCE_THRESHOLD = 0.5f;

  // Minimum time span (in milliseconds) to trigger a location update
  private static final long UPDATE_TIMING_THRESHOLD = 5 * 1000l;

  private final TrackerBinder mBinder = new TrackerBinder();

  private Activity mActivity;

  private LocationManager locationManager;
  private LocationListener mListener;
  private Location mCurrentLocation;
  private boolean mIsEnabled = false;
  private SimpleDateFormat mDateFormat;
  private BufferedWriter mBufferedWriter;

  public TrackerService() {

  }

  @Override
  public IBinder onBind(Intent intent) {
    return mBinder;
  }

  @Override
  public void onCreate() {
    super.onCreate();
    String logPath = Environment.getExternalStorageDirectory().getPath();
    logPath += "/Android/data/" + getPackageName() + "/logs";
    File logDir = new File(logPath);

    if (!logDir.exists()) {
      if (!logDir.mkdirs()) {
        return;
      }
    }

    mDateFormat = new SimpleDateFormat("yyyy-MM-dd", new Locale("pt-BR"));
    logPath += "/" + mDateFormat.format(new Date()) + ".log";

    File logFile = new File(logPath);
    if (!logFile.exists()) {
      try {
        logFile.createNewFile();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    try {
      mBufferedWriter = new BufferedWriter(new FileWriter(logPath, true));
      mDateFormat = new SimpleDateFormat("HH:mm:ss:SSS", new Locale("pt-BR"));
      String logStartEntry = String.format("\n" +
              "=============================\n" +
              " New App run at %s\n" +
              "=============================\n",
          mDateFormat.format(new Date())
      );
      mBufferedWriter.write(logStartEntry);
      mBufferedWriter.flush();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void onDestroy() {
    try {
      mBufferedWriter.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    stopTracking();
    super.onDestroy();
  }

  public void startTracking(LocationListener listener) {
    mListener = listener;

    if (isEnabled()) {
      // No need to restart if already running
      return;
    }

    locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

    try {
      // Get coarser location from Network Provider
      if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
            UPDATE_TIMING_THRESHOLD, UPDATE_DISTANCE_THRESHOLD, this);
      }

      // Get finer location from GPS
      if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
            UPDATE_TIMING_THRESHOLD, UPDATE_DISTANCE_THRESHOLD, this);
      }

      mIsEnabled = true;

      String logStartEntry = String.format("\nStarted tracking at %s\n",
          mDateFormat.format(new Date())
      );
      mBufferedWriter.write(logStartEntry);
      mBufferedWriter.flush();
    } catch (Exception e) {
      e.printStackTrace();
      mListener = null;

      new SnackBar.Builder(mActivity)
          .withMessageId(R.string.snackbar_error_could_not_start_location_manager)
          .show();
    }
  }

  public void setActivity(Activity activity) {
    mActivity = activity;
  }

  public void stopTracking() {
    mListener = null;
    mIsEnabled = false;

    if (locationManager != null) {
      locationManager.removeUpdates(this);
    }

    try {
      String logStartEntry = String.format("\nStopped tracking at %s\n",
          mDateFormat.format(new Date())
      );
      mBufferedWriter.write(logStartEntry);
      mBufferedWriter.flush();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public boolean isEnabled() {
    return mIsEnabled;
  }

  public Location getLocation() {
    return mCurrentLocation;
  }

  public double getLatitude() {
    // Return invalid latitude if location is not determined
    return mCurrentLocation != null ? mCurrentLocation.getLatitude() : Double.NaN;
  }

  public double getLongitude() {
    // Return invalid longitude if location is not determined
    return mCurrentLocation != null ? mCurrentLocation.getLongitude() : Double.NaN;
  }

  @Override
  public void onLocationChanged(Location location) {
    mCurrentLocation = location;

    if (mListener != null) {
      mListener.onLocationChanged(location);
    }

    if (mBufferedWriter != null){
      String logEntry = String.format("%s  Lat: %3.6f  Lng: %3.6f  Provider: %s  Accuracy: %3.3f\n",
          mDateFormat.format(new Date()),
          location.getLatitude(),
          location.getLongitude(),
          padRight(location.getProvider(), 7),
          location.getAccuracy());
      try {
        mBufferedWriter.write(logEntry);
        mBufferedWriter.flush();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  @Override
  public void onStatusChanged(String provider, int status, Bundle extras) {
    if (mListener != null) {
      mListener.onStatusChanged(provider, status, extras);
    }
  }

  @Override
  public void onProviderEnabled(String provider) {
    if (mListener != null) {
      mListener.onProviderEnabled(provider);
    }
  }

  @Override
  public void onProviderDisabled(String provider) {
    if (mListener != null) {
      mListener.onProviderDisabled(provider);
    }
  }

  public class TrackerBinder extends Binder {
    public TrackerService getService() {
      return TrackerService.this;
    }
  }

  private static String padRight(String s, int n) {
    return String.format("%1$-" + n + "s", s);
  }
}
