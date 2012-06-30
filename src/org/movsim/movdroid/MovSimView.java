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
package org.movsim.movdroid;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.movsim.simulator.SimulationRunnable;
import org.movsim.simulator.SimulationRunnable.UpdateDrawingCallback;
import org.movsim.simulator.Simulator;
import org.movsim.simulator.roadnetwork.Lane;
import org.movsim.simulator.roadnetwork.RoadMapping;
import org.movsim.simulator.roadnetwork.RoadNetwork;
import org.movsim.simulator.roadnetwork.RoadSegment;
import org.movsim.simulator.roadnetwork.TrafficLight;
import org.movsim.simulator.roadnetwork.TrafficLight.TrafficLightStatus;
import org.movsim.simulator.roadnetwork.TrafficSink;
import org.movsim.simulator.roadnetwork.TrafficSource;
import org.movsim.simulator.vehicles.Vehicle;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.FloatMath;

public class MovSimView extends ViewBase implements UpdateDrawingCallback {
    

    protected StatusControlCallbacks statusControlCallbacks;

    private Simulator simulator;
    private SimulationRunnable simulationRunnable;
    private Properties properties;
    protected final RoadNetwork roadNetwork;
    
    // pre-allocate Path and Paint objects
    private final Path roadPath = new Path();
    private final Path linePath = new Path();
    private final Path vehiclePath = new Path();
    private final Paint vehiclePaint = new Paint();
    private final DashPathEffect roadLineDashPathEffect = new DashPathEffect(new float[] { 10, 20 }, 1);
    // pre-allocate clipping path for road mappings
    private final Path clipPath = new Path();

    // colors
    protected int roadColor;
    protected int roadEdgeColor;
    protected int roadLineColor;
    protected int sourceColor;
    protected int sinkColor;

    private double vmaxForColorSpectrum;

    protected boolean drawRoadId;
    protected boolean drawSources;
    protected boolean drawSinks;
    protected boolean drawSpeedLimits;
    protected boolean drawSlopes;

    protected int brakeLightColor = Color.RED;

    float lineWidth;
    float lineLength;
    float gapLength;
    float gapLengthExit;

    protected enum VehicleColorMode {
        VELOCITY_COLOR, LANE_CHANGE, ACCELERATION_COLOR, VEHICLE_COLOR, VEHICLE_LABEL_COLOR, HIGHLIGHT_VEHICLE, EXIT_COLOR
    }

    /** Color mode displayed on startup */
    protected VehicleColorMode vehicleColorMode = VehicleColorMode.VELOCITY_COLOR;

    protected VehicleColorMode vehicleColorModeSave;
    private int[] accelerationColors;
    private final double[] accelerations = new double[] { -7.5, -0.1, 0.2 };

    /** vehicle mouse-over support */
    String popupString;
    String popupStringExitEndRoad;
    protected Vehicle vehiclePopup;

    protected long lastVehicleViewed = -1;
    protected long vehicleToHighlightId = -1;
    
    /**
     * Callbacks from this TrafficCanvas to the application UI.
     * 
     */
    public interface StatusControlCallbacks {
        /**
         * Callback to get the UI to display a status message.
         * 
         * @param message
         *            the status message
         */
        public void showStatusMessage(String message);

        public void stateChanged();
    }

    public MovSimView(Context context, Simulator simulator) {
        super(context, simulator.getSimulationRunnable());
        this.simulator = simulator;
        this.roadNetwork = simulator.getRoadNetwork();
        simulationRunnable = simulator.getSimulationRunnable();
        simulationRunnable.setUpdateDrawingCallback(this);

        if (getProperties() == null) {
            setProperties(loadProperties());
        }

        initGraphicConfigFieldsFromProperties();
    }

    @Override
    public void updateDrawing(double arg0) {
        postInvalidate();
    }

