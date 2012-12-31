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
package org.movsim.movdroid.util;

import java.util.Arrays;
import java.util.Locale;

public final class HighscoreEntry {
    private static final String CSV_SEPARATOR = ";";
    
    public enum Quantity {
        totalSimulationTime("Time (s)"), totalTravelTime("Total Traveltime (s)"), totalTravelDistance(
                "Total Distance (km)"), totalFuelUsedLiters("Fuel (liters)");

        final String label;

        private Quantity(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private double[] quantities = new double[Quantity.values().length];

    private String playerName = "";

    public HighscoreEntry() {
    }

    public HighscoreEntry(String line) {
        String[] entries = line.split(CSV_SEPARATOR);
        for (Quantity quantity : Quantity.values()) {
            quantities[quantity.ordinal()] = Double.parseDouble(entries[quantity.ordinal()]);
        }
        if (entries.length == Quantity.values().length + 1) {
            playerName = entries[entries.length - 1];
        }
    }

    public double getQuantity(Quantity quantity) {
        return quantities[quantity.ordinal()];
    }

    public void setQuantity(Quantity quantity, double value) {
        quantities[quantity.ordinal()] = value;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String username) {
        playerName = username;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (double d : quantities) {
            sb.append(String.format(Locale.US, "%.6f", d)).append(CSV_SEPARATOR);
        }
        sb.append(playerName).append(CSV_SEPARATOR);
        return sb.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((playerName == null) ? 0 : playerName.hashCode());
        result = prime * result + Arrays.hashCode(quantities);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        HighscoreEntry other = (HighscoreEntry) obj;
        if (playerName == null) {
            if (other.playerName != null)
                return false;
        } else if (!playerName.equals(other.playerName))
            return false;
        if (!Arrays.equals(quantities, other.quantities))
            return false;
        return true;
    }

}
