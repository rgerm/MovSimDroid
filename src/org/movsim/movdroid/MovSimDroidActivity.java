/*
 * Copyright (C) 2012 by Ralph Germ, Martin Budden, Arne Kesting, Martin Treiber
 *                       <ralph.germ@gmail.com>
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

import org.apache.log4j.Level;
import org.movsim.input.ProjectMetaData;
import org.movsim.movdroid.graphics.MovSimView;
import org.movsim.movdroid.util.FormatUtil;
import org.movsim.movdroid.util.HighscoreEntry;
import org.movsim.movdroid.util.OnFirstBoot;
import org.movsim.movdroid.util.ViewProperties;
import org.movsim.simulator.SimulationRun;
import org.movsim.simulator.SimulationRunnable;
import org.movsim.simulator.Simulator;
import org.movsim.simulator.roadnetwork.RoadNetwork;
import org.movsim.simulator.roadnetwork.RoadSegment;
import org.movsim.simulator.roadnetwork.TrafficLight;
import org.movsim.simulator.roadnetwork.VariableMessageSignBase;
import org.movsim.simulator.roadnetwork.VariableMessageSignDiversion;
import org.movsim.utilities.Units;

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

    protected static final int MAX_RANK_FOR_HIGHSCORE = 50;

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
    private String projectPath;

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

        projectMetaData.setParseFromInputstream(true);
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
            if (projectName.equals("routing")) {
                if (roadNetwork.hasVariableMessageSign() && roadSegment.userId().equals("1")) {
                    if (diversionOn == false) {
                        diversionOn = true;
                        roadSegment.addVariableMessageSign(variableMessageSign);
                    } else {
                        diversionOn = false;
                        roadSegment.removeVariableMessageSign(variableMessageSign);
                    }
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
        String infoText = res.getString(R.string.introduction_text);
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
        createInputStreams(projectName, projectPath);
        roadNetwork.clear();
        simulator.initialize();
        simulationRunnable.start();
        simulationRunnable.pause();
        movSimView.forceRepaintBackground();
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
        projectPath = res.getStringArray(R.array.projectPath)[itemPosition];
        createInputStreams(projectName, projectPath);
        simulator.loadScenarioFromXml(projectName, projectPath);
        simulationRunnable.start();
        simulationRunnable.pause();
        if (projectName.equals("cloverleaf")) {
            roadNetwork.setHasVariableMessageSign(true);
        }
        movSimView.resetGraphicproperties();
        movSimView.forceRepaintBackground();
        if (menu != null) {
            menu.getItem(0).setIcon(R.drawable.ic_action_start).setTitle(R.string.start);
        }
        return true;
    }

    private void createInputStreams(String name, String path) {
        try {
            String full = path + name;
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

                StringBuilder gamePerformanceMessage = new StringBuilder("");

                if (isGame()) {
                    if (projectName.equals("routing")) {
                        if (simulationTime < 260) {
                            gamePerformanceMessage.append(res.getStringArray(R.array.highscoreRouting)[0]);
                        } else if (simulationTime < 285) {
                            gamePerformanceMessage.append(res.getStringArray(R.array.highscoreRouting)[1]);
                        } else if (simulationTime < 315) {
                            gamePerformanceMessage.append(res.getStringArray(R.array.highscoreRouting)[2]);
                        } else if (simulationTime < 360) {
                            gamePerformanceMessage.append(res.getStringArray(R.array.highscoreRouting)[3]);
                        } else {
                            gamePerformanceMessage.append(res.getStringArray(R.array.highscoreRouting)[4]);
                        }
                    } else if (projectName.equals("ramp_metering")) {
                        if (simulationTime < 280) {
                            gamePerformanceMessage.append(res.getStringArray(R.array.highscoreRampMetring)[0]);
                        } else if (simulationTime < 290) {
                            gamePerformanceMessage.append(res.getStringArray(R.array.highscoreRampMetring)[1]);
                        } else if (simulationTime < 300) {
                            gamePerformanceMessage.append(res.getStringArray(R.array.highscoreRampMetring)[2]);
                        } else if (simulationTime < 310) {
                            gamePerformanceMessage.append(res.getStringArray(R.array.highscoreRampMetring)[3]);
                        } else {
                            gamePerformanceMessage.append(res.getStringArray(R.array.highscoreRampMetring)[4]);
                        }
                    }

                    HighscoreEntry highscoreEntry = new HighscoreEntry();
                    highscoreEntry.setQuantity(HighscoreEntry.Quantity.totalSimulationTime, simulationTime);
                    highscoreEntry.setQuantity(HighscoreEntry.Quantity.totalTravelTime,
                            roadNetwork.totalVehicleTravelTime());
                    highscoreEntry.setQuantity(HighscoreEntry.Quantity.totalTravelDistance,
                            roadNetwork.totalVehicleTravelDistance() * Units.M_TO_KM);
                    highscoreEntry.setQuantity(HighscoreEntry.Quantity.totalFuelUsedLiters,
                            roadNetwork.totalVehicleFuelUsedLiters());

//                    highscoreForGames(highscoreEntry);
                }

                showInfo(message.toString(), gamePerformanceMessage.toString());
            }

            private void highscoreForGames(final HighscoreEntry highscoreEntry) {
                String highscoreFilename = ProjectMetaData.getInstance().getProjectName() + "_highscore.txt";
                TreeSet<HighscoreEntry> sortedResults = new TreeSet<HighscoreEntry>(new Comparator<HighscoreEntry>() {
                    @Override
                    public int compare(HighscoreEntry o1, HighscoreEntry o2) {
                        Double d1 = new Double(o1.getQuantity(HighscoreEntry.Quantity.totalSimulationTime));
                        Double d2 = new Double(o2.getQuantity(HighscoreEntry.Quantity.totalSimulationTime));
                        return d1.compareTo(d2);
                    }
                });
                sortedResults.addAll(readHighscore(highscoreFilename));

                int rank = determineRanking(highscoreEntry, sortedResults);
//                JOptionPane.showMessageDialog(null, getDialogMessage(highscoreEntry, sortedResults.size(), rank));

                if (rank <= MAX_RANK_FOR_HIGHSCORE) {
                    highscoreEntry.setPlayerName("Me");
                }

                sortedResults.add(highscoreEntry);

                writeFile(highscoreFilename, sortedResults);

                displayHighscore(sortedResults);
            }

            private int determineRanking(HighscoreEntry resultEntry, TreeSet<HighscoreEntry> sortedResults) {
                int ranking = 1;
                for (HighscoreEntry entry : sortedResults) {
                    if (sortedResults.comparator().compare(resultEntry, entry) < 0) {
                        return ranking;
                    }
                    ++ranking;

                }
                return ranking;
            }

//            private String getDialogMessage(HighscoreEntry entry, int highscoreSize, int rank) {
//                return String.format(simulationFinished,
//                        (int) highscoreEntry.getQuantity(Quantity.totalSimulationTime),
//                        (int) highscoreEntry.getQuantity(Quantity.totalTravelTime),
//                        (int) highscoreEntry.getQuantity(Quantity.totalTravelDistance),
//                        highscoreEntry.getQuantity(Quantity.totalFuelUsedLiters), highscoreSize + 1, rank);
//           }

            private void writeFile(String highscoreFilename, Iterable<HighscoreEntry> highscores) {
//                PrintWriter hswriter = FileUtils.getWriter(highscoreFilename);
//                for (HighscoreEntry entry : highscores) {
//                    hswriter.println(entry.toString());
//                }
//                hswriter.close();
            }

            private void displayHighscore(TreeSet<HighscoreEntry> results) {
                // TODO Auto-generated method stub
                for (HighscoreEntry entry: results) {
                    int row = 0;
                    if (row  > MAX_RANK_FOR_HIGHSCORE) {
                        break;
                    }
                    for (HighscoreEntry.Quantity quantity : HighscoreEntry.Quantity.values()) {
                        System.out.println(String.format("%d", row+1));
                        System.out.println(String.format("%s", entry.getPlayerName()));
                        System.out.println(String.format("%.1f", entry.getQuantity(quantity)));
                    }
                    ++row;
                }
            }

            private List<HighscoreEntry> readHighscore(String filename) {
                List<HighscoreEntry> highscore = new LinkedList<HighscoreEntry>();

                try {
                    InputStream score = getAssets().open(filename);
                    BufferedReader hsreader = new BufferedReader(new InputStreamReader(score));
                    String line;
                    while ((line = hsreader.readLine()) != null) {
                        highscore.add(new HighscoreEntry(line));
                    }
                } catch (IOException e1) {
                    return new LinkedList<HighscoreEntry>();
                }

                return highscore;
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

    public boolean isGame() {
        return Boolean.parseBoolean(ViewProperties.getApplicationProps().getProperty("isGame"));
    }

}