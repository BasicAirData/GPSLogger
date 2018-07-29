/**
 * FragmentSettings - Java Class for Android
 * Created by G.Capelli (BasicAirData) on 23/7/2016
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package eu.basicairdata.graziano.gpslogger;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.preference.SwitchPreferenceCompat;
import android.support.v7.view.ContextThemeWrapper;
import android.util.Log;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class FragmentSettings extends PreferenceFragmentCompat {

    private static final float M_TO_FT = 3.280839895f;

    SharedPreferences.OnSharedPreferenceChangeListener prefListener;

    private SharedPreferences prefs;
    public double altcor;       // manual offset
    public double altcorm;    // Manual offset in m

    private ProgressDialog mProgressDialog;
    public boolean Downloaded = false;


    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.app_preferences);

        File tsd = new File(Environment.getExternalStorageDirectory() + "/GPSLogger");
        boolean isGPSLoggerFolder = true;
        if (!tsd.exists()) {
            isGPSLoggerFolder = tsd.mkdir();
        }
        tsd = new File(Environment.getExternalStorageDirectory() + "/GPSLogger/AppData");
        if (!tsd.exists()) {
            isGPSLoggerFolder = tsd.mkdir();
        }
        Log.w("myApp", "[#] FragmentSettings.java - " + (isGPSLoggerFolder ? "Folder /GPSLogger/AppData OK" : "Unable to create folder /GPSLogger/AppData"));

        prefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        // Chech if EGM96 file is downloaded and complete;
        File sd = new File(getActivity().getApplicationContext().getFilesDir() + "/WW15MGH.DAC");
        File sd_old = new File(Environment.getExternalStorageDirectory() + "/GPSLogger/AppData/WW15MGH.DAC");
        if ((sd.exists() && (sd.length() == 2076480)) || (sd_old.exists() && (sd_old.length() == 2076480))) {
            Downloaded = true;
        } else {
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
            SharedPreferences.Editor editor1 = settings.edit();
            editor1.putBoolean("prefEGM96AltitudeCorrection", false);
            editor1.commit();
            SwitchPreferenceCompat EGM96 = (SwitchPreferenceCompat) super.findPreference("prefEGM96AltitudeCorrection");
            EGM96.setChecked(false);
        }

        // Instantiate Progress dialog
        mProgressDialog = new ProgressDialog(getActivity());
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setCancelable(true);
        mProgressDialog.setMessage(getString(R.string.pref_EGM96AltitudeCorrection_download_progress));

        prefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (key.equals("prefUM")) {
                    altcorm = Double.valueOf(prefs.getString("prefAltitudeCorrection", "0"));
                    altcor = prefs.getString("prefUM", "0").equals("0") ? altcorm : altcorm * M_TO_FT;
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("prefAltitudeCorrectionRaw", String.valueOf(altcor));
                    editor.commit();
                    EditTextPreference pAltitudeCorrection = (EditTextPreference) findPreference("prefAltitudeCorrectionRaw");
                    pAltitudeCorrection.setText(prefs.getString("prefAltitudeCorrectionRaw", "0"));
                }

                if (key.equals("prefAltitudeCorrectionRaw")) {
                    try {
                        double d = Double.parseDouble(sharedPreferences.getString("prefAltitudeCorrectionRaw", "0"));
                        altcor = d;
                    }
                    catch(NumberFormatException nfe)
                    {
                        altcor = 0;
                        EditTextPreference Alt = (EditTextPreference) findPreference("prefAltitudeCorrectionRaw");
                        Alt.setText("0");
                    }

                    altcorm = prefs.getString("prefUM", "0").equals("0") ? altcor : altcor / M_TO_FT;
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("prefAltitudeCorrection", String.valueOf(altcorm));
                    editor.commit();
                }

                if (key.equals("prefEGM96AltitudeCorrection")) {
                    if (sharedPreferences.getBoolean(key, false)) {
                        if (!Downloaded) {

                        /* new AlertDialog.Builder(this)                         // Confirmation dialog for file download
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setTitle("Question")
                            .setMessage("Download EGM96 coefficients (file size 2 MB)?")
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // Start Download
                                }

                            })
                            .setNegativeButton("No", null)
                            .show();         */

                            // execute this when the downloader must be fired
                            final DownloadTask downloadTask = new DownloadTask(getActivity());
                            downloadTask.execute("http://earth-info.nga.mil/GandG/wgs84/gravitymod/egm96/binary/WW15MGH.DAC");

                            mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                                @Override
                                public void onCancel(DialogInterface dialog) {
                                    downloadTask.cancel(true);
                                }
                            });

                            PrefEGM96SetToFalse();
                        }
                    }
                }
                SetupPreferences();
            }
        };
    }

    @Override
    public void onResume() {
        // Remove dividers between preferences
        setDivider(new ColorDrawable(Color.TRANSPARENT));
        setDividerHeight(0);

        prefs.registerOnSharedPreferenceChangeListener(prefListener);
        //Log.w("myApp", "[#] FragmentSettings.java - onResume");
        SetupPreferences();
        super.onResume();
    }

    @Override
    public void onPause() {
        prefs.unregisterOnSharedPreferenceChangeListener(prefListener);
        Log.w("myApp", "[#] FragmentSettings.java - onPause");
        EventBus.getDefault().post(EventBusMSG.UPDATE_SETTINGS);
        super.onPause();
    }


    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        Log.w("myApp", "[#] FragmentSettings.java - onCreatePreferences");
    }

    public void SetupPreferences() {

        ListPreference pUM = (ListPreference) findPreference("prefUM");
        ListPreference pUMSpeed = (ListPreference) findPreference("prefUMSpeed");
        ListPreference pGPSDistance = (ListPreference) findPreference("prefGPSdistance");
        ListPreference pGPSUpdateFrequency = (ListPreference) findPreference("prefGPSupdatefrequency");
        ListPreference pKMLAltitudeMode = (ListPreference) findPreference("prefKMLAltitudeMode");
        ListPreference pGPXVersion = (ListPreference) findPreference("prefGPXVersion");
        ListPreference pShowTrackStatsType = (ListPreference) findPreference("prefShowTrackStatsType");
        ListPreference pShowDirections = (ListPreference) findPreference("prefShowDirections");
        ListPreference pViewTracksWith = (ListPreference) findPreference("prefViewTracksWith");
        EditTextPreference pAltitudeCorrection = (EditTextPreference) findPreference("prefAltitudeCorrectionRaw");

        altcorm = Double.valueOf(prefs.getString("prefAltitudeCorrection", "0"));
        altcor = prefs.getString("prefUM", "0").equals("0") ? altcorm : altcorm * M_TO_FT;

        if (prefs.getString("prefUM", "0").equals("0")) {       // Metric
            pUMSpeed.setEntries(R.array.UMSpeed_Metric);
            pGPSDistance.setEntries(R.array.GPSDistance_Metric);
            pAltitudeCorrection.setSummary(altcor != 0 ? getString(R.string.pref_AltitudeCorrection_summary_offset) + " = " + Double.valueOf(Math.round(altcor*1000d)/1000d).toString() + " m" : getString(R.string.pref_AltitudeCorrection_summary_not_defined));

        }
        if (prefs.getString("prefUM", "0").equals("8")) {       // Imperial
            pUMSpeed.setEntries(R.array.UMSpeed_Imperial);
            pGPSDistance.setEntries(R.array.GPSDistance_Imperial);
            pAltitudeCorrection.setSummary(altcor != 0 ? getString(R.string.pref_AltitudeCorrection_summary_offset) + " = " + Double.valueOf(Math.round(altcor*1000d)/1000d).toString() + " ft" : getString(R.string.pref_AltitudeCorrection_summary_not_defined));
        }
        if (prefs.getString("prefUM", "0").equals("16")) {       // Aerial / Nautical
            pUMSpeed.setEntries(R.array.UMSpeed_AerialNautical);
            pGPSDistance.setEntries(R.array.GPSDistance_Imperial);
            pAltitudeCorrection.setSummary(altcor != 0 ? getString(R.string.pref_AltitudeCorrection_summary_offset) + " = " + Double.valueOf(Math.round(altcor*1000d)/1000d).toString() + " ft" : getString(R.string.pref_AltitudeCorrection_summary_not_defined));
        }

        Log.w("myApp", "[#] FragmentSettings.java - prefAltitudeCorrectionRaw = " + prefs.getString("prefAltitudeCorrectionRaw", "0")) ;
        Log.w("myApp", "[#] FragmentSettings.java - prefAltitudeCorrection = " + prefs.getString("prefAltitudeCorrection", "0")) ;

        // Set all summaries
        pUMSpeed.setSummary(pUMSpeed.getEntry());
        pUM.setSummary(pUM.getEntry());
        pGPSDistance.setSummary(pGPSDistance.getEntry());
        pGPSUpdateFrequency.setSummary(pGPSUpdateFrequency.getEntry());
        pKMLAltitudeMode.setSummary(pKMLAltitudeMode.getEntry());
        pGPXVersion.setSummary(pGPXVersion.getEntry());
        pShowTrackStatsType.setSummary(pShowTrackStatsType.getEntry());
        pShowDirections.setSummary(pShowDirections.getEntry());
        pViewTracksWith.setSummary(pViewTracksWith.getEntry());
    }


    public void PrefEGM96SetToFalse() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor1 = settings.edit();
        editor1.putBoolean("prefEGM96AltitudeCorrection", false);
        editor1.commit();
        SwitchPreferenceCompat EGM96 = (SwitchPreferenceCompat) super.findPreference("prefEGM96AltitudeCorrection");
        EGM96.setChecked(false);
    }

    public void PrefEGM96SetToTrue() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor1 = settings.edit();
        editor1.putBoolean("prefEGM96AltitudeCorrection", true);
        editor1.commit();
        SwitchPreferenceCompat EGM96 = (SwitchPreferenceCompat) super.findPreference("prefEGM96AltitudeCorrection");
        EGM96.setChecked(true);
    }


    // ----------------------------------------------------------------.----- EGM96 - Download file

    // usually, subclasses of AsyncTask are declared inside the activity class.
    // that way, you can easily modify the UI thread from here
    private class DownloadTask extends AsyncTask<String, Integer, String> {

        private Context context;
        //private PowerManager.WakeLock mWakeLock;

        public DownloadTask(Context context) {
            this.context = context;
        }

        @Override
        protected String doInBackground(String... sUrl) {
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;
            try {
                URL url = new URL(sUrl[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                // expect HTTP 200 OK, so we don't mistakenly save error report
                // instead of the file
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage();
                }

                // this will be useful to display download percentage
                // might be -1: server did not report the length
                int fileLength = connection.getContentLength();

                // download the file
                input = connection.getInputStream();
                output = new FileOutputStream(getActivity().getApplicationContext().getFilesDir() + "/WW15MGH.DAC");

                byte data[] = new byte[4096];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    // allow canceling with back button
                    if (isCancelled()) {
                        input.close();
                        return null;
                    }
                    total += count;
                    // publishing the progress....
                    if (fileLength > 0) // only if total length is known
                        publishProgress((int) (total * 2028 / fileLength));
                    output.write(data, 0, count);
                }
            } catch (Exception e) {
                return e.toString();
            } finally {
                try {
                    if (output != null)
                        output.close();
                    if (input != null)
                        input.close();
                } catch (IOException ignored) {
                }

                if (connection != null)
                    connection.disconnect();
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // take CPU lock to prevent CPU from going off if the user
            // presses the power button during download
            //PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            //mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
            //        getClass().getName());
            //mWakeLock.acquire();
            mProgressDialog.show();
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            // if we get here, length is known, now set indeterminate to false
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setMax(2028);
            mProgressDialog.setProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            if (getActivity() != null) {
                //mWakeLock.release();
                mProgressDialog.dismiss();
                if (result != null)
                    Toast.makeText(context, getString(R.string.toast_download_error) + ": " + result, Toast.LENGTH_LONG).show();
                else {
                    File sd = new File(getActivity().getApplicationContext().getFilesDir() + "/WW15MGH.DAC");
                    File sd_old = new File(Environment.getExternalStorageDirectory() + "/GPSLogger/AppData/WW15MGH.DAC");
                    if ((sd.exists() && (sd.length() == 2076480)) || (sd_old.exists() && (sd_old.length() == 2076480))) {
                        Downloaded = true;
                        Toast.makeText(context, getString(R.string.toast_download_completed), Toast.LENGTH_SHORT).show();
                        PrefEGM96SetToTrue();

                        // Ask to switch to Absolute Altitude Mode if not already active.
                        ListPreference pKMLAltitudeMode = (ListPreference) findPreference("prefKMLAltitudeMode");
                        if (!(pKMLAltitudeMode.getValue().equals("0"))) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(getContext(), R.style.StyledDialog));
                            builder.setMessage(getResources().getString(R.string.pref_message_switch_to_absolute_altitude_mode));
                            builder.setIcon(android.R.drawable.ic_menu_info_details);
                            builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
                                    SharedPreferences.Editor editor1 = settings.edit();
                                    editor1.putString("prefKMLAltitudeMode", "0");
                                    editor1.commit();
                                    ListPreference pKMLAltitudeMode = (ListPreference) findPreference("prefKMLAltitudeMode");
                                    pKMLAltitudeMode.setValue("0");
                                    pKMLAltitudeMode.setSummary(R.string.pref_KML_altitude_mode_absolute);
                                }
                            });
                            builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.dismiss();
                                }
                            });
                            AlertDialog dialog = builder.create();
                            dialog.show();
                        }

                    } else {
                        Toast.makeText(context, getString(R.string.toast_download_failed), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }
}