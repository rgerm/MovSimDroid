/*
 * Copyright (C) 2012 by Ralph Germ, Martin Budden, Arne Kesting, Martin Treiber
 * <ralph.germ@gmail.com>
 * -----------------------------------------------------------------------------------------
 * 
 * This file is part of
 * 
 * MovSimDroid.
 * 
 * MovSimDroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * MovSimDroid is distributed in the hope that it will be useful,
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

import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.Level;
import org.movsim.input.ProjectMetaData;
import org.movsim.movdroid.graphics.MovSimTrafficView;
import org.movsim.movdroid.util.FormatUtil;
import org.movsim.movdroid.util.OnFirstBoot;
import org.movsim.simulator.SimulationRun;
import org.movsim.simulator.SimulationRunnable;
import org.movsim.simulator.Simulator;
import org.movsim.simulator.roadnetwork.RoadNetwork;
import org.movsim.simulator.roadnetwork.VariableMessageSignBase;
import org.movsim.simulator.roadnetwork.VariableMessageSignDiversion;
import org.movsim.utilities.Units;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;

import com.actionbarsherlock.app.ActionBar.OnNavigationListener;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SubMenu;
import com.actionbarsherlock.view.Window;

import de.mindpipe.android.logging.log4j.LogConfigurator;

public class MovSimDroidActivity extends SherlockActivity implements OnNavigationListener,
        SimulationRun.CompletionCallback, SimulationRunnable.UpdateStatusCallback {

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

    private VariableMessageSignBase variableMessageSign = new VariableMessageSignDiversion();
    private MovSimTrafficView trafficView;
    private Menu menu;
    private RoadNetwork roadNetwork;
    private Resources res;
    private String projectName;
    private int projectPosition = 0;
    private String projectPath;
    private MovSimActionBar movsimActionBar;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        super.onCreate(savedInstanceState);
        res = getResources();

        // Replace parser from MovSim. -> Default values from DTD are not set. -> update xml files from MovSim before!
        System.setProperty("org.xml.sax.driver", "org.xmlpull.v1.sax2.Driver");

        setupSimulator();

        movsimActionBar = new MovSimActionBar(this, simulator);

        trafficView = new MovSimTrafficView(this, simulator, projectMetaData);

        setContentView(trafficView);
    }

    private void setupSimulator() {
        projectMetaData = ProjectMetaData.getInstance();

        projectMetaData.setParseFromInputstream(true);
        projectMetaData.setInstantaneousFileOutput(false);

        simulator = new Simulator(projectMetaData);

        simulationRunnable = simulator.getSimulationRunnable();
        simulationRunnable.setCompletionCallback(this);
        simulationRunnable.addUpdateStatusCallback(this);

        roadNetwork = simulator.getRoadNetwork();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        menu.clear();
        menu.add(res.getString(R.string.start)).setIcon(R.drawable.ic_action_start)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        menu.add(res.getString(R.string.restart)).setIcon(R.drawable.ic_action_restart)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        menu.add(res.getString(R.string.action)).setIcon(R.drawable.ic_action_trafficlight)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

        SubMenu subMenu1 = menu.addSubMenu(R.string.menu);
        subMenu1.add(res.getString(R.string.faster));
        subMenu1.add(res.getString(R.string.slower));
        subMenu1.add(res.getString(R.string.info));
        subMenu1.add(res.getString(R.string.movsimInfo));

        MenuItem subMenu1Item = subMenu1.getItem();
        subMenu1Item.setIcon(R.drawable.abs__ic_menu_moreoverflow_holo_dark);
        subMenu1Item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        movsimActionBar.selectAction(item);
        return true;
    }

    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        OnFirstBoot.show(this, "start", itemPosition + "start.accepted",
                res.getStringArray(R.array.infoScenario)[itemPosition], res.getString(R.string.onFirstBoot_title));
        // project selection
        projectPosition = itemPosition;
        projectName = res.getStringArray(R.array.projectName)[itemPosition];
        projectPath = res.getStringArray(R.array.projectPath)[itemPosition];
        createInputStreams();
        simulator.loadScenarioFromXml(projectName, projectPath);
        simulationRunnable.start();
        simulationRunnable.pause();
        if (projectName.equals("routing")) {
            roadNetwork.setHasVariableMessageSign(true);
        }
        trafficView.resetGraphicproperties();
        trafficView.forceRepaintBackground();
        if (menu != null) {
            menu.getItem(0).setIcon(R.drawable.ic_action_start).setTitle(R.string.start);
        }
        return true;
    }

    void createInputStreams() {
        try {
            String full = projectPath + projectName;
            InputStream movsimXml = getAssets().open(full + ".xml");
            projectMetaData.setMovsimXml(movsimXml);
            InputStream is = getAssets().open(full + ".xodr");
            projectMetaData.setNetworkXml(is);
            InputStream isProp = getAssets().open(full + ".properties");
            projectMetaData.setProjectProperties(isProp);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        onCreateOptionsMenu(menu);

        if (!simulationRunnable.isPaused()) {
            MenuItem item = menu.getItem(0);
            item.setIcon(R.drawable.ic_action_pause);
            item.setTitle(R.string.pause);
        }
    }

    @Override
    public void simulationComplete(final double simulationTime) {
        final double totalVehicleTravelTime = roadNetwork.totalVehicleTravelTime();
        final double totalVehicleTravelDistance = roadNetwork.totalVehicleTravelDistance() * Units.M_TO_KM;
        final double totalVehicleFuelUsedLiters = roadNetwork.totalVehicleFuelUsedLiters();
        final String formatedSimulationDuration = FormatUtil.getFormatedTime(simulationTime);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                SimulationFinised finished = new SimulationFinised(res, totalVehicleTravelTime,
                        totalVehicleTravelDistance, totalVehicleFuelUsedLiters, formatedSimulationDuration,
                        simulationTime, MovSimDroidActivity.this);
            }
        });
    }

    @Override
    public void updateStatus(double simulationTime) {
        if (simulator.isFinished()) {
            // hack to simulationComplete
            simulationRunnable.setDuration(simulationTime);
        }
    }

    @Override
    protected void onPause() {
        simulationRunnable.pause();
        menu.getItem(0).setIcon(R.drawable.ic_action_start).setTitle(R.string.start);
        super.onPause();
    }

    public void showInfo(String info, String highscore) {
        Intent intent = new Intent();
        intent.putExtra("message", info);
        intent.putExtra("highscore", highscore);
        intent.setClass(MovSimDroidActivity.this, InfoDialog.class);
        startActivity(intent);
    }

    public VariableMessageSignBase getVariableMessageSign() {
        return variableMessageSign;
    }

    public MovSimTrafficView getMovSimTrafficView() {
        return trafficView;
    }

    public Menu getMenu() {
        return menu;
    }

}