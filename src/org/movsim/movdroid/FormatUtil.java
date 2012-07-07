package org.movsim.movdroid;

import java.util.Formatter;

public class FormatUtil {
    public static String getFormatedTime(double timeInSeconds) {
        int intTime = (int) timeInSeconds;
        final int hours = intTime / 3600;
        intTime = intTime % 3600;
        final int min = intTime / 60;
        intTime = intTime % 60;
        final StringBuilder stringBuilder = new StringBuilder();
        final Formatter formatter = new Formatter(stringBuilder);
        formatter.format("%02d:%02d:%02d", hours, min, intTime);
        return stringBuilder.toString();
    }
}
