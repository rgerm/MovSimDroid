/*
 * Copyright (C) 2012, 2013 by Ralph Germ, Martin Budden, Arne Kesting, Martin Treiber
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

import java.util.List;

import org.movsim.input.ProjectMetaData;
import org.movsim.movdroid.util.FormatUtil;
import org.movsim.movdroid.util.HighscoreEntry;
import org.movsim.movdroid.util.ViewProperties;

import android.content.Intent;
import android.content.res.Resources;

public class SimulationFinished {

    private static List<HighscoreEntry> results;

    public static List<HighscoreEntry> getResults() {
        return results;
    }

    public SimulationFinished(Resources res, double totalVehicleTravelTime, double totalVehicleTravelDistance,
            double totalVehicleFuelUsedLiters, String formatedSimulationDuration, double simulationTime, MovSimDroidActivity movSimDroidActivity) {

        final StringBuffer message = new StringBuffer(res.getString(R.string.simulation_finished_in))
                .append(formatedSimulationDuration).append(res.getString(R.string.total_travel_time))
                .append(FormatUtil.getFormatedTime(totalVehicleTravelTime))
                .append(res.getString(R.string.total_travel_distance))
                .append(String.format("%.3f", totalVehicleTravelDistance))
                .append(res.getString(R.string.total_fuel_used))
                .append(String.format("%.1f", totalVehicleFuelUsedLiters));

        StringBuilder gamePerformanceMessage = new StringBuilder("");

        if (isGame()) {
            String projectName = ProjectMetaData.getInstance().getProjectName();
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
            highscoreEntry.setQuantity(HighscoreEntry.Quantity.totalTravelTime, totalVehicleTravelTime);
            highscoreEntry.setQuantity(HighscoreEntry.Quantity.totalTravelDistance, totalVehicleTravelDistance);
            highscoreEntry.setQuantity(HighscoreEntry.Quantity.totalFuelUsedLiters, totalVehicleFuelUsedLiters);

            HighScoreForGame highScoreForGame = new HighScoreForGame(movSimDroidActivity, highscoreEntry);
            results = highScoreForGame.getSortedResults();
            
            Intent intent = new Intent();
            intent.putExtra("message", message.toString());
            intent.putExtra("highscore", gamePerformanceMessage.toString());
            intent.putExtra("rank", String.valueOf(highScoreForGame.getRank()));
            intent.setClass(movSimDroidActivity, HighScoreView.class);
            movSimDroidActivity.startActivity(intent);
            
        } else {
            movSimDroidActivity.showInfo(message.toString());   
        }
    }

    private boolean isGame() {
        return Boolean.parseBoolean(ViewProperties.getApplicationProps().getProperty("isGame"));
    }

}
