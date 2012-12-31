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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

import org.movsim.input.ProjectMetaData;
import org.movsim.movdroid.util.HighscoreEntry;

import android.content.Context;

public class HighScoreForGame {
    private static final int MAX_RANK_FOR_HIGHSCORE = 50;
    private MovSimDroidActivity movSimDroidActivity;
    
    public HighScoreForGame(MovSimDroidActivity movSimDroidActivity, HighscoreEntry highscoreEntry) {
        this.movSimDroidActivity = movSimDroidActivity;
        String highscoreFilename = ProjectMetaData.getInstance().getProjectName() + "_highscore.txt";
        TreeSet<HighscoreEntry> sortedResults = new TreeSet<HighscoreEntry>(new Comparator<HighscoreEntry>() {
            @Override
            public int compare(HighscoreEntry o1, HighscoreEntry o2) {
                Double d1 = Double.valueOf(o1.getQuantity(HighscoreEntry.Quantity.totalSimulationTime));
                Double d2 = Double.valueOf(o2.getQuantity(HighscoreEntry.Quantity.totalSimulationTime));
                return d1.compareTo(d2);
            }
        });
        sortedResults.addAll(readHighscore(highscoreFilename));

        int rank = determineRanking(highscoreEntry, sortedResults);
        if (rank <= MAX_RANK_FOR_HIGHSCORE) {
            highscoreEntry.setPlayerName("Elmo");
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
    
    private void writeFile(String highscoreFilename, Iterable<HighscoreEntry> highscores) {
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(movSimDroidActivity.openFileOutput(highscoreFilename, Context.MODE_PRIVATE)));
            for (HighscoreEntry entry : highscores) {
                writer.write(entry.toString());
                writer.newLine();
            }
            writer.flush();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<HighscoreEntry> readHighscore(String filename) {
        List<HighscoreEntry> highscore = new LinkedList<HighscoreEntry>();

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(movSimDroidActivity.openFileInput(filename)));
            String line;
            while ((line = reader.readLine()) != null) {
                highscore.add(new HighscoreEntry(line));
            }
        } catch (IOException e1) {
            return new LinkedList<HighscoreEntry>();
        }

        return highscore;
    }
    
    private void displayHighscore(TreeSet<HighscoreEntry> results) {
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
}