    protected void initGraphicConfigFieldsFromProperties() {
        setDrawRoadId(Boolean.parseBoolean(properties.getProperty("drawRoadId", "true")));
        setDrawSinks(Boolean.parseBoolean(properties.getProperty("drawSinks", "true")));
        setDrawSources(Boolean.parseBoolean(properties.getProperty("drawSources", "true")));
        setDrawSlopes(Boolean.parseBoolean(properties.getProperty("drawSlopes", "true")));
        setDrawSpeedLimits(Boolean.parseBoolean(properties.getProperty("drawSpeedLimits", "true")));

        roadColor = Color.GRAY;
        roadEdgeColor = Color.BLACK;
        roadLineColor = Color.WHITE;
        sourceColor = Color.WHITE;
        sinkColor = Color.BLACK;

        setVmaxForColorSpectrum(Double.parseDouble(properties.getProperty("vmaxForColorSpectrum", "140")));

        lineWidth = Float.parseFloat(properties.getProperty("lineWidth", "1.0"));
        lineLength = Float.parseFloat(properties.getProperty("lineLength", "5.0"));
        gapLength = Float.parseFloat(properties.getProperty("gapLength", "15.0"));
        gapLengthExit = Float.parseFloat(properties.getProperty("gapLengthExit", "6.0"));

        scale = Float.parseFloat(properties.getProperty("initialScale", "0.707106781"));
        setSleepTime(Integer.parseInt(properties.getProperty("initial_sleep_time", "26")));
    }

    protected Properties loadProperties() {
        Properties applicationProps = null;
        try {
            // create and load default properties
            Resources resources = this.getResources();
            AssetManager assetManager = resources.getAssets();
            Properties defaultProperties = new Properties();
            final InputStream is = assetManager.open("defaultviewerconfig.properties");
            defaultProperties.load(is);
            is.close();

            // create application properties with default
            applicationProps = new Properties(defaultProperties);

            // now load specific project properties //TODO viewer properties
            // String path = ProjectMetaData.getInstance().getPathToProjectXmlFile();
            // String projectName = ProjectMetaData.getInstance().getProjectName();
            // if (ProjectMetaData.getInstance().isXmlFromResources()) {
            // final InputStream inputStream = TrafficCanvas.class.getResourceAsStream(path + projectName
            // + ".properties");
            // defaultProperties.load(inputStream);
            // inputStream.close();
            // } else {
            // InputStream in = new FileInputStream(path + projectName + ".properties");
            // applicationProps.load(in);
            // in.close();
            // }

        } catch (FileNotFoundException e) {
            // ignore exception.
        } catch (IOException e) {
            e.printStackTrace();
        }
        return applicationProps;
    }

    @Override
    protected void reset() {
        super.reset();
        simulator.reset();
    }

  

    /**
     * Returns the color of the vehicle. The color may depend on the vehicle's properties, such as its velocity.
     * 
     * @param vehicle
     * @param simulationTime
     */
    protected int vehicleColor(Vehicle vehicle, double simulationTime) {
        int color;
        final int count;

        switch (vehicleColorMode) {
        case ACCELERATION_COLOR:
            final double a = vehicle.physicalQuantities().getAcc();
            count = accelerations.length;
            for (int i = 0; i < count; ++i) {
                if (a < accelerations[i])
                    return accelerationColors[i];
            }
            return accelerationColors[accelerationColors.length - 1];
        default:
            final double v = vehicle.physicalQuantities().getSpeed() * 3.6;
            color = getColorAccordingToSpectrum(0, getVmaxForColorSpectrum(), v);
        }
        return color;
    }

    public int getColorAccordingToSpectrum(double vmin, double vmax, double v) {

        final double hue_vmin = 1.00; // hue value for minimum speed value; red
        final double hue_vmax = 1.84; // hue value for max speed (1 will be subtracted); violetblue

        float vRelative = (vmax > vmin) ? (float) ((v - vmin) / (vmax - vmin)) : 0;
        vRelative = Math.min(Math.max(0, vRelative), 1);
        final float h = (float) (hue_vmin + vRelative * (hue_vmax - hue_vmin));
        final float s = (float) 1.0;
        final float b = (float) 0.92;
        int[] rgbArray = hsv2rgb(h, s, b);

        final int rgb = Color.rgb(rgbArray[0], rgbArray[1], rgbArray[2]);
        return v > 0 ? rgb : Color.BLACK;
    }

    private int[] hsv2rgb(float h, float s, float v) {
        h = (h % 1 + 1) % 1;
        int i = (int) FloatMath.floor(h * 6);
        double    f = h * 6 - i;
        double    p = v * (1 - s);
        double    q = v * (1 - s * f);
        double    t = v * (1 - s * (1 - f));
        
        switch (i) {
         case 0:
             return new int [] {(int) (v*256), (int) (t*256), (int) (p*256)};
         case 1:
             return new int [] {(int) (q*256), (int) (v*256), (int) (p*256)};
         case 2:
             return new int [] {(int) (p*256), (int) (v*256), (int) (t*256)};
         case 3:
             return new int [] {(int) (p*256), (int) (q*256), (int) (v*256)};
         case 4:
             return new int [] {(int) (t*256), (int) (p*256), (int) (v*256)};
         case 5:
             return new int [] {(int) (v*256), (int) (p*256), (int) (q*256)};
        }
        return null;
    }

