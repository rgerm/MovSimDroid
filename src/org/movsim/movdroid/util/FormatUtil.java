/*
 * Copyright (C) 2012 by Ralph Germ, Martin Budden, Arne Kesting, Martin Treiber
 *                       <ralph.germ@gmail.com>
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
package org.movsim.movdroid.util;

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
