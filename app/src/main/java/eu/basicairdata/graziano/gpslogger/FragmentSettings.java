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

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

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
                Log.w("myApp", "[#] FragmentSettings.java - SharedPreferences.OnSharedPreferenceChangeListener, key = " + key);
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

                if (key.equals("prefColorTheme")) {
                    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
                    SharedPreferences.Editor editor1 = settings.edit();
                    editor1.putString(key, sharedPreferences.getString(key, "2"));
                    editor1.commit();

                    getActivity().getWindow().setWindowAnimations(R.style.MyCrossfadeAnimation_Window);
                    AppCompatDelegate.setDefaultNightMode(Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(getContext()).getString("prefColorTheme", "2")));
                    //getActivity().recreate();
                }

                SetupPreferences();
            }
        };
    }

    @Override
    public void onResume() {
        super.onResume();
        // Remove dividers between preferences
        setDivider(new ColorDrawable(Color.TRANSPARENT));
        setDividerHeight(0);

        prefs.registerOnSharedPreferenceChangeListener(prefListener);
        //Log.w("myApp", "[#] FragmentSettings.java - onResume");
        GPSApplication.getInstance().getExternalViewerChecker().makeAppInfoList();
        SetupPreferences();
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
        ListPreference pColorTheme = (ListPreference) findPreference("prefColorTheme");
        EditTextPreference pAltitudeCorrection = (EditTextPreference) findPreference("prefAltitudeCorrectionRaw");
        Preference pTracksViewer = (Preference) findPreference("prefTracksViewer");

        // Keep Screen On Flag
        if (prefs.getBoolean("prefKeepScreenOn", true)) getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Track Viewer

        final ArrayList<AppInfo> ail = new ArrayList<>(GPSApplication.getInstance().getExternalViewerChecker().getAppInfoList());
        switch (GPSApplication.getInstance().getExternalViewerChecker().size()) {
            case 0:
                pTracksViewer.setEnabled(false);    // No viewers installed
                pTracksViewer.setOnPreferenceClickListener(null);
                break;

            case 1:
                pTracksViewer.setEnabled(true);     // 1 viewer installed
                pTracksViewer.setOnPreferenceClickListener(null);
                break;

            default:
                pTracksViewer.setEnabled(true);     // 2 or more viewers installed
                pTracksViewer.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        //Log.w("myApp", "[#] FragmentSettings.java - prefTracksViewer");
                        ExternalViewerChecker externalViewerChecker = GPSApplication.getInstance().getExternalViewerChecker();
                        if (externalViewerChecker.size() >= 1) {
                            final Dialog dialog = new Dialog(getActivity());
                            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                            View view = getLayoutInflater().inflate(R.layout.appdialog_list, null);
                            ListView lv = (ListView) view.findViewById(R.id.id_appdialog_list);

                            final ArrayList<AppInfo> aild = new ArrayList<>();

                            // Add "Select every Time" menu item
                            AppInfo askai = new AppInfo();
                            askai.Label = getString(R.string.pref_track_viewer_select_every_time);
                            if (PreferenceManager.getDefaultSharedPreferences(getContext()).getString("prefColorTheme", "2").equals("1")) {
                                askai.Icon = getResources().getDrawable(R.mipmap.ic_visibility_black_24dp);
                                askai.Icon.setAlpha(150);
                            } else {
                                askai.Icon = getResources().getDrawable(R.mipmap.ic_visibility_white_24dp);
                                askai.Icon.setAlpha(255);
                            }
                            aild.add(askai);
                            aild.addAll(ail);

                            AppDialogList clad = new AppDialogList(getActivity(), aild);

                            lv.setAdapter(clad);
                            lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                @Override
                                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                    // TODO: Set Preference
                                    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
                                    SharedPreferences.Editor editor1 = settings.edit();
                                    editor1.putString("prefTracksViewer", aild.get(position).PackageName);
                                    editor1.commit();
                                    SetupPreferences();
                                    dialog.dismiss();
                                }
                            });
                            dialog.setContentView(view);
                            dialog.show();
                        }
                        return true;
                    }
                });
        }
        // ------------

        if (ail.isEmpty())
            pTracksViewer.setSummary(R.string.pref_track_viewer_not_installed);                                        // no Viewers installed
        else if (ail.size() == 1)
            pTracksViewer.setSummary(ail.get(0).Label + (ail.get(0).GPX ? " (GPX)" : " (KML)"));                                                                              // 1 Viewer installed
        else {
            pTracksViewer.setSummary(R.string.pref_track_viewer_select_every_time);                                       // ask every time
            String pn = prefs.getString("prefTracksViewer", "");
            Log.w("myApp", "[#] FragmentSettings.java - prefTracksViewer = " + pn);
            for (AppInfo ai : ail) {
                if (ai.PackageName.equals(pn)) {
                    //Log.w("myApp", "[#] FragmentSettings.java - Found " + ai.Label);
                    pTracksViewer.setSummary(ai.Label + (ai.GPX ? " (GPX)" : " (KML)"));                                // Default Viewer available!
                }
            }
        }


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
        pColorTheme.setSummary(pColorTheme.getEntry());
        pUMSpeed.setSummary(pUMSpeed.getEntry());
        pUM.setSummary(pUM.getEntry());
        pGPSDistance.setSummary(pGPSDistance.getEntry());
        pGPSUpdateFrequency.setSummary(pGPSUpdateFrequency.getEntry());
        pKMLAltitudeMode.setSummary(pKMLAltitudeMode.getEntry());
        pGPXVersion.setSummary(pGPXVersion.getEntry());
        pShowTrackStatsType.setSummary(pShowTrackStatsType.getEntry());
        pShowDirections.setSummary(pShowDirections.getEntry());
        //pViewTracksWith.setSummary(pViewTracksWith.getEntry());
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

        // Disables the SSL certificate checking for new instances of {@link HttpsURLConnection} This has been created to
        // usually aid testing on a local box, not for use on production. On this case it is OK
        // Code found on https://gist.github.com/tobiasrohloff/72e32bc4e215522c4bcc

        private void disableSSLCertificateChecking() {
            TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                @Override
                public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                    // Not implemented
                }

                @Override
                public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                    // Not implemented
                }
            } };

            try {
                SSLContext sc = SSLContext.getInstance("TLS");

                sc.init(null, trustAllCerts, new java.security.SecureRandom());

                HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            } catch (KeyManagementException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }


        @Override
        protected String doInBackground(String... sUrl) {
            boolean redirect = false;
            String HTTPSUrl = "";
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;
            try {
                URL url = new URL(sUrl[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.setInstanceFollowRedirects(true);
                connection.connect();

                // Redirection HTTP -> HTTPS is insecure.
                //
                // Unfortunately the July 2019 the National Geospatial-Intelligence Agency started to change
                // its Website in a not predictable Way for Us (the EGM File started to return a HTTP 302) and,
                // when we patched the Code, We decided to keep opened all the Possibilities in order to restore
                // the Functionality and minimize the Possibility that the File could become unavailable again.
                //
                // We are watching if the remote Situation remains stable:
                // The Plan is to completely remove the HTTP Request in favor of a direct HTTPS one,
                // at least for Android 5+ that support TLS Protocol.

                // expect HTTP 200 OK, so we don't mistakenly save error report
                // instead of the file
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    if ((connection.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP)
                            || (connection.getResponseCode() == HttpURLConnection.HTTP_MOVED_PERM)
                            || (connection.getResponseCode() == HttpURLConnection.HTTP_SEE_OTHER)) {
                        // REDIRECTED !!
                        HTTPSUrl = connection.getHeaderField("Location");
                        connection.disconnect();
                        if (HTTPSUrl.startsWith("https")) {
                            redirect = true;
                            Log.w("myApp", "[#] FragmentSettings.java - Download of EGM Grid redirected to " + HTTPSUrl) ;
                        }
                    }
                    else return "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage();
                }

                if (!redirect) {
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
            if (!redirect) return null;
            else {
                // REDIRECTION. Try with HTTPS:
                HttpsURLConnection connection_https = null;
                try {
                    URL url = new URL(HTTPSUrl);

                    connection_https = (HttpsURLConnection) url.openConnection();
                    connection_https.setInstanceFollowRedirects(true);

                    disableSSLCertificateChecking();

                    connection_https = (HttpsURLConnection) url.openConnection();
                    connection_https.connect();

                    // expect HTTP 200 OK, so we don't mistakenly save error report
                    // instead of the file
                    if (connection_https.getResponseCode() != HttpURLConnection.HTTP_OK) {
                        return "Server returned HTTP " + connection_https.getResponseCode()
                                + " " + connection_https.getResponseMessage();
                    }

                    // this will be useful to display download percentage
                    // might be -1: server did not report the length
                    int fileLength = connection_https.getContentLength();

                    // download the file
                    input = connection_https.getInputStream();
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

                    if (connection_https != null)
                        connection_https.disconnect();
                }
                return null;
            }
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
                        /*
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
                        */

                    } else {
                        Toast.makeText(context, getString(R.string.toast_download_failed), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }
}