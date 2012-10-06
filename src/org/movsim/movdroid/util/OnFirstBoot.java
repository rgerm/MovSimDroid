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
package org.movsim.movdroid.util;

import org.movsim.movdroid.R;
import org.movsim.movdroid.R.string;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class OnFirstBoot {
    private static String accepted;

    static interface OnAgreedTo {
        void onAgreedTo();
    }

    public static boolean show(final Activity activity, String sharedPreferencesName, String accepted, String text, String title) {
        OnFirstBoot.accepted = accepted;
        final SharedPreferences preferences = activity.getSharedPreferences(sharedPreferencesName, Activity.MODE_PRIVATE);

        if (!preferences.getBoolean(accepted, false)) {
            alertDialog(activity, preferences, text, title);
            return false;
        }
        return true;
    }

    public static SharedPreferences.Editor getPrefs(final Activity activity) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(activity);
        SharedPreferences.Editor editor = settings.edit();
        return editor;
    }

    public static void alertDialog(final Activity activity, final SharedPreferences preferences, String text, String title) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(title);
        builder.setCancelable(true);
        builder.setPositiveButton(R.string.onFirstBoot_accept, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                accept(preferences);
                if (activity instanceof OnAgreedTo) {
                    ((OnAgreedTo) activity).onAgreedTo();
                }
            }
        });
        builder.setMessage(text);
        builder.create().show();
    }

    private static void accept(SharedPreferences preferences) {
        preferences.edit().putBoolean(accepted, true).commit();
    }

}
