package org.movsim.movdroid;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.OnNavigationListener;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SubMenu;
import com.actionbarsherlock.view.Window;

public class MovSimDroidActivity extends SherlockActivity implements OnNavigationListener {

    

    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        getSupportActionBar().setBackgroundDrawable(
                getResources().getDrawable(R.drawable.abs__ab_transparent_dark_holo));


        Context context = getSupportActionBar().getThemedContext();
        ArrayAdapter<CharSequence> list = ArrayAdapter.createFromResource(context, R.array.project,
                R.layout.sherlock_spinner_item);
        list.setDropDownViewResource(R.layout.sherlock_spinner_dropdown_item);

        getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        getSupportActionBar().setListNavigationCallbacks(list, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add("Start").setIcon(R.drawable.ic_action_start)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);


        SubMenu subMenu1 = menu.addSubMenu("Menu");
        subMenu1.add("Sample");
        subMenu1.add("Menu");
        subMenu1.add("Items");

        MenuItem subMenu1Item = subMenu1.getItem();
        subMenu1Item.setIcon(R.drawable.abs__ic_menu_moreoverflow_holo_dark);
        subMenu1Item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Toast.makeText(this, "Got click: " + item.getTitle(), Toast.LENGTH_SHORT).show();

        // ActionBar Buttons
        if (item.getTitle().equals("Start")) {
            item.setIcon(R.drawable.ic_action_pause);
            item.setTitle("Pause");
        } else if (item.getTitle().equals("Pause")) {
            item.setIcon(R.drawable.ic_action_start);
            item.setTitle("Start");
        }

        return true;
    }

    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        // project selection
        Toast.makeText(this, "Got click: " + itemPosition, Toast.LENGTH_SHORT).show();
        return true;
    }

    // this is called on rotation instead in onCreate
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
    
}