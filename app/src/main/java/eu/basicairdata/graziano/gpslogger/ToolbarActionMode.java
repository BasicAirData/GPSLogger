package eu.basicairdata.graziano.gpslogger;

import android.os.Handler;
import androidx.appcompat.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import static eu.basicairdata.graziano.gpslogger.GPSApplication.NOT_AVAILABLE;


public class ToolbarActionMode implements ActionMode.Callback {

    private Menu actionmenu;
    private MenuItem menuItemDelete;
    private MenuItem menuItemExport;
    private MenuItem menuItemShare;
    private MenuItem menuItemView;
    private MenuItem menuItemEdit;
    private final GPSApplication gpsApplication = GPSApplication.getInstance();


    // A flag that avoids to start more than one job at a time on Actionmode Toolbar
    private boolean ActionmodeButtonPressed = false;

    private final Handler handler_ActionmodeButtonPressed = new Handler();
    private final Runnable r_ActionmodeButtonPressed = new Runnable() {
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
        menuItemEdit = actionmenu.findItem(R.id.cardmenu_edit);
        menuItemEdit.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menuItemShare = actionmenu.findItem(R.id.cardmenu_share);
        menuItemShare.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menuItemView = actionmenu.findItem(R.id.cardmenu_view);
        menuItemView.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menuItemExport = actionmenu.findItem(R.id.cardmenu_export);
        menuItemExport.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menuItemDelete = actionmenu.findItem(R.id.cardmenu_delete);
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
        if ((gpsApplication.getNumberOfSelectedTracks() > 0) && gpsApplication.getGPSActivity_activeTab() == 2) {
            GPSApplication.getInstance().DeselectAllTracks();
            GPSApplication.getInstance().setLastClickId(NOT_AVAILABLE);
        }
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
            menuItemView.setVisible((gpsApplication.getNumberOfSelectedTracks() <= 1) && (gpsApplication.isContextMenuViewVisible()));
            menuItemEdit.setVisible(gpsApplication.getNumberOfSelectedTracks() <= 1);
            menuItemShare.setVisible(gpsApplication.isContextMenuShareVisible() && (gpsApplication.getPrefExportGPX() || gpsApplication.getPrefExportKML() || gpsApplication.getPrefExportTXT()));
            menuItemExport.setVisible(gpsApplication.getPrefExportGPX() || gpsApplication.getPrefExportKML() || gpsApplication.getPrefExportTXT());
            menuItemDelete.setVisible(!gpsApplication.getSelectedTracks().contains(gpsApplication.getCurrentTrack()));

            if (menuItemView.isVisible()) {
                if (!gpsApplication.getViewInApp().equals("")) {
                    menuItemView.setTitle(gpsApplication.getString(R.string.card_menu_view, gpsApplication.getViewInApp()));
                    if (gpsApplication.getViewInAppIcon() != null)
                        menuItemView.setIcon(gpsApplication.getViewInAppIcon());
                    else
                        menuItemView.setIcon(R.drawable.ic_visibility_24dp);
                } else {
                    menuItemView.setTitle(gpsApplication.getString(R.string.card_menu_view_selector)).setIcon(R.drawable.ic_visibility_24dp);
                }
            }
        }
    }
}
