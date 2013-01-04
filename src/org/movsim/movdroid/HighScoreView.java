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
import java.util.TreeSet;

import org.movsim.movdroid.util.HighscoreEntry;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;

public class HighScoreView extends SherlockActivity {
    
    /**
     * This static class holds the Views in the rows of the ListView, so that
     * they don't have to be created every time for every row
     */
    static class Viewholder {
        TextView textTop;
        TextView textBottom;
        TextView textTopValue;
        TextView textBottomValue;
    }
    
    private ListView highscoreListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.highscorelist);

        // Resources res = getResources();
        Bundle bundle = this.getIntent().getExtras();
        String message = bundle.getString("message");
        String highscore = bundle.getString("highscore");
        TreeSet<HighscoreEntry> sortedResults = (TreeSet<HighscoreEntry>) bundle.get("highscorelist");
        
        ((TextView) findViewById(R.id.text)).setText(message);
        if (highscore != null) {
            ((TextView) findViewById(R.id.highscore)).setText(highscore);
        }
        
        highscoreListView = (ListView) findViewById(R.id.highscoreListView);
        ResultsAdapter resultsAdapter = new ResultsAdapter(this, R.layout.row, (List<HighscoreEntry>) sortedResults);
        highscoreListView.setAdapter(resultsAdapter);
    }
    
    private class ResultsAdapter extends ArrayAdapter<HighscoreEntry> {

        private Object items;

        public ResultsAdapter(Context context, int textViewResourceId, List<HighscoreEntry> items) {
            super(context, textViewResourceId, items);
            this.items = items;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // TODO Auto-generated method stub
            return super.getView(position, convertView, parent);
        }
        
        
    }
}
