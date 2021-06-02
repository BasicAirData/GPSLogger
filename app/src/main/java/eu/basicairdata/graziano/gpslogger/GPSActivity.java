/*
 * GPSActivity - Java Class for Android
 * Created by G.Capelli on 5/5/2016
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

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import androidx.annotation.NonNull;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.tabs.TabLayout;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static eu.basicairdata.graziano.gpslogger.GPSApplication.TOAST_VERTICAL_OFFSET;

/**
 * The main Activity.
 * Here you can view the status of GPS, of the current Track and the list
 * of the recorded tracks.
 * <p>
 * The tabbed interface contains the 3 following Fragments:
 * <ul>
 *     <li>Page 1 = FragmentGPSFix: it shows the status of the GPS and the FIX</li>
 *     <li>Page 2 = FragmentTrack: it shows the statistics of the current Track</li>
 *     <li>Page 3 = FragmentTracklist: it lists the archive of the recorded tracks</li>
 * </ul>
 * The Activity is driven by a bottom bar (FragmentRecordingControls),
 * that is visible on the first 2 pages, and can host an Action-mode Toolbar
 * on top of the third page (ToolbarActionMode).
 */
public class GPSActivity extends AppCompatActivity {

    private static final int REQUEST_ID_MULTIPLE_PERMISSIONS = 1;

