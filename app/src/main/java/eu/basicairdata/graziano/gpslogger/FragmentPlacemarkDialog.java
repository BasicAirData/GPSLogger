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
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;

/**
 * The dialog that appears when the user adds a new Annotation (Placemark).
 */
public class FragmentPlacemarkDialog extends DialogFragment {

    static class TipUILine {
        TextView textviewTipUILine;
        LinearLayout linearLayoutTipUILine;
        ImageView imageViewTipUILine;

        public TipUILine(View view, int i) {
             textviewTipUILine = view.findViewWithTag("placemark_tv_tip" + i);
             linearLayoutTipUILine = view.findViewWithTag("placemark_ll_tip" + i);
             imageViewTipUILine = view.findViewWithTag("placemark_pin_tip" + i);
        }
    }

    ArrayList<String> tipArrayList = new ArrayList<>();
    ArrayList<TipUILine> uiLineArrayList = new ArrayList<>();

    SharedPreferences preferences;
    SharedPreferences.Editor editor;

    ImageView imageViewPin;

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

        imageViewPin = view.findViewById(R.id.placemark_pin);
        imageViewPin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(tipArrayList.size() < 5) tipArrayList.add(new String(etDescription.getText().toString().trim()));
                writeTipPrefs();
                initTipUIList(view);
                setPinButtonState();
            }
        });

        preferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        editor = preferences.edit();

        etDescription = view.findViewById(R.id.placemark_description);
        etDescription.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Log.w("myApp", "[#] FragmentPlacemarkDialog.java - onEditorAction");
                setPinButtonState();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                // TODO Auto-generated method stub
            }

            @Override
            public void afterTextChanged(Editable s) {

                // TODO Auto-generated method stub
            }
        });

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

        // Set tip string list
        for (int i = 0; i < 5; i++) {
            String stip = preferences.getString("prefAnnotationDescTip" + i, "");
            Log.w("myApp", "[#] FragmentPlacemarkDialog.java - Tip " + i + " = " + stip);
            if (!stip.isEmpty()) tipArrayList.add(stip);
        }

        initTipUIList(view);
        setPinButtonState();

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


    public void writeTipPrefs() {
        for (int i = 0; i < 5; i++) {
            String str;
            try {
                str = tipArrayList.get(i);
                // The tip string exists
                editor.putString("prefAnnotationDescTip" + i, str);

            } catch (Exception e) {
                // The tip string doesn't exist
                // Deletes preferences
                if (preferences.contains("prefAnnotationDescTip" + i)) {
                    editor.remove("prefAnnotationDescTip" + i);
                }
            }
        }
        editor.commit();
    }


    public void setPinButtonState() {
        if ((tipArrayList.size() >= 5) || (etDescription.getText().toString().trim().isEmpty())) {
            // Tip list full
            imageViewPin.setEnabled(false);
            imageViewPin.setAlpha(0.4f);
            return;
        } else {
            // Tip list has free spaces
            for (int i = 0; i < tipArrayList.size(); i++) {
                if (etDescription.getText().toString().trim().equals(tipArrayList.get(i))) {
                    // Tip already present
                    imageViewPin.setEnabled(false);
                    imageViewPin.setAlpha(0.4f);
                    return;
                }
            }
        }
        imageViewPin.setEnabled(true);
        imageViewPin.setAlpha(1.0f);
    }


    public void initTipUIList(View view) {
        // Set TipUILine list
        for (int i = 0; i < 5; i++) {
            TipUILine tip = new TipUILine(view, i);
            if (i < tipArrayList.size()) {
                tip.textviewTipUILine.setText(tipArrayList.get(i));
                tip.linearLayoutTipUILine.setVisibility(View.VISIBLE);
            } else {
                tip.linearLayoutTipUILine.setVisibility(View.GONE);
            }
            tip.textviewTipUILine.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    etDescription.setText(tip.textviewTipUILine.getText());
                    etDescription.setSelection(etDescription.getText().length());
                }
            });
            tip.textviewTipUILine.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    etDescription.setText(etDescription.getText().toString() + tip.textviewTipUILine.getText().toString());
                    etDescription.setSelection(etDescription.getText().length());
                    return true;
                }
            });
            int finalI = i;
            tip.imageViewTipUILine.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    tipArrayList.remove(finalI);
                    writeTipPrefs();
                    initTipUIList(view);
                    setPinButtonState();
                }
            });
            uiLineArrayList.add(tip);
        }
    }
}