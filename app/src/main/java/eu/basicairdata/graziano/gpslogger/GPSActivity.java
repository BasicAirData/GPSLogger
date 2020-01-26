/**
 * GPSActivity - Java Class for Android
 * Created by G.Capelli (BasicAirData) on 5/5/2016
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
 *
 */

package eu.basicairdata.graziano.gpslogger;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.view.ActionMode;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.Toolbar;
import android.util.Log;
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

public class GPSActivity extends AppCompatActivity {

    private static final int REQUEST_ID_MULTIPLE_PERMISSIONS = 1;

    private final GPSApplication GPSApp = GPSApplication.getInstance();

    private Toolbar toolbar;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private ActionMode actionMode;
    private View bottomSheet;
    private MenuItem menutrackfinished = null;
    private int activeTab = 1;
    final Context context = this;

    private boolean prefKeepScreenOn = true;
    private boolean show_toast_grant_storage_permission = false;
    private int theme;

    private BottomSheetBehavior mBottomSheetBehavior;

    Toast ToastClickAgain;


    @Override
    public void onRestart(){
        Log.w("myApp", "[#] " + this + " - onRestart()");

        if (Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("prefColorTheme", "2")) != theme) {
            Log.w("myApp", "[#] GPSActivity.java - it needs to be recreated (Theme changed)");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // Normal behaviour for Android 5 +
                this.recreate();
            } else {
                // Workaround to a bug on Android 4.4.X platform (google won't fix because Android 4.4 is obsolete)
                // Android 4.4.X: taskAffinity & launchmode='singleTask' violating Activity's lifecycle
                // https://issuetracker.google.com/issues/36998700
                finish();
                startActivity(new Intent(this, getClass()));
            }
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
        super.onRestart();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.w("myApp", "[#] " + this + " - onCreate()");

