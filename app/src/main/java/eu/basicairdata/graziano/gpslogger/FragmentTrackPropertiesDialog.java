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
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;

public class FragmentTrackPropertiesDialog extends DialogFragment {

    EditText DescEditText;

    //@SuppressLint("InflateParams")
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder createPlacemarkAlert = new AlertDialog.Builder(getActivity());
        createPlacemarkAlert.setTitle(R.string.finalize_track);
        //createPlacemarkAlert.setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_stop_24, getActivity().getTheme()));

        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View view = inflater.inflate(R.layout.fragment_track_properties_dialog, null);

        DescEditText = view.findViewById(R.id.track_description);
        //DescEditText.setHint(getString(R.string.track_id) + " " + String.valueOf(GPSApplication.getInstance().getCurrentTrack().getId()));
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

        createPlacemarkAlert.setView(view)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        if (isAdded()) {
                            String PlacemarkDescription = DescEditText.getText().toString();
                            final GPSApplication gpsApp = GPSApplication.getInstance();
                            //gpsApp.setPlacemarkDescription(PlacemarkDescription.trim());
                            gpsApp.setNewTrackFlag(false);
                            gpsApp.setRecording(false);
                            EventBus.getDefault().post(EventBusMSG.NEW_TRACK);
                            Toast.makeText(getActivity(), getString(R.string.toast_track_saved_into_tracklist), Toast.LENGTH_SHORT).show();
                            //Log.w("myApp", "[#] FragmentPlacemarkDialog.java - posted ADD_PLACEMARK: " + PlacemarkDescription);
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