    /**
     * Callback to allow the application to make any further required drawing after the vehicles have been moved.
     */
    protected void drawAfterVehiclesMoved(Canvas canvas, double simulationTime, long iterationCount) {
    }

    /**
     * <p>
     * Draws the foreground: everything that moves each timestep. For the traffic simulation that means draw all the vehicles:<br />
     * For each roadSection, draw all the vehicles in the roadSection, positioning them using the roadMapping for that roadSection.
     * </p>
     * 
     * <p>
     * This method is synchronized with the <code>SimulationRunnable.run()</code> method, so that vehicles are not updated, added or removed
     * while they are being drawn.
     * </p>
     * <p>
     * tm The abstract method paintAfterVehiclesMoved is called after the vehicles have been moved, to allow any further required drawing on
     * the canvas.
     * </p>
     * 
     * @param g
     */
    @Override
    protected void drawForeground(Canvas g) {
        // moveVehicles occurs in the UI thread, so must synchronize with the
        // update of the road network in the calculation thread.

        final long timeBeforePaint_ms = System.currentTimeMillis();

        synchronized (simulationRunnable.dataLock) {

            drawTrafficLights(g);

            final double simulationTime = this.simulationTime();

            for (final RoadSegment roadSegment : roadNetwork) {
                final RoadMapping roadMapping = roadSegment.roadMapping();
                assert roadMapping != null;

                // DrawRoadMapping.clipPath(g, clipPath, roadMapping); //TODO uncommet
                for (final Vehicle vehicle : roadSegment) {
                    drawVehicle(g, simulationTime, roadMapping, vehicle);
                }
            }

            totalAnimationTime += System.currentTimeMillis() - timeBeforePaint_ms;

            drawAfterVehiclesMoved(g, simulationRunnable.simulationTime(), simulationRunnable.iterationCount());

        }
    }

    private void drawVehicle(Canvas canvas, double simulationTime, RoadMapping roadMapping, Vehicle vehicle) {
        // draw vehicle polygon at new position
        final RoadMapping.PolygonFloat polygon = roadMapping.mapFloat(vehicle, simulationTime);
        vehiclePath.reset();

        vehiclePath.moveTo(polygon.xPoints[0], polygon.yPoints[0]);
        vehiclePath.lineTo(polygon.xPoints[1], polygon.yPoints[1]);
        vehiclePath.lineTo(polygon.xPoints[2], polygon.yPoints[2]);
        vehiclePath.lineTo(polygon.xPoints[3], polygon.yPoints[3]);
        vehiclePath.close();

        vehiclePaint.setColor(vehicleColor(vehicle, simulationRunnable.simulationTime()));
        vehiclePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        canvas.drawPath(vehiclePath, vehiclePaint);
        if (vehicle.isBrakeLightOn()) {
            // draw the brake lights
            vehiclePath.reset();
            // points 2 & 3 are at the rear of vehicle
            vehiclePath.moveTo(polygon.xPoints[2], polygon.yPoints[2]);
            vehiclePath.lineTo(polygon.xPoints[3], polygon.yPoints[3]);
            vehiclePath.close();
            vehiclePaint.setColor(Color.RED);
            vehiclePaint.setStyle(Paint.Style.STROKE);
            canvas.drawPath(vehiclePath, vehiclePaint);
        }
    }

    /**
     * Draws the background: everything that does not move each timestep. The background consists of the road segments and the sources and
     * sinks, if they are visible.
     * 
     * @param g
     */
    @Override
    protected void drawBackground(Canvas g) {
        if (drawSources) {
            drawSources(g);
        }
        if (drawSinks) {
            drawSinks(g);
        }
        drawRoadSegments(g);

        // if (drawSpeedLimits) {
        // drawSpeedLimits(g);
        // }
        //
        // if (drawSlopes) {
        // drawSlopes(g);
        // }
        //
        // if (drawRoadId) {
        // drawRoadSectionIds(g);
        // }

    }

