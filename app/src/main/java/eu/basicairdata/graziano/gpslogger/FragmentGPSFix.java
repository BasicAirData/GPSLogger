/*
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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;


public class FragmentGPSFix extends Fragment {

    public static final int GPS_DISABLED = 0;
    public static final int GPS_OUTOFSERVICE = 1;
    public static final int GPS_TEMPORARYUNAVAILABLE = 2;
    public static final int GPS_SEARCHING = 3;
    public static final int GPS_STABILIZING = 4;
    public static final int GPS_OK = 5;

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

    private TableLayout TLCoordinates;
    private TableLayout TLAltitude;
    private TableLayout TLSpeed;
    private TableLayout TLBearing;
    private TableLayout TLAccuracy;

    private String FLatitude = "";
    private String FLongitude = "";
    private String FLatitudeUM = "";
    private String FLongitudeUM = "";
    private String FAltitude = "";
    private String FAltitudeUM = "";
    private String FSpeed = "";
    private String FSpeedUM = "";
    private String FBearing = "";
    private String FAccuracy = "";



    public FragmentGPSFix() {
        // Required empty public constructor
    }

    @Subscribe
    public void onEvent(String msg) {
        //Log.w("myApp", "[#] FragmentGPSFix.java - onEvent!!!!");
        if (msg.equals("UPDATE_FIX")) {
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
        View view = inflater.inflate(R.layout.fragment_gpsfix, container, false);

        TVLatitude = (TextView) view.findViewById(R.id.id_textView_Latitude);
        TVLongitude = (TextView) view.findViewById(R.id.id_textView_Longitude);
        TVLatitudeUM = (TextView) view.findViewById(R.id.id_textView_LatitudeUM);
        TVLongitudeUM = (TextView) view.findViewById(R.id.id_textView_LongitudeUM);
        TVAltitude = (TextView) view.findViewById(R.id.id_textView_Altitude);
        TVAltitudeUM = (TextView) view.findViewById(R.id.id_textView_AltitudeUM);
        TVSpeed = (TextView) view.findViewById(R.id.id_textView_Speed);
        TVSpeedUM = (TextView) view.findViewById(R.id.id_textView_SpeedUM);
        TVBearing = (TextView) view.findViewById(R.id.id_textView_Bearing);
        TVAccuracy = (TextView) view.findViewById(R.id.id_textView_Accuracy);
        TVAccuracyUM = (TextView) view.findViewById(R.id.id_textView_AccuracyUM);
        TVGPSFixStatus = (TextView) view.findViewById(R.id.id_textView_GPSFixStatus);
        TVDirectionUM = (TextView) view.findViewById(R.id.id_textView_BearingUM);

        TLCoordinates = (TableLayout) view.findViewById(R.id.id_TableLayout_Coordinates) ;
        TLAltitude = (TableLayout) view.findViewById(R.id.id_TableLayout_Altitude);
        TLSpeed = (TableLayout) view.findViewById(R.id.id_TableLayout_Speed);
        TLBearing = (TableLayout) view.findViewById(R.id.id_TableLayout_Bearing);
        TLAccuracy = (TableLayout) view.findViewById(R.id.id_TableLayout_Accuracy);

        return view;
    }

    @Override
    public void onResume() {
        //Log.w("myApp", "[#] FragmentGPSFix: onResume() - " + FLatitude);
        EventBus.getDefault().register(this);
        Update();
        super.onResume();
    }

    @Override
    public void onPause() {
        EventBus.getDefault().unregister(this);
        //Log.w("myApp", "[#] FragmentGPSFix: onPause()");
        super.onPause();
    }

    public void Update() {
        //Log.w("myApp", "[#] FragmentGPSFix.java - Update(Location location)");
        GPSApplication gpsApplication = GPSApplication.getInstance();
        final LocationExtended location = gpsApplication.getCurrentLocationExtended();
        final double AltitudeManualCorrection = gpsApplication.getPrefAltitudeCorrection();
        int prefDirections = gpsApplication.getPrefShowDirections();
        final int GPSStatus = gpsApplication.getGPSStatus();
        final boolean EGMAltitudeCorrection = gpsApplication.getPrefEGM96AltitudeCorrection();
        if (isAdded()) {
            if ((location != null) && (GPSStatus == GPS_OK)) {

                FLatitude = location.getFormattedLatitude();
                FLongitude = location.getFormattedLongitude();
                FLatitudeUM = location.getFormattedLatitudeUM();
                FLongitudeUM = location.getFormattedLongitudeUM();
                FAltitude = location.getFormattedAltitudeCorrected(AltitudeManualCorrection, EGMAltitudeCorrection);
                FAltitudeUM = location.getFormattedAltitudeUM();
                FSpeed = location.getFormattedSpeed();
                FSpeedUM = location.getFormattedSpeedUM();
                FBearing = location.getFormattedBearing();
                FAccuracy = location.getFormattedAccuracy();

                TVLatitude.setText(FLatitude);
                TVLongitude.setText(FLongitude);
                TVLatitudeUM.setText(FLatitudeUM);
                TVLongitudeUM.setText(FLongitudeUM);
                TVAltitude.setText(FAltitude);
                TVAltitudeUM.setText(FAltitudeUM);
                TVSpeed.setText(FSpeed);
                TVSpeedUM.setText(FSpeedUM);
                TVBearing.setText(FBearing);
                TVAccuracy.setText(FAccuracy);
                TVAccuracyUM.setText(FAltitudeUM);

                TVGPSFixStatus.setVisibility(View.GONE);

                TVDirectionUM.setVisibility(prefDirections == 0 ? View.GONE : View.VISIBLE);

                TLCoordinates.setVisibility(FLatitude.equals("") ? View.INVISIBLE : View.VISIBLE);
                TLAltitude.setVisibility(FAltitude.equals("") ? View.INVISIBLE : View.VISIBLE);
                TLSpeed.setVisibility(FSpeed.equals("") ? View.INVISIBLE : View.VISIBLE);
                TLBearing.setVisibility(FBearing.equals("") ? View.INVISIBLE : View.VISIBLE);
                TLAccuracy.setVisibility(FAccuracy.equals("") ? View.INVISIBLE : View.VISIBLE);

            } else {
                TLCoordinates.setVisibility(View.INVISIBLE);
                TLAltitude.setVisibility(View.INVISIBLE);
                TLSpeed.setVisibility(View.INVISIBLE);
                TLBearing.setVisibility(View.INVISIBLE);
                TLAccuracy.setVisibility(View.INVISIBLE);

                TVGPSFixStatus.setVisibility(View.VISIBLE);
                switch (GPSStatus) {
                    case GPS_DISABLED:
                        TVGPSFixStatus.setText(R.string.gps_disabled);
                        break;
                    case GPS_OUTOFSERVICE:
                        TVGPSFixStatus.setText(R.string.gps_out_of_service);
                        break;
                    case GPS_TEMPORARYUNAVAILABLE:
                        TVGPSFixStatus.setText(R.string.gps_temporary_unavailable);
                        break;
                    case GPS_SEARCHING:
                        TVGPSFixStatus.setText(R.string.gps_searching);
                        break;
                    case GPS_STABILIZING:
                        TVGPSFixStatus.setText(R.string.gps_stabilizing);
                        break;
                }
            }
        }
    }
}