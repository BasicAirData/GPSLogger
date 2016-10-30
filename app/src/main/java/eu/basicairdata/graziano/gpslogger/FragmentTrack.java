/*
 * FragmentTrack - Java Class for Android
 * Created by G.Capelli (BasicAirData) on 4/6/2016
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


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;


public class FragmentTrack extends Fragment {

    private TextView TVDuration;
    private TextView TVTrackName;
    private TextView TVTrackID;
    private TextView TVDistance;
    private TextView TVDistanceUM;
    private TextView TVMaxSpeed;
    private TextView TVMaxSpeedUM;
    private TextView TVAverageSpeed;
    private TextView TVAverageSpeedUM;
    private TextView TVAltitudeGap;
    private TextView TVAltitudeGapUM;
    private TextView TVOverallDirection;
    private TextView TVTrackStatus;
    private TextView TVDirectionUM;

    private TableLayout TLTrack;
    private TableLayout TLDuration;
    private TableLayout TLSpeedMax;
    private TableLayout TLSpeedAvg;
    private TableLayout TLDistance;
    private TableLayout TLAltitudeGap;
    private TableLayout TLOverallDirection;

    private String FTrackID = "";
    private String FTrackName = "";
    private String FDuration = "";
    private String FSpeedMax = "";
    private String FSpeedUM = "";
    private String FSpeedAvg = "";
    private String FDistance = "";
    private String FDistanceUM = "";
    private String FAltitudeGap = "";
    private String FAltitudeUM = "";
    private String FOverallDirection = "";


    public FragmentTrack() {
        // Required empty public constructor
    }

    @Subscribe
    public void onEvent(String msg) {
        if (msg.equals("UPDATE_TRACK")) {
            (getActivity()).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Update();
                }
            });
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
        View view = inflater.inflate(R.layout.fragment_track, container, false);

        TVDuration = (TextView) view.findViewById(R.id.id_textView_Duration);
        TVTrackID = (TextView) view.findViewById(R.id.id_textView_TrackIDLabel);
        TVTrackName = (TextView) view.findViewById(R.id.id_textView_TrackName);
        TVDistance = (TextView) view.findViewById(R.id.id_textView_Distance);
        TVMaxSpeed = (TextView) view.findViewById(R.id.id_textView_SpeedMax);
        TVAverageSpeed = (TextView) view.findViewById(R.id.id_textView_SpeedAvg);
        TVAltitudeGap = (TextView) view.findViewById(R.id.id_textView_AltitudeGap);
        TVOverallDirection = (TextView) view.findViewById(R.id.id_textView_OverallDirection);
        TVTrackStatus = (TextView) view.findViewById(R.id.id_textView_TrackStatus);
        TVDirectionUM = (TextView) view.findViewById(R.id.id_textView_OverallDirectionUM);

        TVDistanceUM = (TextView) view.findViewById(R.id.id_textView_DistanceUM);
        TVMaxSpeedUM = (TextView) view.findViewById(R.id.id_textView_SpeedMaxUM);
        TVAverageSpeedUM = (TextView) view.findViewById(R.id.id_textView_SpeedAvgUM);
        TVAltitudeGapUM = (TextView) view.findViewById(R.id.id_textView_AltitudeGapUM);

        TLTrack = (TableLayout) view.findViewById(R.id.id_tableLayout_TrackName) ;
        TLDuration = (TableLayout) view.findViewById(R.id.id_tableLayout_Duration) ;
        TLSpeedMax = (TableLayout) view.findViewById(R.id.id_tableLayout_SpeedMax) ;
        TLDistance = (TableLayout) view.findViewById(R.id.id_tableLayout_Distance) ;
        TLSpeedAvg = (TableLayout) view.findViewById(R.id.id_tableLayout_SpeedAvg) ;
        TLAltitudeGap = (TableLayout) view.findViewById(R.id.id_tableLayout_AltitudeGap) ;
        TLOverallDirection = (TableLayout) view.findViewById(R.id.id_tableLayout_OverallDirection) ;


        return view;
    }

    @Override
    public void onResume() {
        EventBus.getDefault().register(this);
        Update();
        super.onResume();
    }

    @Override
    public void onPause() {
        EventBus.getDefault().unregister(this);
        super.onPause();
    }

    public void Update() {
        GPSApplication gpsApplication = GPSApplication.getInstance();
        final Track track = gpsApplication.getCurrentTrack();
        final int prefDirections = gpsApplication.getPrefShowDirections();
        final boolean EGMAltitudeCorrection = gpsApplication.getPrefEGM96AltitudeCorrection();

        if (isAdded()) {
            if ((track != null) && (track.getNumberOfLocations() + track.getNumberOfPlacemarks() > 0)) {

                FTrackID = getString(R.string.track_id) + " " + String.valueOf(track.getId());
                FTrackName = track.getName();
                FDuration = track.getFormattedPrefTime();
                FSpeedMax = track.getFormattedSpeedMax();
                FSpeedUM = track.getFormattedSpeedUM();
                FSpeedAvg = track.getFormattedPrefSpeedAverage();
                FDistance = track.getFormattedDistance();
                FDistanceUM = track.getFormattedDistanceUM();
                FAltitudeGap = track.getFormattedAltitudeGap(EGMAltitudeCorrection);
                FOverallDirection = track.getFormattedBearing();
                FAltitudeUM = track.getFormattedAltitudeUM();

                //Log.w("myApp", "[#] FragmentTrack.java - duration = " + FDuration);

                TVTrackID.setText(FTrackID);
                TVTrackName.setText(FTrackName);
                TVDuration.setText(FDuration);
                TVMaxSpeed.setText(FSpeedMax);
                TVAverageSpeed.setText(FSpeedAvg);
                TVDistance.setText(FDistance);
                TVAltitudeGap.setText(FAltitudeGap);
                TVOverallDirection.setText(FOverallDirection);

                TVMaxSpeedUM.setText(FSpeedUM);
                TVAverageSpeedUM.setText(FSpeedUM);
                TVDistanceUM.setText(FDistanceUM);
                TVAltitudeGapUM.setText(FAltitudeUM);

                // Colorize the Altitude Gap textview depending on the altitude filter
                final boolean isValidAltitude = track.isValidAltitude();
                TVAltitudeGap.setTextColor(isValidAltitude ? getResources().getColor(R.color.textColorPrimary) : getResources().getColor(R.color.textColorSecondary));
                TVAltitudeGapUM.setTextColor(isValidAltitude ? getResources().getColor(R.color.textColorPrimary) : getResources().getColor(R.color.textColorSecondary));

                TVTrackStatus.setVisibility(View.GONE);

                TVDirectionUM.setVisibility(prefDirections == 0 ? View.GONE : View.VISIBLE);

                TLTrack.setVisibility(FTrackName.equals("") ? View.INVISIBLE : View.VISIBLE);
                TLDuration.setVisibility(FDuration.equals("") ? View.INVISIBLE : View.VISIBLE);
                TLSpeedMax.setVisibility(FSpeedMax.equals("") ? View.INVISIBLE : View.VISIBLE);
                TLSpeedAvg.setVisibility(FSpeedAvg.equals("") ? View.INVISIBLE : View.VISIBLE);
                TLDistance.setVisibility(FDistance.equals("") ? View.INVISIBLE : View.VISIBLE);
                TLOverallDirection.setVisibility(FOverallDirection.equals("") ? View.INVISIBLE : View.VISIBLE);
                TLAltitudeGap.setVisibility(FAltitudeGap.equals("") ? View.INVISIBLE : View.VISIBLE);

            } else {
                TVTrackStatus.setVisibility(View.VISIBLE);

                TLTrack.setVisibility(View.INVISIBLE);
                TLDuration.setVisibility(View.INVISIBLE);
                TLSpeedMax.setVisibility(View.INVISIBLE);
                TLSpeedAvg.setVisibility(View.INVISIBLE);
                TLDistance.setVisibility(View.INVISIBLE);
                TLOverallDirection.setVisibility(View.INVISIBLE);
                TLAltitudeGap.setVisibility(View.INVISIBLE);
            }
        }
    }
}