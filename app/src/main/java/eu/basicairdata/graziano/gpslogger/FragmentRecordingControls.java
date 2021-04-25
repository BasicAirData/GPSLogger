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

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class FragmentRecordingControls extends Fragment{

    public FragmentRecordingControls() {
        // Required empty public constructor
    }


    private TextView TVGeoPointsNumber;
    private TextView TVPlacemarksNumber;
    private TextView TVLockButton;
    private TextView TVStopButton;
    private TextView TVAnnotateButton;
    private TextView TVRecordButton;
    
    final GPSApplication gpsApplication = GPSApplication.getInstance();


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recording_controls, container, false);

        TVLockButton = view.findViewById(R.id.id_lock);
        TVLockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onToggleLock();
            }
        });

        TVStopButton = view.findViewById(R.id.id_stop);
        TVStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onRequestStop();
            }
        });

        TVAnnotateButton = view.findViewById(R.id.id_annotate);
        TVAnnotateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onRequestAnnotation();
            }
        });

        TVRecordButton = view.findViewById(R.id.id_record);
        TVRecordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onToggleRecord();
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


    public void onToggleRecord() {
        if (isAdded()) {
            if (!gpsApplication.getBottomBarLocked()) {
                if (!gpsApplication.getStopFlag()) {
                    gpsApplication.setRecording(!gpsApplication.getRecording());
                    Update();
                }
            } else {
                Toast.makeText(gpsApplication.getApplicationContext(), getString(R.string.toast_bottom_bar_locked), Toast.LENGTH_SHORT).show();
            }
        }
    }


    public void onRequestAnnotation() {
        if (isAdded()) {
            if (!gpsApplication.getBottomBarLocked()) {
                if (!gpsApplication.getStopFlag()) {
                    gpsApplication.setPlacemarkRequest(!gpsApplication.getPlacemarkRequest());
                    Update();
                }
            } else {
                Toast.makeText(gpsApplication.getApplicationContext(), getString(R.string.toast_bottom_bar_locked), Toast.LENGTH_SHORT).show();
            }
        }
    }


    public void onRequestStop() {
        if (isAdded()) {
            if (!gpsApplication.getBottomBarLocked()) {
                if (!gpsApplication.getStopFlag()) {
                    gpsApplication.setStopFlag(true);
                    gpsApplication.setRecording(false);
                    gpsApplication.setPlacemarkRequest(false);
                    Update();
                    if (gpsApplication.getCurrentTrack().getNumberOfLocations() + gpsApplication.getCurrentTrack().getNumberOfPlacemarks() > 0) {
                        FragmentManager fm = getActivity().getSupportFragmentManager();
                        FragmentTrackPropertiesDialog tpDialog = new FragmentTrackPropertiesDialog();
                        gpsApplication.setTrackToEdit(gpsApplication.getCurrentTrack());
                        tpDialog.setTitleResource(R.string.finalize_track);
                        tpDialog.setIsAFinalization(true);
                        tpDialog.show(fm, "");
                    } else {
                        Toast.makeText(gpsApplication.getApplicationContext(), getString(R.string.toast_nothing_to_save), Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                Toast.makeText(gpsApplication.getApplicationContext(), getString(R.string.toast_bottom_bar_locked), Toast.LENGTH_SHORT).show();
            }
        }
    }


    public void onToggleLock() {
        if (isAdded()) {
            gpsApplication.setBottomBarLocked(!gpsApplication.getBottomBarLocked());
            Update();
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


    private void setButtonToClickedState(@NonNull TextView button, int imageId, int stringId) {
        button.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        if (imageId != 0) button.setCompoundDrawablesWithIntrinsicBounds(0, imageId, 0, 0);
        button.setTextColor(getResources().getColor(R.color.textColorRecControlSecondary_Active));
        if (stringId != 0) button.setText(getString(stringId));
        setTextViewDrawableColor(button.getCompoundDrawables()[1], getResources().getColor(R.color.textColorRecControlPrimary_Active));
    }


    private void setButtonToNormalState(@NonNull TextView button, int imageId, int stringId) {
        button.setBackgroundColor(Color.TRANSPARENT);
        if (imageId != 0) button.setCompoundDrawablesWithIntrinsicBounds(0, imageId, 0, 0);
        button.setTextColor(getResources().getColor(R.color.textColorRecControlSecondary));
        if (stringId != 0) button.setText(getString(stringId));
        setTextViewDrawableColor(button.getCompoundDrawables()[1], getResources().getColor(R.color.textColorRecControlPrimary));
    }


    private void setButtonToDisabledState(@NonNull TextView button, int imageId, int stringId) {
        button.setBackgroundColor(Color.TRANSPARENT);
        if (imageId != 0) button.setCompoundDrawablesWithIntrinsicBounds(0, imageId, 0, 0);
        button.setTextColor(getResources().getColor(R.color.textColorRecControlDisabled));
        if (stringId != 0) button.setText(getString(stringId));
        setTextViewDrawableColor(button.getCompoundDrawables()[1], getResources().getColor(R.color.textColorRecControlDisabled));
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
                    if (isRec) setButtonToClickedState(TVRecordButton, R.drawable.ic_pause_24, R.string.pause);
                    else setButtonToNormalState(TVRecordButton, R.drawable.ic_record_24, R.string.record);
                }
                if (TVAnnotateButton != null) {
                    if (isAnnot) setButtonToClickedState(TVAnnotateButton, 0, 0);
                    else setButtonToNormalState(TVAnnotateButton, 0, 0);
                }
                if (TVLockButton != null) {
                    if (isLck) setButtonToClickedState(TVLockButton, R.drawable.ic_unlock_24, R.string.unlock);
                    else setButtonToNormalState(TVLockButton, R.drawable.ic_lock_24, R.string.lock);
                }
                if (TVStopButton != null) {
                    TVStopButton.setClickable(isRec || isAnnot || (track.getNumberOfLocations() + track.getNumberOfPlacemarks() > 0));
                    if (isRec || isAnnot || (track.getNumberOfLocations() + track.getNumberOfPlacemarks() > 0) || gpsApplication.getStopFlag()) {
                        if (gpsApplication.getStopFlag()) setButtonToClickedState(TVStopButton, 0, 0);
                        else setButtonToNormalState(TVStopButton, 0, 0);
                    } else {
                        setButtonToDisabledState(TVStopButton, 0, 0);
                    }
                }
            }
        }
    }
}