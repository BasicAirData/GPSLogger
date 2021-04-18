/**
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


import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class FragmentRecordingControls extends Fragment{

    public FragmentRecordingControls() {
        // Required empty public constructor
    }


    private TextView TVGeoPointsNumber;
    private TextView TVPlacemarksNumber;
    private TextView TVRecordButton;
    private TextView TVAnnotateButton;
    private TextView TVLockButton;

    final GPSApplication gpsApplication = GPSApplication.getInstance();


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recording_controls, container, false);


        TVAnnotateButton = view.findViewById(R.id.id_annotate);
        TVAnnotateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onPlacemarkRequest();
            }
        });

        TVRecordButton = view.findViewById(R.id.id_record);
        TVRecordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ontoggleRecordGeoPoint();
            }
        });

        TVLockButton = view.findViewById(R.id.id_lock);
        TVLockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ontoggleLock();
            }
        });

        TVGeoPointsNumber = view.findViewById(R.id.id_textView_GeoPoints);
        TVPlacemarksNumber = view.findViewById(R.id.id_textView_Placemarks);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Workaround for Nokia Devices, Android 9
        // https://github.com/BasicAirData/GPSLogger/issues/77
        if (EventBus.getDefault().isRegistered(this)) {
            //Log.w("myApp", "[#] FragmentRecordingControls - EventBus: FragmentRecordingControls already registered");
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

    public void ontoggleRecordGeoPoint() {
        if (isAdded()) {
            if (!gpsApplication.getBottomBarLocked()) {
                final boolean grs = gpsApplication.getRecording();
                boolean newRecordingState = !grs;
                gpsApplication.setRecording(newRecordingState);
                EventBus.getDefault().post(EventBusMSG.UPDATE_TRACK);
                TVRecordButton.setBackgroundColor(newRecordingState ? getResources().getColor(R.color.colorPrimary) : Color.TRANSPARENT);
                TVRecordButton.setCompoundDrawablesWithIntrinsicBounds(0, newRecordingState ? R.drawable.ic_pause_24 : R.drawable.ic_record_24, 0, 0);
                TVRecordButton.setTextColor(getResources().getColor(newRecordingState ? R.color.textColorRecControlSecondary_Active : R.color.textColorRecControlSecondary));
                TVRecordButton.setText(getString(newRecordingState ? R.string.pause : R.string.record));
                setTextViewDrawableColor(TVRecordButton.getCompoundDrawables()[1],
                        getResources().getColor(newRecordingState ? R.color.textColorRecControlPrimary_Active : R.color.textColorRecControlPrimary));
            } else {
                EventBus.getDefault().post(EventBusMSG.TOAST_BOTTOM_BAR_LOCKED);
            }
        }
    }

    public void onPlacemarkRequest() {
        if (isAdded()) {
            if (!gpsApplication.getBottomBarLocked()) {
            final boolean pr = gpsApplication.getPlacemarkRequest();
            boolean newPlacemarkRequestState = !pr;
            gpsApplication.setPlacemarkRequest(newPlacemarkRequestState);
            TVAnnotateButton.setBackgroundColor(newPlacemarkRequestState ? getResources().getColor(R.color.colorPrimary) : Color.TRANSPARENT);
            TVAnnotateButton.setTextColor(getResources().getColor(newPlacemarkRequestState ? R.color.textColorRecControlSecondary_Active : R.color.textColorRecControlSecondary));
            setTextViewDrawableColor(TVAnnotateButton.getCompoundDrawables()[1],
                    getResources().getColor(newPlacemarkRequestState ? R.color.textColorRecControlPrimary_Active : R.color.textColorRecControlPrimary));
        } else {
                EventBus.getDefault().post(EventBusMSG.TOAST_BOTTOM_BAR_LOCKED);
            }
        }
    }

    public void ontoggleLock() {
        if (isAdded()) {
            boolean lck = !gpsApplication.getBottomBarLocked();
            gpsApplication.setBottomBarLocked(lck);
            //EventBus.getDefault().post(EventBusMSG.UPDATE_TRACK);
            TVLockButton.setBackgroundColor(lck ? getResources().getColor(R.color.colorPrimary) : Color.TRANSPARENT);
            TVLockButton.setCompoundDrawablesWithIntrinsicBounds(0, lck ? R.drawable.ic_unlock_24 : R.drawable.ic_lock_24, 0, 0);
            TVLockButton.setTextColor(getResources().getColor(lck ? R.color.textColorRecControlSecondary_Active : R.color.textColorRecControlSecondary));
            TVLockButton.setText(getString(lck ? R.string.unlock : R.string.lock));
            setTextViewDrawableColor(TVLockButton.getCompoundDrawables()[1],
                    getResources().getColor(lck ? R.color.textColorRecControlPrimary_Active : R.color.textColorRecControlPrimary));
        }
    }

    @Subscribe (threadMode = ThreadMode.MAIN)
    public void onEvent(Short msg) {
        if (msg == EventBusMSG.UPDATE_TRACK) {
            Update();
        }
    }

    private void setTextViewDrawableColor(Drawable drawable, int color) {
        if (drawable != null) {
            drawable.clearColorFilter();
            drawable.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
        }
    }

    public void Update() {
        if (isAdded()) {
            final Track track = gpsApplication.getCurrentTrack();
            final boolean isRec = gpsApplication.getRecording();
            final boolean isAnnot = gpsApplication.getPlacemarkRequest();
            final boolean isLck = gpsApplication.getBottomBarLocked();
            if (track != null) {
                if (TVGeoPointsNumber != null)            TVGeoPointsNumber.setText(track.getNumberOfLocations() == 0 ? "" : String.valueOf(track.getNumberOfLocations()));
                if (TVPlacemarksNumber != null)           TVPlacemarksNumber.setText(String.valueOf(track.getNumberOfPlacemarks() == 0 ? "" : track.getNumberOfPlacemarks()));
                if (TVRecordButton != null) {
                    TVRecordButton.setBackgroundColor(isRec ? getResources().getColor(R.color.colorPrimary) : Color.TRANSPARENT);
                    TVRecordButton.setCompoundDrawablesWithIntrinsicBounds(0, isRec ? R.drawable.ic_pause_24 : R.drawable.ic_record_24, 0, 0);
                    TVRecordButton.setTextColor(getResources().getColor(isRec ? R.color.textColorRecControlSecondary_Active : R.color.textColorRecControlSecondary));
                    TVRecordButton.setText(getString(isRec ? R.string.pause : R.string.record));
                    setTextViewDrawableColor(TVRecordButton.getCompoundDrawables()[1],
                            getResources().getColor(isRec ? R.color.textColorRecControlPrimary_Active : R.color.textColorRecControlPrimary));
                }
                if (TVAnnotateButton != null) {
                    TVAnnotateButton.setBackgroundColor(isAnnot ? getResources().getColor(R.color.colorPrimary) : Color.TRANSPARENT);
                    TVAnnotateButton.setTextColor(getResources().getColor(isAnnot ? R.color.textColorRecControlSecondary_Active : R.color.textColorRecControlSecondary));
                    setTextViewDrawableColor(TVAnnotateButton.getCompoundDrawables()[1],
                            getResources().getColor(isAnnot ? R.color.textColorRecControlPrimary_Active : R.color.textColorRecControlPrimary));

                }
                if (TVLockButton != null) {
                    TVLockButton.setBackgroundColor(isLck ? getResources().getColor(R.color.colorPrimary) : Color.TRANSPARENT);
                    TVLockButton.setCompoundDrawablesWithIntrinsicBounds(0, isLck ? R.drawable.ic_unlock_24 : R.drawable.ic_lock_24, 0, 0);
                    TVLockButton.setTextColor(getResources().getColor(isLck ? R.color.textColorRecControlSecondary_Active : R.color.textColorRecControlSecondary));
                    TVLockButton.setText(getString(isLck ? R.string.unlock : R.string.lock));
                    setTextViewDrawableColor(TVLockButton.getCompoundDrawables()[1],
                            getResources().getColor(isLck ? R.color.textColorRecControlPrimary_Active : R.color.textColorRecControlPrimary));
                }
            }
        }
    }
}