    /**
     * Draws each road segment in the road network.
     * 
     * @param g
     */
    private void drawRoadSegments(Canvas g) {
        for (final RoadSegment roadSegment : roadNetwork) {
            final RoadMapping roadMapping = roadSegment.roadMapping();
            // System.out.println("draw roadSegment: " + roadSegment);
            assert roadMapping != null;
            drawRoadSegment(g, roadMapping);
            drawRoadSegmentLines(g, roadMapping); // in one step (parallel or sequential update)?!
        }
    }

    private void drawRoadSegment(Canvas g, RoadMapping roadMapping) {
        // final BasicStroke roadStroke = new BasicStroke((float) roadMapping.roadWidth(), BasicStroke.CAP_BUTT,
        // BasicStroke.JOIN_MITER);
        // g.setStroke(roadStroke);
        // g.setColor(roadMapping.roadColor());
        // PaintRoadMapping.paintRoadMapping(g, roadMapping);
        assert roadMapping != null;

        paint.setStrokeWidth((float) roadMapping.roadWidth());
        paint.setColor(roadMapping.roadColor());
        roadPath.reset();

        final double lateralOffset = 0.5 * roadMapping.trafficLaneMin() * roadMapping.laneWidth();
        final Path path = DrawRoadMapping.drawRoadMapping(roadPath, roadMapping, lateralOffset);
        if (path == null) {
            // default drawing splits the road into line sections and draws those
            final double roadLength = roadMapping.roadLength();
            final double sectionLength = 20; // draw the road in sections 20 meters long
            double roadPos = 0.0;
            RoadMapping.PosTheta posTheta = roadMapping.map(roadPos);
            roadPath.moveTo((float) posTheta.x, (float) posTheta.y);
            while (roadPos < roadLength) {
                roadPos += sectionLength;
                posTheta = roadMapping.map(roadPos);
                roadPath.lineTo((float) posTheta.x, (float) posTheta.y);
            }
            g.drawPath(roadPath, paint);
            // paint.setStrokeWidth(1.0f);
            // paint.setColor(colors.roadLineColor);
            // paint.setStyle(Paint.Style.STROKE);
            // canvas.drawPath(linePath, paint);
        } else {
            g.drawPath(roadPath, paint);
        }
    }

    /**
     * Draws the road lines and road edges.
     * 
     * @param g
     */
    private void drawRoadSegmentLines(Canvas canvas, RoadMapping roadMapping) {
        // final float dashPhase = (float) (roadMapping.roadLength() % (lineLength + gapLength));
        //
        // final Stroke lineStroke = new BasicStroke(lineWidth, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f,
        // new float[] { lineLength, gapLength }, dashPhase);
        // g.setStroke(lineStroke);
        // g.setColor(roadLineColor);
        //
        // // draw the road lines
        // final int laneCount = roadMapping.laneCount();
        // for (int lane = 1; lane < laneCount; ++lane) {
        // final double offset = roadMapping.laneInsideEdgeOffset(lane);
        // if (lane == roadMapping.trafficLaneMin() || lane == roadMapping.trafficLaneMax()) {
        // // use exit stroke pattern for on-ramps, off-ramps etc
        // final Stroke exitStroke = new BasicStroke(lineWidth, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER,
        // 10.0f, new float[] { 5.0f, gapLengthExit }, 5.0f);
        // g.setStroke(exitStroke);
        // } else {
        // g.setStroke(lineStroke);
        // }
        // PaintRoadMapping.paintRoadMapping(g, roadMapping, offset);
        // }
        //
        // // draw the road edges
        // g.setStroke(new BasicStroke());
        // g.setColor(roadEdgeColor);
        // // inside edge
        // double offset = roadMapping.laneInsideEdgeOffset(0);
        // PaintRoadMapping.paintRoadMapping(g, roadMapping, offset);
        // // outside edge
        // offset = roadMapping.laneInsideEdgeOffset(laneCount);
        // PaintRoadMapping.paintRoadMapping(g, roadMapping, offset);
        //
        double offset;

        // draw the road lines
        final int laneCount = roadMapping.laneCount() - 1;

        paint.setStrokeWidth(1.0f);

        // draw the road lines
        paint.setPathEffect(roadLineDashPathEffect);
        paint.setColor(roadLineColor);
        for (int lane = Lane.LANE2; lane < laneCount; ++lane) {
            offset = roadMapping.laneInsideEdgeOffset(lane);
            linePath.reset();
            DrawRoadMapping.drawRoadMapping(linePath, roadMapping, offset);
            canvas.drawPath(linePath, paint);
        }

        // draw the road edges
        paint.setPathEffect(null);
        paint.setColor(roadEdgeColor);
        offset = roadMapping.laneInsideEdgeOffset(Lane.LANE1);
        linePath.reset();
        DrawRoadMapping.drawRoadMapping(linePath, roadMapping, offset);
        canvas.drawPath(linePath, paint);

        offset = roadMapping.laneInsideEdgeOffset(laneCount);
        linePath.reset();
        DrawRoadMapping.drawRoadMapping(linePath, roadMapping, offset);
        canvas.drawPath(linePath, paint);

    }

