/*
 * FragmentRecordingControls - Java Class for Android
 * Created by G.Capelli (BasicAirData) on 20/5/2016
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


import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

public class FragmentRecordingControls extends Fragment{

    public FragmentRecordingControls() {
        // Required empty public constructor
    }

    TableLayout tableLayoutGeoPoints;
    TableLayout tableLayoutPlacemarks;

    private TextView TVGeoPoints;
    private TextView TVPlacemarks;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recording_controls, container, false);

        tableLayoutGeoPoints = (TableLayout) view.findViewById(R.id.id_TableLayout_GeoPoints);
        tableLayoutGeoPoints.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ontoggleRecordGeoPoint(v);
            }
        });

        tableLayoutPlacemarks = (TableLayout) view.findViewById(R.id.id_TableLayout_Placemarks);
        tableLayoutPlacemarks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onPlacemarkRequest(v);
            }
        });

        TVGeoPoints = (TextView) view.findViewById(R.id.id_textView_GeoPoints);
        TVPlacemarks = (TextView) view.findViewById(R.id.id_textView_Placemarks);

        return view;
    }

    @Override
    public void onResume() {
        EventBus.getDefault().register(this);
        super.onResume();
        Update();
    }

    @Override
    public void onPause() {
        EventBus.getDefault().unregister(this);
        super.onPause();
    }

    public void ontoggleRecordGeoPoint(View view) {
        if (isAdded()) {
            boolean grs = GPSApplication.getInstance().getRecording();
            if(grs) {
                AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(getActivity(), R.style.StyledDialog));
                builder.setMessage(getResources().getString(R.string.tracking_message_stop_confirmation));
                builder.setIcon(android.R.drawable.ic_menu_info_details);
                builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        GPSApplication gpsApp = GPSApplication.getInstance();
                        gpsApp.setRecording(!gpsApp.getRecording());
                        tableLayoutGeoPoints.setBackgroundColor(gpsApp.getRecording() ? getResources().getColor(R.color.colorPrimary) : getResources().getColor(R.color.colorTrackBackground));
                        dialog.dismiss();
                    }
                });
                builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
            else {
                GPSApplication.getInstance().setRecording(!grs);
                tableLayoutGeoPoints.setBackgroundColor(GPSApplication.getInstance().getRecording() ? getResources().getColor(R.color.colorPrimary) : getResources().getColor(R.color.colorTrackBackground));
            }
        }
    }

    public void onPlacemarkRequest(View view) {
        if (isAdded()) {
            GPSApplication gpsApplication = GPSApplication.getInstance();
            final Boolean pr = gpsApplication.getPlacemarkRequest();
            boolean newPlacemarkRequestState = !pr;
            gpsApplication.setPlacemarkRequest(newPlacemarkRequestState);
            tableLayoutPlacemarks.setBackgroundColor(newPlacemarkRequestState ? getResources().getColor(R.color.colorPrimary) : getResources().getColor(R.color.colorTrackBackground));
        }
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

    public void Update() {
        if (isAdded()) {
            GPSApplication gpsApplication = GPSApplication.getInstance();
            final Track track = gpsApplication.getCurrentTrack();
            final Boolean grs = gpsApplication.getRecording();
            final Boolean pr = gpsApplication.getPlacemarkRequest();
            if (track != null) {
                if (TVGeoPoints != null)
                    TVGeoPoints.setText(String.valueOf(track.getNumberOfLocations()));
                if (TVPlacemarks != null)
                    TVPlacemarks.setText(String.valueOf(track.getNumberOfPlacemarks()));
                if (tableLayoutGeoPoints != null)
                    tableLayoutGeoPoints.setBackgroundColor(grs ? getResources().getColor(R.color.colorPrimary) : getResources().getColor(R.color.colorTrackBackground));
                if (tableLayoutPlacemarks != null)
                    tableLayoutPlacemarks.setBackgroundColor(pr ? getResources().getColor(R.color.colorPrimary) : getResources().getColor(R.color.colorTrackBackground));
            }
        }
    }
}