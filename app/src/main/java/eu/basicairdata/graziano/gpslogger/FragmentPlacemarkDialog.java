/*
 * FragmentPlacemarkDialog - Java Class for Android
 * Created by G.Capelli on 9/7/2016
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
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import org.greenrobot.eventbus.EventBus;

/**
 * The dialog that appears when the user adds a new Annotation (Placemark).
 */
public class FragmentPlacemarkDialog extends DialogFragment {

    EditText etDescription;

    //@SuppressLint("InflateParams")
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder createPlacemarkAlert = new AlertDialog.Builder(getActivity());
        createPlacemarkAlert.setTitle(R.string.dlg_add_annotation);
        //createPlacemarkAlert.setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_add_location_24dp, getActivity().getTheme()));

        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View view = inflater.inflate(R.layout.fragment_placemark_dialog, null);

        etDescription = view.findViewById(R.id.placemark_description);
        etDescription.postDelayed(new Runnable()
        {
            public void run()
            {
                if (isAdded()) {
                    etDescription.requestFocus();
                    InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputMethodManager.showSoftInput(etDescription, InputMethodManager.SHOW_IMPLICIT);
                }
            }
        }, 200);

        createPlacemarkAlert.setView(view)
                .setPositiveButton(R.string.dlg_button_add, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        if (isAdded()) {
                            String placemarkDescription = etDescription.getText().toString();
                            final GPSApplication gpsApp = GPSApplication.getInstance();
                            gpsApp.setPlacemarkDescription(placemarkDescription.trim());
                            EventBus.getDefault().post(EventBusMSG.ADD_PLACEMARK);
                            //Log.w("myApp", "[#] FragmentPlacemarkDialog.java - posted ADD_PLACEMARK: " + placemarkDescription);
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
}