    private void drawTrafficLights(Canvas g) {
        for (final RoadSegment roadSegment : roadNetwork) {
            drawTrafficLightsOnRoad(g, roadSegment);
        }
    }

    private void drawTrafficLightsOnRoad(Canvas g, RoadSegment roadSegment) {
        if (roadSegment.trafficLights() == null) {
            return;
        }
        final RoadMapping roadMapping = roadSegment.roadMapping();
        assert roadMapping != null;

        final int offset = -(int) ((roadMapping.laneCount() / 2.0 + 1.5) * roadMapping.laneWidth());
        final int size = (int) (2 * roadMapping.laneWidth());
        final int radius = (int) (1.8 * roadMapping.laneWidth());
        for (final TrafficLight trafficLight : roadSegment.trafficLights()) {
            g.drawColor(Color.DKGRAY);
            paint.setColor(Color.DKGRAY);
            final RoadMapping.PosTheta posTheta = roadMapping.map(trafficLight.position(), offset);
            g.drawRect((int) posTheta.x - size / 2, (int) posTheta.y - size / 2, size, size, paint);
            final TrafficLightStatus status = trafficLight.status();
            if (status == TrafficLightStatus.GREEN) {
                g.drawColor(Color.GREEN);
            } else if (status == TrafficLightStatus.RED) {
                g.drawColor(Color.RED);
            } else if (status == TrafficLightStatus.RED_GREEN) {
                g.drawColor(Color.MAGENTA);
            } else {
                g.drawColor(Color.YELLOW);
            }
            // g.fillOval((int) posTheta.x - radius / 2, (int) posTheta.y - radius / 2, radius, radius);
        }
    }

    private void drawSpeedLimits(Canvas g) {
        for (final RoadSegment roadSegment : roadNetwork) {
            drawSpeedLimitsOnRoad(g, roadSegment);
        }
    }

    private void drawSlopes(Canvas g) {
        for (final RoadSegment roadSegment : roadNetwork) {
            drawSlopesOnRoad(g, roadSegment);
        }
    }

