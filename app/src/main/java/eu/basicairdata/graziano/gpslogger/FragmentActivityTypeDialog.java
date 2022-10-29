/*
 * FragmentActivityTypeDialog - Java Class for Android
 * Created by G.Capelli on 18/10/2022
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
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.NonNull;

import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.util.ArrayList;


/**
 * The Dialog that shows the Activity Type list.
 * The user can choose the activity type he wants by clicking the icon.
 */
public class FragmentActivityTypeDialog extends DialogFragment {

    /**
     * The class that defines a category of activities (a row)
     */
    private static class ActivityCategory {
        public LinearLayout layout;
        public ArrayList <ActivityType> activityTypeList = new ArrayList<>();

        ActivityCategory(LinearLayout layout) {
            this.layout = layout;
        }
    }

   /**
    * The class that defines an activity (represented by an icon)
    */
    private static class ActivityType {
        public int value;
        public int drawableId;

        ActivityType(int value, int drawableId) {
            this.value = value;
            this.drawableId = drawableId;
        }
    }


    //@SuppressLint("InflateParams")
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder createPlacemarkAlert = new AlertDialog.Builder(getActivity());

        final float SCALE = getContext().getResources().getDisplayMetrics().density;
        final int ICON_MARGIN = (int) (4 * SCALE + 0.5f);

        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View view = inflater.inflate(R.layout.fragment_activity_type_dialog, null);
        createPlacemarkAlert.setView(view);

        ArrayList <ActivityCategory> activityCategories = new ArrayList<>();

        // Fitness
        ActivityCategory acFitness = new ActivityCategory(view.findViewById(R.id.tracktype_main_linearlayout_cat_fitness));
        acFitness.activityTypeList.add(new ActivityType(0, R.drawable.ic_tracktype_walk_24dp));
        acFitness.activityTypeList.add(new ActivityType(0, R.drawable.ic_tracktype_hiking_24));
        acFitness.activityTypeList.add(new ActivityType(0, R.drawable.ic_tracktype_nordic_walking_24));
        acFitness.activityTypeList.add(new ActivityType(0, R.drawable.ic_tracktype_run_24dp));
        activityCategories.add(acFitness);

        // Water
        ActivityCategory acWatersports = new ActivityCategory(view.findViewById(R.id.tracktype_main_linearlayout_cat_water));
        acWatersports.activityTypeList.add(new ActivityType(0, R.drawable.ic_tracktype_pool_24));
        acWatersports.activityTypeList.add(new ActivityType(0, R.drawable.ic_tracktype_scuba_diving_24));
        acWatersports.activityTypeList.add(new ActivityType(0, R.drawable.ic_tracktype_rowing_24));
        acWatersports.activityTypeList.add(new ActivityType(0, R.drawable.ic_tracktype_kayaking_24));
        acWatersports.activityTypeList.add(new ActivityType(0, R.drawable.ic_tracktype_surfing_24));
        acWatersports.activityTypeList.add(new ActivityType(0, R.drawable.ic_tracktype_kitesurfing_24));
        acWatersports.activityTypeList.add(new ActivityType(0, R.drawable.ic_tracktype_sailing_24));
        acWatersports.activityTypeList.add(new ActivityType(0, R.drawable.ic_tracktype_directions_boat_24));
        activityCategories.add(acWatersports);

        // Snow and Ice
        ActivityCategory acSnowIce = new ActivityCategory(view.findViewById(R.id.tracktype_main_linearlayout_cat_snow));
        acSnowIce.activityTypeList.add(new ActivityType(0, R.drawable.ic_tracktype_downhill_skiing_24));
        acSnowIce.activityTypeList.add(new ActivityType(0, R.drawable.ic_tracktype_snowboarding_24));
        acSnowIce.activityTypeList.add(new ActivityType(0, R.drawable.ic_tracktype_sledding_24));
        acSnowIce.activityTypeList.add(new ActivityType(0, R.drawable.ic_tracktype_snowmobile_24));
        acSnowIce.activityTypeList.add(new ActivityType(0, R.drawable.ic_tracktype_snowshoeing_24));
        acSnowIce.activityTypeList.add(new ActivityType(0, R.drawable.ic_tracktype_ice_skating_24));
        activityCategories.add(acSnowIce);

        // Air
        ActivityCategory acAir = new ActivityCategory(view.findViewById(R.id.tracktype_main_linearlayout_cat_air));
        acAir.activityTypeList.add(new ActivityType(0, R.drawable.ic_tracktype_flight_24dp));
        acAir.activityTypeList.add(new ActivityType(0, R.drawable.ic_tracktype_rocket_24));
        acAir.activityTypeList.add(new ActivityType(0, R.drawable.ic_tracktype_paragliding_24));
        activityCategories.add(acAir);

