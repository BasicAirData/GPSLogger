/*
 * FragmentGPSFix - Java Class for Android
 * Created by G.Capelli on 10/5/2016
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
 *
 */

package eu.basicairdata.graziano.gpslogger;

import static android.content.Context.CLIPBOARD_SERVICE;
import static android.content.Context.POWER_SERVICE;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.os.PowerManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import static eu.basicairdata.graziano.gpslogger.GPSApplication.NOT_AVAILABLE;
import static eu.basicairdata.graziano.gpslogger.GPSApplication.GPS_DISABLED;
import static eu.basicairdata.graziano.gpslogger.GPSApplication.GPS_OUTOFSERVICE;
import static eu.basicairdata.graziano.gpslogger.GPSApplication.GPS_TEMPORARYUNAVAILABLE;
import static eu.basicairdata.graziano.gpslogger.GPSApplication.GPS_SEARCHING;
import static eu.basicairdata.graziano.gpslogger.GPSApplication.GPS_STABILIZING;
import static eu.basicairdata.graziano.gpslogger.GPSApplication.GPS_OK;
import static eu.basicairdata.graziano.gpslogger.GPSApplication.TOAST_VERTICAL_OFFSET;

/**
 * The Fragment that displays the information of the Fix
 * on the first tab (GPS FIX) of the main Activity (GPSActivity).
 */
public class FragmentGPSFix extends Fragment {

    private final PhysicalDataFormatter phdformatter = new PhysicalDataFormatter();
    private final GPSApplication gpsApp = GPSApplication.getInstance();
    private boolean isAWarningClicked = false;

    private FrameLayout flGPSFix;
    private TextView tvLatitude;
    private TextView tvLongitude;
    private TextView tvLatitudeUM;
    private TextView tvLongitudeUM;
    private TextView tvAltitude;
    private TextView tvAltitudeUM;
    private TextView tvSpeed;
    private TextView tvSpeedUM;
    private TextView tvBearing;
    private TextView tvAccuracy;
    private TextView tvAccuracyUM;
    private TextView tvGPSFixStatus;
    private TextView tvDirectionUM;
    private TextView tvTime;
    private TextView tvTimeLabel;
    private TextView tvSatellites;
    private TableLayout tlCoordinates;
    private TableLayout tlAltitude;
    private TableLayout tlSpeed;
    private TableLayout tlBearing;
    private TableLayout tlAccuracy;
    private TableLayout tlTime;
    private TableLayout tlSatellites;
    private CardView cvWarningLocationDenied;
    private CardView cvWarningGPSDisabled;
    private CardView cvWarningBackgroundRestricted;
    private CardView cvWarningBatteryOptimised;
    private LinearLayout llTimeSatellites;
    private ImageView iwWarningBatteryOptimisedClose;
    private ImageView iwCopyCoordinatesToClipboard;


    private PhysicalData phdLatitude;
    private PhysicalData phdLongitude;
    private PhysicalData phdAltitude;
    private PhysicalData phdSpeed;
    private PhysicalData phdBearing;
    private PhysicalData phdAccuracy;
    private PhysicalData phdTime;

    private LocationExtended location;
    private double AltitudeManualCorrection;
    private int prefDirections;
    private int GPSStatus = GPS_DISABLED;
    private boolean EGMAltitudeCorrection;
    private boolean isValidAltitude;
    private boolean isBackgroundActivityRestricted;
    private PowerManager powerManager;

