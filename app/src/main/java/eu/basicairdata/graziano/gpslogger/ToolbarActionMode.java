/*
 * ToolbarActionMode - Java Class for Android
 * Created by G.Capelli on 6/01/2019
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

import android.os.Handler;
import androidx.appcompat.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import static eu.basicairdata.graziano.gpslogger.GPSApplication.NOT_AVAILABLE;

/**
 * The Actionmode Toolbar for the Tracklist.
 * It comes out when one or more Tracks are selected on Tracklist.
 * This Toolbar contains the action that could be done with the selected Tracks.
 * For example Delete, Share, View, Export...
 */
public class ToolbarActionMode implements ActionMode.Callback {

    private Menu actionMenu;
    private MenuItem menuItemDelete;
    private MenuItem menuItemExport;
    private MenuItem menuItemShare;
    private MenuItem menuItemView;
    private MenuItem menuItemEdit;
    private boolean isActionmodeButtonPressed = false;            // A flag that avoids to start more than one job at a time

    private final GPSApplication gpsApp = GPSApplication.getInstance();
    private final Handler actionmodeButtonPressedHandler = new Handler();
    private final Runnable actionmodeButtonPressedRunnable = new Runnable() {
        @Override
        public void run() {
            setActionmodeButtonPressed(false);
        }
    };

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.getMenuInflater().inflate(R.menu.card_menu, menu);
        EventBus.getDefault().register(this);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        actionMenu = menu;
        menuItemEdit = actionMenu.findItem(R.id.cardmenu_edit);
        menuItemEdit.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menuItemShare = actionMenu.findItem(R.id.cardmenu_share);
        menuItemShare.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menuItemView = actionMenu.findItem(R.id.cardmenu_view);
        menuItemView.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menuItemExport = actionMenu.findItem(R.id.cardmenu_export);
        menuItemExport.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menuItemDelete = actionMenu.findItem(R.id.cardmenu_delete);
        menuItemDelete.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        EvaluateVisibility();
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        if (!isActionmodeButtonPressed()) {
            switch (item.getItemId()) {
                case R.id.cardmenu_delete:
                    setActionmodeButtonPressed(true);
                    EventBus.getDefault().post(EventBusMSG.ACTION_BULK_DELETE_TRACKS);
                    break;
                case R.id.cardmenu_export:
                    setActionmodeButtonPressed(true);
                    EventBus.getDefault().post(EventBusMSG.ACTION_BULK_EXPORT_TRACKS);
                    break;
                case R.id.cardmenu_view:
                    setActionmodeButtonPressed(true);
                    EventBus.getDefault().post(EventBusMSG.ACTION_BULK_VIEW_TRACKS);
                    break;
                case R.id.cardmenu_share:
                    setActionmodeButtonPressed(true);
                    EventBus.getDefault().post(EventBusMSG.ACTION_BULK_SHARE_TRACKS);
                    break;
                case R.id.cardmenu_edit:
                    setActionmodeButtonPressed(true);
                    EventBus.getDefault().post(EventBusMSG.ACTION_EDIT_TRACK);
                    break;
                default:
                    return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        EventBus.getDefault().unregister(this);
        if ((gpsApp.getNumberOfSelectedTracks() > 0) && gpsApp.getGPSActivityActiveTab() == 2) {
            GPSApplication.getInstance().deselectAllTracks();
            GPSApplication.getInstance().setLastClickId(NOT_AVAILABLE);
        }
    }

    /**
     * The EventBus receiver for Normal Messages.
     */
    @Subscribe (threadMode = ThreadMode.MAIN)
    public void onEvent(EventBusMSGNormal msg) {
        switch (msg.eventBusMSG) {
            case EventBusMSG.TRACKLIST_SELECT:
            case EventBusMSG.TRACKLIST_DESELECT:
                EvaluateVisibility();
        }
    }

    /**
     * The EventBus receiver for Short Messages.
     */
    @Subscribe (threadMode = ThreadMode.MAIN)
    public void onEvent(Short msg) {
        switch (msg) {
            case EventBusMSG.UPDATE_ACTIONBAR:
            case EventBusMSG.UPDATE_TRACKLIST:
                EvaluateVisibility();
        }
    }

    public boolean isActionmodeButtonPressed() {
        return isActionmodeButtonPressed;
    }

    /**
     * Sets the isActionmodeButtonPressed.
     * If sets to true, it starts the handler that resets the value to false.
     */
    public void setActionmodeButtonPressed(boolean actionmodeButtonPressed) {
        isActionmodeButtonPressed = actionmodeButtonPressed;
        if (actionmodeButtonPressed) {
            actionmodeButtonPressedHandler.postDelayed(actionmodeButtonPressedRunnable, 500);    // The Flag remains active for 500 ms
        } else actionmodeButtonPressedHandler.removeCallbacks(actionmodeButtonPressedRunnable);
    }

    /**
     * Evaluates the visibility of the buttons on the Toolbar basing on the selection,
     * the installed apps, and the Preferences.
     * It sets also the tooltip text and the icon of the View button.
     */
    public void EvaluateVisibility() {
        if (GPSApplication.getInstance().getNumberOfSelectedTracks() > 0) {
            menuItemView.setVisible((gpsApp.getNumberOfSelectedTracks() <= 1) && (gpsApp.isContextMenuViewVisible()));
            menuItemEdit.setVisible(gpsApp.getNumberOfSelectedTracks() <= 1);
            menuItemShare.setVisible(gpsApp.isContextMenuShareVisible() && (gpsApp.getPrefExportGPX() || gpsApp.getPrefExportKML() || gpsApp.getPrefExportTXT()));
            menuItemExport.setVisible(gpsApp.getPrefExportGPX() || gpsApp.getPrefExportKML() || gpsApp.getPrefExportTXT());
            menuItemDelete.setVisible(!gpsApp.getSelectedTracks().contains(gpsApp.getCurrentTrack()));

            if (menuItemView.isVisible()) {
                if (!gpsApp.getViewInApp().equals("")) {
                    menuItemView.setTitle(gpsApp.getString(R.string.card_menu_view, gpsApp.getViewInApp()));
                    if (gpsApp.getViewInAppIcon() != null)
                        menuItemView.setIcon(gpsApp.getViewInAppIcon());
                    else
                        menuItemView.setIcon(R.drawable.ic_visibility_24dp);
                } else {
                    menuItemView.setTitle(gpsApp.getString(R.string.card_menu_view_selector)).setIcon(R.drawable.ic_visibility_24dp);
                }
            }
        }
    }
}
