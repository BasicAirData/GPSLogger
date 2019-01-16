/**
 * FragmentJobProgress - Java Class for Android
 * Created by G.Capelli (BasicAirData) on 6/1/2019
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

import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class FragmentJobProgress extends Fragment {

    ProgressBar progressBar;


    public FragmentJobProgress() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_job_progress, container, false);
        progressBar = (ProgressBar) view.findViewById(R.id.id_jobProgressBar);
        progressBar.setProgress(GPSApplication.getInstance().getJobProgress());
        return view;
    }

    @Override
    public void onResume() {
        EventBus.getDefault().register(this);
        Update();
        super.onResume();
    }

    @Override
    public void onPause() {
        EventBus.getDefault().unregister(this);
        super.onPause();
    }

    @Subscribe (threadMode = ThreadMode.MAIN)
    public void onEvent(Short msg) {
        if (msg == EventBusMSG.UPDATE_JOB_PROGRESS) {
            Update();
        }
    }

    public void Update() {
        if (isAdded()) {
            progressBar.setProgress((GPSApplication.getInstance().getJobProgress() == 100) || (GPSApplication.getInstance().getJobsPending() == 0 ) ? 0 : GPSApplication.getInstance().getJobProgress());
        }
    }
}
