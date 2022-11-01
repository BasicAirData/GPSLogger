/*
 * FragmentTrackPropertiesDialog - Java Class for Android
 * Created by G.Capelli on 18/4/2021
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

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.PorterDuff;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentManager;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;

import static eu.basicairdata.graziano.gpslogger.GPSApplication.NOT_AVAILABLE;
import static eu.basicairdata.graziano.gpslogger.GPSApplication.TOAST_VERTICAL_OFFSET;

/**
 * The Dialog that shows the properties of a Track.
 * The user can use it to edit the description and the activity type.
 * As extra feature of this dialog, The OK Button can finalize the Track.
 */
public class FragmentTrackPropertiesDialog extends DialogFragment {

    private EditText etDescription;
    private final ImageView[] tracktypeImageView = new ImageView[7];
    private ImageView tracktypeMore;

    private Track trackToEdit = null;                              // The track to edit
    private int title = 0;                                         // The resource id for the title
    private boolean finalizeTrackWithOk = false;                   // True if the "OK" button finalizes the track and creates a new one

    private static final String KEY_TITLE = "_title";
    private static final String KEY_ISFINALIZATION = "_isFinalization";

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(KEY_TITLE, title);
        outState.putBoolean(KEY_ISFINALIZATION, finalizeTrackWithOk);
    }

    //@SuppressLint("InflateParams")
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder createPlacemarkAlert = new AlertDialog.Builder(getActivity());
        trackToEdit = GPSApplication.getInstance().getTrackToEdit();

        if (trackToEdit == null) dismiss();

        if (savedInstanceState != null) {
            title = savedInstanceState.getInt(KEY_TITLE, 0);
            finalizeTrackWithOk = savedInstanceState.getBoolean(KEY_ISFINALIZATION, false);
        } else {
            GPSApplication.getInstance().setSelectedTrackTypeOnDialog(trackToEdit.getType());
        }

        if (title != 0) createPlacemarkAlert.setTitle(title);
        //createPlacemarkAlert.setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_stop_24, getActivity().getTheme()));

        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View view = inflater.inflate(R.layout.fragment_track_properties_dialog, null);

        if (trackToEdit != null) {
            etDescription = view.findViewById(R.id.track_description);
            if (!trackToEdit.getDescription().isEmpty()) {
                etDescription.setText(trackToEdit.getDescription());
            }
            etDescription.setHint(GPSApplication.getInstance().getString(R.string.track_id) + " " + trackToEdit.getId());
        }

        tracktypeImageView[0] = view.findViewById(R.id.tracktype_0);
        tracktypeImageView[1] = view.findViewById(R.id.tracktype_1);
        tracktypeImageView[2] = view.findViewById(R.id.tracktype_2);
        tracktypeImageView[3] = view.findViewById(R.id.tracktype_3);
        tracktypeImageView[4] = view.findViewById(R.id.tracktype_4);
        tracktypeImageView[5] = view.findViewById(R.id.tracktype_5);
        tracktypeImageView[6] = view.findViewById(R.id.tracktype_6);

        tracktypeMore = view.findViewById(R.id.tracktype_more);
        tracktypeMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Shows the Tracktype Dialog
                FragmentManager fm = getActivity().getSupportFragmentManager();
                FragmentActivityTypeDialog fragmentActivityTypeDialog = new FragmentActivityTypeDialog();
                fragmentActivityTypeDialog.show(fm, "");
            }
        });

        // Disable all images
        for (int i = 0; i < tracktypeImageView.length; i++) {
            tracktypeImageView[i].setImageResource(Track.ACTIVITY_DRAWABLE_RESOURCE[i]);
            tracktypeImageView[i].setColorFilter(getResources().getColor(R.color.colorIconDisabledOnDialog), PorterDuff.Mode.SRC_IN);
            tracktypeImageView[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    for (int i = 0; i < tracktypeImageView.length; i++) {
                        if (view == tracktypeImageView[i]) {
                            tracktypeImageView[i].setColorFilter(getResources().getColor(R.color.textColorRecControlPrimary), PorterDuff.Mode.SRC_IN);
                            GPSApplication.getInstance().setSelectedTrackTypeOnDialog(i);
                        } else
                            tracktypeImageView[i].setColorFilter(getResources().getColor(R.color.colorIconDisabledOnDialog), PorterDuff.Mode.SRC_IN);
                    }
                }
            });
        }
        // Activate the right image
        if (GPSApplication.getInstance().getSelectedTrackTypeOnDialog() != NOT_AVAILABLE)
            tracktypeImageView[GPSApplication.getInstance().getSelectedTrackTypeOnDialog()].setColorFilter(getResources().getColor(R.color.textColorRecControlPrimary), PorterDuff.Mode.SRC_IN);
        else if ((trackToEdit != null) && (trackToEdit.getEstimatedTrackType() != Track.TRACK_TYPE_ND))
            tracktypeImageView[trackToEdit.getEstimatedTrackType()].setColorFilter(getResources().getColor(R.color.textColorRecControlSecondary), PorterDuff.Mode.SRC_IN);

        createPlacemarkAlert.setView(view)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        if (isAdded()) {
                            String trackDescription = etDescription.getText().toString();
                            trackToEdit.setDescription (trackDescription.trim());
                            if (GPSApplication.getInstance().getSelectedTrackTypeOnDialog() != NOT_AVAILABLE)
                                trackToEdit.setType(GPSApplication.getInstance().getSelectedTrackTypeOnDialog());  // the user selected a track type!
                            GPSApplication.getInstance().gpsDataBase.updateTrack(trackToEdit);
                            if (finalizeTrackWithOk) {
                                // a request to finalize a track
                                EventBus.getDefault().post(EventBusMSG.NEW_TRACK);
                                Toast toast = Toast.makeText(GPSApplication.getInstance().getApplicationContext(), R.string.toast_track_saved_into_tracklist, Toast.LENGTH_SHORT);
                                toast.setGravity(Gravity.BOTTOM, 0, TOAST_VERTICAL_OFFSET);
                                toast.show();
                            } else {
                                GPSApplication.getInstance().UpdateTrackList();
                                EventBus.getDefault().post(EventBusMSG.UPDATE_TRACK);
                            }
                        }
                    }
                })
                .setNeutralButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        return createPlacemarkAlert.create();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }

    /**
     * Sets the title of the Dialog.
     *
     * @param titleResource The Resource String of the title
     */
    public void setTitleResource(int titleResource) {
        title = titleResource;
    }

    /**
     * If true, the dialog finalizes the track when the user press the OK Button.
     *
     * @param finalize true if the dialog should finalize the track
     */
    public void setFinalizeTrackWithOk(boolean finalize) {
        finalizeTrackWithOk = finalize;
    }
}