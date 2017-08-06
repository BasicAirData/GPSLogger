/**
 * FragmentPlacemarkDialog - Java Class for Android
 * Created by G.Capelli (BasicAirData) on 9/7/2016
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

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import org.greenrobot.eventbus.EventBus;

public class FragmentPlacemarkDialog extends DialogFragment {

    EditText DescEditText;

    //@SuppressLint("InflateParams")
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder createPlacemarkAlert = new AlertDialog.Builder(getActivity(), R.style.StyledDialog);

        createPlacemarkAlert.setTitle(R.string.dlg_add_placemark);
        createPlacemarkAlert.setIcon(R.mipmap.ic_add_location_white_24dp);

        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View view = (View) inflater.inflate(R.layout.fragment_placemark_dialog, null);

        DescEditText = (EditText) view.findViewById(R.id.placemark_description);
        DescEditText.postDelayed(new Runnable()
        {
            public void run()
            {
                if (isAdded()) {
                    DescEditText.requestFocus();
                    InputMethodManager mgr = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    mgr.showSoftInput(DescEditText, InputMethodManager.SHOW_IMPLICIT);
                }
            }
        }, 200);

        createPlacemarkAlert.setView(view)

                //.setPositiveButton(R.string.conti_nue, new DialogInterface.OnClickListener() {
                .setPositiveButton(R.string.dlg_button_add, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        if (isAdded()) {
                            String PlacemarkDescription = DescEditText.getText().toString();
                            final GPSApplication GlobalVariables = (GPSApplication) getActivity().getApplicationContext();
                            GlobalVariables.setPlacemarkDescription(PlacemarkDescription.trim());
                            EventBus.getDefault().post(EventBusMSG.ADD_PLACEMARK);
                            //Log.w("myApp", "[#] FragmentPlacemarkDialog.java - posted ADD_PLACEMARK: " + PlacemarkDescription);
                        }
                    }
                })
                //.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                .setNegativeButton(R.string.dlg_button_cancel, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int id) {

                    }
                });
        return createPlacemarkAlert.create();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getDialog().getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }
}