        setTheme(R.style.MyMaterialTheme);
        theme = Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("prefColorTheme", "2"));

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
                        activeTab = tab.getPosition();
                        GPSApp.setGPSActivity_activeTab(activeTab);
                        updateBottomSheetPosition();
                        ActivateActionModeIfNeeded();
                    }
                });

        bottomSheet = findViewById( R.id.id_bottomsheet );

        mBottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        mBottomSheetBehavior.setHideable (false);

        ToastClickAgain = Toast.makeText(this, getString(R.string.toast_track_finished_click_again), Toast.LENGTH_SHORT);
    }


    @Override
    public void onStart() {
        Log.w("myApp", "[#] " + this + " - onStart()");
        super.onStart();
        activeTab = tabLayout.getSelectedTabPosition();
        GPSApp.setGPSActivity_activeTab(activeTab);
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
        LoadPreferences();
        EventBus.getDefault().post(EventBusMSG.APP_RESUME);
        if (menutrackfinished != null) menutrackfinished.setVisible(!GPSApp.getCurrentTrack().getName().equals(""));

        // Check for Location runtime Permissions (for Android 23+)
        if (!GPSApp.isLocationPermissionChecked()) {
            CheckLocationPermission();
            GPSApp.setLocationPermissionChecked(true);
        }

        ActivateActionModeIfNeeded();

        if (GPSApp.FlagExists(GPSApp.FLAG_RECORDING) && !GPSApp.getRecording()) {
            // The app is crashed in background
            Log.w("myApp", "[#] GPSActivity.java - THE APP HAS BEEN KILLED IN BACKGROUND DURING A RECORDING !!!");
            GPSApp.FlagRemove(GPSApp.FLAG_RECORDING);

            AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.StyledDialog));
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

        if (GPSApp.isJustStarted() && (GPSApp.getCurrentTrack().getNumberOfLocations() + GPSApp.getCurrentTrack().getNumberOfPlacemarks() > 0)) {
            Toast.makeText(this.context, getString(R.string.toast_active_track_not_empty), Toast.LENGTH_LONG).show();
            GPSApp.setJustStarted(false);
        } else GPSApp.setJustStarted(false);

        if (show_toast_grant_storage_permission) {
            Toast.makeText(this.context, getString(R.string.please_grant_storage_permission), Toast.LENGTH_LONG).show();
            show_toast_grant_storage_permission = false;
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
    public boolean onPrepareOptionsMenu(Menu menu) {
        menutrackfinished = menu.findItem(R.id.action_track_finished);
        menutrackfinished.setVisible(!GPSApp.getCurrentTrack().getName().equals(""));
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            GPSApp.setHandlerTimer(60000);
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        if (id == R.id.action_track_finished) {
            if (GPSApp.getNewTrackFlag()) {
                // This is the second click
                GPSApp.setNewTrackFlag(false);
                GPSApp.setRecording(false);
                EventBus.getDefault().post(EventBusMSG.NEW_TRACK);
                ToastClickAgain.cancel();
                Toast.makeText(this, getString(R.string.toast_track_saved_into_tracklist), Toast.LENGTH_SHORT).show();
            } else {
                // This is the first click
                GPSApp.setNewTrackFlag(true); // Start the timer
                ToastClickAgain.show();
            }
            return true;
        }
        if (id == R.id.action_about) {
            // Show About Dialog
            FragmentManager fm = getSupportFragmentManager();
            FragmentAboutDialog aboutDialog = new FragmentAboutDialog();
            aboutDialog.show(fm, "");
            return true;
        }
        if (id == R.id.action_shutdown) {
            ShutdownApp();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private void updateBottomSheetPosition() {
        activeTab = tabLayout.getSelectedTabPosition();
        if (activeTab != 2) {
            mBottomSheetBehavior.setPeekHeight(1);
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            //Log.w("myApp", "[#] GPSActivity.java - mBottomSheetBehavior.setPeekHeight(" + bottomSheet.getHeight() +");");
            mBottomSheetBehavior.setPeekHeight(bottomSheet.getHeight());
        } else {
            mBottomSheetBehavior.setPeekHeight(1);
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED) ;
        }
    }


    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(new FragmentGPSFix(), getString(R.string.tab_gpsfix));
        adapter.addFragment(new FragmentTrack(), getString(R.string.tab_track));
        adapter.addFragment(new FragmentTracklist(), getString(R.string.tab_tracklist));
        viewPager.setAdapter(adapter);
    }

    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }
    }

    @Subscribe
    public void onEvent(EventBusMSGNormal msg) {
        switch (msg.MSGType) {
            case EventBusMSG.TRACKLIST_SELECT:
            case EventBusMSG.TRACKLIST_DESELECT:
                ActivateActionModeIfNeeded();
        }
    }

    @Subscribe
    public void onEvent(Short msg) {
        switch (msg) {
            case EventBusMSG.REQUEST_ADD_PLACEMARK:
                // Show Placemark Dialog
                FragmentManager fm = getSupportFragmentManager();
                FragmentPlacemarkDialog placemarkDialog = new FragmentPlacemarkDialog();
                placemarkDialog.show(fm, "");
                break;
            case EventBusMSG.UPDATE_TRACKLIST:
            case EventBusMSG.NOTIFY_TRACKS_DELETED:
                ActivateActionModeIfNeeded();
                break;
            case EventBusMSG.APPLY_SETTINGS:
                LoadPreferences();
                break;

            case EventBusMSG.UPDATE_TRACK:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (menutrackfinished != null)
                            menutrackfinished.setVisible(!GPSApp.getCurrentTrack().getName().equals(""));
                    }
                });
                break;
            case EventBusMSG.TOAST_TRACK_EXPORTED:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, getString(R.string.toast_track_exported), Toast.LENGTH_LONG).show();
                    }
                });
                break;
            case EventBusMSG.TOAST_STORAGE_PERMISSION_REQUIRED:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, getString(R.string.please_grant_storage_permission), Toast.LENGTH_LONG).show();
                    }
                });
                break;
            case EventBusMSG.TOAST_UNABLE_TO_WRITE_THE_FILE:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, getString(R.string.export_unable_to_write_file), Toast.LENGTH_LONG).show();
                    }
                });
        }
    }

    private void LoadPreferences() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        prefKeepScreenOn = preferences.getBoolean("prefKeepScreenOn", true);
        if (prefKeepScreenOn) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void ShutdownApp()
    {
        if ((GPSApp.getCurrentTrack().getNumberOfLocations() > 0)
                || (GPSApp.getCurrentTrack().getNumberOfPlacemarks() > 0)
                || (GPSApp.getRecording())
                || (GPSApp.getPlacemarkRequest())) {

            AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.StyledDialog));
            builder.setMessage(getResources().getString(R.string.message_exit_finalizing));
            builder.setIcon(android.R.drawable.ic_menu_info_details);
            builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    GPSApp.setRecording(false);
                    GPSApp.setPlacemarkRequest(false);
                    EventBus.getDefault().post(EventBusMSG.NEW_TRACK);
                    GPSApp.StopAndUnbindGPSService();
                    GPSApp.setLocationPermissionChecked(false);

                    dialog.dismiss();
                    GPSApp.setJustStarted(true);
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
                    GPSApp.setRecording(false);
                    GPSApp.setPlacemarkRequest(false);
                    GPSApp.StopAndUnbindGPSService();
                    GPSApp.setLocationPermissionChecked(false);

                    dialog.dismiss();
                    GPSApp.setJustStarted(true);
                    finish();
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        } else {
            GPSApp.setRecording(false);
            GPSApp.setPlacemarkRequest(false);
            GPSApp.StopAndUnbindGPSService();
            GPSApp.setLocationPermissionChecked(false);

            finish();
        }
    }

    private void ActivateActionModeIfNeeded() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if ((GPSApp.getNumberOfSelectedTracks() > 0) && (activeTab == 2)) {
                    if (actionMode == null)
                        actionMode = (startSupportActionMode(new ToolbarActionMode()));
                    actionMode.setTitle(GPSApp.getNumberOfSelectedTracks() > 1 ? String.valueOf(GPSApp.getNumberOfSelectedTracks()) : "");
                } else if (actionMode != null) {
                    actionMode.finish();
                    actionMode = null;
                }
            }
        });
    }


    public boolean CheckLocationPermission() {
        Log.w("myApp", "[#] GPSActivity.java - Check Location Permission...");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.w("myApp", "[#] GPSActivity.java - Location Permission granted");
            return true;    // Permission Granted
        } else {
            Log.w("myApp", "[#] GPSActivity.java - Location Permission denied");
            boolean showRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION);
            if (showRationale || !GPSApp.isLocationPermissionChecked()) {
                Log.w("myApp", "[#] GPSActivity.java - Location Permission denied, need new check");
                List<String> listPermissionsNeeded = new ArrayList<>();
                listPermissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
                ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]) , REQUEST_ID_MULTIPLE_PERMISSIONS);
            }
            return false;
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
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
                            GPSApp.setGPSLocationUpdates(false);
                            GPSApp.setGPSLocationUpdates(true);
                            GPSApp.updateGPSLocationFrequency();
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
                            File sd = new File(Environment.getExternalStorageDirectory() + "/GPSLogger");
                            if (!sd.exists()) {
                                sd.mkdir();
                            }
                            sd = new File(Environment.getExternalStorageDirectory() + "/GPSLogger/AppData");
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
                                    egm96.LoadGridFromFile(Environment.getExternalStorageDirectory() + "/GPSLogger/AppData/WW15MGH.DAC", getApplicationContext().getFilesDir() + "/WW15MGH.DAC");
                                }
                            }

                            if (GPSApp.getJobsPending() > 0) GPSApp.ExecuteJob();

                        } else {
                            Log.w("myApp", "[#] GPSActivity.java - WRITE_EXTERNAL_STORAGE = PERMISSION_DENIED");
                            if (GPSApp.getJobsPending() > 0) {
                                // Shows toast "Unable to write the file"
                                show_toast_grant_storage_permission = true;
                                EventBus.getDefault().post(EventBusMSG.TOAST_STORAGE_PERMISSION_REQUIRED);
                                GPSApp.setJobsPending(0);
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
}
