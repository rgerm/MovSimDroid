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

import android.os.Bundle;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;

public class InfoDialog extends SherlockActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.text);

        // Resources res = getResources();
        Bundle bundle = this.getIntent().getExtras();
        String message = bundle.getString("message");
        String highscore = bundle.getString("highscore");

        ((TextView) findViewById(R.id.text)).setText(message);
        if (highscore != null) {
            ((TextView) findViewById(R.id.highscore)).setText(highscore);
        }
    }
}