    /**
     * The Observer that calculate the new available height when the layout is changed.
     * If the height is enough, it set the setSpaceForExtraTilesAvailable flag
     * that enable the visualization of the extra tiles:
     * <ul>
     *     <li>Time and Satellites for FragmentGPSFix</li>
     *     <li>Trackpoints ane Annotation for FragmentTrack</li>
     *  * </ul>
     */
    ViewTreeObserver.OnGlobalLayoutListener viewTreeObserverOnGLL = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                flGPSFix.getViewTreeObserver().removeGlobalOnLayoutListener(this);
            } else {
                flGPSFix.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
            //int width  = flGPSFix.getMeasuredWidth();
            //int height = flGPSFix.getMeasuredHeight();
            //Log.w("myApp", "[#] FragmentGPSFix MEASURED: " + width + " x " + height);
            int viewHeight   = tlSpeed.getMeasuredHeight() + (int)(6*getResources().getDisplayMetrics().density);
            int layoutHeight = flGPSFix.getHeight() - (int)(6*getResources().getDisplayMetrics().density);
            boolean isTimeAndSatellitesVisible;
            if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
                isTimeAndSatellitesVisible = layoutHeight >= 6*viewHeight;
                //Log.w("myApp", "[#] FragmentGPSFix MEASURED: " + layoutHeight + " / " + 6*viewHeight + " -> " + isTimeAndSatellitesVisible);
            } else {
                isTimeAndSatellitesVisible = layoutHeight >= 3.9*viewHeight;
                //Log.w("myApp", "[#] FragmentGPSFix MEASURED: " + layoutHeight + " / " + 3.9*viewHeight + " -> " + isTimeAndSatellitesVisible);
            }
            GPSApplication.getInstance().setSpaceForExtraTilesAvailable(isTimeAndSatellitesVisible);
            update();
        }
    };

    public FragmentGPSFix() {
        // Required empty public constructor
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
        flGPSFix = view.findViewById(R.id.id_fragmentgpsfixFrameLayout);

        // TextViews
        tvLatitude = view.findViewById(R.id.id_textView_Latitude);
        tvLongitude = view.findViewById(R.id.id_textView_Longitude);
        tvLatitudeUM = view.findViewById(R.id.id_textView_LatitudeUM);
        tvLongitudeUM = view.findViewById(R.id.id_textView_LongitudeUM);
        tvAltitude = view.findViewById(R.id.id_textView_Altitude);
        tvAltitudeUM = view.findViewById(R.id.id_textView_AltitudeUM);
        tvSpeed = view.findViewById(R.id.id_textView_Speed);
        tvSpeedUM = view.findViewById(R.id.id_textView_SpeedUM);
        tvBearing = view.findViewById(R.id.id_textView_Bearing);
        tvAccuracy = view.findViewById(R.id.id_textView_Accuracy);
        tvAccuracyUM = view.findViewById(R.id.id_textView_AccuracyUM);
        tvGPSFixStatus = view.findViewById(R.id.id_textView_GPSFixStatus);
        tvDirectionUM = view.findViewById(R.id.id_textView_BearingUM);
        tvTime = view.findViewById(R.id.id_textView_Time);
        tvTimeLabel = view.findViewById(R.id.id_textView_TimeLabel);
        tvSatellites = view.findViewById(R.id.id_textView_Satellites);

        cvWarningLocationDenied = view.findViewById(R.id.card_view_warning_location_denied);
        cvWarningGPSDisabled = view.findViewById(R.id.card_view_warning_enable_location_service);
        cvWarningBackgroundRestricted = view.findViewById(R.id.card_view_warning_background_restricted);
        cvWarningBatteryOptimised = view.findViewById(R.id.card_view_warning_battery_optimised);

        // TableLayouts
        tlCoordinates = view.findViewById(R.id.id_TableLayout_Coordinates) ;
        tlAltitude = view.findViewById(R.id.id_TableLayout_Altitude);
        tlSpeed = view.findViewById(R.id.id_TableLayout_Speed);
        tlBearing = view.findViewById(R.id.id_TableLayout_Bearing);
        tlAccuracy = view.findViewById(R.id.id_TableLayout_Accuracy);
        tlTime = view.findViewById(R.id.id_TableLayout_Time);
        tlSatellites = view.findViewById(R.id.id_TableLayout_Satellites);

        // ImageViews

        iwWarningBatteryOptimisedClose = view.findViewById(R.id.id_warning_battery_optimised_close);
        iwCopyCoordinatesToClipboard = view.findViewById(R.id.id_coordinates_copy);

        // LinearLayouts
        llTimeSatellites = view.findViewById(R.id.id_linearLayout_Time_Satellites);

        powerManager = (PowerManager) gpsApp.getSystemService(POWER_SERVICE);

        cvWarningGPSDisabled.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isAWarningClicked && (GPSStatus == GPS_DISABLED)) {
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

        iwWarningBatteryOptimisedClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gpsApp.setBatteryOptimisedWarningVisible(false);
                update();
            }
        });

        iwCopyCoordinatesToClipboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(CLIPBOARD_SERVICE);
                // Coordinates GG°MM'SS.SSSS"
