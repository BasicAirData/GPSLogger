/*
 * FragmentTrackTypeDialog - Java Class for Android
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
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;

import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;


/**
 * The Dialog that shows the Activity Type list.
 * The user can choose the activity type he wants by clicking the icon.
 */
public class FragmentTrackTypeDialog extends DialogFragment {

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
     * The class that defines the activities
     */
    public static class ActivityType {
        public int value;
        public int drawableId;

        ActivityType(int value) {
            this.value = value;
            this.drawableId = Track.ACTIVITY_DRAWABLE_RESOURCE[value];
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
        final View view = inflater.inflate(R.layout.fragment_track_type_dialog, null);
        createPlacemarkAlert.setView(view);


        ArrayList <ActivityCategory> activityCategories = new ArrayList<>();

        // Fitness
        ActivityCategory acFitness = new ActivityCategory(view.findViewById(R.id.tracktype_main_linearlayout_cat_fitness));
        acFitness.activityTypeList.add(new ActivityType(Track.TRACK_TYPE_WALK));
        acFitness.activityTypeList.add(new ActivityType(Track.TRACK_TYPE_HIKING));
        acFitness.activityTypeList.add(new ActivityType(Track.TRACK_TYPE_NORDICWALKING));
        acFitness.activityTypeList.add(new ActivityType(Track.TRACK_TYPE_RUN));
        activityCategories.add(acFitness);

        // Water
        ActivityCategory acWatersports = new ActivityCategory(view.findViewById(R.id.tracktype_main_linearlayout_cat_water));
        acWatersports.activityTypeList.add(new ActivityType(Track.TRACK_TYPE_SWIMMING));
        acWatersports.activityTypeList.add(new ActivityType(Track.TRACK_TYPE_SCUBADIVING));
        acWatersports.activityTypeList.add(new ActivityType(Track.TRACK_TYPE_ROWING));
        acWatersports.activityTypeList.add(new ActivityType(Track.TRACK_TYPE_KAYAKING));
        acWatersports.activityTypeList.add(new ActivityType(Track.TRACK_TYPE_SURFING));
        acWatersports.activityTypeList.add(new ActivityType(Track.TRACK_TYPE_KITESURFING));
        acWatersports.activityTypeList.add(new ActivityType(Track.TRACK_TYPE_SAILING));
        acWatersports.activityTypeList.add(new ActivityType(Track.TRACK_TYPE_BOAT));
        activityCategories.add(acWatersports);

        // Snow and Ice
        ActivityCategory acSnowIce = new ActivityCategory(view.findViewById(R.id.tracktype_main_linearlayout_cat_snow));
        acSnowIce.activityTypeList.add(new ActivityType(Track.TRACK_TYPE_DOWNHILLSKIING));
        acSnowIce.activityTypeList.add(new ActivityType(Track.TRACK_TYPE_SNOWBOARDING));
        acSnowIce.activityTypeList.add(new ActivityType(Track.TRACK_TYPE_SLEDDING));
        acSnowIce.activityTypeList.add(new ActivityType(Track.TRACK_TYPE_SNOWMOBILE));
        acSnowIce.activityTypeList.add(new ActivityType(Track.TRACK_TYPE_SNOWSHOEING));
        acSnowIce.activityTypeList.add(new ActivityType(Track.TRACK_TYPE_ICESKATING));
        activityCategories.add(acSnowIce);

        // Air
        ActivityCategory acAir = new ActivityCategory(view.findViewById(R.id.tracktype_main_linearlayout_cat_air));
        acAir.activityTypeList.add(new ActivityType(Track.TRACK_TYPE_FLIGHT));
        acAir.activityTypeList.add(new ActivityType(Track.TRACK_TYPE_HELICOPTER));
        acAir.activityTypeList.add(new ActivityType(Track.TRACK_TYPE_ROCKET));
        acAir.activityTypeList.add(new ActivityType(Track.TRACK_TYPE_PARAGLIDING));
        acAir.activityTypeList.add(new ActivityType(Track.TRACK_TYPE_AIRBALLOON));
        activityCategories.add(acAir);

        // Wheel
        ActivityCategory acWheel = new ActivityCategory(view.findViewById(R.id.tracktype_main_linearlayout_cat_wheel));
        acWheel.activityTypeList.add(new ActivityType(Track.TRACK_TYPE_BICYCLE));
        acWheel.activityTypeList.add(new ActivityType(Track.TRACK_TYPE_SKATEBOARDING));
        acWheel.activityTypeList.add(new ActivityType(Track.TRACK_TYPE_ROLLERSKATING));
        acWheel.activityTypeList.add(new ActivityType(Track.TRACK_TYPE_WHEELCHAIR));
        activityCategories.add(acWheel);

        // Wheel Mobility
        ActivityCategory acMobility = new ActivityCategory(view.findViewById(R.id.tracktype_main_linearlayout_cat_mobility));
        acMobility.activityTypeList.add(new ActivityType(Track.TRACK_TYPE_ELECTRICSCOOTER));
        acMobility.activityTypeList.add(new ActivityType(Track.TRACK_TYPE_MOPED));
        acMobility.activityTypeList.add(new ActivityType(Track.TRACK_TYPE_MOTORCYCLE));
        acMobility.activityTypeList.add(new ActivityType(Track.TRACK_TYPE_CAR));
        acMobility.activityTypeList.add(new ActivityType(Track.TRACK_TYPE_TRUCK));
        acMobility.activityTypeList.add(new ActivityType(Track.TRACK_TYPE_BUS));
        acMobility.activityTypeList.add(new ActivityType(Track.TRACK_TYPE_TRAIN));
        acMobility.activityTypeList.add(new ActivityType(Track.TRACK_TYPE_AGRICULTURE));
        activityCategories.add(acMobility);

        // Other
        ActivityCategory acOther = new ActivityCategory(view.findViewById(R.id.tracktype_main_linearlayout_cat_other));
        acOther.activityTypeList.add(new ActivityType(Track.TRACK_TYPE_STEADY));
        acOther.activityTypeList.add(new ActivityType(Track.TRACK_TYPE_MOUNTAIN));
        acOther.activityTypeList.add(new ActivityType(Track.TRACK_TYPE_CITY));
        acOther.activityTypeList.add(new ActivityType(Track.TRACK_TYPE_FOREST));
        acOther.activityTypeList.add(new ActivityType(Track.TRACK_TYPE_WORK));
        acOther.activityTypeList.add(new ActivityType(Track.TRACK_TYPE_PHOTOGRAPHY));
        acOther.activityTypeList.add(new ActivityType(Track.TRACK_TYPE_RESEARCH));
        activityCategories.add(acOther);

        // Other
        ActivityCategory acOtherSports = new ActivityCategory(view.findViewById(R.id.tracktype_main_linearlayout_cat_other_sports));
        acOtherSports.activityTypeList.add(new ActivityType(Track.TRACK_TYPE_SOCCER));
        acOtherSports.activityTypeList.add(new ActivityType(Track.TRACK_TYPE_GOLF));
        acOtherSports.activityTypeList.add(new ActivityType(Track.TRACK_TYPE_PETS));
        acOtherSports.activityTypeList.add(new ActivityType(Track.TRACK_TYPE_MAP));
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
                iv.setColorFilter(getResources().getColor(aType.value == GPSApplication.getInstance().getSelectedTrackTypeOnDialog() ? R.color.textColorRecControlPrimary : R.color.colorIconDisabledOnDialog), PorterDuff.Mode.SRC_IN);

                // set Layout Params
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                iv.setLayoutParams(lp);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    iv.setTooltipText(Track.ACTIVITY_DESCRIPTION[aType.value]);
                }

                iv.setTag(aType.value);

                // set Margins
                LinearLayout.LayoutParams marginParams = (LinearLayout.LayoutParams) iv.getLayoutParams();
                marginParams.setMargins(ICON_MARGIN, ICON_MARGIN, ICON_MARGIN, ICON_MARGIN); // left, top, right, bottom
                iv.setLayoutParams(marginParams);

                ac.layout.addView(iv);

                iv.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        GPSApplication.getInstance().setSelectedTrackTypeOnDialog((Integer)view.getTag());
                        EventBus.getDefault().post(EventBusMSG.REFRESH_TRACKTYPE);
                        //((ImageView)view).setColorFilter(getResources().getColor(R.color.textColorRecControlPrimary), PorterDuff.Mode.SRC_IN);
                        dismiss();
                    }
                });
            }
        }

        return createPlacemarkAlert.create();
    }
}
