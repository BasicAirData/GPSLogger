/*
 * GPSActivity - Java Class for Android
 * Created by G.Capelli (BasicAirData) on 5/5/2016
 * v.2.0.0
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
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GPSActivity extends AppCompatActivity {

    private static final int RESULT_SETTINGS = 1;

    private Toolbar toolbar;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private View bottomSheet;
    private MenuItem menutrackfinished = null;
    private int activeTab = 1;

    private boolean prefKeepScreenOn = true;

    private BottomSheetBehavior mBottomSheetBehavior;

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

        tabLayout.setOnTabSelectedListener(
                new TabLayout.ViewPagerOnTabSelectedListener(viewPager) {
                    @Override
                    public void onTabSelected(TabLayout.Tab tab) {
                        super.onTabSelected(tab);
                        activeTab = tab.getPosition();
                        updateBottomSheetPosition();
                    }
                });

        bottomSheet = findViewById( R.id.id_bottomsheet );

        mBottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        mBottomSheetBehavior.setHideable (false);

        activeTab = tabLayout.getSelectedTabPosition();

        // Check for runtime Permissions (for Android 23+)
        if (!GPSApplication.getInstance().isPermissionsChecked()) {
            GPSApplication.getInstance().setPermissionsChecked(true);
            CheckPermissions();
        }

        LoadPreferences();
    }

    @Override
    public void onResume() {
        EventBus.getDefault().register(this);
        Log.w("myApp", "[#] GPSActivity.java - onResume()");
        EventBus.getDefault().post("APP_RESUME");
        super.onResume();
        if (menutrackfinished != null) menutrackfinished.setVisible(!GPSApplication.getInstance().getCurrentTrack().getName().equals(""));
    }

    @Override
    public void onPause() {
        EventBus.getDefault().post("APP_PAUSE");
        Log.w("myApp", "[#] GPSActivity.java - onPause()");
        EventBus.getDefault().unregister(this);
        super.onPause();
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
            GPSApplication.getInstance().setRecording(false);
            EventBus.getDefault().post("NEW_TRACK");
            return true;
        }
        if (id == R.id.action_about) {
            // Show About Dialog
            FragmentManager fm = getSupportFragmentManager();
            FragmentAboutDialog aboutDialog = new FragmentAboutDialog();
            aboutDialog.show(fm, "");
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
    public void onEvent(String msg) {
        if (msg.equals("REQUEST_ADD_PLACEMARK")) {
            // Show Placemark Dialog
            FragmentManager fm = getSupportFragmentManager();
            FragmentPlacemarkDialog placemarkDialog = new FragmentPlacemarkDialog();
            placemarkDialog.show(fm, "");
        }
        if (msg.equals("APPLY_SETTINGS")) {
            LoadPreferences();
        }
        if (msg.equals("UPDATE_TRACK")) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (menutrackfinished != null) menutrackfinished.setVisible(!GPSApplication.getInstance().getCurrentTrack().getName().equals(""));
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

    public void CheckPermissions () {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
        } else Log.w("myApp", "[#] GPSActivity.java - ACCESS_FINE_LOCATION = PERMISSION_GRANTED");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.INTERNET}, 1);
        } else Log.w("myApp", "[#] GPSActivity.java - INTERNET = PERMISSION_GRANTED");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
        } else Log.w("myApp", "[#] GPSActivity.java - WRITE_EXTERNAL_STORAGE = PERMISSION_GRANTED");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 0: {       // ACCESS_FINE_LOCATION
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.w("myApp", "[#] GPSActivity.java - ACCESS_FINE_LOCATION = PERMISSION_GRANTED");
                } else {
                    Log.w("myApp", "[#] GPSActivity.java - ACCESS_FINE_LOCATION = PERMISSION_DENIED");
                }
                return;
            }
            case 1: {       // INTERNET
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.w("myApp", "[#] GPSActivity.java - INTERNET = PERMISSION_GRANTED");
                } else {
                    Log.w("myApp", "[#] GPSActivity.java - INTERNET = PERMISSION_DENIED");
                }
                return;
            }
            case 2: {       // WRITE_EXTERNAL_STORAGE
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
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
                            egm96.LoadGridFromFile(Environment.getExternalStorageDirectory() + "/GPSLogger/AppData/WW15MGH.DAC");
                        }
                    }
                } else {
                    Log.w("myApp", "[#] GPSActivity.java - WRITE_EXTERNAL_STORAGE = PERMISSION_DENIED");
                }
            }
        }
    }
}
