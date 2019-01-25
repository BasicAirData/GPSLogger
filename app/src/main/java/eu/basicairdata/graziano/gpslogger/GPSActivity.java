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

    private Toolbar toolbar;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private ActionMode actionMode;
    private View bottomSheet;
    private MenuItem menutrackfinished = null;
    private int activeTab = 1;
    final Context context = this;

    private boolean prefKeepScreenOn = true;

    private BottomSheetBehavior mBottomSheetBehavior;

    Toast ToastClickAgain;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_gps);
        toolbar = (Toolbar) findViewById(R.id.id_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        viewPager = (ViewPager) findViewById(R.id.id_viewpager);
        viewPager.setOffscreenPageLimit(3);

        setupViewPager(viewPager);

        tabLayout = (TabLayout) findViewById(R.id.id_tablayout);
        tabLayout.setTabMode(TabLayout.MODE_FIXED);
        tabLayout.setupWithViewPager(viewPager);

        tabLayout.addOnTabSelectedListener(
                new TabLayout.ViewPagerOnTabSelectedListener(viewPager) {
                    @Override
                    public void onTabSelected(TabLayout.Tab tab) {
                        super.onTabSelected(tab);
                        activeTab = tab.getPosition();
                        GPSApplication.getInstance().setGPSActivity_activeTab(activeTab);
                        updateBottomSheetPosition();
                        ActivateActionModeIfNeeded();
                    }
                });

        bottomSheet = findViewById( R.id.id_bottomsheet );

        mBottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        mBottomSheetBehavior.setHideable (false);

        activeTab = tabLayout.getSelectedTabPosition();
        GPSApplication.getInstance().setGPSActivity_activeTab(activeTab);

        ToastClickAgain = Toast.makeText(this, getString(R.string.toast_track_finished_click_again), Toast.LENGTH_SHORT);
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
        Log.w("myApp", "[#] GPSActivity.java - onResume()");
        LoadPreferences();
        EventBus.getDefault().post(EventBusMSG.APP_RESUME);
        if (menutrackfinished != null) menutrackfinished.setVisible(!GPSApplication.getInstance().getCurrentTrack().getName().equals(""));

        // Check for Location runtime Permissions (for Android 23+)
        if (!GPSApplication.getInstance().isLocationPermissionChecked()) {
            CheckLocationPermission();
            GPSApplication.getInstance().setLocationPermissionChecked(true);
        }

        ActivateActionModeIfNeeded();
    }

    @Override
    public void onPause() {
        super.onPause();
        EventBus.getDefault().post(EventBusMSG.APP_PAUSE);
        Log.w("myApp", "[#] GPSActivity.java - onPause()");
        EventBus.getDefault().unregister(this);
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
        menutrackfinished.setVisible(!GPSApplication.getInstance().getCurrentTrack().getName().equals(""));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            GPSApplication.getInstance().setHandlerTimer(60000);
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        if (id == R.id.action_track_finished) {
            if (GPSApplication.getInstance().getNewTrackFlag()) {
                // This is the second click
                GPSApplication.getInstance().setNewTrackFlag(false);
                GPSApplication.getInstance().setRecording(false);
                EventBus.getDefault().post(EventBusMSG.NEW_TRACK);
                ToastClickAgain.cancel();
                Toast.makeText(this, getString(R.string.toast_track_saved_into_tracklist), Toast.LENGTH_SHORT).show();
            } else {
                // This is the first click
                GPSApplication.getInstance().setNewTrackFlag(true); // Start the timer
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
            case EventBusMSG.TRACKLIST_SELECTION:
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
                            menutrackfinished.setVisible(!GPSApplication.getInstance().getCurrentTrack().getName().equals(""));
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
            case EventBusMSG.STORAGE_PERMISSION_REQUIRED:
                GPSApplication.getInstance().setJobsPending(0);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, getString(R.string.please_grant_storage_permission), Toast.LENGTH_LONG).show();
                    }
                });
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
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
        if ((GPSApplication.getInstance().getCurrentTrack().getNumberOfLocations() > 0)
                || (GPSApplication.getInstance().getCurrentTrack().getNumberOfPlacemarks() > 0)
                || (GPSApplication.getInstance().getRecording())
                || (GPSApplication.getInstance().getPlacemarkRequest())) {

            AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.StyledDialog));
            builder.setMessage(getResources().getString(R.string.message_exit_confirmation));
            builder.setIcon(android.R.drawable.ic_menu_info_details);
            builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    GPSApplication.getInstance().setRecording(false);
                    GPSApplication.getInstance().setPlacemarkRequest(false);
                    EventBus.getDefault().post(EventBusMSG.NEW_TRACK);
                    GPSApplication.getInstance().StopAndUnbindGPSService();
                    GPSApplication.getInstance().setLocationPermissionChecked(false);

                    dialog.dismiss();
                    finish();
                }
            });
            builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        } else {
            GPSApplication.getInstance().setRecording(false);
            GPSApplication.getInstance().setPlacemarkRequest(false);
            GPSApplication.getInstance().StopAndUnbindGPSService();
            GPSApplication.getInstance().setLocationPermissionChecked(false);

            finish();
        }
    }

    private void ActivateActionModeIfNeeded() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if ((GPSApplication.getInstance().getNumberOfSelectedTracks() > 0) && (activeTab == 2)) {
                    if (actionMode == null)
                        actionMode = (startSupportActionMode(new ToolbarActionMode()));
                    actionMode.setTitle(GPSApplication.getInstance().getNumberOfSelectedTracks() > 1 ? String.valueOf(GPSApplication.getInstance().getNumberOfSelectedTracks()) : "");
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
            if (showRationale || !GPSApplication.getInstance().isLocationPermissionChecked()) {
                Log.w("myApp", "[#] GPSActivity.java - Location Permission denied, need new check");
                List<String> listPermissionsNeeded = new ArrayList<>();
                listPermissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
                ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]) , REQUEST_ID_MULTIPLE_PERMISSIONS);
            } else {

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
                            Log.w("myApp", "[#] GPSActivity.java - ACCESS_FINE_LOCATION = PERMISSION_GRANTED");
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

                            if (GPSApplication.getInstance().getJobsPending() > 0) GPSApplication.getInstance().ExecuteJob();

                        } else {
                            Log.w("myApp", "[#] GPSActivity.java - WRITE_EXTERNAL_STORAGE = PERMISSION_DENIED");
                            if (GPSApplication.getInstance().getJobsPending() > 0) {
                                // Shows toast "Unable to write the file"
                                final Context context = this;
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(context, getString(R.string.export_unable_to_write_file), Toast.LENGTH_LONG).show();
                                    }
                                });
                                //EventBus.getDefault().post(new EventBusMSGNormal(EventBusMSG.TOAST_UNABLE_TO_WRITE_THE_FILE, MSG.id));
                                //Log.w("myApp", "[#] GPSActivity.java - EventBusMSG.TOAST_UNABLE_TO_WRITE_THE_FILE " + MSG.id);
                                GPSApplication.getInstance().setJobsPending(0);
                            }
                        }
                        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
                        SharedPreferences.Editor editor1 = settings.edit();
                        editor1.putBoolean("prefIsStoragePermissionChecked", true);
                        editor1.commit();
                        GPSApplication.getInstance().setStoragePermissionChecked(true);
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
