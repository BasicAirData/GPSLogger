/**
 * TrackAdapter - Java Class for Android
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
import android.graphics.ColorMatrixColorFilter;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
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


class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.TrackHolder> {

    private static final float[] NEGATIVE = {
            -1.0f,      0,      0,     0,  248, // red
                0,  -1.0f,      0,     0,  248, // green
                0,      0,  -1.0f,     0,  248, // blue
                0,      0,      0, 1.00f,    0  // alpha
    };
    private ColorMatrixColorFilter colorMatrixColorFilter = new ColorMatrixColorFilter(NEGATIVE);

    private final static int NOT_AVAILABLE = -100000;

    boolean isLightTheme = false;

    private List<Track> dataSet;

    private static final Bitmap[] bmpTrackType = {
            BitmapFactory.decodeResource(GPSApplication.getInstance().getResources(), R.mipmap.ic_place_white_24dp),
            BitmapFactory.decodeResource(GPSApplication.getInstance().getResources(), R.mipmap.ic_directions_walk_white_24dp),
            BitmapFactory.decodeResource(GPSApplication.getInstance().getResources(), R.mipmap.ic_terrain_white_24dp),
            BitmapFactory.decodeResource(GPSApplication.getInstance().getResources(), R.mipmap.ic_directions_run_white_24dp),
            BitmapFactory.decodeResource(GPSApplication.getInstance().getResources(), R.mipmap.ic_directions_bike_white_24dp),
            BitmapFactory.decodeResource(GPSApplication.getInstance().getResources(), R.mipmap.ic_directions_car_white_24dp),
            BitmapFactory.decodeResource(GPSApplication.getInstance().getResources(), R.mipmap.ic_flight_white_24dp)
    };

    private static final Bitmap bmpCurrentTrackRecording = BitmapFactory.decodeResource(GPSApplication.getInstance().getResources(), R.mipmap.ic_recording_48dp);
    private static final Bitmap bmpCurrentTrackPaused = BitmapFactory.decodeResource(GPSApplication.getInstance().getResources(), R.mipmap.ic_paused_white_48dp);

    private long StartAnimationTime = 0;
    private long PointsCount = GPSApplication.getInstance().getCurrentTrack().getNumberOfLocations() + GPSApplication.getInstance().getCurrentTrack().getNumberOfPlacemarks();

    class TrackHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private final PhysicalDataFormatter phdformatter = new PhysicalDataFormatter();
        private PhysicalData phd;
        private Track track;
        private int TT;

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
                EventBus.getDefault().post(new EventBusMSGNormal(track.isSelected() ? EventBusMSG.TRACKLIST_SELECT : EventBusMSG.TRACKLIST_DESELECT, track.getId()));
                //Log.w("myApp", "[#] TrackAdapter.java - Selected track id = " + track.getId());
            }
        }


        TrackHolder(View itemView) {
            super(itemView);

            itemView.setOnClickListener(this);

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
                imageViewThumbnail.setColorFilter(colorMatrixColorFilter);
                imageViewPulse.setColorFilter(colorMatrixColorFilter);
                imageViewIcon.setColorFilter(colorMatrixColorFilter);
            }
        }


        void UpdateTrackStats(Track trk) {
            //textViewTrackName.setText(trk.getName());

            if (trk.getNumberOfLocations() >= 1) {
                phd = phdformatter.format(trk.getEstimatedDistance(),PhysicalDataFormatter.FORMAT_DISTANCE);
                textViewTrackLength.setText(phd.Value + " " + phd.UM);
                phd = phdformatter.format(trk.getPrefTime(),PhysicalDataFormatter.FORMAT_DURATION);
                textViewTrackDuration.setText(phd.Value);
                phd = phdformatter.format(trk.getEstimatedAltitudeGap(GPSApplication.getInstance().getPrefEGM96AltitudeCorrection()),PhysicalDataFormatter.FORMAT_ALTITUDE);
                textViewTrackAltitudeGap.setText(phd.Value + " " + phd.UM);
                phd = phdformatter.format(trk.getSpeedMax(),PhysicalDataFormatter.FORMAT_SPEED);
                textViewTrackMaxSpeed.setText(phd.Value + " " + phd.UM);
                phd = phdformatter.format(trk.getPrefSpeedAverage(),PhysicalDataFormatter.FORMAT_SPEED_AVG);
                textViewTrackAverageSpeed.setText(phd.Value + " " + phd.UM);
            } else {
                textViewTrackLength.setText("");
                textViewTrackDuration.setText("");
                textViewTrackAltitudeGap.setText("");
                textViewTrackMaxSpeed.setText("");
                textViewTrackAverageSpeed.setText("");
            }
            textViewTrackGeopoints.setText(String.valueOf(trk.getNumberOfLocations()));
            textViewTrackPlacemarks.setText(String.valueOf(trk.getNumberOfPlacemarks()));

            TT = trk.getTrackType();
            if (TT != NOT_AVAILABLE) imageViewIcon.setImageBitmap(bmpTrackType[TT]);
            else imageViewIcon.setImageBitmap(null);

            if (GPSApplication.getInstance().getRecording()) {
                imageViewThumbnail.setImageBitmap(bmpCurrentTrackRecording);
                imageViewPulse.setVisibility(View.VISIBLE);
                if ((PointsCount != trk.getNumberOfLocations()+trk.getNumberOfPlacemarks()) && (System.currentTimeMillis() - StartAnimationTime >= 700L)) {
                    PointsCount = trk.getNumberOfLocations()+trk.getNumberOfPlacemarks();
                    Animation sunRise = AnimationUtils.loadAnimation(GPSApplication.getInstance().getApplicationContext(), R.anim.record_pulse);
                    imageViewPulse.startAnimation(sunRise);
                    StartAnimationTime = System.currentTimeMillis();
                }
            } else {
                imageViewPulse.setVisibility(View.INVISIBLE);
                imageViewThumbnail.setImageBitmap(bmpCurrentTrackPaused);
            }
        }


        void BindTrack(Track trk) {
            track = trk;

            card.setSelected(track.isSelected());

            imageViewPulse.setVisibility(View.INVISIBLE);
            textViewTrackName.setText(track.getName());
            textViewTrackDescription.setText(GPSApplication.getInstance().getString(R.string.track_id) + " " + track.getId());

            if (trk.getNumberOfLocations() >= 1) {
                phd = phdformatter.format(track.getEstimatedDistance(),PhysicalDataFormatter.FORMAT_DISTANCE);
                textViewTrackLength.setText(phd.Value + " " + phd.UM);
                phd = phdformatter.format(track.getPrefTime(),PhysicalDataFormatter.FORMAT_DURATION);
                textViewTrackDuration.setText(phd.Value);
                phd = phdformatter.format(track.getEstimatedAltitudeGap(GPSApplication.getInstance().getPrefEGM96AltitudeCorrection()),PhysicalDataFormatter.FORMAT_ALTITUDE);
                textViewTrackAltitudeGap.setText(phd.Value + " " + phd.UM);
                phd = phdformatter.format(track.getSpeedMax(),PhysicalDataFormatter.FORMAT_SPEED);
                textViewTrackMaxSpeed.setText(phd.Value + " " + phd.UM);
                phd = phdformatter.format(track.getPrefSpeedAverage(),PhysicalDataFormatter.FORMAT_SPEED_AVG);
                textViewTrackAverageSpeed.setText(phd.Value + " " + phd.UM);
            } else {
                textViewTrackLength.setText("");
                textViewTrackDuration.setText("");
                textViewTrackAltitudeGap.setText("");
                textViewTrackMaxSpeed.setText("");
                textViewTrackAverageSpeed.setText("");
            }
            textViewTrackGeopoints.setText(String.valueOf(track.getNumberOfLocations()));
            textViewTrackPlacemarks.setText(String.valueOf(track.getNumberOfPlacemarks()));

            TT = trk.getTrackType();
            if (TT != NOT_AVAILABLE) imageViewIcon.setImageBitmap(bmpTrackType[TT]);
            else imageViewIcon.setImageBitmap(null);

            if (GPSApplication.getInstance().getCurrentTrack().getId() == track.getId()) {
                imageViewThumbnail.setImageBitmap (GPSApplication.getInstance().getRecording() ? bmpCurrentTrackRecording : bmpCurrentTrackPaused);
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