package br.gov.sp.prefeitura.mobilab.mobiaudio.models;

import android.content.ContentResolver;
import android.location.Location;
import android.net.Uri;

/**
 * Created by Guilherme Araújo
 * 2015 © MobiONE
 * http://www.mobioneapps.com
 */

public class PointOfInterest {
  private final int id;
  private final String mediaFilename;
  private final String name;
  private final double latitude, longitude;

  public PointOfInterest (int id, String name, String filename, double latitude, double longitude) {
    this.id = id;
    this.name = name;
    this.mediaFilename = filename;
    this.latitude = latitude;
    this.longitude = longitude;
  }

  public Location getLocation() {
    Location l = new Location(name);
    l.setLatitude(latitude);
    l.setLongitude(longitude);
    return l;
  }

  public int getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public Uri getMediaUri(String packageName) {
    return new Uri.Builder()
        .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
        .authority(packageName)
        .appendPath("raw")
        .appendPath(mediaFilename)
        .build();
  }
}
