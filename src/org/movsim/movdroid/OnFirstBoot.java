package org.movsim.movdroid;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class OnFirstBoot {
    private static String accepted;
    private static String start;

    static interface OnAgreedTo {
        void onAgreedTo();
    }

    public static boolean show(final Activity activity, String accepted, String start, String text, String title) {
        OnFirstBoot.accepted = accepted;
        OnFirstBoot.start = start;
        final SharedPreferences preferences = activity.getSharedPreferences(start, Activity.MODE_PRIVATE);

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
