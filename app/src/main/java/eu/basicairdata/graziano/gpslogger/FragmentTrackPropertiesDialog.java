/**
 * FragmentTrackPropertiesDialog - Java Class for Android
 * Created by G.Capelli (BasicAirData) on 18/4/2021
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

import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;

import static eu.basicairdata.graziano.gpslogger.GPSApplication.NOT_AVAILABLE;

public class FragmentTrackPropertiesDialog extends DialogFragment {

    private EditText DescEditText;
    private final ImageView[] tracktypeImageView = new ImageView[7];

    private int selectedTrackType = NOT_AVAILABLE;                  // The track type selected by the user
    private Track _trackToEdit = null;                              // The track to edit
    private int _title = 0;                                         // The resource id for the title
    private boolean _isAFinalization = false;                       // True if the "OK" button finalize the track and creates a new one

    private static final String KEY_SELTRACKTYPE = "selectedTrackType";
    private static final String KEY_TITLE = "_title";
    private static final String KEY_ISFINALIZATION = "_isFinalization";



    public void setTitleResource(int titleResource) {
        _title = titleResource;
    }


    public void setIsAFinalization(boolean isAFinalization) {
        _isAFinalization = isAFinalization;
    }


    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        // TODO Auto-generated method stub
        super.onSaveInstanceState(outState);

        outState.putInt(KEY_SELTRACKTYPE, selectedTrackType);
        outState.putInt(KEY_TITLE, _title);
        outState.putBoolean(KEY_ISFINALIZATION, _isAFinalization);
    }



        //@SuppressLint("InflateParams")
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder createPlacemarkAlert = new AlertDialog.Builder(getActivity());

        _trackToEdit = GPSApplication.getInstance().getTrackToEdit();

        if (savedInstanceState != null) {
            _title = savedInstanceState.getInt(KEY_TITLE, 0);
            selectedTrackType = savedInstanceState.getInt(KEY_SELTRACKTYPE, NOT_AVAILABLE);
            _isAFinalization = savedInstanceState.getBoolean(KEY_ISFINALIZATION, false);
        } else {
            selectedTrackType = _trackToEdit.getType();
        }

        if (_title != 0) createPlacemarkAlert.setTitle(_title);
        //createPlacemarkAlert.setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_stop_24, getActivity().getTheme()));

        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View view = inflater.inflate(R.layout.fragment_track_properties_dialog, null);

        DescEditText = view.findViewById(R.id.track_description);
        if (!_trackToEdit.getDescription().isEmpty()) {
            DescEditText.setText(_trackToEdit.getDescription());
        }
        DescEditText.setHint(GPSApplication.getInstance().getString(R.string.track_id) + " " + _trackToEdit.getId());

//        DescEditText.postDelayed(new Runnable()
//        {
//            public void run()
//            {
//                if (isAdded()) {
//                    DescEditText.requestFocus();
//                    InputMethodManager mgr = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
//                    mgr.showSoftInput(DescEditText, InputMethodManager.SHOW_IMPLICIT);
//                }
//            }
//        }, 200);

        tracktypeImageView[Track.TRACK_TYPE_STEADY    ] = view.findViewById(R.id.tracktype_steady);
        tracktypeImageView[Track.TRACK_TYPE_MOUNTAIN  ] = view.findViewById(R.id.tracktype_mountain);
        tracktypeImageView[Track.TRACK_TYPE_WALK      ] = view.findViewById(R.id.tracktype_walk);
        tracktypeImageView[Track.TRACK_TYPE_RUN       ] = view.findViewById(R.id.tracktype_run);
        tracktypeImageView[Track.TRACK_TYPE_BICYCLE   ] = view.findViewById(R.id.tracktype_bicycle);
        tracktypeImageView[Track.TRACK_TYPE_CAR       ] = view.findViewById(R.id.tracktype_car);
        tracktypeImageView[Track.TRACK_TYPE_FLIGHT    ] = view.findViewById(R.id.tracktype_flight);

        // Disable all images
        for (int i = 0; i< tracktypeImageView.length; i++) {
            tracktypeImageView[i].setColorFilter(getResources().getColor(R.color.colorIconDisabledOnDialog), PorterDuff.Mode.SRC_IN);
            tracktypeImageView[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    for (int i = 0; i < tracktypeImageView.length; i++) {
                        if (view == tracktypeImageView[i]) {
                            tracktypeImageView[i].setColorFilter(getResources().getColor(R.color.textColorRecControlPrimary), PorterDuff.Mode.SRC_IN);
                            selectedTrackType = i;
                        } else
                            tracktypeImageView[i].setColorFilter(getResources().getColor(R.color.colorIconDisabledOnDialog), PorterDuff.Mode.SRC_IN);
                    }
                }
            });
        }
        // Activate the right image
        if (selectedTrackType != NOT_AVAILABLE)
            tracktypeImageView[selectedTrackType].setColorFilter(getResources().getColor(R.color.textColorRecControlPrimary), PorterDuff.Mode.SRC_IN);
        else if (_trackToEdit.getTrackType() != Track.TRACK_TYPE_ND)
            tracktypeImageView[_trackToEdit.getTrackType()].setColorFilter(getResources().getColor(R.color.textColorRecControlSecondary), PorterDuff.Mode.SRC_IN);

        createPlacemarkAlert.setView(view)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        if (isAdded()) {
                            String trackDescription = DescEditText.getText().toString();
                            _trackToEdit.setDescription (trackDescription.trim());
                            if (selectedTrackType != NOT_AVAILABLE) _trackToEdit.setTrackType(selectedTrackType);  // the user selected a track type!
                            GPSApplication.getInstance().GPSDataBase.updateTrack(_trackToEdit);
                            if (_isAFinalization) {
                                // a request to finalize a track
                                EventBus.getDefault().post(EventBusMSG.NEW_TRACK);
                                Toast.makeText(getActivity(), getString(R.string.toast_track_saved_into_tracklist), Toast.LENGTH_SHORT).show();
                            } else {
                                GPSApplication.getInstance().UpdateTrackList();
                            }
                        }
                    }
                })
                .setNeutralButton(R.string.dlg_button_cancel, new DialogInterface.OnClickListener() {
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
}