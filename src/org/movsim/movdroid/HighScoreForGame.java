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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

import org.movsim.input.ProjectMetaData;
import org.movsim.movdroid.util.HighscoreEntry;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract.Profile;

@SuppressLint("NewApi")
public class HighScoreForGame {
    private static final int MAX_RANK_FOR_HIGHSCORE = 100;
    private MovSimDroidActivity movSimDroidActivity;
    private TreeSet<HighscoreEntry> sortedResults;
    private int rank;

    public HighScoreForGame(MovSimDroidActivity movSimDroidActivity, HighscoreEntry highscoreEntry) {
        this.movSimDroidActivity = movSimDroidActivity;
        String highscoreFilename = ProjectMetaData.getInstance().getProjectName() + "_highscore.txt";
        sortedResults = new TreeSet<HighscoreEntry>(new Comparator<HighscoreEntry>() {
            @Override
            public int compare(HighscoreEntry o1, HighscoreEntry o2) {
                Double d1 = Double.valueOf(o1.getQuantity(HighscoreEntry.Quantity.totalSimulationTime));
                Double d2 = Double.valueOf(o2.getQuantity(HighscoreEntry.Quantity.totalSimulationTime));
                return d1.compareTo(d2);
            }
        });
        sortedResults.addAll(readHighscore(highscoreFilename));

        rank = determineRanking(highscoreEntry, sortedResults);
        if (rank <= MAX_RANK_FOR_HIGHSCORE) {
            Integer apiLevel = Integer.valueOf(android.os.Build.VERSION.SDK_INT);
            if (apiLevel < 14) {
                highscoreEntry.setPlayerName("Me");
            } else {
                Cursor cursor = movSimDroidActivity.getContentResolver().query(Profile.CONTENT_URI, null, null, null,
                        null);
                int count = cursor.getCount();
                String[] columnNames = cursor.getColumnNames();
                boolean b = cursor.moveToFirst();
                int position = cursor.getPosition();
                if (count == 1 && position == 0) {
                    for (int j = 0; j < columnNames.length; j++) {
                        String columnName = columnNames[j];
                        if (columnName.equals("display_name")) {
                            String columnValue = cursor.getString(cursor.getColumnIndex(columnName));
                            highscoreEntry.setPlayerName(columnValue);
                        }
                    }
                }
                cursor.close();
            }
        }

        sortedResults.add(highscoreEntry);

        writeFile(highscoreFilename, sortedResults);
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
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(movSimDroidActivity.openFileOutput(
                    highscoreFilename, Context.MODE_PRIVATE)));
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
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    movSimDroidActivity.openFileInput(filename)));
            String line;
            while ((line = reader.readLine()) != null) {
                highscore.add(new HighscoreEntry(line));
            }
        } catch (IOException e1) {
            BufferedReader reader;
            try {
                reader = new BufferedReader(new InputStreamReader(movSimDroidActivity.getAssets().open(filename)));
                String line;
                while ((line = reader.readLine()) != null) {
                    highscore.add(new HighscoreEntry(line));
                }
            } catch (IOException e) {
                e.printStackTrace();
                return new LinkedList<HighscoreEntry>();
            }
        }

        return highscore;
    }

    public int getRank() {
        return rank;
    }

    public List<HighscoreEntry> getSortedResults() {
        return (List<HighscoreEntry>) new ArrayList<HighscoreEntry>(sortedResults);
    }

}
