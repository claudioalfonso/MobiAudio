package br.gov.sp.prefeitura.mobilab.mobiaudio;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.github.mrengineer13.snackbar.SnackBar;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import br.gov.sp.prefeitura.mobilab.mobiaudio.models.PointOfInterest;
import br.gov.sp.prefeitura.mobilab.mobiaudio.services.GeofenceService;
import br.gov.sp.prefeitura.mobilab.mobiaudio.services.TrackerService;

/**
 * Created by Guilherme Araújo
 * 2015 © MobiONE
 * http://www.mobioneapps.com
 */

public class MainActivity extends AppCompatActivity implements GeofenceService.GeofenceListener {

  private TrackerService mTrackerService;
  private GeofenceService mGeofenceService;
  private MediaPlayer mPlayer;

  private boolean mTrackerEnabled = false;

  private TextView mPOINameTextView;

  private final ServiceConnection mGeofenceConnection = new ServiceConnection() {
    public void onServiceConnected(ComponentName className, IBinder binder) {
      mGeofenceService = ((GeofenceService.GeofenceBinder) binder).getService();
      mGeofenceService.setRadius(25);
      mGeofenceService.setListener(MainActivity.this);
      mGeofenceService.setPOIFeed(mPOIList);
    }

    public void onServiceDisconnected(ComponentName className) {
      mGeofenceService = null;
    }
  };

  private final ServiceConnection mTrackerConnection = new ServiceConnection() {
    public void onServiceConnected(ComponentName className, IBinder binder) {
      mTrackerService = ((TrackerService.TrackerBinder) binder).getService();
      mTrackerService.setActivity(MainActivity.this);
    }

    public void onServiceDisconnected(ComponentName className) {
      mTrackerService = null;
    }
  };

  private List<PointOfInterest> mPOIList;

  private PointOfInterest mCurrentPOI;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    if (getResources().getBoolean(R.bool.is_tablet)) {
      setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    } else {
      setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    mTrackerEnabled = getSharedPreferences("MobiAudio", MODE_PRIVATE).getBoolean("Tracking", false);
    ToggleButton button = (ToggleButton) findViewById(R.id.toggleButton);
    button.setChecked(mTrackerEnabled);

    mPOINameTextView = (TextView) findViewById(R.id.textView_poi_name);
    mPOINameTextView.setText("---");

    loadPOIFeed();
  }

  @Override
  protected void onResume() {
    super.onResume();
    Intent geofenceIntent = new Intent(this, GeofenceService.class);
    bindService(geofenceIntent, mGeofenceConnection, BIND_AUTO_CREATE);
    startService(geofenceIntent);

    Intent trackerIntent = new Intent(this, TrackerService.class);
    bindService(trackerIntent, mTrackerConnection, BIND_AUTO_CREATE);
    startService(trackerIntent);
  }

  @Override
  protected void onPause() {
    super.onPause();
    unbindService(mGeofenceConnection);
    unbindService(mTrackerConnection);

    getSharedPreferences("MobiAudio", MODE_PRIVATE)
        .edit()
        .putBoolean("Tracking", mTrackerEnabled)
        .commit();
  }

  @Override
  public void onApproachPointOfInterest(PointOfInterest POI) {
    mCurrentPOI = POI;
    playAudioDescription(POI);
    mPOINameTextView.setText(POI.getName());
  }

  @Override
  public void onDepartingFromPointOfInterest(PointOfInterest POI) {
    mPOINameTextView.setText("---");
  }

  private void playAudioDescription(PointOfInterest POI) {
    if (POI == null) {
      return;
    }

    if (mPlayer == null) {
      mPlayer = MediaPlayer.create(this, R.raw.clip);
    }

    try {
      mPlayer.reset();
      mPlayer.setDataSource(this, POI.getMediaUri(getPackageName()));
      mPlayer.prepare();
      mPlayer.start();
    } catch (Exception e) {
      e.printStackTrace();

      new SnackBar.Builder(this)
          .withMessageId(R.string.snackbar_error_audio_file_not_found)
          .show();
    }
  }

  // Point of Interest Management
  private void loadPOIFeed() {
    try {
      InputStream inputStream = getResources().openRawResource(R.raw.feed_test);

      Writer writer = new StringWriter();
      char[] buffer = new char[1024];

      try {
        Reader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
        int n;
        while ((n = reader.read(buffer)) != -1) {
          writer.write(buffer, 0, n);
        }
      } catch (IOException e) {
        e.printStackTrace();
      } finally {
        inputStream.close();
      }

      String feedJSON = writer.toString();
      writer.close();

      Gson gson = new GsonBuilder()
          .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
          .create();

      mPOIList = gson.fromJson(feedJSON, new TypeToken<List<PointOfInterest>>(){}.getType());
    } catch (Exception e) {
      mPOIList = new ArrayList<>();
      e.printStackTrace();
    }
  }

  private PointOfInterest getPreviousPOI() {
    int i = mPOIList.indexOf(mCurrentPOI);

    if (i == -1) i = mPOIList.size();
    if (i == 0) i += mPOIList.size();

    return mPOIList.get(i - 1);
  }

  private PointOfInterest getNextPOI() {
    int i = mPOIList.indexOf(mCurrentPOI);

    if (i == mPOIList.size() - 1) i = -1;

    return mPOIList.get(i + 1);
  }

  // Interface Controls
  public void toggleGPS(View view) {
    if (mTrackerEnabled) {
      mTrackerService.stopTracking();
      mGeofenceService.clearHistory();

      if (mPlayer != null) {
        mPlayer.stop();
      }

      mPOINameTextView.setText("---");
      mCurrentPOI = null;

      mTrackerEnabled = false;
    } else {
      mTrackerService.startTracking(mGeofenceService);
      mTrackerEnabled = true;
    }
  }

  public void play(View view) {
    playAudioDescription(mCurrentPOI);
  }

  public void pause(View view) {
    if (mPlayer != null) {
      mPlayer.pause();
    }
  }

  public void previousTrack(View view) {
    mCurrentPOI = getPreviousPOI();
    mPOINameTextView.setText(mCurrentPOI.getName());
  }

  public void nextTrack(View view) {
    mCurrentPOI = getNextPOI();
    mPOINameTextView.setText(mCurrentPOI.getName());
  }
}
