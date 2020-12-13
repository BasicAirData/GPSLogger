/**
 * FragmentGPSFix - Java Class for Android
 * Created by G.Capelli (BasicAirData) on 10/5/2016
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
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;

import java.util.Locale;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import static eu.basicairdata.graziano.gpslogger.GPSApplication.NOT_AVAILABLE;

public class FragmentGPSFix extends Fragment {

    private PhysicalDataFormatter phdformatter = new PhysicalDataFormatter();

    private boolean isAWarningClicked = false;

    private FrameLayout FLGPSFix;

    private TextView TVLatitude;
    private TextView TVLongitude;
    private TextView TVLatitudeUM;
    private TextView TVLongitudeUM;
    private TextView TVAltitude;
    private TextView TVAltitudeUM;
    private TextView TVSpeed;
    private TextView TVSpeedUM;
    private TextView TVBearing;
    private TextView TVAccuracy;
    private TextView TVAccuracyUM;
    private TextView TVGPSFixStatus;
    private TextView TVDirectionUM;
    private TextView TVTime;
    private TextView TVTimeLabel;
    private TextView TVSatellites;

    private CardView CVWarningLocationDenied;
    private CardView CVWarningGPSDisabled;
    private CardView CVWarningBackgroundRestricted;

    private TableLayout TLCoordinates;
    private TableLayout TLAltitude;
    private TableLayout TLSpeed;
    private TableLayout TLBearing;
    private TableLayout TLAccuracy;
    private TableLayout TLTime;
    private TableLayout TLSatellites;

    private LinearLayout LLTimeSatellites;

    private PhysicalData phdLatitude;
    private PhysicalData phdLongitude;
    private PhysicalData phdAltitude;
    private PhysicalData phdSpeed;
    private PhysicalData phdBearing;
    private PhysicalData phdAccuracy;
    private PhysicalData phdTime;

    final GPSApplication gpsApplication = GPSApplication.getInstance();

    public FragmentGPSFix() {
        // Required empty public constructor
    }

    @Subscribe (threadMode = ThreadMode.MAIN)
    public void onEvent(Short msg) {
        if (msg == EventBusMSG.UPDATE_FIX) {
            Update();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_gpsfix, container, false);

        // FrameLayouts
        FLGPSFix            = view.findViewById(R.id.id_fragmentgpsfixFrameLayout);

        // TextViews
        TVLatitude          = view.findViewById(R.id.id_textView_Latitude);
        TVLongitude         = view.findViewById(R.id.id_textView_Longitude);
        TVLatitudeUM        = view.findViewById(R.id.id_textView_LatitudeUM);
        TVLongitudeUM       = view.findViewById(R.id.id_textView_LongitudeUM);
        TVAltitude          = view.findViewById(R.id.id_textView_Altitude);
        TVAltitudeUM        = view.findViewById(R.id.id_textView_AltitudeUM);
        TVSpeed             = view.findViewById(R.id.id_textView_Speed);
        TVSpeedUM           = view.findViewById(R.id.id_textView_SpeedUM);
        TVBearing           = view.findViewById(R.id.id_textView_Bearing);
        TVAccuracy          = view.findViewById(R.id.id_textView_Accuracy);
        TVAccuracyUM        = view.findViewById(R.id.id_textView_AccuracyUM);
        TVGPSFixStatus      = view.findViewById(R.id.id_textView_GPSFixStatus);
        TVDirectionUM       = view.findViewById(R.id.id_textView_BearingUM);
        TVTime              = view.findViewById(R.id.id_textView_Time);
        TVTimeLabel         = view.findViewById(R.id.id_textView_TimeLabel);
        TVSatellites        = view.findViewById(R.id.id_textView_Satellites);

        CVWarningLocationDenied         = view.findViewById(R.id.card_view_warning_location_denied);
        CVWarningGPSDisabled            = view.findViewById(R.id.card_view_warning_enable_location_service);
        CVWarningBackgroundRestricted   = view.findViewById(R.id.card_view_warning_background_restricted);

        // TableLayouts
        TLCoordinates       = view.findViewById(R.id.id_TableLayout_Coordinates) ;
        TLAltitude          = view.findViewById(R.id.id_TableLayout_Altitude);
        TLSpeed             = view.findViewById(R.id.id_TableLayout_Speed);
        TLBearing           = view.findViewById(R.id.id_TableLayout_Bearing);
        TLAccuracy          = view.findViewById(R.id.id_TableLayout_Accuracy);
        TLTime              = view.findViewById(R.id.id_TableLayout_Time);
        TLSatellites        = view.findViewById(R.id.id_TableLayout_Satellites);

        // LinearLayouts
        LLTimeSatellites    = view.findViewById(R.id.id_linearLayout_Time_Satellites);

        CVWarningGPSDisabled.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isAWarningClicked && (GPSStatusValue == GPSStatus.GPS_DISABLED)) {
                    isAWarningClicked = true;
                    // Go to Settings screen
                    Intent callGPSSettingIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    try {
                        startActivityForResult(callGPSSettingIntent, 0);
                    } catch (Exception e) {
                        isAWarningClicked = false;
                        // Unable to open Intent
                    }
                }
            }
        });

        CVWarningBackgroundRestricted.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isAWarningClicked && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)) {
                    isAWarningClicked = true;
                    // Go to Settings screen
                    Intent callAppSettingIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getContext().getPackageName(), null);
                    callAppSettingIntent.setData(uri);

                    try {
                        startActivityForResult(callAppSettingIntent, 0);
                    } catch (Exception e) {
                        isAWarningClicked = false;
                        // Unable to open Intent
                    }
                }
            }
        });

        CVWarningLocationDenied.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isAWarningClicked) {
                    //isAWarningClicked = true;
                    GPSActivity gpsActivity = (GPSActivity) getActivity();
                    gpsActivity.CheckLocationPermission();
                }
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        isAWarningClicked = false;

        // Workaround for Nokia Devices, Android 9
        // https://github.com/BasicAirData/GPSLogger/issues/77
        if (EventBus.getDefault().isRegistered(this)) {
            //Log.w("myApp", "[#] FragmentGPSFix.java - EventBus: FragmentGPSFix already registered");
            EventBus.getDefault().unregister(this);
        }

        EventBus.getDefault().register(this);
        Update();
    }

    @Override
    public void onPause() {
        EventBus.getDefault().unregister(this);
        super.onPause();
    }


    private LocationExtended location;
    private double AltitudeManualCorrection;
    private int prefDirections;
    private GPSStatus GPSStatusValue = GPSStatus.GPS_DISABLED;
    private boolean EGMAltitudeCorrection;
    private boolean isValidAltitude;
    private boolean isBackgroundActivityRestricted;

    public void Update() {
        //Log.w("myApp", "[#] FragmentGPSFix.java - Update(Location location)");
        location = gpsApplication.getCurrentLocationExtended();
        AltitudeManualCorrection = gpsApplication.getPrefAltitudeCorrection();
        prefDirections = gpsApplication.getPrefShowDirections();
        GPSStatusValue = gpsApplication.getGPSStatus();
        EGMAltitudeCorrection = gpsApplication.getPrefEGM96AltitudeCorrection();
        isBackgroundActivityRestricted = gpsApplication.isBackgroundActivityRestricted();
        if (isAdded()) {
            if ((location != null) && (GPSStatusValue == GPSStatus.GPS_OK)) {

                phdLatitude = phdformatter.format(location.getLatitude(), PhysicalDataFormatter.FORMAT_LATITUDE);
                phdLongitude = phdformatter.format(location.getLongitude(), PhysicalDataFormatter.FORMAT_LONGITUDE);
                phdAltitude = phdformatter.format(location.getAltitudeCorrected(AltitudeManualCorrection, EGMAltitudeCorrection), PhysicalDataFormatter.FORMAT_ALTITUDE);
                phdSpeed = phdformatter.format(location.getSpeed(), PhysicalDataFormatter.FORMAT_SPEED);
                phdBearing = phdformatter.format(location.getBearing(), PhysicalDataFormatter.FORMAT_BEARING);
                phdAccuracy = phdformatter.format(location.getAccuracy(), PhysicalDataFormatter.FORMAT_ACCURACY);
                phdTime = phdformatter.format(location.getTime(), PhysicalDataFormatter.FORMAT_TIME);

                TVLatitude.setText(phdLatitude.Value);
                TVLongitude.setText(phdLongitude.Value);
                TVLatitudeUM.setText(phdLatitude.UM);
                TVLongitudeUM.setText(phdLongitude.UM);
                TVAltitude.setText(phdAltitude.Value);
                TVAltitudeUM.setText(phdAltitude.UM);
                TVSpeed.setText(phdSpeed.Value);
                TVSpeedUM.setText(phdSpeed.UM);
                TVBearing.setText(phdBearing.Value);
                TVAccuracy.setText(phdAccuracy.Value);
                TVAccuracyUM.setText(phdAccuracy.UM);
                TVTime.setText(phdTime.Value);
                TVTimeLabel.setText(phdTime.UM.isEmpty() ? getString(R.string.time) : String.format(Locale.getDefault(), "%s (%s)", getString(R.string.time), phdTime.UM));
                TVSatellites.setText(location.getNumberOfSatellitesUsedInFix() != NOT_AVAILABLE ? location.getNumberOfSatellitesUsedInFix() + "/" + location.getNumberOfSatellites() : "");

                // Colorize the Altitude textview depending on the altitude EGM Correction
                isValidAltitude = EGMAltitudeCorrection && (location.getAltitudeEGM96Correction() != NOT_AVAILABLE);
                TVAltitude.setTextColor(isValidAltitude ? getResources().getColor(R.color.textColorPrimary) : getResources().getColor(R.color.textColorSecondary));
                TVAltitudeUM.setTextColor(isValidAltitude ? getResources().getColor(R.color.textColorPrimary) : getResources().getColor(R.color.textColorSecondary));

                TVGPSFixStatus.setVisibility(View.GONE);

                TVDirectionUM.setVisibility(prefDirections == 0 ? View.GONE : View.VISIBLE);

                TLCoordinates.setVisibility(phdLatitude.Value.equals("") ? View.INVISIBLE : View.VISIBLE);
                TLAltitude.setVisibility(phdAltitude.Value.equals("") ? View.INVISIBLE : View.VISIBLE);
                TLSpeed.setVisibility(phdSpeed.Value.equals("") ? View.INVISIBLE : View.VISIBLE);
                TLBearing.setVisibility(phdBearing.Value.equals("") ? View.INVISIBLE : View.VISIBLE);
                TLAccuracy.setVisibility(phdAccuracy.Value.equals("") ? View.INVISIBLE : View.VISIBLE);
                TLTime.setVisibility(View.VISIBLE);
                TLSatellites.setVisibility(location.getNumberOfSatellitesUsedInFix() == NOT_AVAILABLE ? View.INVISIBLE : View.VISIBLE);

                FLGPSFix.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                    @Override
                    public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                               int oldLeft, int oldTop, int oldRight, int oldBottom) {
                        FLGPSFix.removeOnLayoutChangeListener(this);

                        int ViewHeight   = TLTime.getMeasuredHeight() + (int)(6*getResources().getDisplayMetrics().density);
                        int LayoutHeight = FLGPSFix.getHeight() - (int)(6*getResources().getDisplayMetrics().density);
                        //Log.w("myApp", "[#]");
                        //Log.w("myApp", "[#] -----------------------------------");
                        boolean isTimeAndSatellitesVisible;
                        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
                            isTimeAndSatellitesVisible = LayoutHeight >= 6*ViewHeight;
                            //Log.w("myApp", "[#] " + LayoutHeight + " / " + 6*ViewHeight + " -> " + isTimeAndSatellitesVisible);
                        } else {
                            isTimeAndSatellitesVisible = LayoutHeight >= 4*ViewHeight;
                            //Log.w("myApp", "[#] " + LayoutHeight + " / " + 4*ViewHeight + " -> " + isTimeAndSatellitesVisible);
                        }
                        LLTimeSatellites.setVisibility(isTimeAndSatellitesVisible ? View.VISIBLE : View.GONE);

                        //Log.w("myApp", "[#] -----------------------------------");
                        //Log.w("myApp", "[#] Available Height = " + LayoutHeight + " px");
                        //Log.w("myApp", "[#] Density          = " + getResources().getDisplayMetrics().density);
                        //Log.w("myApp", "[#] Tile Height      = " + ViewHeight + " px");
                    }
                });
                TVGPSFixStatus.setVisibility(View.INVISIBLE);
                CVWarningBackgroundRestricted.setVisibility(View.GONE);
                CVWarningGPSDisabled.setVisibility(View.GONE);
                CVWarningLocationDenied.setVisibility(View.GONE);
            } else {
                TLCoordinates.setVisibility(View.INVISIBLE);
                TLAltitude.setVisibility(View.INVISIBLE);
                TLSpeed.setVisibility(View.INVISIBLE);
                TLBearing.setVisibility(View.INVISIBLE);
                TLAccuracy.setVisibility(View.INVISIBLE);
                TLTime.setVisibility(View.INVISIBLE);
                TLSatellites.setVisibility(View.INVISIBLE);

                TVGPSFixStatus.setVisibility(View.VISIBLE);
                switch (GPSStatusValue) {
                    case GPS_DISABLED:
                        TVGPSFixStatus.setText(R.string.gps_disabled);
                        CVWarningGPSDisabled.setVisibility(View.VISIBLE);
                        break;
                    case GPS_OUTOFSERVICE:
                        TVGPSFixStatus.setText(R.string.gps_out_of_service);
                        CVWarningGPSDisabled.setVisibility(View.GONE);
                        break;
                    case GPS_TEMPORARYUNAVAILABLE:
                        //TVGPSFixStatus.setText(R.string.gps_temporary_unavailable);
                        //break;
                    case GPS_SEARCHING:
                        TVGPSFixStatus.setText(R.string.gps_searching);
                        CVWarningGPSDisabled.setVisibility(View.GONE);
                        break;
                    case GPS_STABILIZING:
                        TVGPSFixStatus.setText(R.string.gps_stabilizing);
                        CVWarningGPSDisabled.setVisibility(View.GONE);
                        break;
                }

                if (isBackgroundActivityRestricted) {
                    CVWarningBackgroundRestricted.setVisibility(View.VISIBLE);
                } else {
                    CVWarningBackgroundRestricted.setVisibility(View.GONE);
                }

                if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    TVGPSFixStatus.setText(R.string.gps_not_accessible);
                    CVWarningLocationDenied.setVisibility(View.VISIBLE);
                } else {
                    CVWarningLocationDenied.setVisibility(View.GONE);
                }
            }
        }
    }
}