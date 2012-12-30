package org.movsim.movdroid;

import org.movsim.input.ProjectMetaData;
import org.movsim.simulator.SimulationRunnable;
import org.movsim.simulator.Simulator;
import org.movsim.simulator.roadnetwork.RoadNetwork;
import org.movsim.simulator.roadnetwork.RoadSegment;
import org.movsim.simulator.roadnetwork.TrafficLight;
import org.movsim.simulator.roadnetwork.VariableMessageSignBase;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.widget.ArrayAdapter;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.MenuItem;

public class MovSimActionBar {
	private Resources res;
	private ActionBar actionBar;
	private Simulator simulator;
	private RoadNetwork roadNetwork;
	private SimulationRunnable simulationRunnable;
	private ProjectMetaData projectMetaData;
	private String projectName;
	private boolean diversionOn = false;
	private MovSimDroidActivity movSimDroidActivity;

	public MovSimActionBar(MovSimDroidActivity movSimDroidActivity,
			Simulator simulator) {
		this.movSimDroidActivity = movSimDroidActivity;
		this.simulator = simulator;
		res = movSimDroidActivity.getResources();
		roadNetwork = simulator.getRoadNetwork();
		simulationRunnable = simulator.getSimulationRunnable();
		projectMetaData = ProjectMetaData.getInstance();
		projectName = projectMetaData.getProjectName();

		initActiomBar(movSimDroidActivity);
	}

	private void initActiomBar(MovSimDroidActivity movSimDroidActivity) {
		actionBar = movSimDroidActivity.getSupportActionBar();
		actionBar.setBackgroundDrawable(res
				.getDrawable(R.drawable.abs__ab_transparent_dark_holo));

		Context context = actionBar.getThemedContext();
		ArrayAdapter<CharSequence> list = ArrayAdapter.createFromResource(
				context, R.array.project, R.layout.sherlock_spinner_item);
		list.setDropDownViewResource(R.layout.sherlock_spinner_dropdown_item);

		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		actionBar.setListNavigationCallbacks(list, movSimDroidActivity);
	}
	
	public void selectAction(MenuItem item) {
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
			actionScenarioInfo(item.getItemId());
		} else if (title.equals(res.getString(R.string.movsimInfo))) {
			actionInfo();
		} else if (title.equals(res.getString(R.string.action))) {
			actionInteraction();
		}
	}

	private void actionInteraction() {
		for (RoadSegment roadSegment : roadNetwork) {
			if (projectName.equals("routing")) {
				if (roadNetwork.hasVariableMessageSign()
						&& roadSegment.userId().equals("1")) {
					VariableMessageSignBase variableMessageSign = movSimDroidActivity
							.getVariableMessageSign();
					if (diversionOn == false) {
						diversionOn = true;
						roadSegment.addVariableMessageSign(variableMessageSign);
					} else {
						diversionOn = false;
						roadSegment
								.removeVariableMessageSign(variableMessageSign);
					}
				}
			}
			if (roadSegment.trafficLights() != null) {
				for (final TrafficLight trafficLight : roadSegment
						.trafficLights()) {
					trafficLight.nextState();
					movSimDroidActivity.getMovSimView()
							.forceRepaintBackground();
				}
			}
		}
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
		movSimDroidActivity.createInputStreams();
		roadNetwork.clear();
		simulator.initialize();
		simulationRunnable.start();
		simulationRunnable.pause();
		movSimDroidActivity.getMovSimView().forceRepaintBackground();
		reset();
	}
	

    private void reset() {
        diversionOn = false;
        movSimDroidActivity.getMenu().getItem(0).setIcon(R.drawable.ic_action_start).setTitle(R.string.start);
    }

	private void actionInfo() {
		String infoText = res.getString(R.string.introduction_text);
		showInfo(infoText, "");
	}

	private void actionScenarioInfo(int projectPosition) {
		String infoText = res.getStringArray(R.array.infoScenario)[projectPosition];
		showInfo(infoText, "");
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

	public void showInfo(String info, String highscore) {
		Intent intent = new Intent();
		intent.putExtra("message", info);
		intent.putExtra("highscore", highscore);
		intent.setClass(movSimDroidActivity, InfoDialog.class);
		movSimDroidActivity.startActivity(intent);
	}

}
