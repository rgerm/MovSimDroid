/*
 * Copyright (C) 2010, 2011, 2012 by Arne Kesting, Martin Treiber, Ralph Germ, Martin Budden
 *                                   <movsim.org@gmail.com>
 * -----------------------------------------------------------------------------------------
 * 
 * This file is part of
 * 
 * MovSim - the multi-model open-source vehicular-traffic simulator.
 * 
 * MovSim is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * MovSim is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with MovSim. If not, see <http://www.gnu.org/licenses/>
 * or <http://www.movsim.org>.
 * 
 * -----------------------------------------------------------------------------------------
 */
package org.movsim.movdroid;

import org.apache.log4j.Level;
import org.movsim.input.ProjectMetaData;
import org.movsim.simulator.SimulationRun;
import org.movsim.simulator.SimulationRunnable;
import org.movsim.simulator.Simulator;

import android.content.Context;
import android.content.Intent;
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

import de.mindpipe.android.logging.log4j.LogConfigurator;

public class MovSimDroidActivity extends SherlockActivity implements OnNavigationListener,
        SimulationRun.CompletionCallback {

    // MovSim core uses slf4j as a logging facade for log4j.
    static {
        final LogConfigurator logConfigurator = new LogConfigurator();

        logConfigurator.setUseFileAppender(false);
        logConfigurator.setUseLogCatAppender(true);
        // logConfigurator.setFileName(Environment.getExternalStorageDirectory() + "myapp.log");
        logConfigurator.setRootLevel(Level.INFO);
        // Set log level of a specific logger
        logConfigurator.setLevel("org.apache", Level.INFO);
        logConfigurator.configure();
    }

    private ProjectMetaData projectMetaData;
    private Simulator simulator;
    private SimulationRunnable simulationRunnable;
    
    private MovSimView movSimView;
    private Menu menu;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        super.onCreate(savedInstanceState);

        // Replace parser from MovSim. -> Default values from DTD are not set. -> update xml files from MovSim before!
        System.setProperty("org.xml.sax.driver", "org.xmlpull.v1.sax2.Driver");

        initActionBar();

        setupSimulator();

        movSimView = new MovSimView(this, simulator);
        setContentView(movSimView);

        // statusTime = (TextView) findViewById(R.id.statusTime);
        // statusVehicles = (TextView) findViewById(R.id.statusVehiclesOnRoads);
        // setStatusViews();

    }

    private void setupSimulator() {
        projectMetaData = ProjectMetaData.getInstance();

        projectMetaData.setXmlFromResources(true);
        projectMetaData.setInstantaneousFileOutput(false);

        simulator = new Simulator(projectMetaData);

        simulationRunnable = simulator.getSimulationRunnable();
        simulationRunnable.setCompletionCallback(this);

        simulator.loadScenarioFromXml("offramp", "/sim/buildingBlocks/");
    }

    private void initActionBar() {
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
        this.menu = menu;
        menu.add("Start").setIcon(R.drawable.ic_action_start)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        menu.add("Restart").setIcon(R.drawable.ic_action_restart)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

        SubMenu subMenu1 = menu.addSubMenu("Menu");
        subMenu1.add("Faster");
        subMenu1.add("Slower");
        subMenu1.add("Info");

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
            if (!simulationRunnable.isPaused()) {
                simulationRunnable.start();
            } else {
                simulationRunnable.resume();
            }
        } else if (item.getTitle().equals("Pause")) {
            item.setIcon(R.drawable.ic_action_start);
            item.setTitle("Start");
            simulationRunnable.pause();
        } else if (item.getTitle().equals("Restart")) {
            simulator.getRoadNetwork().clear();
            simulator.initialize();
        } else if (item.getTitle().equals("Faster")) {
            int sleepTime = simulationRunnable.sleepTime();
            sleepTime -= sleepTime <= 5 ? 1 : 5;
            if (sleepTime < 0) {
                sleepTime = 0;
            }
            simulationRunnable.setSleepTime(sleepTime);
        } else if (item.getTitle().equals("Slower")) {
            int sleepTime = simulationRunnable.sleepTime();
            sleepTime += sleepTime < 5 ? 1 : 5;
            if (sleepTime > 400) {
                sleepTime = 400;
            }
            simulationRunnable.setSleepTime(sleepTime);
        } else if (item.getTitle().equals("Info")) {
            Intent intent = new Intent();
            intent.setClass(MovSimDroidActivity.this, InfoDialog.class);
            startActivity(intent);
        }
        return true;
    }

    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        // project selection
        Toast.makeText(this, "Got click: " + itemPosition, Toast.LENGTH_SHORT).show();
        if (itemPosition == 1) {
            simulator.loadScenarioFromXml("routing", "/sim/games/");
        } else if (itemPosition == 2) {
            simulator.loadScenarioFromXml("ringroad_1lane", "/sim/buildingBlocks/");
        } else if (itemPosition == 3) {
            simulator.loadScenarioFromXml("cloverleaf", "/sim/buildingBlocks/");
        } else {
            simulator.loadScenarioFromXml("offramp", "/sim/buildingBlocks/");
        }
        simulationRunnable.pause();
        menu.getItem(0).setIcon(R.drawable.ic_action_start).setTitle("Start");
        movSimView.resetGraphicproperties();
        movSimView.forceRepaintBackground();
        return true;
    }

    // this is called on rotation instead in onCreate. TODO Does not work with ICS anymore
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void simulationComplete(double arg0) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), "Simulation finished", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onPause() {
        simulationRunnable.pause();
        menu.getItem(0).setIcon(R.drawable.ic_action_start);
        menu.getItem(0).setTitle("Start");
        super.onPause();
    }

}