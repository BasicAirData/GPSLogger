/*
 * FragmentTrack - Java Class for Android
 * Created by G.Capelli on 4/6/2016
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

import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * The Fragment that displays the information of the current Track
 * on the second tab (Track) of the main Activity (GPSActivity).
 */
public class FragmentTrack extends Fragment {

    private PhysicalDataFormatter phdformatter = new PhysicalDataFormatter();
    final GPSApplication gpsApp = GPSApplication.getInstance();

    private FrameLayout flTrack;

    private TextView tvDuration;
    private TextView tvTrackName;
    private TextView tvTrackID;
    private TextView tvDistance;
    private TextView tvDistanceUM;
    private TextView tvAnnotations;
    private TextView tvTrackpoints;
    private TextView tvMaxSpeed;
    private TextView tvMaxSpeedUM;
    private TextView tvAverageSpeed;
    private TextView tvAverageSpeedUM;
    private TextView tvAltitudeGap;
    private TextView tvAltitudeGapUM;
    private TextView tvOverallDirection;
    private TextView tvTrackStatus;
    private TextView tvDirectionUM;
    private TableLayout tlTrack;
    private TableLayout tlTrackpoints;
    private TableLayout tlAnnotations;
    private TableLayout tlDuration;
    private TableLayout tlSpeedMax;
    private TableLayout tlSpeedAvg;
    private TableLayout tlDistance;
    private TableLayout tlAltitudeGap;
    private TableLayout tlOverallDirection;

    private LinearLayout llTrackpointsAnnotations;

    private PhysicalData phdDuration;
    private PhysicalData phdSpeedMax;
    private PhysicalData phdSpeedAvg;
    private PhysicalData phdDistance;
    private PhysicalData phdAltitudeGap;
    private PhysicalData phdOverallDirection;

