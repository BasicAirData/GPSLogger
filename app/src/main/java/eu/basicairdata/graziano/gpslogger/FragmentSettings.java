/*
 * FragmentSettings - Java Class for Android
 * Created by G.Capelli on 23/7/2016
 * This file is part of BasicAirData GPS Logger
 *
 * Copyright (C) 2011 BasicAirData
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

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.res.ResourcesCompat;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.EditText;
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
import java.text.DecimalFormat;
import java.util.ArrayList;

import static eu.basicairdata.graziano.gpslogger.GPSApplication.FILETYPE_GPX;

/**
 * The Fragment that manages the Settings on the SettingsActivity
 */
public class FragmentSettings extends PreferenceFragmentCompat {

    private static final int REQUEST_ACTION_OPEN_DOCUMENT_TREE = 3;

    SharedPreferences.OnSharedPreferenceChangeListener prefListener;
    private SharedPreferences prefs;
    public double intervalfilter;    // interval filter
    public double distfilter;        // distance filter
    public double distfilterm;       // distance filter in m
    public double altcor;            // manual offset
    public double altcorm;           // Manual offset in m
    private ProgressDialog progressDialog;
    public boolean isDownloaded = false;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.app_preferences);

        // TODO: check it!
        File tsd = new File(GPSApplication.getInstance().getPrefExportFolder());
        if (!tsd.exists()) tsd.mkdir();
        tsd = new File(GPSApplication.DIRECTORY_TEMP);
        if (!tsd.exists()) tsd.mkdir();
        //Log.w("myApp", "[#] FragmentSettings.java - " + (isGPSLoggerFolder ? "Folder /GPSLogger/AppData OK" : "Unable to create folder /GPSLogger/AppData"));

        prefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        // Check if EGM96 file is downloaded and the size of the file is correct;
        isDownloaded = EGM96.getInstance().isGridAvailable(GPSApplication.getInstance().getApplicationContext().getFilesDir().toString()) ||
                EGM96.getInstance().isGridAvailable(GPSApplication.getInstance().getPrefExportFolder());
        if (!isDownloaded) {
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
            SharedPreferences.Editor editor1 = settings.edit();
            editor1.putBoolean("prefEGM96AltitudeCorrection", false);
            editor1.commit();
            SwitchPreferenceCompat egm96 = super.findPreference("prefEGM96AltitudeCorrection");
            egm96.setChecked(false);
        }

        // Instantiate Progress dialog
        progressDialog = new ProgressDialog(getActivity());
        progressDialog.setIndeterminate(true);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCancelable(true);
        progressDialog.setMessage(getString(R.string.pref_EGM96AltitudeCorrection_download_progress));

        prefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                Log.w("myApp", "[#] FragmentSettings.java - SharedPreferences.OnSharedPreferenceChangeListener, key = " + key);
                if (key.equals("prefUM")) {
                    altcorm = Double.valueOf(prefs.getString("prefAltitudeCorrection", "0"));
                    altcor = isUMMetric() ? altcorm : altcorm * PhysicalDataFormatter.M_TO_FT;
                    distfilterm = Double.valueOf(prefs.getString("prefGPSdistance", "0"));
                    distfilter = isUMMetric() ? distfilterm : distfilterm * PhysicalDataFormatter.M_TO_FT;
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("prefAltitudeCorrectionRaw", String.valueOf(altcor));
                    editor.putString("prefGPSdistanceRaw", String.valueOf(distfilter));
                    editor.commit();
                    EditTextPreference etpAltitudeCorrection = findPreference("prefAltitudeCorrectionRaw");
                    etpAltitudeCorrection.setText(prefs.getString("prefAltitudeCorrectionRaw", "0"));
                    EditTextPreference etpGPSDistance = findPreference("prefGPSdistanceRaw");
                    etpGPSDistance.setText(prefs.getString("prefGPSdistanceRaw", "0"));
                }
                if (key.equals("prefAltitudeCorrectionRaw")) {
                    try {
                        altcor = Double.parseDouble(sharedPreferences.getString("prefAltitudeCorrectionRaw", "0"));
                    }
                    catch(NumberFormatException nfe)
                    {
                        altcor = 0;
                        EditTextPreference etpAltitudeCorrection = findPreference("prefAltitudeCorrectionRaw");
                        etpAltitudeCorrection.setText("0");
                    }
                    altcorm = isUMMetric() ? altcor : altcor / PhysicalDataFormatter.M_TO_FT;
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("prefAltitudeCorrection", String.valueOf(altcorm));
                    editor.commit();
                }
                if (key.equals("prefGPSdistanceRaw")) {
                    try {
                        distfilter = Double.parseDouble(sharedPreferences.getString("prefGPSdistanceRaw", "0"));
                        distfilter = Math.abs(distfilter);
                    }
                    catch(NumberFormatException nfe)
                    {
                        distfilter = 0;
                        EditTextPreference etpDistanceFilter = findPreference("prefGPSdistanceRaw");
                        etpDistanceFilter.setText("0");
                    }
                    distfilterm = isUMMetric() ? distfilter : distfilter / PhysicalDataFormatter.M_TO_FT;
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("prefGPSdistance", String.valueOf(distfilterm));
                    editor.commit();
                }
                if (key.equals("prefEGM96AltitudeCorrection")) {
                    if (sharedPreferences.getBoolean(key, false)) {
                        isDownloaded = EGM96.getInstance().isGridAvailable(GPSApplication.getInstance().getApplicationContext().getFilesDir().toString()) ||
                                EGM96.getInstance().isGridAvailable(GPSApplication.getInstance().getPrefExportFolder());
                        if (!isDownloaded) {
                            // execute this when the downloader must be fired
                            final DownloadTask downloadTask = new DownloadTask(getActivity());
                            // Original Link not available anymore
                            //downloadTask.execute("http://earth-info.nga.mil/GandG/wgs84/gravitymod/egm96/binary/WW15MGH.DAC");
                            // Found a copy of EGM Binary grid hosted on OSGeo.org Website.
                            // The connection is not secured with HTTPS for now, we chosen to use it anyway.
                            downloadTask.execute("http://download.osgeo.org/proj/vdatum/egm96_15/outdated/WW15MGH.DAC");

                            progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                                @Override
                                public void onCancel(DialogInterface dialog) {
                                    downloadTask.cancel(true);
                                }
                            });

                            PrefEGM96SetToFalse();
                        } else {
                            EGM96.getInstance().loadGrid(GPSApplication.getInstance().getPrefExportFolder(), GPSApplication.getInstance().getApplicationContext().getFilesDir().toString());
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

        EditTextPreference gpsDistanceETP = getPreferenceManager().findPreference("prefGPSdistanceRaw");
        gpsDistanceETP.setOnBindEditTextListener(new EditTextPreference.OnBindEditTextListener() {
            @Override
            public void onBindEditText(EditText editText) {
                editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                editText.selectAll();
            }
        });

        EditTextPreference altitudeCorrectionETP = getPreferenceManager().findPreference("prefAltitudeCorrectionRaw");
        altitudeCorrectionETP.setOnBindEditTextListener(new EditTextPreference.OnBindEditTextListener() {
            @Override
            public void onBindEditText(EditText editText) {
                editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                editText.selectAll();
            }
        });

        EditTextPreference gpsIntervalETP = getPreferenceManager().findPreference("prefGPSinterval");
        gpsIntervalETP.setOnBindEditTextListener(new EditTextPreference.OnBindEditTextListener() {
            @Override
            public void onBindEditText(EditText editText) {
                editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                editText.selectAll();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Remove dividers between preferences
        setDivider(new ColorDrawable(Color.TRANSPARENT));
        setDividerHeight(0);
        prefs.registerOnSharedPreferenceChangeListener(prefListener);
        //Log.w("myApp", "[#] FragmentSettings.java - onResume");
        GPSApplication.getInstance().getExternalViewerChecker().makeExternalViewersList();
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

    /**
     * Returns true when the Unit of Measurement is set to Metric, false otherwise
     */
    private boolean isUMMetric() {
        return prefs.getString("prefUM", "0").equals("0");
    }

    /**
     * Sets up the Preference screen, by setting the right summaries, adding listeners
     * and manage the visibility of each preference.
     */
    public void SetupPreferences() {
        ListPreference pUM = findPreference("prefUM");
        ListPreference pUMSpeed = findPreference("prefUMOfSpeed");
        EditTextPreference pGPSDistance = findPreference("prefGPSdistanceRaw");
        EditTextPreference pGPSInterval = findPreference("prefGPSinterval");
        ListPreference pGPSUpdateFrequency = findPreference("prefGPSupdatefrequency");
        ListPreference pKMLAltitudeMode = findPreference("prefKMLAltitudeMode");
        ListPreference pGPXVersion = findPreference("prefGPXVersion");
        ListPreference pShowTrackStatsType = findPreference("prefShowTrackStatsType");
        ListPreference pShowDirections = findPreference("prefShowDirections");
        ListPreference pColorTheme = findPreference("prefColorTheme");
        Preference pExportFolder = findPreference("prefExportFolder");
        EditTextPreference pAltitudeCorrection = findPreference("prefAltitudeCorrectionRaw");
        Preference pTracksViewer = findPreference("prefTracksViewer");

        // Adds the unit of measurement to EditTexts title
        pGPSDistance.setDialogTitle(getString(R.string.pref_GPS_distance_filter) + " ("
                + (isUMMetric() ? getString(R.string.UM_m) : getString(R.string.UM_ft)) + ")");
        pAltitudeCorrection.setDialogTitle(getString(R.string.pref_AltitudeCorrection) + " ("
                + (isUMMetric() ? getString(R.string.UM_m) : getString(R.string.UM_ft)) + ")");
        pGPSInterval.setDialogTitle(getString(R.string.pref_GPS_interval_filter) + " ("
                + getString(R.string.UM_s) + ")");

        // Keep Screen On Flag
        if (prefs.getBoolean("prefKeepScreenOn", true)) getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Track Viewer
        final ArrayList<ExternalViewer> evList = new ArrayList<>(GPSApplication.getInstance().getExternalViewerChecker().getExternalViewersList());
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

                            final ArrayList<ExternalViewer> aild = new ArrayList<>();

                            // Add "Select every Time" menu item
                            ExternalViewer askai = new ExternalViewer();
                            askai.label = getString(R.string.pref_track_viewer_select_every_time);
                            askai.icon = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_visibility_24dp, getActivity().getTheme());

                            aild.add(askai);
                            aild.addAll(evList);

                            ExternalViewerAdapter clad = new ExternalViewerAdapter(getActivity(), aild);

                            lv.setAdapter(clad);
                            lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                @Override
                                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                    // TODO: Set Preference
                                    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
                                    SharedPreferences.Editor editor1 = settings.edit();
                                    editor1.putString("prefTracksViewer", aild.get(position).packageName);
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            pExportFolder.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Log.w("myApp", "[#] FragmentSettings.java - pExportFolder preference clicked");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        // Choose a directory using the system's file picker.
                        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                        intent.putExtra("android.content.extra.SHOW_ADVANCED", true);
                        intent.putExtra("android.content.extra.FANCY", true);
                        //intent.putExtra("android.content.extra.SHOW_FILESIZE", true);
                        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);

                        startActivityForResult(intent, REQUEST_ACTION_OPEN_DOCUMENT_TREE);
                    }
                    return false;
                }
            });
        } else pExportFolder.setVisible(false);

        // ------------
        if (evList.isEmpty())
            pTracksViewer.setSummary(R.string.pref_track_viewer_not_installed);                                        // no Viewers installed
        else if (evList.size() == 1)
            pTracksViewer.setSummary(evList.get(0).label + (evList.get(0).fileType.equals(FILETYPE_GPX) ? " (GPX)" : " (KML)"));                                                                              // 1 Viewer installed
        else {
            pTracksViewer.setSummary(R.string.pref_track_viewer_select_every_time);                                       // ask every time
            String pn = prefs.getString("prefTracksViewer", "");
            Log.w("myApp", "[#] FragmentSettings.java - prefTracksViewer = " + pn);
            for (ExternalViewer ev : evList) {
                if (ev.packageName.equals(pn)) {
                    //Log.w("myApp", "[#] FragmentSettings.java - Found " + ev.Label);
                    pTracksViewer.setSummary(ev.label + (ev.fileType.equals(FILETYPE_GPX) ? " (GPX)" : " (KML)"));                                // Default Viewer available!
                }
            }
        }

        // Set all summaries
        try {
            altcorm = Double.valueOf(prefs.getString("prefAltitudeCorrection", "0"));
        } catch(NumberFormatException nfe) {
            altcorm = 0;
        }
        altcor = isUMMetric() ? altcorm : altcorm * PhysicalDataFormatter.M_TO_FT;

        try {
            distfilterm = Math.abs(Double.valueOf(prefs.getString("prefGPSdistance", "0")));
        } catch(NumberFormatException nfe) {
            distfilterm = 0;
        }
        distfilter = isUMMetric() ? distfilterm : distfilterm * PhysicalDataFormatter.M_TO_FT;

        try {
            intervalfilter = Double.valueOf(prefs.getString("prefGPSinterval", "0"));
        } catch(NumberFormatException nfe) {
            intervalfilter = 0;
        }

        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("prefAltitudeCorrectionRaw", String.valueOf(altcor));
        editor.putString("prefGPSdistanceRaw", String.valueOf(distfilter));
        editor.commit();

        DecimalFormat df = new DecimalFormat();
        df.setMinimumFractionDigits(0);
        df.setMaximumFractionDigits(3);

        if (isUMMetric()) {       // Metric
            // TODO: change the value of the UM for speeds?
            pGPSDistance.setSummary(distfilter != 0
                    ? df.format(distfilter) + " " + getString(R.string.UM_m)
                    : getString(R.string.pref_GPS_filter_disabled));
            pAltitudeCorrection.setSummary(altcor != 0
                    ? df.format(altcor) + " " + getString(R.string.UM_m)
                    : getString(R.string.pref_AltitudeCorrection_summary_not_defined));
        }
        if (prefs.getString("prefUM", "0").equals("8")) {       // Imperial
            // TODO: change the value of the UM for speeds?
            pGPSDistance.setSummary(distfilter != 0
                    ? df.format(distfilter) + " " + getString(R.string.UM_ft)
                    : getString(R.string.pref_GPS_filter_disabled));
            pAltitudeCorrection.setSummary(altcor != 0
                    ? df.format(altcor) + " " + getString(R.string.UM_ft)
                    : getString(R.string.pref_AltitudeCorrection_summary_not_defined));
        }
        if (prefs.getString("prefUM", "0").equals("16")) {       // Aerial / Nautical
            // TODO: change the value of the UM for speeds?
            pGPSDistance.setSummary(distfilter != 0
                    ? df.format(distfilter) + " " + getString(R.string.UM_ft)
                    : getString(R.string.pref_GPS_filter_disabled));
            pAltitudeCorrection.setSummary(altcor != 0
                    ? df.format(altcor) + " " + getString(R.string.UM_ft)
                    : getString(R.string.pref_AltitudeCorrection_summary_not_defined));
        }

        pGPSInterval.setSummary(intervalfilter != 0
                ? df.format(intervalfilter) + " " + getString(R.string.UM_s)
                : getString(R.string.pref_GPS_filter_disabled));

        pColorTheme.setSummary(pColorTheme.getEntry());
        pUMSpeed.setSummary(pUMSpeed.getEntry());
        pUM.setSummary(pUM.getEntry());
        pGPSUpdateFrequency.setSummary(pGPSUpdateFrequency.getEntry());
        pKMLAltitudeMode.setSummary(pKMLAltitudeMode.getEntry());
        pGPXVersion.setSummary(pGPXVersion.getEntry());
        pShowTrackStatsType.setSummary(pShowTrackStatsType.getEntry());
        pShowDirections.setSummary(pShowDirections.getEntry());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (GPSApplication.getInstance().isExportFolderWritable())
                pExportFolder.setSummary(GPSApplication.getInstance().extractFolderNameFromEncodedUri(prefs.getString("prefExportFolder", "")));
            else
                pExportFolder.setSummary(getString(R.string.pref_not_set));
        }
    }

    /**
     * It manages the return code of the Intent.ACTION_OPEN_DOCUMENT_TREE
     * that returns the local exportation folder.
     *
     * it Requires api >= Build.VERSION_CODES.LOLLIPOP
     */
    @Override
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (requestCode == REQUEST_ACTION_OPEN_DOCUMENT_TREE && resultCode == Activity.RESULT_OK) {
            // The result data contains a URI for the document or directory that
            // the user selected.

            if (resultData != null) {
                Uri treeUri = resultData.getData();
                getActivity().grantUriPermission(getActivity().getPackageName(), treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                GPSApplication.getInstance().getContentResolver().takePersistableUriPermission(treeUri, Intent
                        .FLAG_GRANT_READ_URI_PERMISSION | Intent
                        .FLAG_GRANT_WRITE_URI_PERMISSION);
                //getContentResolver().takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                Log.w("myApp", "[#] GPSActivity.java - onActivityResult URI: " + treeUri.toString());
                Log.w("myApp", "[#] GPSActivity.java - onActivityResult URI: " + treeUri.getPath());
                Log.w("myApp", "[#] GPSActivity.java - onActivityResult URI: " + treeUri.getEncodedPath());

                GPSApplication.getInstance().setPrefExportFolder(treeUri.toString());
                SetupPreferences();
            }
        }
        super.onActivityResult(resultCode, resultCode, resultData);
    }

    /**
     * Sets the PrefEGM96 to false
     */
    public void PrefEGM96SetToFalse() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor1 = settings.edit();
        editor1.putBoolean("prefEGM96AltitudeCorrection", false);
        editor1.commit();
        SwitchPreferenceCompat prefEGM96 = super.findPreference("prefEGM96AltitudeCorrection");
        prefEGM96.setChecked(false);
    }

    /**
     * Sets the PrefEGM96 to true
     */
    public void PrefEGM96SetToTrue() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor1 = settings.edit();
        editor1.putBoolean("prefEGM96AltitudeCorrection", true);
        editor1.commit();
        SwitchPreferenceCompat prefEGM96 = super.findPreference("prefEGM96AltitudeCorrection");
        prefEGM96.setChecked(true);
        EGM96.getInstance().loadGrid(GPSApplication.getInstance().getPrefExportFolder(), GPSApplication.getInstance().getApplicationContext().getFilesDir().toString());
    }

    // ------------------------------------------------------------- Download of the EGM96 grid file

    /**
     * The Class that manages the download of the EGM96 grid file.
     * The WW15MGH.DAC file is downloaded into the getFilesDir() folder.
     * It displays and keeps updated a progress dialog
     * that shows the progress of the download
     */
    private class DownloadTask extends AsyncTask<String, Integer, String> {
        // usually, subclasses of AsyncTask are declared inside the activity class.
        // that way, you can easily modify the UI thread from here

        private final Context context;
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
            progressDialog.show();
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            // if we get here, length is known, now set indeterminate to false
            progressDialog.setIndeterminate(false);
            progressDialog.setMax(2028);
            progressDialog.setProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            if (getActivity() != null) {
                //mWakeLock.release();
                progressDialog.dismiss();
                if (result != null)
                    Toast.makeText(context, getString(R.string.toast_download_error) + ": " + result, Toast.LENGTH_LONG).show();
                else {
                    isDownloaded = EGM96.getInstance().isGridAvailable(GPSApplication.getInstance().getApplicationContext().getFilesDir().toString()) ||
                            EGM96.getInstance().isGridAvailable(GPSApplication.getInstance().getPrefExportFolder());
                    if (isDownloaded) {
                        Toast.makeText(context, getString(R.string.toast_download_completed), Toast.LENGTH_SHORT).show();
                        PrefEGM96SetToTrue();
                    } else {
                        Toast.makeText(context, getString(R.string.toast_download_failed), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }
}