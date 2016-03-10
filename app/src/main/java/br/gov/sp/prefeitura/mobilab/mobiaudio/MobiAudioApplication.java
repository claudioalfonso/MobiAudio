package br.gov.sp.prefeitura.mobilab.mobiaudio;

import android.app.Application;

/**
 * Created by Guilherme Araújo
 * 2015 © MobiONE
 * http://www.mobioneapps.com
 */

public class MobiAudioApplication extends Application {
  @Override
  public void onCreate() {
    super.onCreate();
    getSharedPreferences("MobiAudio", MODE_PRIVATE)
        .edit()
        .putBoolean("Tracking", false)
        .commit();
  }
}