    private final GPSApplication gpsApp = GPSApplication.getInstance();
    private Toolbar toolbar;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private ActionMode actionMode;
    private View bottomSheet;
    private BottomSheetBehavior bottomSheetBehavior;
    private boolean showToastGrantStoragePermission = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.w("myApp", "[#] " + this + " - onCreate()");
        setTheme(R.style.MyMaterialTheme);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_gps);
        toolbar = findViewById(R.id.id_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        viewPager = findViewById(R.id.id_viewpager);
        viewPager.setOffscreenPageLimit(3);

        setupViewPager(viewPager);

        tabLayout = findViewById(R.id.id_tablayout);
        tabLayout.setTabMode(TabLayout.MODE_FIXED);
        tabLayout.setupWithViewPager(viewPager);

        tabLayout.addOnTabSelectedListener(
                new TabLayout.ViewPagerOnTabSelectedListener(viewPager) {
                    @Override
                    public void onTabSelected(TabLayout.Tab tab) {
                        super.onTabSelected(tab);
                        gpsApp.setGPSActivityActiveTab(tab.getPosition());
                        updateBottomSheetPosition();
                        activateActionModeIfNeeded();
                    }
                });

        bottomSheet = findViewById(R.id.id_bottomsheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setHideable(false);
    }

    @Override
    public void onStart() {
        Log.w("myApp", "[#] " + this + " - onStart()");
        super.onStart();
        gpsApp.setGPSActivityActiveTab(tabLayout.getSelectedTabPosition());
    }

    @Override
    public void onStop() {
        Log.w("myApp", "[#] " + this + " - onStop()");
        super.onStop();
    }

    @Override
    public void onResume() {
        Log.w("myApp", "[#] " + this + " - onResume()");
        super.onResume();

        // Workaround for Nokia Devices, Android 9
        // https://github.com/BasicAirData/GPSLogger/issues/77
        if (EventBus.getDefault().isRegistered(this)) {
            //Log.w("myApp", "[#] GPSActivity.java - EventBus: GPSActivity already registered");
            EventBus.getDefault().unregister(this);
        }

        EventBus.getDefault().register(this);
        loadPreferences();
        EventBus.getDefault().post(EventBusMSG.APP_RESUME);

        // Check for Location runtime Permissions (for Android 23+)
        if (!gpsApp.isLocationPermissionChecked()) {
            checkLocationPermission();
            gpsApp.setLocationPermissionChecked(true);
        }

        activateActionModeIfNeeded();

        if (gpsApp.preferenceFlagExists(GPSApplication.FLAG_RECORDING) && !gpsApp.isRecording()) {
            // The app is crashed in background
            Log.w("myApp", "[#] GPSActivity.java - THE APP HAS BEEN KILLED IN BACKGROUND DURING A RECORDING !!!");
            gpsApp.clearPreferenceFlag_NoBackup(GPSApplication.FLAG_RECORDING);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                builder.setMessage(getResources().getString(R.string.dlg_app_killed) + "\n\n" + getResources().getString(R.string.dlg_app_killed_description));
                builder.setNeutralButton(R.string.open_android_app_settings, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        intent.setData(uri);
                        try {
                            startActivity(intent);
                        } catch (Exception e) {
                            //Log.w("myApp", "[#] GPSActivity.java - Unable to open the settings screen");
                        }
                        dialog.dismiss();
                    }
                });
            }
            else builder.setMessage(getResources().getString(R.string.dlg_app_killed));
            builder.setIcon(android.R.drawable.ic_menu_info_details);
            builder.setPositiveButton(R.string.about_ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        }
        if (gpsApp.isJustStarted() && (gpsApp.getCurrentTrack().getNumberOfLocations() + gpsApp.getCurrentTrack().getNumberOfPlacemarks() > 0)) {
            Toast toast = Toast.makeText(gpsApp.getApplicationContext(), R.string.toast_active_track_not_empty, Toast.LENGTH_LONG);
            toast.setGravity(Gravity.BOTTOM, 0, TOAST_VERTICAL_OFFSET);
            toast.show();
        }
        gpsApp.setJustStarted(false);
        if (showToastGrantStoragePermission) {
            Toast toast = Toast.makeText(gpsApp.getApplicationContext(), R.string.please_grant_storage_permission, Toast.LENGTH_LONG);
            toast.setGravity(Gravity.BOTTOM, 0, TOAST_VERTICAL_OFFSET);
            toast.show();
            showToastGrantStoragePermission = false;
        }
    }

    @Override
    public void onPause() {
        Log.w("myApp", "[#] " + this + " - onPause()");
        EventBus.getDefault().post(EventBusMSG.APP_PAUSE);
        EventBus.getDefault().unregister(this);
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        ShutdownApp();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        updateBottomSheetPosition();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            gpsApp.setHandlerTime(60000);
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        if (id == R.id.action_about) {
            // Shows About Dialog
            FragmentManager fm = getSupportFragmentManager();
            FragmentAboutDialog aboutDialog = new FragmentAboutDialog();
            aboutDialog.show(fm, "");
            return true;
        }
        if (id == R.id.action_online_help) {
            if (isBrowserInstalled()) {
                // Opens the default browser and shows the Getting Started Guide page
                String url = "https://www.basicairdata.eu/projects/android/android-gps-logger/getting-started-guide-for-gps-logger/";
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                i.setData(Uri.parse(url));
                startActivity(i);
            } else {
                Toast toast = Toast.makeText(gpsApp.getApplicationContext(), R.string.toast_no_browser_installed, Toast.LENGTH_LONG);
                toast.setGravity(Gravity.BOTTOM, 0, TOAST_VERTICAL_OFFSET);
                toast.show();
            }
            return true;
        }
        if (id == R.id.action_shutdown) {
            ShutdownApp();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_ID_MULTIPLE_PERMISSIONS: {
                Map<String, Integer> perms = new HashMap<>();
                if (grantResults.length > 0) {
                    // Fill with actual results from user
                    for (int i = 0; i < permissions.length; i++) perms.put(permissions[i], grantResults[i]);
                    // Check for permissions
                    if (perms.containsKey(Manifest.permission.ACCESS_FINE_LOCATION)) {
                        if (perms.get(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            Log.w("myApp", "[#] GPSActivity.java - ACCESS_FINE_LOCATION = PERMISSION_GRANTED; setGPSLocationUpdates!");
                            gpsApp.setGPSLocationUpdates(false);
                            gpsApp.setGPSLocationUpdates(true);
                            gpsApp.updateGPSLocationFrequency();
                        } else {
                            Log.w("myApp", "[#] GPSActivity.java - ACCESS_FINE_LOCATION = PERMISSION_DENIED");
                        }
                    }
                    if (perms.containsKey(Manifest.permission.INTERNET)) {
                        if (perms.get(Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED) {
                            Log.w("myApp", "[#] GPSActivity.java - INTERNET = PERMISSION_GRANTED");
                        } else {
                            Log.w("myApp", "[#] GPSActivity.java - INTERNET = PERMISSION_DENIED");
                        }
                    }
                    if (perms.containsKey(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        if (perms.get(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                            Log.w("myApp", "[#] GPSActivity.java - WRITE_EXTERNAL_STORAGE = PERMISSION_GRANTED");
                            // ---------------------------------------------------- Create the Directories if not exist
                            File sd = new File(GPSApplication.DIRECTORY_EXPORT);
                            if (!sd.exists()) {
                                sd.mkdir();
                            }
                            sd = new File(GPSApplication.DIRECTORY_TEMP);
                            if (!sd.exists()) {
                                sd.mkdir();
                            }
                            sd = new File(getApplicationContext().getFilesDir() + "/Thumbnails");
                            if (!sd.exists()) {
                                sd.mkdir();
                            }
                            EGM96 egm96 = EGM96.getInstance();
                            if (egm96 != null) {
                                if (!egm96.isEGMGridLoaded()) {
                                    //Log.w("myApp", "[#] GPSApplication.java - Loading EGM Grid...");
                                    egm96.LoadGridFromFile(GPSApplication.DIRECTORY_TEMP + "/WW15MGH.DAC", getApplicationContext().getFilesDir() + "/WW15MGH.DAC");
                                }
                            }
                            if (gpsApp.getJobsPending() > 0) gpsApp.executeJob();
                        } else {
                            Log.w("myApp", "[#] GPSActivity.java - WRITE_EXTERNAL_STORAGE = PERMISSION_DENIED");
                            if (gpsApp.getJobsPending() > 0) {
                                // Shows toast "Unable to write the file"
                                showToastGrantStoragePermission = true;
                                EventBus.getDefault().post(EventBusMSG.TOAST_STORAGE_PERMISSION_REQUIRED);
                                gpsApp.setJobsPending(0);
                            }
                        }
                    }
                }
                break;
            }
            default: {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            }
        }
    }

    /**
     * The EventBus receiver for Normal Messages.
     */
    @Subscribe
    public void onEvent(EventBusMSGNormal msg) {
        switch (msg.eventBusMSG) {
            case EventBusMSG.TRACKLIST_SELECT:
            case EventBusMSG.TRACKLIST_DESELECT:
                activateActionModeIfNeeded();
        }
    }

    /**
     * The EventBus receiver for Short Messages.
     */
    @Subscribe
    public void onEvent(Short msg) {
        switch (msg) {
            case EventBusMSG.REQUEST_ADD_PLACEMARK:
                // Shows the Placemark Dialog
                FragmentManager fm = getSupportFragmentManager();
                FragmentPlacemarkDialog placemarkDialog = new FragmentPlacemarkDialog();
                placemarkDialog.show(fm, "");
                break;
            case EventBusMSG.UPDATE_TRACKLIST:
            case EventBusMSG.NOTIFY_TRACKS_DELETED:
                activateActionModeIfNeeded();
                break;
            case EventBusMSG.APPLY_SETTINGS:
                loadPreferences();
                break;
            case EventBusMSG.TOAST_TRACK_EXPORTED:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast toast = Toast.makeText(gpsApp.getApplicationContext(), R.string.toast_track_exported, Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.BOTTOM, 0, TOAST_VERTICAL_OFFSET);
                        toast.show();
                    }
                });
                break;
            case EventBusMSG.TOAST_STORAGE_PERMISSION_REQUIRED:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast toast = Toast.makeText(gpsApp.getApplicationContext(), R.string.please_grant_storage_permission, Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.BOTTOM, 0, TOAST_VERTICAL_OFFSET);
                        toast.show();
                    }
                });
                break;
            case EventBusMSG.TOAST_UNABLE_TO_WRITE_THE_FILE:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast toast = Toast.makeText(gpsApp.getApplicationContext(), R.string.export_unable_to_write_file, Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.BOTTOM, 0, TOAST_VERTICAL_OFFSET);
                        toast.show();
                    }
                });
        }
    }

    /**
     * @return true if a browser is installed on the device.
     */
    private Boolean isBrowserInstalled() {
        String url = "https://www.basicairdata.eu/projects/android/android-gps-logger/getting-started-guide-for-gps-logger/";
        Uri webAddress = Uri.parse(url);
        Intent intentWeb = new Intent(Intent.ACTION_VIEW, webAddress);
        intentWeb.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return (intentWeb.resolveActivity(getPackageManager()) != null);
    }

    /**
     * Expands/Collapses the bottom bar, basing on the active tab.
     */
    private void updateBottomSheetPosition() {
        gpsApp.setGPSActivityActiveTab(tabLayout.getSelectedTabPosition());
        if (gpsApp.getGPSActivityActiveTab() != 2) {
            bottomSheetBehavior.setPeekHeight(1);
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            //Log.w("myApp", "[#] GPSActivity.java - mBottomSheetBehavior.setPeekHeight(" + bottomSheet.getHeight() +");");
            bottomSheetBehavior.setPeekHeight(bottomSheet.getHeight());
        } else {
            bottomSheetBehavior.setPeekHeight(1);
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED) ;
        }
    }

    /**
     * Adds the 3 Fragments to the ViewPager Adapter.
     */
    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(new FragmentGPSFix(), getString(R.string.tab_gpsfix));
        adapter.addFragment(new FragmentTrack(), getString(R.string.tab_track));
        adapter.addFragment(new FragmentTracklist(), getString(R.string.tab_tracklist));
        viewPager.setAdapter(adapter);
    }

    /**
     * The ViewPager Adapter Class.
     */
    static class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> fragmentsList = new ArrayList<>();
        private final List<String> fragmentsTitleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            return fragmentsList.get(position);
        }

        @Override
        public int getCount() {
            return fragmentsList.size();
        }

        public void addFragment(Fragment fragment, String title) {
            fragmentsList.add(fragment);
            fragmentsTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return fragmentsTitleList.get(position);
        }
    }

    /**
     * Loads the preferences related to this Activity: the prefKeepScreenOn Flag.
     */
    private void loadPreferences() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if (preferences.getBoolean("prefKeepScreenOn", true)) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    /**
     * Performs all the operation in order to safely shutdown the Application:
     * it check if a Track is active and, if needed, shows a confirmation
     * dialog where asks to finalize the track.
     */
    private void ShutdownApp() {
        if ((gpsApp.getCurrentTrack().getNumberOfLocations() > 0)
                || (gpsApp.getCurrentTrack().getNumberOfPlacemarks() > 0)
                || (gpsApp.isRecording())
                || (gpsApp.isPlacemarkRequested())) {

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(getResources().getString(R.string.message_exit_finalizing));
            builder.setIcon(android.R.drawable.ic_menu_info_details);
            builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    gpsApp.setRecording(false);
                    gpsApp.setPlacemarkRequested(false);
                    EventBus.getDefault().post(EventBusMSG.NEW_TRACK);
                    gpsApp.stopAndUnbindGPSService();
                    gpsApp.setLocationPermissionChecked(false);

                    dialog.dismiss();
                    gpsApp.setJustStarted(true);
                    finish();
                }
            });
            builder.setNeutralButton(R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                }
            });
            builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    gpsApp.setRecording(false);
                    gpsApp.setPlacemarkRequested(false);
                    gpsApp.stopAndUnbindGPSService();
                    gpsApp.setLocationPermissionChecked(false);

                    dialog.dismiss();
                    gpsApp.setJustStarted(true);
                    finish();
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        } else {
            gpsApp.setRecording(false);
            gpsApp.setPlacemarkRequested(false);
            gpsApp.stopAndUnbindGPSService();
            gpsApp.setLocationPermissionChecked(false);

            finish();
        }
    }

    /**
     * Activates the Action Mode upper Toolbar if needed.
     * The Toolbar will be shown if the Tab 3 is active and one or more Tracks
     * are selected into the Tracklist.
     */
    private void activateActionModeIfNeeded() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if ((gpsApp.getNumberOfSelectedTracks() > 0) && (gpsApp.getGPSActivityActiveTab()  == 2)) {
                    if (actionMode == null) actionMode = (startSupportActionMode(new ToolbarActionMode()));
                    if (actionMode != null) actionMode.setTitle(gpsApp.getNumberOfSelectedTracks() > 1 ? String.valueOf(gpsApp.getNumberOfSelectedTracks()) : "");
                } else if (actionMode != null) {
                    actionMode.finish();
                    actionMode = null;
                }
            }
        });
    }

    /**
     * Checks that the Location permission is granted.
     * If not, requests it using the standard ActivityCompat.requestPermissions method.
     */
    public void checkLocationPermission() {
        Log.w("myApp", "[#] GPSActivity.java - Check Location Permission...");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.w("myApp", "[#] GPSActivity.java - Location Permission granted");
            // Permission Granted
        } else {
            Log.w("myApp", "[#] GPSActivity.java - Location Permission denied");
            boolean showRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION);
            if (showRationale || !gpsApp.isLocationPermissionChecked()) {
                Log.w("myApp", "[#] GPSActivity.java - Location Permission denied, need new check");
                List<String> listPermissionsNeeded = new ArrayList<>();
                listPermissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
                ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]) , REQUEST_ID_MULTIPLE_PERMISSIONS);
            }
        }
    }
}