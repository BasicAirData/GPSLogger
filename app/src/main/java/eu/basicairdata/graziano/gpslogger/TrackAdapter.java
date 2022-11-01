/*
 * TrackAdapter - Java Class for Android
 * Created by G.Capelli on 19/6/2016
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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

import static eu.basicairdata.graziano.gpslogger.GPSApplication.NOT_AVAILABLE;

/**
 * The Adapter for the Card View of the Tracklist.
 */
class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.TrackHolder> {

    private static final Bitmap BMP_CURRENT_TRACK_RECORDING = BitmapFactory.decodeResource(GPSApplication.getInstance().getResources(), R.mipmap.ic_recording_48dp);
    private static final Bitmap BMP_CURRENT_TRACK_PAUSED = BitmapFactory.decodeResource(GPSApplication.getInstance().getResources(), R.mipmap.ic_paused_white_48dp);

    private final List<Track> dataSet;
    boolean isLightTheme;
    private long startAnimationTime = 0;
    private long pointsCount = GPSApplication.getInstance().getCurrentTrack().getNumberOfLocations() + GPSApplication.getInstance().getCurrentTrack().getNumberOfPlacemarks();

    /**
     * The ViewHolder for the TrackAdapter.
     */
    class TrackHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

        private final PhysicalDataFormatter phdformatter = new PhysicalDataFormatter();
        private PhysicalData phd;
        private Track track;
        private int tt;

        private final CardView card;
        private final TextView textViewTrackName;
        private final TextView textViewTrackDescription;
        private final TextView textViewTrackLength;
        private final TextView textViewTrackDuration;
        private final TextView textViewTrackAltitudeGap;
        private final TextView textViewTrackMaxSpeed;
        private final TextView textViewTrackAverageSpeed;
        private final TextView textViewTrackGeopoints;
        private final TextView textViewTrackPlacemarks;
        private final ImageView imageViewThumbnail;
        private final ImageView imageViewPulse;
        private final ImageView imageViewIcon;

        @Override
        public void onClick(View v) {
            if (GPSApplication.getInstance().getJobsPending() == 0) {
                track.setSelected(!track.isSelected());
                card.setSelected(track.isSelected());
                GPSApplication.getInstance().setLastClickId(track.getId());
                GPSApplication.getInstance().setLastClickState(track.isSelected());
                //Log.w("myApp", "[#] TrackAdapter.java - " + (track.isSelected() ? "Selected" : "Deselected") + " id = " + GPSApplication.getInstance().getLastClickId());
                EventBus.getDefault().post(new EventBusMSGNormal(track.isSelected() ? EventBusMSG.TRACKLIST_SELECT : EventBusMSG.TRACKLIST_DESELECT, track.getId()));
                //Log.w("myApp", "[#] TrackAdapter.java - Selected track id = " + track.getId());
            }
        }

        @Override
        public boolean onLongClick(View view) {
            if ((GPSApplication.getInstance().getJobsPending() == 0)
                    && (GPSApplication.getInstance().getLastClickId() != track.getId())
                    && (GPSApplication.getInstance().getNumberOfSelectedTracks() > 0)) {
                //Log.w("myApp", "[#] TrackAdapter.java - onLongClick");
                EventBus.getDefault().post(new EventBusMSGNormal(EventBusMSG.TRACKLIST_RANGE_SELECTION, track.getId()));
            }
            return false;
        }

        TrackHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
            // CardView
            card                        = itemView.findViewById(R.id.card_view);
            // TextViews
            textViewTrackName           = itemView.findViewById(R.id.id_textView_card_TrackName);
            textViewTrackDescription    = itemView.findViewById(R.id.id_textView_card_TrackDesc);
            textViewTrackLength         = itemView.findViewById(R.id.id_textView_card_length);
            textViewTrackDuration       = itemView.findViewById(R.id.id_textView_card_duration);
            textViewTrackAltitudeGap    = itemView.findViewById(R.id.id_textView_card_altitudegap);
            textViewTrackMaxSpeed       = itemView.findViewById(R.id.id_textView_card_maxspeed);
            textViewTrackAverageSpeed   = itemView.findViewById(R.id.id_textView_card_averagespeed);
            textViewTrackGeopoints      = itemView.findViewById(R.id.id_textView_card_geopoints);
            textViewTrackPlacemarks     = itemView.findViewById(R.id.id_textView_card_placemarks);
            // ImageViews
            imageViewThumbnail          = itemView.findViewById(R.id.id_imageView_card_minimap);
            imageViewPulse              = itemView.findViewById(R.id.id_imageView_card_pulse);
            imageViewIcon               = itemView.findViewById(R.id.id_imageView_card_tracktype);
            if (isLightTheme) {
                imageViewThumbnail.setColorFilter(GPSApplication.colorMatrixColorFilter);
                imageViewPulse.setColorFilter(GPSApplication.colorMatrixColorFilter);
                //imageViewIcon.setColorFilter(GPSApplication.colorMatrixColorFilter);
            }
        }

        /**
         * Updates the statistics of the current track's card, using the given data.
         *
         * @param trk the track containing the data
         */
        void UpdateTrackStats(Track trk) {
            //textViewTrackName.setText(trk.getName());
            if (trk.getNumberOfLocations() >= 1) {
                phd = phdformatter.format(trk.getEstimatedDistance(),PhysicalDataFormatter.FORMAT_DISTANCE);
                textViewTrackLength.setText(phd.value + " " + phd.um);
                phd = phdformatter.format(trk.getPrefTime(),PhysicalDataFormatter.FORMAT_DURATION);
                textViewTrackDuration.setText(phd.value);
                phd = phdformatter.format(trk.getEstimatedAltitudeGap(GPSApplication.getInstance().getPrefEGM96AltitudeCorrection()),PhysicalDataFormatter.FORMAT_ALTITUDE);
                textViewTrackAltitudeGap.setText(phd.value + " " + phd.um);
                phd = phdformatter.format(trk.getSpeedMax(),PhysicalDataFormatter.FORMAT_SPEED);
                textViewTrackMaxSpeed.setText(phd.value + " " + phd.um);
                phd = phdformatter.format(trk.getPrefSpeedAverage(),PhysicalDataFormatter.FORMAT_SPEED_AVG);
                textViewTrackAverageSpeed.setText(phd.value + " " + phd.um);
            } else {
                textViewTrackLength.setText("");
                textViewTrackDuration.setText("");
                textViewTrackAltitudeGap.setText("");
                textViewTrackMaxSpeed.setText("");
                textViewTrackAverageSpeed.setText("");
            }
            textViewTrackGeopoints.setText(String.valueOf(trk.getNumberOfLocations()));
            textViewTrackPlacemarks.setText(String.valueOf(trk.getNumberOfPlacemarks()));

            tt = trk.getEstimatedTrackType();
            if (tt != NOT_AVAILABLE) imageViewIcon.setImageResource(Track.ACTIVITY_DRAWABLE_RESOURCE[tt]);
            else imageViewIcon.setImageBitmap(null);

            if (GPSApplication.getInstance().isRecording()) {
                imageViewThumbnail.setImageBitmap(BMP_CURRENT_TRACK_RECORDING);
                imageViewPulse.setVisibility(View.VISIBLE);
                if ((pointsCount != trk.getNumberOfLocations()+trk.getNumberOfPlacemarks()) && (System.currentTimeMillis() - startAnimationTime >= 700L)) {
                    pointsCount = trk.getNumberOfLocations()+trk.getNumberOfPlacemarks();
                    Animation sunRise = AnimationUtils.loadAnimation(GPSApplication.getInstance().getApplicationContext(), R.anim.record_pulse);
                    imageViewPulse.startAnimation(sunRise);
                    startAnimationTime = System.currentTimeMillis();
                }
            } else {
                imageViewPulse.setVisibility(View.INVISIBLE);
                imageViewThumbnail.setImageBitmap(BMP_CURRENT_TRACK_PAUSED);
            }
        }

        /**
         * Binds a card using the given data.
         *
         * @param trk the track containing the data
         */
        void BindTrack(Track trk) {
            track = trk;
            card.setSelected(track.isSelected());
            imageViewPulse.setVisibility(View.INVISIBLE);
            textViewTrackName.setText(track.getName());
            if (track.getDescription().isEmpty())
                textViewTrackDescription.setText(GPSApplication.getInstance().getString(R.string.track_id) + " " + track.getId());
            else textViewTrackDescription.setText(track.getDescription());
            if (trk.getNumberOfLocations() >= 1) {
                phd = phdformatter.format(track.getEstimatedDistance(),PhysicalDataFormatter.FORMAT_DISTANCE);
                textViewTrackLength.setText(phd.value + " " + phd.um);
                phd = phdformatter.format(track.getPrefTime(),PhysicalDataFormatter.FORMAT_DURATION);
                textViewTrackDuration.setText(phd.value);
                phd = phdformatter.format(track.getEstimatedAltitudeGap(GPSApplication.getInstance().getPrefEGM96AltitudeCorrection()),PhysicalDataFormatter.FORMAT_ALTITUDE);
                textViewTrackAltitudeGap.setText(phd.value + " " + phd.um);
                phd = phdformatter.format(track.getSpeedMax(),PhysicalDataFormatter.FORMAT_SPEED);
                textViewTrackMaxSpeed.setText(phd.value + " " + phd.um);
                phd = phdformatter.format(track.getPrefSpeedAverage(),PhysicalDataFormatter.FORMAT_SPEED_AVG);
                textViewTrackAverageSpeed.setText(phd.value + " " + phd.um);
            } else {
                textViewTrackLength.setText("");
                textViewTrackDuration.setText("");
                textViewTrackAltitudeGap.setText("");
                textViewTrackMaxSpeed.setText("");
                textViewTrackAverageSpeed.setText("");
            }
            textViewTrackGeopoints.setText(String.valueOf(track.getNumberOfLocations()));
            textViewTrackPlacemarks.setText(String.valueOf(track.getNumberOfPlacemarks()));

            tt = trk.getEstimatedTrackType();
            if (tt != NOT_AVAILABLE)
                try {
                    imageViewIcon.setImageResource(Track.ACTIVITY_DRAWABLE_RESOURCE[tt]);
                } catch (IndexOutOfBoundsException e) {
                    imageViewIcon.setImageBitmap(null);
                }
            else imageViewIcon.setImageBitmap(null);

            if (GPSApplication.getInstance().getCurrentTrack().getId() == track.getId()) {
                imageViewThumbnail.setImageBitmap (GPSApplication.getInstance().isRecording() ? BMP_CURRENT_TRACK_RECORDING : BMP_CURRENT_TRACK_PAUSED);
            }
            else {
                Glide.clear(imageViewThumbnail);
                Glide
                        .with(GPSApplication.getInstance().getApplicationContext())
                        .load(GPSApplication.getInstance().getApplicationContext().getFilesDir().toString() + "/Thumbnails/" + track.getId() + ".png")
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        //.skipMemoryCache(true)
                        .error(null)
                        .dontAnimate()
                        .into(imageViewThumbnail);
            }
        }
    }

    TrackAdapter(List<Track> data) {
        synchronized(data) {
            this.dataSet = data;
        }
    }

    @Override
    public TrackHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new TrackHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.card_trackinfo, parent, false));
    }

    @Override
    public void onBindViewHolder(TrackHolder holder, int listPosition) {
        holder.BindTrack(dataSet.get(listPosition));
    }

    @Override
    public int getItemCount() {
        return dataSet.size();
    }
}