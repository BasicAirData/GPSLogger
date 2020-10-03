package eu.basicairdata.graziano.gpslogger;

import android.os.Handler;
import android.support.v7.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class ToolbarActionMode implements ActionMode.Callback {

    private Menu actionmenu;
    private GPSApplication gpsApplication = GPSApplication.getInstance();


    // A flag that avoids to start more than one job at a time on Actionmode Toolbar
    private boolean ActionmodeButtonPressed = false;

    private final Handler handler_ActionmodeButtonPressed = new Handler();
    private Runnable r_ActionmodeButtonPressed = new Runnable() {
        @Override
        public void run() {
            setActionmodeButtonPressed(false);
        }
    };

    public boolean isActionmodeButtonPressed() {
        return ActionmodeButtonPressed;
    }

    public void setActionmodeButtonPressed(boolean actionmodeButtonPressed) {
        ActionmodeButtonPressed = actionmodeButtonPressed;
        if (actionmodeButtonPressed) {
            handler_ActionmodeButtonPressed.postDelayed(r_ActionmodeButtonPressed, 500);    // The Flag remains active for 500 ms
        } else handler_ActionmodeButtonPressed.removeCallbacks(r_ActionmodeButtonPressed);
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.getMenuInflater().inflate(R.menu.card_menu, menu);
        EventBus.getDefault().register(this);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        actionmenu = menu;
        actionmenu.findItem(R.id.cardmenu_share).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        actionmenu.findItem(R.id.cardmenu_view).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        actionmenu.findItem(R.id.cardmenu_export).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        actionmenu.findItem(R.id.cardmenu_delete).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
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
        if ((gpsApplication.getNumberOfSelectedTracks() > 0) && gpsApplication.getGPSActivity_activeTab() == 2) GPSApplication.getInstance().DeselectAllTracks();
    }

    @Subscribe (threadMode = ThreadMode.MAIN)
    public void onEvent(EventBusMSGNormal msg) {
        switch (msg.MSGType) {
            case EventBusMSG.TRACKLIST_SELECT:
            case EventBusMSG.TRACKLIST_DESELECT:
                EvaluateVisibility();
        }
    }

    @Subscribe (threadMode = ThreadMode.MAIN)
    public void onEvent(Short msg) {
        switch (msg) {
            case EventBusMSG.UPDATE_ACTIONBAR:
            case EventBusMSG.UPDATE_TRACKLIST:
                EvaluateVisibility();
        }
    }

    public void EvaluateVisibility() {
        if (GPSApplication.getInstance().getNumberOfSelectedTracks() > 0) {
            actionmenu.findItem(R.id.cardmenu_view).setVisible((gpsApplication.getNumberOfSelectedTracks() <= 1) && (gpsApplication.isContextMenuViewVisible()));
            actionmenu.findItem(R.id.cardmenu_share).setVisible(gpsApplication.isContextMenuShareVisible() && (gpsApplication.getPrefExportGPX() || gpsApplication.getPrefExportKML() || gpsApplication.getPrefExportTXT()));
            actionmenu.findItem(R.id.cardmenu_export).setVisible(gpsApplication.getPrefExportGPX() || gpsApplication.getPrefExportKML() || gpsApplication.getPrefExportTXT());
            actionmenu.findItem(R.id.cardmenu_delete).setVisible(!gpsApplication.getSelectedTracks().contains(gpsApplication.getCurrentTrack()));

            if (actionmenu.findItem(R.id.cardmenu_view).isVisible()) {
                if (!gpsApplication.getViewInApp().equals("")) {
                    actionmenu.findItem(R.id.cardmenu_view).setTitle(gpsApplication.getString(R.string.card_menu_view, gpsApplication.getViewInApp()));
                    if (gpsApplication.getViewInAppIcon() != null)
                        actionmenu.findItem(R.id.cardmenu_view).setIcon(gpsApplication.getViewInAppIcon());
                    else
                        actionmenu.findItem(R.id.cardmenu_view).setIcon(R.mipmap.ic_visibility_white_24dp);
                } else {
                    actionmenu.findItem(R.id.cardmenu_view).setTitle(gpsApplication.getString(R.string.card_menu_view_selector)).setIcon(R.mipmap.ic_visibility_white_24dp);
                }
            }
        }
    }
}