    private void drawSpeedLimitsOnRoad(Canvas g, RoadSegment roadSegment) {
        if (roadSegment.speedLimits() == null) {
            return;
        }

        final RoadMapping roadMapping = roadSegment.roadMapping();
        assert roadMapping != null;
        final double offset = -(roadMapping.laneCount() / 2.0 + 1.5) * roadMapping.laneWidth();
        final int redRadius2 = (int) (2.5 * roadMapping.laneWidth()) / 2;
        final int whiteRadius2 = (int) (2.0 * roadMapping.laneWidth()) / 2;
        final int fontHeight = whiteRadius2;
        final int offsetY = (int) (0.4 * fontHeight);
        //        final Font font = new Font("SansSerif", Font.BOLD, fontHeight); //$NON-NLS-1$
        // final FontMetrics fontMetrics = getFontMetrics(font);
        //
        // for (final SpeedLimit speedLimit : roadSegment.speedLimits()) {
        //
        // g.setFont(font);
        // final RoadMapping.PosTheta posTheta = roadMapping.map(speedLimit.getPosition(), offset);
        //
        // final double speedLimitValueKmh = speedLimit.getSpeedLimitKmh();
        // if (speedLimitValueKmh < 150) {
        // g.setColor(Color.RED);
        // g.fillOval((int) posTheta.x - redRadius2, (int) posTheta.y - redRadius2, 2 * redRadius2, 2 * redRadius2);
        // g.setColor(Color.WHITE);
        // g.fillOval((int) posTheta.x - whiteRadius2, (int) posTheta.y - whiteRadius2, 2 * whiteRadius2,
        // 2 * whiteRadius2);
        // g.setColor(Color.BLACK);
        // final String text = String.valueOf((int) (speedLimit.getSpeedLimitKmh()));
        // final int textWidth = fontMetrics.stringWidth(text);
        // g.drawString(text, (int) (posTheta.x - textWidth / 2.0), (int) (posTheta.y + offsetY));
        // } else {
        // // Draw a line between points (x1,y1) and (x2,y2)
        // // draw speed limit clearing
        // g.setColor(Color.BLACK);
        // g.fillOval((int) posTheta.x - redRadius2, (int) posTheta.y - redRadius2, 2 * redRadius2, 2 * redRadius2);
        // g.setColor(Color.WHITE);
        // g.fillOval((int) posTheta.x - whiteRadius2, (int) posTheta.y - whiteRadius2, 2 * whiteRadius2,
        // 2 * whiteRadius2);
        // g.setColor(Color.BLACK);
        // final int xOnCircle = (int) (whiteRadius2 * Math.cos(Math.toRadians(45.)));
        // final int yOnCircle = (int) (whiteRadius2 * Math.sin(Math.toRadians(45.)));
        // final Graphics2D g2 = g;
        // final Line2D line = new Line2D.Double((int) posTheta.x - xOnCircle, (int) posTheta.y + yOnCircle,
        // (int) posTheta.x + xOnCircle, (int) posTheta.y - yOnCircle);
        // g2.setStroke(new BasicStroke(2)); // thicker than just one pixel when calling g.drawLine
        // g2.draw(line);
        // }
        // }
    }

    private void drawSlopesOnRoad(Canvas g, RoadSegment roadSegment) {
        // if (roadSegment.slopes() == null) {
        // return;
        // }
        //
        // final RoadMapping roadMapping = roadSegment.roadMapping();
        // assert roadMapping != null;
        // final double laneWidth = 10; // ;
        // final double offset = -(roadMapping.laneCount() / 2.0 + 1.5) * (roadMapping.laneWidth() + 1);
        // final int redRadius2 = (int) (2.5 * laneWidth) / 2;
        // final int whiteRadius2 = (int) (2.0 * laneWidth) / 2;
        // final int fontHeight = whiteRadius2;
        // final int offsetY = (int) (0.4 * fontHeight);
        //        final Font font = new Font("SansSerif", Font.BOLD, fontHeight); //$NON-NLS-1$
        // final FontMetrics fontMetrics = getFontMetrics(font);
        //
        // for (final Slope slope : roadSegment.slopes()) {
        // g.setFont(font);
        // final RoadMapping.PosTheta posTheta = roadMapping.map(slope.getPosition(), offset);
        //
        // final double gradient = slope.getGradient() * 100;
        // if (gradient != 0) {
        // g.setColor(Color.BLACK);
        // final String text = String.valueOf((int) (gradient)) + " %";
        // final int textWidth = fontMetrics.stringWidth(text);
        // g.drawString(text, (int) (posTheta.x - textWidth / 2.0), (int) (posTheta.y + offsetY));
        //
        // } else {
        // // Draw a line between points (x1,y1) and (x2,y2)
        // // draw speed limit clearing
        // g.setColor(Color.BLACK);
        // g.fillOval((int) posTheta.x - redRadius2, (int) posTheta.y - redRadius2, 2 * redRadius2, 2 * redRadius2);
        // g.setColor(Color.WHITE);
        // g.fillOval((int) posTheta.x - whiteRadius2, (int) posTheta.y - whiteRadius2, 2 * whiteRadius2,
        // 2 * whiteRadius2);
        // g.setColor(Color.BLACK);
        // final int xOnCircle = (int) (whiteRadius2 * Math.cos(Math.toRadians(45.)));
        // final int yOnCircle = (int) (whiteRadius2 * Math.sin(Math.toRadians(45.)));
        // final Graphics2D g2 = g;
        // final Line2D line = new Line2D.Double((int) posTheta.x - xOnCircle, (int) posTheta.y + yOnCircle,
        // (int) posTheta.x + xOnCircle, (int) posTheta.y - yOnCircle);
        // g2.setStroke(new BasicStroke(2)); // thicker than just one pixel when calling g.drawLine
        // g2.draw(line);
        // }
        // }
    }