    private String fTrackID = "";
    private String fTrackName = "";
    private Track track;
    private int prefDirections;
    private boolean EGMAltitudeCorrection;
    private boolean isValidAltitude;

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
                flTrack.getViewTreeObserver().removeGlobalOnLayoutListener(this);
            } else {
                flTrack.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
            //int width  = flTrack.getMeasuredWidth();
            //int height = flTrack.getMeasuredHeight();
            //Log.w("myApp", "[#] FragmentTrack MEASURED: " + width + " x " + height);
            int viewHeight   = tlDistance.getMeasuredHeight() + (int)(6*getResources().getDisplayMetrics().density);
            int layoutHeight = flTrack.getHeight() - (int)(6*getResources().getDisplayMetrics().density);
            boolean isTimeAndSatellitesVisible;
            if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
                isTimeAndSatellitesVisible = layoutHeight >= 6*viewHeight;
                //Log.w("myApp", "[#] FragmentTrack MEASURED: " + layoutHeight + " / " + 6*viewHeight + " -> " + isTimeAndSatellitesVisible);
            } else {
                isTimeAndSatellitesVisible = layoutHeight >= 3.9*viewHeight;
                //Log.w("myApp", "[#] FragmentTrack MEASURED: " + layoutHeight + " / " + 3.9*viewHeight + " -> " + isTimeAndSatellitesVisible);
            }
            GPSApplication.getInstance().setSpaceForExtraTilesAvailable(isTimeAndSatellitesVisible);
            update();
        }
    };

    public FragmentTrack() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_track, container, false);

        // FrameLayouts
        flTrack = view.findViewById(R.id.id_fragmenttrackFrameLayout);

        // TextViews
        tvDuration = view.findViewById(R.id.id_textView_Duration);
        tvTrackID = view.findViewById(R.id.id_textView_TrackIDLabel);
        tvTrackName = view.findViewById(R.id.id_textView_TrackName);
        tvTrackpoints = view.findViewById(R.id.id_textView_Trackpoints);
        tvAnnotations = view.findViewById(R.id.id_textView_Annotations);
        tvDistance = view.findViewById(R.id.id_textView_Distance);
        tvMaxSpeed = view.findViewById(R.id.id_textView_SpeedMax);
        tvAverageSpeed = view.findViewById(R.id.id_textView_SpeedAvg);
        tvAltitudeGap = view.findViewById(R.id.id_textView_AltitudeGap);
        tvOverallDirection = view.findViewById(R.id.id_textView_OverallDirection);
        tvTrackStatus = view.findViewById(R.id.id_textView_TrackStatus);
        tvDirectionUM = view.findViewById(R.id.id_textView_OverallDirectionUM);
        tvDistanceUM = view.findViewById(R.id.id_textView_DistanceUM);
        tvMaxSpeedUM = view.findViewById(R.id.id_textView_SpeedMaxUM);
        tvAverageSpeedUM = view.findViewById(R.id.id_textView_SpeedAvgUM);
        tvAltitudeGapUM = view.findViewById(R.id.id_textView_AltitudeGapUM);

        // TableLayouts
        tlTrack = view.findViewById(R.id.id_tableLayout_TrackName) ;
        tlTrackpoints = view.findViewById(R.id.id_TableLayout_Trackpoints) ;
        tlAnnotations = view.findViewById(R.id.id_TableLayout_Annotations) ;
        tlDuration = view.findViewById(R.id.id_tableLayout_Duration) ;
        tlSpeedMax = view.findViewById(R.id.id_tableLayout_SpeedMax) ;
        tlDistance = view.findViewById(R.id.id_tableLayout_Distance) ;
        tlSpeedAvg = view.findViewById(R.id.id_tableLayout_SpeedAvg) ;
        tlAltitudeGap = view.findViewById(R.id.id_tableLayout_AltitudeGap) ;
        tlOverallDirection = view.findViewById(R.id.id_tableLayout_OverallDirection) ;

        // LinearLayouts
        llTrackpointsAnnotations = view.findViewById(R.id.id_linearLayout_Annotation_Trackpoints);

        tvTrackStatus.setText(getString(R.string.track_empty) + "\n\n" + getString(R.string.track_start_with_button_below));
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Workaround for Nokia Devices, Android 9
        // https://github.com/BasicAirData/GPSLogger/issues/77
        if (EventBus.getDefault().isRegistered(this)) {
            //Log.w("myApp", "[#] FragmentTrack.java - EventBus: FragmentTrack already registered");
            EventBus.getDefault().unregister(this);
        }

        EventBus.getDefault().register(this);

        ViewTreeObserver vto = flTrack.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(viewTreeObserverOnGLL);

        update();
    }

    @Override
    public void onPause() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            flTrack.getViewTreeObserver().removeGlobalOnLayoutListener(viewTreeObserverOnGLL);
        } else {
            flTrack.getViewTreeObserver().removeOnGlobalLayoutListener(viewTreeObserverOnGLL);
        }
        EventBus.getDefault().unregister(this);
        super.onPause();
    }

    /**
     * The EventBus receiver for Short Messages.
     */
    @Subscribe (threadMode = ThreadMode.MAIN)
    public void onEvent(Short msg) {
        if (msg == EventBusMSG.UPDATE_TRACK) {
            update();
        }
    }

    /**
     * Updates the user interface of the fragment.
     * It takes care of visibility and value of each tile, and Track Status widgets.
     */
    public void update() {
        //Log.w("myApp", "[#] FragmentTrack.java - Update");
        track = gpsApp.getCurrentTrack();
        prefDirections = gpsApp.getPrefShowDirections();
        EGMAltitudeCorrection = gpsApp.getPrefEGM96AltitudeCorrection();

        if (isAdded()) {
            if ((track != null) && (track.getNumberOfLocations() + track.getNumberOfPlacemarks() > 0)) {

                fTrackID = (track.getDescription().isEmpty() ?
                        getString(R.string.track_id) + " " + String.valueOf(track.getId()) :
                        track.getDescription());
                fTrackName = track.getName();
                phdDuration = phdformatter.format(track.getPrefTime(),PhysicalDataFormatter.FORMAT_DURATION);
                phdSpeedMax = phdformatter.format(track.getSpeedMax(),PhysicalDataFormatter.FORMAT_SPEED);
                phdSpeedAvg = phdformatter.format(track.getPrefSpeedAverage(),PhysicalDataFormatter.FORMAT_SPEED_AVG);
                phdDistance = phdformatter.format(track.getEstimatedDistance(),PhysicalDataFormatter.FORMAT_DISTANCE);
                phdAltitudeGap = phdformatter.format(track.getEstimatedAltitudeGap(EGMAltitudeCorrection),PhysicalDataFormatter.FORMAT_ALTITUDE);
                phdOverallDirection = phdformatter.format(track.getBearing(),PhysicalDataFormatter.FORMAT_BEARING);

                tvTrackID.setText(fTrackID);
                tvTrackName.setText(fTrackName);
                tvDuration.setText(phdDuration.value);
                tvMaxSpeed.setText(phdSpeedMax.value);
                tvAverageSpeed.setText(phdSpeedAvg.value);
                tvDistance.setText(phdDistance.value);
                tvAltitudeGap.setText(phdAltitudeGap.value);
                tvOverallDirection.setText(phdOverallDirection.value);

                tvMaxSpeedUM.setText(phdSpeedMax.um);
                tvAverageSpeedUM.setText(phdSpeedAvg.um);
                tvDistanceUM.setText(phdDistance.um);
                tvAltitudeGapUM.setText(phdAltitudeGap.um);

                llTrackpointsAnnotations.setVisibility(gpsApp.isSpaceForExtraTilesAvailable() ? View.VISIBLE : View.GONE);

                tvAnnotations.setText(String.valueOf(track.getNumberOfPlacemarks()));
                tvTrackpoints.setText(String.valueOf(track.getNumberOfLocations()));

                // Colorize the Altitude Gap textview depending on the altitude filter
                isValidAltitude = track.isValidAltitude();
                tvAltitudeGap.setTextColor(isValidAltitude ? getResources().getColor(R.color.textColorPrimary) : getResources().getColor(R.color.textColorSecondary));
                tvAltitudeGapUM.setTextColor(isValidAltitude ? getResources().getColor(R.color.textColorPrimary) : getResources().getColor(R.color.textColorSecondary));

                tvTrackStatus.setVisibility(View.INVISIBLE);
                tvDirectionUM.setVisibility(prefDirections == 0 ? View.GONE : View.VISIBLE);

                tlTrack.setVisibility(fTrackName.equals("") ? View.INVISIBLE : View.VISIBLE);
                tlDuration.setVisibility(phdDuration.value.equals("") ? View.INVISIBLE : View.VISIBLE);
                tlSpeedMax.setVisibility(phdSpeedMax.value.equals("") ? View.INVISIBLE : View.VISIBLE);
                tlSpeedAvg.setVisibility(phdSpeedAvg.value.equals("") ? View.INVISIBLE : View.VISIBLE);
                tlDistance.setVisibility(phdDistance.value.equals("") ? View.INVISIBLE : View.VISIBLE);
                tlOverallDirection.setVisibility(phdOverallDirection.value.equals("") ? View.INVISIBLE : View.VISIBLE);
                tlAltitudeGap.setVisibility(phdAltitudeGap.value.equals("") ? View.INVISIBLE : View.VISIBLE);
                tlTrackpoints.setVisibility(track.getNumberOfLocations() > 0 ? View.VISIBLE : View.INVISIBLE);
                tlAnnotations.setVisibility(track.getNumberOfPlacemarks() + track.getNumberOfLocations() > 0 ? View.VISIBLE : View.INVISIBLE);
            } else {
                tvTrackStatus.setVisibility(View.VISIBLE);

                tlTrack.setVisibility(View.INVISIBLE);
                tlDuration.setVisibility(View.INVISIBLE);
                tlSpeedMax.setVisibility(View.INVISIBLE);
                tlSpeedAvg.setVisibility(View.INVISIBLE);
                tlDistance.setVisibility(View.INVISIBLE);
                tlOverallDirection.setVisibility(View.INVISIBLE);
                tlAltitudeGap.setVisibility(View.INVISIBLE);
                tlTrackpoints.setVisibility(View.INVISIBLE);
                tlAnnotations.setVisibility(View.INVISIBLE);
            }
        }
    }
}