        // Wheel
        ActivityCategory acWheel = new ActivityCategory(view.findViewById(R.id.tracktype_main_linearlayout_cat_wheel));
        acWheel.activityTypeList.add(new ActivityType(0, R.drawable.ic_tracktype_bike_24dp));
        acWheel.activityTypeList.add(new ActivityType(0, R.drawable.ic_tracktype_skateboarding_24));
        acWheel.activityTypeList.add(new ActivityType(0, R.drawable.ic_tracktype_roller_skating_24));
        acWheel.activityTypeList.add(new ActivityType(0, R.drawable.ic_tracktype_accessible_24));
        activityCategories.add(acWheel);

        // Wheel Mobility
        ActivityCategory acMobility = new ActivityCategory(view.findViewById(R.id.tracktype_main_linearlayout_cat_mobility));
        acMobility.activityTypeList.add(new ActivityType(0, R.drawable.ic_tracktype_electric_scooter_24));
        acMobility.activityTypeList.add(new ActivityType(0, R.drawable.ic_tracktype_moped_24));
        acMobility.activityTypeList.add(new ActivityType(0, R.drawable.ic_tracktype_sports_motorsports_24));
        acMobility.activityTypeList.add(new ActivityType(0, R.drawable.ic_tracktype_car_24dp));
        acMobility.activityTypeList.add(new ActivityType(0, R.drawable.ic_tracktype_local_shipping_24));
        acMobility.activityTypeList.add(new ActivityType(0, R.drawable.ic_tracktype_directions_bus_24));
        acMobility.activityTypeList.add(new ActivityType(0, R.drawable.ic_tracktype_tram_24));
        acMobility.activityTypeList.add(new ActivityType(0, R.drawable.ic_tracktype_train_24));
        activityCategories.add(acMobility);

        // Other
        ActivityCategory acOther = new ActivityCategory(view.findViewById(R.id.tracktype_main_linearlayout_cat_other));
        acOther.activityTypeList.add(new ActivityType(0, R.drawable.ic_tracktype_place_24dp));
        acOther.activityTypeList.add(new ActivityType(0, R.drawable.ic_tracktype_mountain_24dp));
        acOther.activityTypeList.add(new ActivityType(0, R.drawable.ic_tracktype_location_city_24));
        acOther.activityTypeList.add(new ActivityType(0, R.drawable.ic_tracktype_work_24));
        acOther.activityTypeList.add(new ActivityType(0, R.drawable.ic_tracktype_forest_24));
        acOther.activityTypeList.add(new ActivityType(0, R.drawable.ic_tracktype_camera_24));
        acOther.activityTypeList.add(new ActivityType(0, R.drawable.ic_tracktype_search_24));
        activityCategories.add(acOther);

        // Other
        ActivityCategory acOtherSports = new ActivityCategory(view.findViewById(R.id.tracktype_main_linearlayout_cat_other_sports));
        acOtherSports.activityTypeList.add(new ActivityType(0, R.drawable.ic_tracktype_sports_soccer_24));
        acOtherSports.activityTypeList.add(new ActivityType(0, R.drawable.ic_tracktype_golf_24));
        acOtherSports.activityTypeList.add(new ActivityType(0, R.drawable.ic_tracktype_pets_24));
        acOtherSports.activityTypeList.add(new ActivityType(0, R.drawable.ic_tracktype_map_24));
        activityCategories.add(acOtherSports);

        // Method that works with Android 4.X
        // https://stackoverflow.com/questions/35915974/23-2-0-set-vector-drawable-as-background-in-4-x/35918375#35918375
        Resources resources = getContext().getResources();
        Resources.Theme theme = getContext().getTheme();

        for (ActivityCategory ac : activityCategories) {
            for (ActivityType aType : ac.activityTypeList) {
                ImageView iv = new ImageView(getActivity().getApplicationContext());

                // set and colorize Icon
                Drawable drawable = VectorDrawableCompat.create(resources, aType.drawableId, theme);
                // Method that works with Android 4.X
                iv.setImageDrawable(drawable);
                // For Android 5+
                //iv.setImageResource(aType.drawableId);
                iv.setColorFilter(getResources().getColor(R.color.colorIconDisabledOnDialog), PorterDuff.Mode.SRC_IN);

                // set Layout Params
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                iv.setLayoutParams(lp);

                // set Margins
                LinearLayout.LayoutParams marginParams = (LinearLayout.LayoutParams) iv.getLayoutParams();
                marginParams.setMargins(ICON_MARGIN, ICON_MARGIN, ICON_MARGIN, ICON_MARGIN); // left, top, right, bottom
                iv.setLayoutParams(marginParams);

                ac.layout.addView(iv);
            }
        }

        return createPlacemarkAlert.create();
    }
}
