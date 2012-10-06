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
import org.movsim.simulator.roadnetwork.RoadNetwork;
import org.movsim.simulator.roadnetwork.RoadSegment;
import org.movsim.simulator.roadnetwork.TrafficLight;
import org.movsim.simulator.roadnetwork.VariableMessageSignBase;
import org.movsim.simulator.roadnetwork.VariableMessageSignDiversion;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.ArrayAdapter;

import com.actionbarsherlock.app.ActionBar;
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

    private MovSimView movSimView;
    private Menu menu;
    private RoadNetwork roadNetwork;
    private boolean diversionOn;
    private Resources res;
    private String projectName;
    private int projectPosition = 0;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        super.onCreate(savedInstanceState);
        res = getResources();

        // Replace parser from MovSim. -> Default values from DTD are not set. -> update xml files from MovSim before!
        System.setProperty("org.xml.sax.driver", "org.xmlpull.v1.sax2.Driver");

        initActionBar();

        setupSimulator();

        movSimView = new MovSimView(this, simulator, projectMetaData);

        setContentView(movSimView);
    }

    private void setupSimulator() {
        projectMetaData = ProjectMetaData.getInstance();

        projectMetaData.setXmlFromResources(true);
        projectMetaData.setInstantaneousFileOutput(false);

        simulator = new Simulator(projectMetaData);

        simulationRunnable = simulator.getSimulationRunnable();
        simulationRunnable.setCompletionCallback(this);
        simulationRunnable.addUpdateStatusCallback(this);

        roadNetwork = simulator.getRoadNetwork();
    }

    private void initActionBar() {
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setBackgroundDrawable(res.getDrawable(R.drawable.abs__ab_transparent_dark_holo));

        Context context = actionBar.getThemedContext();
        ArrayAdapter<CharSequence> list = ArrayAdapter.createFromResource(context, R.array.project,
                R.layout.sherlock_spinner_item);
        list.setDropDownViewResource(R.layout.sherlock_spinner_dropdown_item);

        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        actionBar.setListNavigationCallbacks(list, this);
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
        // ActionBar Buttons
        final CharSequence title = item.getTitle();
        if (title.equals(res.getString(R.string.start))) {
            actionStart(item);
        } else if (title.equals(res.getString(R.string.pause))) {
            actonPause(item);
        } else if (title.equals(res.getString(R.string.restart))) {
            actionRestart();
        } else if (title.equals(res.getString(R.string.faster))) {
            actionFaster();
        } else if (title.equals(res.getString(R.string.slower))) {
            actionSlower();
        } else if (title.equals(res.getString(R.string.info))) {
            actionScenarioInfo();
        } else if (title.equals(res.getString(R.string.movsimInfo))) {
            actionInfo();
        } else if (title.equals(res.getString(R.string.action))) {
            actionInteraction();
        }
        return true;
    }

    private void actionInteraction() {
        for (RoadSegment roadSegment : roadNetwork) {
            if (roadNetwork.hasVariableMessageSign() && roadSegment.userId().equals("1")) {
                if (diversionOn == false) {
                    diversionOn = true;
                    roadSegment.addVariableMessageSign(variableMessageSign);
                } else {
                    diversionOn = false;
                    roadSegment.removeVariableMessageSign(variableMessageSign);
                }
            }
            if (roadSegment.trafficLights() != null) {
                for (final TrafficLight trafficLight : roadSegment.trafficLights()) {
                    trafficLight.nextState();
                    movSimView.forceRepaintBackground();
                }
            }
        }
    }

    private void actionInfo() {
        String infoText = res.getString(R.string.movsimInfo);
        showInfo(infoText, "");
    }

    private void actionScenarioInfo() {
        String infoText = res.getStringArray(R.array.infoScenario)[projectPosition];
        showInfo(infoText, "");
    }

    private void showInfo(String info, String highscore) {
        Intent intent = new Intent();
        intent.putExtra("message", info);
        intent.putExtra("highscore", highscore);
        intent.setClass(MovSimDroidActivity.this, InfoDialog.class);
        startActivity(intent);
    }

    private void actionSlower() {
        int sleepTime = simulationRunnable.sleepTime();
        sleepTime += sleepTime < 5 ? 1 : 5;
        if (sleepTime > 500) {
            sleepTime = 500;
        }
        simulationRunnable.setSleepTime(sleepTime);
    }

    private void actionFaster() {
        int sleepTime = simulationRunnable.sleepTime();
        sleepTime -= sleepTime <= 5 ? 1 : 5;
        if (sleepTime < 0) {
            sleepTime = 0;
        }
        simulationRunnable.setSleepTime(sleepTime);
    }

    private void actionRestart() {
        roadNetwork.clear();
        simulator.initialize();
        reset();
    }

    private void actonPause(MenuItem item) {
        item.setIcon(R.drawable.ic_action_start);
        item.setTitle(R.string.start);
        simulationRunnable.pause();
    }

    private void actionStart(MenuItem item) {
        item.setIcon(R.drawable.ic_action_pause);
        item.setTitle(R.string.pause);
        if (!simulationRunnable.isPaused()) {
            simulationRunnable.start();
        } else {
            simulationRunnable.resume();
        }
    }

    private void reset() {
        diversionOn = false;
        menu.getItem(0).setIcon(R.drawable.ic_action_start).setTitle(R.string.start);
    }

    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        OnFirstBoot.show(this, "start", itemPosition + "start.accepted",
                res.getStringArray(R.array.infoScenario)[itemPosition], res.getString(R.string.onFirstBoot_title));
        // project selection
        projectPosition = itemPosition;
        projectName = res.getStringArray(R.array.projectName)[itemPosition];
        String projectPath = res.getStringArray(R.array.projectPath)[itemPosition];
        simulator.loadScenarioFromXml(projectName, projectPath);
        simulationRunnable.start();
        simulationRunnable.pause();
        menu.getItem(0).setIcon(R.drawable.ic_action_start).setTitle(R.string.start);
        movSimView.resetGraphicproperties();
        movSimView.forceRepaintBackground();
        return true;
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
        final double totalVehicleTravelDistance = roadNetwork.totalVehicleTravelDistance() / 1000.0;
        final double totalVehicleFuelUsedLiters = roadNetwork.totalVehicleFuelUsedLiters();
        final String formatedSimulationDuration = FormatUtil.getFormatedTime(simulationTime);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                final StringBuffer message = new StringBuffer(res.getString(R.string.simulation_finished_in))
                        .append(formatedSimulationDuration).append(res.getString(R.string.total_travel_time))
                        .append(FormatUtil.getFormatedTime(totalVehicleTravelTime))
                        .append(res.getString(R.string.total_travel_distance))
                        .append(String.format("%.3f", totalVehicleTravelDistance))
                        .append(res.getString(R.string.total_fuel_used))
                        .append(String.format("%.1f", totalVehicleFuelUsedLiters));

                // TODO highscore
                StringBuilder highscore = new StringBuilder();
                if (projectName.equals("routing")) {
                    if (simulationTime < 260) {
                        highscore.append(res.getStringArray(R.array.highscoreRouting)[0]);
                    } else if (simulationTime < 285) {
                        highscore.append(res.getStringArray(R.array.highscoreRouting)[1]);
                    } else if (simulationTime < 315) {
                        highscore.append(res.getStringArray(R.array.highscoreRouting)[2]);
                    } else if (simulationTime < 360) {
                        highscore.append(res.getStringArray(R.array.highscoreRouting)[3]);
                    } else {
                        highscore.append(res.getStringArray(R.array.highscoreRouting)[4]);
                    }
                } else if (projectName.equals("ramp_metering")) {
                    if (simulationTime < 300) {
                        highscore.append(res.getStringArray(R.array.highscoreRampMetring)[0]);
                    } else if (simulationTime < 325) {
                        highscore.append(res.getStringArray(R.array.highscoreRampMetring)[1]);
                    } else if (simulationTime < 345) {
                        highscore.append(res.getStringArray(R.array.highscoreRampMetring)[2]);
                    } else if (simulationTime < 390) {
                        highscore.append(res.getStringArray(R.array.highscoreRampMetring)[3]);
                    } else {
                        highscore.append(res.getStringArray(R.array.highscoreRampMetring)[4]);
                    }
                }

                showInfo(message.toString(), highscore.toString());
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

}