    /**
     * Draws the ids for the road sections, sources and sinks.
     * 
     * @param g
     */
    private void drawRoadSectionIds(Canvas g) {
        //
        // for (final RoadSegment roadSegment : roadNetwork) {
        // final RoadMapping roadMapping = roadSegment.roadMapping();
        // assert roadMapping != null;
        // final int radius = (int) ((roadMapping.laneCount() + 2) * roadMapping.laneWidth());
        // final RoadMapping.PosTheta posTheta = roadMapping.map(0.0);
        //
        // // draw the road segment's id
        // final int fontHeight = 12;
        //            final Font font = new Font("SansSerif", Font.PLAIN, fontHeight); //$NON-NLS-1$
        // g.setFont(font);
        // g.setColor(Color.BLACK);
        //            g.drawString("R" + roadSegment.userId(), (int) (posTheta.x), (int) (posTheta.y)); //$NON-NLS-1$
        // }
    }

    private void drawSources(Canvas canvas) {
        for (final RoadSegment roadSegment : roadNetwork) {
            final RoadMapping roadMapping = roadSegment.roadMapping();
            assert roadMapping != null;
            final int radius = (int) ((roadMapping.laneCount() + 2) * roadMapping.laneWidth());
            RoadMapping.PosTheta posTheta;

            // draw the road segment source, if there is one
            final TrafficSource trafficSource = roadSegment.getTrafficSource();
            if (trafficSource != null) {
                paint.setColor(Color.WHITE);
                posTheta = roadMapping.startPos();
                canvas.drawCircle((int) posTheta.x, (int) posTheta.y, radius, paint);
            }
        }
    }

    private void drawSinks(Canvas canvas) {
        for (final RoadSegment roadSegment : roadNetwork) {
            final RoadMapping roadMapping = roadSegment.roadMapping();
            assert roadMapping != null;
            final int radius = (int) ((roadMapping.laneCount() + 2) * roadMapping.laneWidth());
            RoadMapping.PosTheta posTheta;

            // draw the road segment sink, if there is one
            final TrafficSink sink = roadSegment.sink();
            if (sink != null) {
                paint.setColor(Color.BLACK);
                posTheta = roadMapping.endPos();
                canvas.drawCircle((int) posTheta.x, (int) posTheta.y, radius, paint);
            }
        }
    }
    
    public void setStatusControlCallbacks(StatusControlCallbacks statusCallbacks) {
        this.statusControlCallbacks = statusCallbacks;
    }

    /**
     * Sets the (locale dependent) message strings.
     * 
     * @param popupString
     *            popup window format string for vehicle that leaves road segment at a specific exit
     * @param popupStringExitEndRoad
     *            popup window format string for vehicle that leaves road segment at end
     */
    public void setMessageStrings(String popupString, String popupStringExitEndRoad) {
        this.popupString = popupString;
        this.popupStringExitEndRoad = popupStringExitEndRoad;
    }

    void setVelocityColors() {
        accelerationColors = new int[] { Color.WHITE, Color.RED, Color.BLACK, Color.GREEN };
    }

    public double getVmaxForColorSpectrum() {
        return vmaxForColorSpectrum;
    }

    public void setVmaxForColorSpectrum(double vmaxForColorSpectrum) {
        this.vmaxForColorSpectrum = vmaxForColorSpectrum;
    }

    public boolean isDrawRoadId() {
        return drawRoadId;
    }

    public void setDrawRoadId(boolean drawRoadId) {
        this.drawRoadId = drawRoadId;
        postInvalidate();
    }

    public boolean isDrawSources() {
        return drawSources;
    }

    public boolean isDrawSinks() {
        return drawSinks;
    }

    public boolean isDrawSpeedLimits() {
        return drawSpeedLimits;
    }

    public boolean isDrawSlopes() {
        return drawSlopes;
    }

    public void setDrawSources(boolean b) {
        this.drawSources = b;
        postInvalidate();
    }

    public void setDrawSinks(boolean b) {
        this.drawSinks = b;
        postInvalidate();
    }

    public void setDrawSpeedLimits(boolean b) {
        this.drawSpeedLimits = b;
        postInvalidate();
    }

    public void setDrawSlopes(boolean b) {
        this.drawSlopes = b;
        postInvalidate();
    }
    
    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }
}
