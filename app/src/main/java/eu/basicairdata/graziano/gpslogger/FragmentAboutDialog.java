/*
 * FragmentAboutDialog - Java Class for Android
 * Created by G.Capelli on 26/7/2016
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
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

/**
 * The About Dialog Fragment
 */
public class FragmentAboutDialog extends DialogFragment {

    private static final String COPYRIGHT_RANGE_END = "2024";           // The number that appears as end-year of the Copyright range

    //@SuppressLint("InflateParams")
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final int APP_ORIGIN_NOT_SPECIFIED     = 0;
        final int APP_ORIGIN_GOOGLE_PLAY_STORE = 1;       // The app has been installed from Google Play Store

        TextView tvVersion;
        TextView tvDescription;

        AlertDialog.Builder createAboutAlert = new AlertDialog.Builder(getActivity(), R.style.MyMaterialThemeAbout);

        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View view = inflater.inflate(R.layout.fragment_about_dialog, null);

        final GPSApplication gpsApp = GPSApplication.getInstance();

        tvVersion = view.findViewById(R.id.id_about_textView_Version);
        String versionName = BuildConfig.VERSION_NAME;
        tvVersion.setText(getString(R.string.about_version) + " " + versionName);

        tvDescription = view.findViewById(R.id.id_about_textView_description);
        tvDescription.setText(getString(R.string.about_description, COPYRIGHT_RANGE_END));

        int appOrigin = APP_ORIGIN_NOT_SPECIFIED;            // Which package manager is used to install this app (for Rate button visualization):
                                                             // APP_ORIGIN_NOT_SPECIFIED, APP_ORIGIN_GOOGLE_PLAY_STORE
        // Determine the app installation source
        try {
            String installer;
            installer = gpsApp.getApplicationContext().getPackageManager().getInstallerPackageName(gpsApp.getApplicationContext().getPackageName());
            if (installer.equals("com.android.vending") || installer.equals("com.google.android.feedback"))
                appOrigin = APP_ORIGIN_GOOGLE_PLAY_STORE;                               // App installed from Google Play Store
            //else appOrigin = APP_ORIGIN_NOT_SPECIFIED;                                  // Otherwise
        } catch (Exception e) {
            Log.w("myApp", "[#] GPSApplication.java - Exception trying to determine the package installer");
            appOrigin = APP_ORIGIN_NOT_SPECIFIED;
        }

        switch (appOrigin) {
            case APP_ORIGIN_GOOGLE_PLAY_STORE:
                tvDescription.setText(tvDescription.getText() + "\n\n" + getString(R.string.about_description_googleplaystore));
                createAboutAlert.setView(view).setNegativeButton(R.string.about_rate_this_app, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        boolean marketfailed = false;
                        try {
                            getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + BuildConfig.APPLICATION_ID)));
                        } catch (Exception e) {
                            // Unable to start the Google Play gpsApp for rating
                            marketfailed = true;
                        }
                        if (marketfailed) {
                            try {               // Try with the web browser
                                getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID)));
                            } catch (Exception e) {
                                // Unable to start also the browser for rating
                                Toast.makeText(getContext(), getString(R.string.about_unable_to_rate), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
                break;
            default:
                break;
        }

        createAboutAlert.setView(view).setPositiveButton(R.string.about_ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {}
            });

        return createAboutAlert.create();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }
}