//                ClipData clip = ClipData.newPlainText("Coordinates",
//                        Location.convert(Math.abs(location.getLatitude()), Location.FORMAT_SECONDS)
//                                .replaceFirst(":", "°")
//                                .replaceFirst(":", "'")
//                                .concat(location.getLatitude() >= 0 ? "\"N " : "\"S ")
//                           + Location.convert(Math.abs(location.getLongitude()), Location.FORMAT_SECONDS)
//                                .replaceFirst(":", "°")
//                                .replaceFirst(":", "'")
//                                .concat(location.getLongitude() >= 0 ? "\"E" : "\"W"));
                // Coordinates GG.GGGGGGGGG, to be preferred
                ClipData clip = ClipData.newPlainText("Coordinates",
                        String.format(Locale.US, "%.9f", location.getLatitude())
                                + ", "
                                + String.format(Locale.US, "%.9f", location.getLongitude())
                );
                clipboard.setPrimaryClip(clip);
                Toast toast = Toast.makeText(gpsApp.getApplicationContext(),
                        gpsApp.getString(R.string.toast_coordinates_copied_to_clipboard), Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.BOTTOM, 0, TOAST_VERTICAL_OFFSET);
                toast.show();
            }
        });

        cvWarningBackgroundRestricted.setOnClickListener(new View.OnClickListener() {
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

        cvWarningBatteryOptimised.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isAWarningClicked && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)) {
                    isAWarningClicked = true;
                    // Go to Settings screen
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);

                    try {
                        getContext().startActivity(intent);
                    } catch (Exception e) {
                        isAWarningClicked = false;
                        // Unable to open Intent
                    }
                }
            }
        });

        cvWarningLocationDenied.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isAWarningClicked) {
                    //isAWarningClicked = true;
                    GPSActivity gpsActivity = (GPSActivity) getActivity();
                    gpsActivity.checkLocationAndNotificationPermission();
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

        ViewTreeObserver vto = flGPSFix.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(viewTreeObserverOnGLL);

        update();
    }

    @Override
    public void onPause() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            flGPSFix.getViewTreeObserver().removeGlobalOnLayoutListener(viewTreeObserverOnGLL);
        } else {
            flGPSFix.getViewTreeObserver().removeOnGlobalLayoutListener(viewTreeObserverOnGLL);
        }

        EventBus.getDefault().unregister(this);
        super.onPause();
    }

    /**
     * The EventBus receiver for Short Messages.
     */
    @Subscribe (threadMode = ThreadMode.MAIN)
    public void onEvent(Short msg) {
        if (msg == EventBusMSG.UPDATE_FIX) {
            update();
        }
    }

    /**
     * Updates the user interface of the fragment.
     * It takes care of visibility and value of each tile, messages, and GPS Status widgets.
     */
    public void update() {
        //Log.w("myApp", "[#] FragmentGPSFix.java - Update");
        location = gpsApp.getCurrentLocationExtended();
        AltitudeManualCorrection = gpsApp.getPrefAltitudeCorrection();
        prefDirections = gpsApp.getPrefShowDirections();
        GPSStatus = gpsApp.getGPSStatus();
        EGMAltitudeCorrection = gpsApp.getPrefEGM96AltitudeCorrection();
        isBackgroundActivityRestricted = gpsApp.isBackgroundActivityRestricted();
        if (isAdded()) {
            if ((location != null) && (GPSStatus == GPS_OK)) {

                phdLatitude = phdformatter.format(location.getLatitude(), PhysicalDataFormatter.FORMAT_LATITUDE);
                phdLongitude = phdformatter.format(location.getLongitude(), PhysicalDataFormatter.FORMAT_LONGITUDE);
                phdAltitude = phdformatter.format(location.getAltitudeCorrected(AltitudeManualCorrection, EGMAltitudeCorrection), PhysicalDataFormatter.FORMAT_ALTITUDE);
                phdSpeed = phdformatter.format(location.getSpeed(), PhysicalDataFormatter.FORMAT_SPEED);
                phdBearing = phdformatter.format(location.getBearing(), PhysicalDataFormatter.FORMAT_BEARING);
                phdAccuracy = phdformatter.format(location.getAccuracy(), PhysicalDataFormatter.FORMAT_ACCURACY);
                phdTime = phdformatter.format(location.getTime(), PhysicalDataFormatter.FORMAT_TIME);

                tvLatitude.setText(phdLatitude.value);
                tvLongitude.setText(phdLongitude.value);
                tvLatitudeUM.setText(phdLatitude.um);
                tvLongitudeUM.setText(phdLongitude.um);
                tvAltitude.setText(phdAltitude.value);
                tvAltitudeUM.setText(phdAltitude.um);
                tvSpeed.setText(phdSpeed.value);
                tvSpeedUM.setText(phdSpeed.um);
                tvBearing.setText(phdBearing.value);
                tvAccuracy.setText(phdAccuracy.value);
                tvAccuracyUM.setText(phdAccuracy.um);
                tvTime.setText(phdTime.value);
                tvTimeLabel.setText(phdTime.um.isEmpty() ? getString(R.string.time) : String.format(Locale.getDefault(), "%s (%s)", getString(R.string.time), phdTime.um));
                tvSatellites.setText(location.getNumberOfSatellitesUsedInFix() != NOT_AVAILABLE ? location.getNumberOfSatellitesUsedInFix() + "/" + location.getNumberOfSatellites() : "");

                // Colorize the Altitude textview depending on the altitude EGM Correction
                isValidAltitude = EGMAltitudeCorrection && (location.getAltitudeEGM96Correction() != NOT_AVAILABLE);
                tvAltitude.setTextColor(isValidAltitude ? getResources().getColor(R.color.textColorPrimary) : getResources().getColor(R.color.textColorSecondary));
                tvAltitudeUM.setTextColor(isValidAltitude ? getResources().getColor(R.color.textColorPrimary) : getResources().getColor(R.color.textColorSecondary));

                tvGPSFixStatus.setVisibility(View.GONE);

                tvDirectionUM.setVisibility(prefDirections == 0 ? View.GONE : View.VISIBLE);

                tlCoordinates.setVisibility(phdLatitude.value.equals("") ? View.INVISIBLE : View.VISIBLE);
                tlAltitude.setVisibility(phdAltitude.value.equals("") ? View.INVISIBLE : View.VISIBLE);
                tlSpeed.setVisibility(phdSpeed.value.equals("") ? View.INVISIBLE : View.VISIBLE);
                tlBearing.setVisibility(phdBearing.value.equals("") ? View.INVISIBLE : View.VISIBLE);
                tlAccuracy.setVisibility(phdAccuracy.value.equals("") ? View.INVISIBLE : View.VISIBLE);
                tlTime.setVisibility(View.VISIBLE);
                tlSatellites.setVisibility(location.getNumberOfSatellitesUsedInFix() == NOT_AVAILABLE ? View.INVISIBLE : View.VISIBLE);

                llTimeSatellites.setVisibility(gpsApp.isSpaceForExtraTilesAvailable() ? View.VISIBLE : View.GONE);

                tvGPSFixStatus.setVisibility(View.INVISIBLE);
                cvWarningBackgroundRestricted.setVisibility(View.GONE);
                cvWarningBatteryOptimised.setVisibility(View.GONE);
                cvWarningGPSDisabled.setVisibility(View.GONE);
                cvWarningLocationDenied.setVisibility(View.GONE);
            } else {
                tlCoordinates.setVisibility(View.INVISIBLE);
                tlAltitude.setVisibility(View.INVISIBLE);
                tlSpeed.setVisibility(View.INVISIBLE);
                tlBearing.setVisibility(View.INVISIBLE);
                tlAccuracy.setVisibility(View.INVISIBLE);
                tlTime.setVisibility(View.INVISIBLE);
                tlSatellites.setVisibility(View.INVISIBLE);

                String ssat = "";
                if (((GPSStatus == GPS_SEARCHING) || (GPSStatus == GPS_STABILIZING) || (GPSStatus == GPS_TEMPORARYUNAVAILABLE)) && (gpsApp.getNumberOfSatellitesUsedInFix() != NOT_AVAILABLE)) {
                    ssat = "\n\n" + gpsApp.getNumberOfSatellitesUsedInFix() + "/" + gpsApp.getNumberOfSatellitesTotal() + " " + getString(R.string.satellites);
                }

                tvGPSFixStatus.setVisibility(View.VISIBLE);
                switch (GPSStatus) {
                    case GPS_DISABLED:
                        tvGPSFixStatus.setText(R.string.gps_disabled);
                        cvWarningGPSDisabled.setVisibility(View.VISIBLE);
                        break;
                    case GPS_OUTOFSERVICE:
                        tvGPSFixStatus.setText(R.string.gps_out_of_service);
                        cvWarningGPSDisabled.setVisibility(View.GONE);
                        break;
                    case GPS_TEMPORARYUNAVAILABLE:
                    case GPS_SEARCHING:
                        tvGPSFixStatus.setText(getString(R.string.gps_searching) + ssat);
                        cvWarningGPSDisabled.setVisibility(View.GONE);
                        break;
                    case GPS_STABILIZING:
                        tvGPSFixStatus.setText(getString(R.string.gps_stabilizing) + ssat);
                        cvWarningGPSDisabled.setVisibility(View.GONE);
                        break;
                }

                if (isBackgroundActivityRestricted) {
                    cvWarningBackgroundRestricted.setVisibility(View.VISIBLE);
                } else {
                    cvWarningBackgroundRestricted.setVisibility(View.GONE);
                }

                if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                        && !powerManager.isIgnoringBatteryOptimizations(gpsApp.getPackageName())
                        && gpsApp.isBatteryOptimisedWarningVisible()) {
                    cvWarningBatteryOptimised.setVisibility(View.VISIBLE);
                } else {
                    cvWarningBatteryOptimised.setVisibility(View.GONE);
                }

                if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    tvGPSFixStatus.setText(R.string.gps_not_accessible);
                    cvWarningLocationDenied.setVisibility(View.VISIBLE);
                } else {
                    cvWarningLocationDenied.setVisibility(View.GONE);
                }
            }
        }
    }
}