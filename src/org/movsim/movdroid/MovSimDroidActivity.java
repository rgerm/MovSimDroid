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
import org.movsim.simulator.roadnetwork.RoadMapping;
import org.movsim.simulator.roadnetwork.RoadNetwork;
import org.movsim.simulator.roadnetwork.RoadSegment;
import org.movsim.simulator.roadnetwork.TrafficLight;
import org.movsim.simulator.roadnetwork.VariableMessageSignBase;
import org.movsim.simulator.roadnetwork.VariableMessageSignDiversion;

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
        menu.add("Action").setIcon(R.drawable.ic_action_trafficlight)
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
        } else {
            if (item.getTitle().equals("Restart")) {
                roadNetwork.clear();
                simulator.initialize();
                reset();
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
            } else if (item.getTitle().equals("Action")) {

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
                        // final RoadMapping roadMapping = roadSegment.roadMapping();
                        for (final TrafficLight trafficLight : roadSegment.trafficLights()) {
                            // final Rectangle2D trafficLightRect = TrafficCanvas.trafficLightRect(roadMapping, trafficLight);
                            // // check if the user has clicked on a traffic light, if they have then change the
                            // // traffic light to the next color
                            // final Point point = e.getPoint();
                            // final Point2D transformedPoint = new Point2D.Float();
                            // final GeneralPath path = new GeneralPath();
                            // try {
                            // // convert from mouse coordinates to canvas coordinates
                            // trafficCanvas.transform.inverseTransform(new Point2D.Float(point.x, point.y), transformedPoint);
                            // } catch (final NoninvertibleTransformException e1) {
                            // e1.printStackTrace();
                            // return;
                            // }
                            // if (trafficLightRect.contains(transformedPoint)) {
                            trafficLight.nextState();
                            movSimView.forceRepaintBackground();
                            // }
                        }
                    }
                }

            }
        }
        return true;
    }

    private void reset() {
        diversionOn = false;
        menu.getItem(0).setIcon(R.drawable.ic_action_start).setTitle("Start");
    }

    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        // project selection
        if (itemPosition == 1) {
            simulator.loadScenarioFromXml("ramp_metering", "/sim/games/");
        } else if (itemPosition == 2) {
            simulator.loadScenarioFromXml("ringroad_1lane", "/sim/buildingBlocks/");
        } else if (itemPosition == 3) {
            simulator.loadScenarioFromXml("cloverleaf", "/sim/buildingBlocks/");
        } else if (itemPosition == 4) {
            simulator.loadScenarioFromXml("offramp", "/sim/buildingBlocks/");
        } else {
            simulator.loadScenarioFromXml("routing", "/sim/games/");
        }
        simulationRunnable.pause();
        menu.getItem(0).setIcon(R.drawable.ic_action_start).setTitle("Start");
        movSimView.resetGraphicproperties();
        movSimView.forceRepaintBackground();
        return true;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void simulationComplete(final double simulationTime) {
        final double totalVehicleTravelTime = roadNetwork.totalVehicleTravelTime();
        final double totalVehicleTravelDistance = roadNetwork.totalVehicleTravelDistance() / 1000.0;
        final double totalVehicleFuelUsedLiters = roadNetwork.totalVehicleFuelUsedLiters();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final StringBuilder message = new StringBuilder("Simulation finished in ");
                message.append(FormatUtil.getFormatedTime(simulationTime));
                message.append("\ntotal travel time: ").append(FormatUtil.getFormatedTime(totalVehicleTravelTime));
                message.append("\ntotal travel distance [km]: ").append(totalVehicleTravelDistance);
                message.append("\ntotal fuel used [l]: ").append(totalVehicleFuelUsedLiters);
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
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
        menu.getItem(0).setIcon(R.drawable.ic_action_start).setTitle("Start");
        super.onPause();
    }

}