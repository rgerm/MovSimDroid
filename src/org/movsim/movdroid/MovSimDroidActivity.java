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
 */package org.movsim.movdroid;

import org.apache.log4j.Level;
import org.movsim.input.ProjectMetaData;
import org.movsim.simulator.SimulationRun;
import org.movsim.simulator.SimulationRunnable;
import org.movsim.simulator.Simulator;

import android.content.Context;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.TextView;
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
        SimulationRun.CompletionCallback, SimulationRunnable.UpdateDrawingCallback {

    static {
        final LogConfigurator logConfigurator = new LogConfigurator();

        logConfigurator.setUseFileAppender(false);
        // logConfigurator.setFileName(Environment.getExternalStorageDirectory() + "myapp.log");
        logConfigurator.setRootLevel(Level.ERROR);
        // Set log level of a specific logger
        logConfigurator.setLevel("org.apache", Level.ERROR);
        logConfigurator.configure();
    }

    private Simulator simulator;
    private TextView statusText;
    private AsyncTask<String, String, String> task;
    private TextView statusTime;
    private ProjectMetaData projectMetaData;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        System.setProperty("org.xml.sax.driver", "org.xmlpull.v1.sax2.Driver");

        initActionBar();

        setupSimulator();

        statusText = (TextView) findViewById(R.id.statusText);
        statusTime = (TextView) findViewById(R.id.statusTime);

    }

    private void setupSimulator() {
        projectMetaData = ProjectMetaData.getInstance();

        projectMetaData.setXmlFromResources(true);
        projectMetaData.setInstantaneousFileOutput(false);
        projectMetaData.setProjectName("offramp");
        projectMetaData.setPathToProjectXmlFile("/sim/buildingBlocks/");

        simulator = new Simulator(projectMetaData);

        simulator.getRoadNetwork().clear();
        simulator.initialize();
        simulator.getSimulationRunnable().setCompletionCallback(this);
        simulator.getSimulationRunnable().setUpdateDrawingCallback(this);

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

            if (!simulator.getSimulationRunnable().isPaused()) {
                simulator.getSimulationRunnable().start();
                statusText.setText("Started");

            } else {
                simulator.getSimulationRunnable().resume();
                statusText.setText("Resume Simulation");
            }

        } else if (item.getTitle().equals("Pause")) {
            item.setIcon(R.drawable.ic_action_start);
            item.setTitle("Start");
            simulator.getSimulationRunnable().pause();
            statusTime.setText("time: " + simulator.getSimulationRunnable().simulationTime());
            statusText.setText("Simulation paused");
        }
        return true;
    }

    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        // project selection
        Toast.makeText(this, "Got click: " + itemPosition, Toast.LENGTH_SHORT).show();
        if (itemPosition == 1) {
            projectMetaData.setProjectName("cloverleaf");
            projectMetaData.setPathToProjectXmlFile("/sim/buildingBlocks/");
            simulator.initialize();
        } else if (itemPosition == 2) {
            projectMetaData.setProjectName("routing");
            projectMetaData.setPathToProjectXmlFile("/sim/games/");
            simulator.initialize();
        } else if (itemPosition == 3) {
            projectMetaData.setProjectName("ringroad_1lane");
            projectMetaData.setPathToProjectXmlFile("/sim/buildingBlocks/");
            simulator.initialize();
        } else {
            projectMetaData.setProjectName("offramp");
            projectMetaData.setPathToProjectXmlFile("/sim/buildingBlocks/");
            simulator.initialize();
        }

        return true;
    }

    // this is called on rotation instead in onCreate
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void simulationComplete(double arg0) {
        System.out.println("Done.");
    }

    @Override
    public void updateDrawing(double simulatioTime) {

    }

}