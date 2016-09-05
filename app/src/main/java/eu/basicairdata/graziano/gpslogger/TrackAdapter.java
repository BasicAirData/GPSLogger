/*
 * TrackInfoAdapter - Java Class for Android
 * Created by G.Capelli (BasicAirData) on 19/6/2016
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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.util.ArrayList;


public class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.MyViewHolder> {

    public static final int NOT_AVAILABLE = -100000;

    private ArrayList<Track> dataSet;
    private int selectedItem = -1;

    private int[] Icons = {R.mipmap.ic_place_white_24dp, R.mipmap.ic_directions_walk_white_24dp, R.mipmap.ic_terrain_white_24dp,
            R.mipmap.ic_directions_run_white_24dp, R.mipmap.ic_directions_bike_white_24dp, R.mipmap.ic_directions_car_white_24dp,
            R.mipmap.ic_flight_white_24dp};

    public class MyViewHolder extends RecyclerView.ViewHolder {

        TextView textViewTrackName;
        TextView textViewTrackLength;
        TextView textViewTrackDuration;
        TextView textViewTrackAltitudeGap;
        TextView textViewTrackMaxSpeed;
        TextView textViewTrackAverageSpeed;
        TextView textViewTrackGeopoints;
        TextView textViewTrackPlacemarks;
        ImageView imageViewThumbnail;
        ImageView imageViewIcon;
        ProgressBar progressBar;


        public MyViewHolder(View itemView) {
            super(itemView);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedItem = getLayoutPosition();
                    if (selectedItem >= 0) {
                        if (dataSet.get(selectedItem).getProgress() == 0) {
                            EventBus.getDefault().post("TRACKLIST_SELECTION " + dataSet.get(selectedItem).getId());
                            Log.w("myApp", "[#] TrackAdapter.java - Selected track: " + dataSet.get(selectedItem).getName() + " (id = " + dataSet.get(selectedItem).getId() + ")");
                        }
                    }
                }
            });

            itemView.setClickable(true);

            this.textViewTrackName = (TextView) itemView.findViewById(R.id.id_textView_card_TrackName);
            this.textViewTrackLength = (TextView) itemView.findViewById(R.id.id_textView_card_length);
            this.textViewTrackDuration = (TextView) itemView.findViewById(R.id.id_textView_card_duration);
            this.textViewTrackAltitudeGap = (TextView) itemView.findViewById(R.id.id_textView_card_altitudegap);
            this.textViewTrackMaxSpeed = (TextView) itemView.findViewById(R.id.id_textView_card_maxspeed);
            this.textViewTrackAverageSpeed = (TextView) itemView.findViewById(R.id.id_textView_card_averagespeed);
            this.textViewTrackGeopoints = (TextView) itemView.findViewById(R.id.id_textView_card_geopoints);
            this.textViewTrackPlacemarks = (TextView) itemView.findViewById(R.id.id_textView_card_placemarks);
            this.imageViewThumbnail = (ImageView) itemView.findViewById(R.id.id_imageView_card_minimap);
            this.imageViewIcon = (ImageView) itemView.findViewById(R.id.id_imageView_card_tracktype);
            this.progressBar = (ProgressBar) itemView.findViewById(R.id.id_progressBar_card);
        }
    }

    public TrackAdapter(ArrayList<Track> data) {
        this.dataSet = data;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent,
                                           int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_trackinfo, parent, false);

        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, final int listPosition) {

        TextView textViewName = holder.textViewTrackName;
        TextView textViewLength = holder.textViewTrackLength;
        TextView textViewDuration = holder.textViewTrackDuration;
        TextView textViewAltitudeGap = holder.textViewTrackAltitudeGap;
        TextView textViewMaxSpeed = holder.textViewTrackMaxSpeed;
        TextView textViewAverageSpeed = holder.textViewTrackAverageSpeed;
        TextView textViewGeopoints = holder.textViewTrackGeopoints;
        TextView textViewPlacemarks = holder.textViewTrackPlacemarks;
        ImageView imageViewThumbnail = holder.imageViewThumbnail;
        ImageView imageViewIcon = holder.imageViewIcon;
        ProgressBar progressBar = holder.progressBar;

        long numberOfPoints = dataSet.get(listPosition).getNumberOfLocations();

        textViewName.setText(dataSet.get(listPosition).getName());
        textViewLength.setText(numberOfPoints > 1 ? dataSet.get(listPosition).getFormattedDistance() + " " + dataSet.get(listPosition).getFormattedDistanceUM() : "");
        textViewDuration.setText(numberOfPoints > 1 ? dataSet.get(listPosition).getFormattedPrefTime() : "");
        textViewAltitudeGap.setText(numberOfPoints > 1 ? dataSet.get(listPosition).getFormattedAltitudeGap(GPSApplication.getInstance().getPrefEGM96AltitudeCorrection()) + " " + dataSet.get(listPosition).getFormattedAltitudeUM() : "");
        textViewMaxSpeed.setText(numberOfPoints > 1 ? dataSet.get(listPosition).getFormattedSpeedMax() + " " + dataSet.get(listPosition).getFormattedSpeedUM() : "");
        textViewAverageSpeed.setText(numberOfPoints > 1 ? dataSet.get(listPosition).getFormattedPrefSpeedAverage() + " " + (dataSet.get(listPosition).getFormattedPrefSpeedAverage().equals("") ? "" : dataSet.get(listPosition).getFormattedSpeedUM()) : "");
        textViewGeopoints.setText(String.valueOf(dataSet.get(listPosition).getNumberOfLocations()));
        textViewPlacemarks.setText(String.valueOf(dataSet.get(listPosition).getNumberOfPlacemarks()));
        progressBar.setProgress(dataSet.get(listPosition).getProgress());

        int TT = dataSet.get(listPosition).getTrackType();
        if (TT != NOT_AVAILABLE) {
            imageViewIcon.setVisibility(View.VISIBLE);
            imageViewIcon.setImageResource(Icons[TT]);
        }
        else imageViewIcon.setVisibility(View.INVISIBLE);

        String Filename = GPSApplication.getInstance().getApplicationContext().getFilesDir() + "/Thumbnails/" + dataSet.get(listPosition).getId() + ".png";
        File file = new File(Filename);
        if (file.exists ()) {
            Bitmap bmp = BitmapFactory.decodeFile(Filename);
            imageViewThumbnail.setImageBitmap(bmp);
        } else imageViewThumbnail.setImageDrawable(null);
    }

    @Override
    public int getItemCount() {
        return dataSet.size();
    }
}