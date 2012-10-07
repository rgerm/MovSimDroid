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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.movsim.input.ProjectMetaData;

public class ViewProperties {

    final static String defaultPropertyName = "/config/defaultviewerconfig.properties";
    private static Properties defaultProperties;
    private static Properties applicationProps;

    /**
     * @return the applicationProps
     */
    public static Properties getApplicationProps() {
        return applicationProps;
    }

    /**
     * Load default properties from the {code /config/defaultviewerconfig.properties} path. Needed for initialization.
     * 
     * @return the properties
     */
    public static Properties loadDefaultProperties() {
        if (defaultProperties == null) {
            defaultProperties = new Properties();
            try {
                // create and load default properties
                final InputStream is = ViewProperties.class.getResourceAsStream(defaultPropertyName);
                defaultProperties.load(is);
                is.close();
                defaultProperties = new Properties(defaultProperties);
            } catch (FileNotFoundException e) {
                // ignore exception.
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
        }
        return defaultProperties;
    }

    /**
     * Load default properties and overwrites them with project specific properties if available
     * 
     * @param projectName
     * @param path
     * @return properties
     */
    public static Properties loadProperties(String projectName, String path) {
        applicationProps = loadDefaultProperties();
        try {
            final File file = new File(path + projectName + ".properties");
            System.out.println("try to read from file=" + file.getName() + ", path=" + file.getAbsolutePath());
            if (ProjectMetaData.getInstance().isXmlFromResources()) {
                final InputStream inputStream = ViewProperties.class.getResourceAsStream(file.toString());
                if (inputStream != null) {
                    applicationProps.load(inputStream);
                    inputStream.close();
                }
            } else {
                final InputStream in = ProjectMetaData.getInstance().getProjectProperties();
                if (in == null) {
                    System.out.println("   InputStream  ist null");
                } else {
                    applicationProps.load(in);
                    in.close();
                }
                
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace(); // do not ignore
        } catch (IOException e) {
            e.printStackTrace();
        }
        return applicationProps;
    }
}
