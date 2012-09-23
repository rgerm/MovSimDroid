package org.movsim.movdroid;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class OnFirstBoot {
    private static final String PREFERENCE_ACCEPTED = "start.accepted";
    private static final String PREFERENCES = "start";

    static interface OnAgreedTo {
        void onAgreedTo();
    }

    public static boolean show(final Activity activity) {
        final SharedPreferences preferences = activity.getSharedPreferences(PREFERENCES, Activity.MODE_PRIVATE);

        if (!preferences.getBoolean(PREFERENCE_ACCEPTED, false)) {
             alertDialog(activity, preferences);
            return false;
        }
        return true;
    }


    public static SharedPreferences.Editor getPrefs(final Activity activity) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(activity);
        SharedPreferences.Editor editor = settings.edit();
        return editor;
    }
    
    public static void alertDialog(final Activity activity, final SharedPreferences preferences) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.onFirstBoot_title);
        builder.setCancelable(true);
        builder.setPositiveButton(R.string.onFirstBoot_accept, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                accept(preferences);
                if (activity instanceof OnAgreedTo) {
                    ((OnAgreedTo) activity).onAgreedTo();
                }
            }
        });
        builder.setMessage(R.string.introduction_text);
        builder.create().show();
    }

    private static void accept(SharedPreferences preferences) {
        preferences.edit().putBoolean(PREFERENCE_ACCEPTED, true).commit();
    }

}
