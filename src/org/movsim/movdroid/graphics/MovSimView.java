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
package org.movsim.movdroid.graphics;

import java.util.Properties;

import org.movsim.input.ProjectMetaData;
import org.movsim.movdroid.util.ViewProperties;
import org.movsim.simulator.SimulationRunnable;
import org.movsim.simulator.SimulationRunnable.UpdateDrawingCallback;
import org.movsim.simulator.Simulator;
import org.movsim.simulator.roadnetwork.RoadMapping;
import org.movsim.simulator.roadnetwork.RoadNetwork;
import org.movsim.simulator.roadnetwork.RoadSegment;
import org.movsim.simulator.roadnetwork.Slope;
import org.movsim.simulator.roadnetwork.SpeedLimit;
import org.movsim.simulator.roadnetwork.TrafficLight;
import org.movsim.simulator.roadnetwork.TrafficLight.TrafficLightStatus;
import org.movsim.simulator.roadnetwork.TrafficSink;
import org.movsim.simulator.roadnetwork.TrafficSource;
import org.movsim.simulator.vehicles.Vehicle;
import org.movsim.utilities.Colors;
import org.movsim.utilities.Units;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Region;
import android.util.FloatMath;
import android.view.MotionEvent;

public class MovSimView extends ViewBase implements UpdateDrawingCallback {

    protected StatusControlCallbacks statusControlCallbacks;

    private Simulator simulator;
    private SimulationRunnable simulationRunnable;
    protected final RoadNetwork roadNetwork;

    // pre-allocate Path and Paint objects
    private final Path roadPath = new Path();
    private final Path linePath = new Path();
    private final Path vehiclePath = new Path();
    private final Paint vehiclePaint = new Paint();
    private final DashPathEffect roadLineDashPathEffect = new DashPathEffect(new float[] { 10, 20 }, 1);
    private final Path clipPath = new Path();

    protected int roadColor;
    protected int roadEdgeColor;
    protected int roadLineColor;
    protected int sourceColor;
    protected int sinkColor;
    protected int brakeLightColor = Color.RED;
    private double vmaxForColorSpectrum;
    protected VehicleColorMode vehicleColorModeSave;
    private int[] accelerationColors;
    private final double[] accelerations = new double[] { -7.5, -0.1, 0.2 };

    protected enum VehicleColorMode {
        VELOCITY_COLOR, LANE_CHANGE, ACCELERATION_COLOR, VEHICLE_COLOR, VEHICLE_LABEL_COLOR, HIGHLIGHT_VEHICLE, EXIT_COLOR
    }

    /** Color mode displayed on startup */
    protected VehicleColorMode vehicleColorMode = VehicleColorMode.VELOCITY_COLOR;

    protected boolean drawRoadId;
    protected boolean drawSources;
    protected boolean drawSinks;
    protected boolean drawSpeedLimits;
    protected boolean drawSlopes;

    float lineWidth;
    float lineLength;
    float gapLength;
    float gapLengthExit;

    String popupString;
    String popupStringExitEndRoad;
    protected Vehicle vehiclePopup;

    protected long lastVehicleViewed = -1;
    protected long vehicleToHighlightId = -1;

    private ProjectMetaData projectMetaData;

    // touch event handling
    private static final int TOUCH_MODE_NONE = 0;
    private static final int TOUCH_MODE_DRAG = 1;
    private static final int TOUCH_MODE_ZOOM = 2;
    private int touchMode = TOUCH_MODE_NONE;
    private float startDragX;
    private float startDragY;
    private float xOffsetSave;
    private float yOffsetSave;
    private float scaleSave;
    // pinch zoom handling
    private static float touchModeZoomHysteresis = 10.0f;
    private float pinchDistance;

    private Region trafficLightRegion = new Region();

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

    public MovSimView(Context context, Simulator simulator, ProjectMetaData projectMetaData) {
        super(context, simulator.getSimulationRunnable());
        this.simulator = simulator;
        this.projectMetaData = projectMetaData;
        this.roadNetwork = simulator.getRoadNetwork();
        simulationRunnable = simulator.getSimulationRunnable();
        simulationRunnable.setUpdateDrawingCallback(this);
    }

    public void resetGraphicproperties() {
        Properties properties = ViewProperties.loadProperties(projectMetaData.getProjectName(),
                projectMetaData.getPathToProjectXmlFile());
        initGraphicConfigFieldsFromProperties(properties);
        scale = Float.parseFloat(properties.getProperty("initialScale"));
        xOffset = Integer.parseInt(properties.getProperty("xOffset"));
        yOffset = Integer.parseInt(properties.getProperty("yOffset"));
    }
    
    @Override
    public void updateDrawing(double arg0) {
        postInvalidate();
    }

    @Override
    protected void reset() {
        super.reset();
        simulator.reset();
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
     * @param canvas
     */
    @Override
    protected void drawForeground(Canvas canvas) {
        // moveVehicles occurs in the UI thread, so must synchronize with the
        // update of the road network in the calculation thread.

        final long timeBeforePaint_ms = System.currentTimeMillis();

        synchronized (simulationRunnable.dataLock) {

            final double simulationTime = this.simulationTime();

            for (final RoadSegment roadSegment : roadNetwork) {
                final RoadMapping roadMapping = roadSegment.roadMapping();
                assert roadMapping != null;
                if (androidVersion < 12) {
                    DrawRoadMapping.clipPath(canvas, clipPath, roadMapping); // TODO clipPath not supported on sdk>12
                }
                for (final Vehicle vehicle : roadSegment) {
                    drawVehicle(canvas, simulationTime, roadMapping, vehicle);
                }
            }

            totalAnimationTime += System.currentTimeMillis() - timeBeforePaint_ms;

            drawAfterVehiclesMoved(canvas, simulationRunnable.simulationTime(), simulationRunnable.iterationCount());

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
     * @param canvas
     */
    @Override
    protected void drawBackground(Canvas canvas) {
        drawTrafficLights(canvas);

        if (drawSources) {
            drawSources(canvas);
        }
        if (drawSinks) {
            drawSinks(canvas);
        }

        if (drawSpeedLimits) {
            drawSpeedLimits(canvas);
        }

        if (drawSlopes) {
            drawSlopes(canvas);
        }

        if (drawRoadId) {
            drawRoadSectionIds(canvas);
        }

        drawRoadSegments(canvas);
    }

    /**
     * Draws each road segment in the road network.
     * 
     * @param canvas
     */
    private void drawRoadSegments(Canvas canvas) {

        for (final RoadSegment roadSegment : roadNetwork) {
            final RoadMapping roadMapping = roadSegment.roadMapping();
            assert roadMapping != null;
            drawRoadSegment(canvas, roadMapping);
            drawRoadSegmentLines(canvas, roadMapping); // in one step (parallel or sequential update)?!
        }
    }

    private void drawRoadSegment(Canvas canvas, RoadMapping roadMapping) {

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
        }

        paint.reset();
        paint.setStrokeWidth((float) roadMapping.roadWidth());
        paint.setColor(roadColor);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawPath(roadPath, paint);
    }

    /**
     * Draws the road lines and road edges.
     * 
     * @param g
     */
    private void drawRoadSegmentLines(Canvas canvas, RoadMapping roadMapping) {

        paint.reset();
        paint.setStyle(Paint.Style.STROKE);

        double offset;
        // draw the road lines
        final int laneCount = roadMapping.laneCount();
        paint.setStrokeWidth(1.0f);
        paint.setPathEffect(roadLineDashPathEffect);
        paint.setColor(roadLineColor);
        for (int lane = 1; lane < laneCount; ++lane) {
            offset = roadMapping.laneInsideEdgeOffset(lane);
            linePath.reset();
            DrawRoadMapping.drawRoadMapping(linePath, roadMapping, offset);
            canvas.drawPath(linePath, paint);
        }
        // draw the road edges
        paint.setPathEffect(null);
        paint.setColor(roadEdgeColor);
        offset = roadMapping.laneInsideEdgeOffset(0);
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

    private void drawTrafficLightsOnRoad(Canvas canvas, RoadSegment roadSegment) {
        if (roadSegment.trafficLights() == null) {
            return;
        }
        final RoadMapping roadMapping = roadSegment.roadMapping();
        assert roadMapping != null;
        paint.reset();
        paint.setStyle(Paint.Style.FILL);
        final int offset = (int) ((roadMapping.laneCount() / 2.0 + 1.5) * roadMapping.laneWidth());
        final int size = (int) (2 * roadMapping.laneWidth());
        final int radius = (int) (1.8 * roadMapping.laneWidth());
        for (final TrafficLight trafficLight : roadSegment.trafficLights()) {
            paint.setColor(Color.DKGRAY);
            final RoadMapping.PosTheta posTheta = roadMapping.map(trafficLight.position(), offset);
            Rect rect = new Rect((int) posTheta.x + offset - radius, (int) posTheta.y + offset - radius,
                    (int) posTheta.x + offset + radius, (int) posTheta.y + offset + radius);
            // trafficLightRegion = new Region(new Rect((int) posTheta.x + offset - radius-(int)trafficLight.position(), (int) posTheta.y +
            // offset - radius, (int) posTheta.x + offset + radius-(int)trafficLight.position(), (int) posTheta.y + offset + radius));
            canvas.drawRect(rect, paint);
            final TrafficLightStatus status = trafficLight.status();
            if (status == TrafficLightStatus.GREEN) {
                paint.setColor(Color.GREEN);
            } else if (status == TrafficLightStatus.RED) {
                paint.setColor(Color.RED);
            } else if (status == TrafficLightStatus.RED_GREEN) {
                paint.setColor(Color.MAGENTA);
            } else {
                paint.setColor(Color.YELLOW);
            }
            canvas.drawCircle((int) posTheta.x + offset, (int) posTheta.y + offset, radius, paint);
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

    private void drawSpeedLimitsOnRoad(Canvas canvas, RoadSegment roadSegment) {
        if (roadSegment.speedLimits() == null) {
            return;
        }
        paint.reset();
        paint.setStyle(Paint.Style.FILL);

        final RoadMapping roadMapping = roadSegment.roadMapping();
        assert roadMapping != null;
        final double offset = -(roadMapping.laneCount() / 2.0 + 1.5) * roadMapping.laneWidth();
        final int redRadius = (int) (3 * roadMapping.laneWidth()) / 2;
        final int whiteRadius = (int) (2 * roadMapping.laneWidth()) / 2;
        final int offsetY = -40;
        final int xOffset = -14;

        for (final SpeedLimit speedLimit : roadSegment.speedLimits()) {

            final RoadMapping.PosTheta posTheta = roadMapping.map(speedLimit.getPosition(), offset);

            final double speedLimitValueKmh = speedLimit.getSpeedLimitKmh();
            if (speedLimitValueKmh < 150) {
                paint.setColor(0xffee1111);
                canvas.drawCircle((int) posTheta.x + xOffset, (int) posTheta.y + redRadius - offsetY, redRadius,
                        paint);
                paint.setColor(0xffeeeeee);
                canvas.drawCircle((int) posTheta.x + xOffset, (int) posTheta.y + redRadius - offsetY, whiteRadius,
                        paint);

                final String text = String.valueOf((int) (speedLimit.getSpeedLimitKmh()));
                paint.setColor(Color.BLACK);
                paint.setAntiAlias(true);
                paint.setTextSize(14);
                canvas.drawText(text, (int) (posTheta.x + xOffset - 7),
                        (int) (posTheta.y + redRadius + 4 - offsetY), paint);
            } else {
                // TODO clearing sign
            }
        }
    }

    private void drawSlopesOnRoad(Canvas canvas, RoadSegment roadSegment) {
        if (roadSegment.slopes() == null) {
            return;
        }
        final RoadMapping roadMapping = roadSegment.roadMapping();
        final double offset = -(roadMapping.laneCount() / 2.0 + 1.5) * (roadMapping.laneWidth() + 1);
        for (final Slope slope : roadSegment.slopes()) {
            final RoadMapping.PosTheta posTheta = roadMapping.map(slope.getPosition(), offset);
            final double gradient = slope.getGradient() * 100;
            if (gradient != 0) {
                paint.setColor(Color.BLACK);
                final String text = String.valueOf((int) (gradient)) + " %";
                canvas.drawText(text, (int) (posTheta.x - 20), (int) (posTheta.y + 20), paint);
            }
        }
    }

    private void drawRoadSectionIds(Canvas canvas) {
        for (final RoadSegment roadSegment : roadNetwork) {
            final RoadMapping roadMapping = roadSegment.roadMapping();
            assert roadMapping != null;
            final int radius = (int) ((roadMapping.laneCount() + 2) * roadMapping.laneWidth());
            final RoadMapping.PosTheta posTheta = roadMapping.map(0.0);

            // draw the road segment's id
            paint.setColor(Color.BLACK);
            paint.setAntiAlias(true);
            paint.setTextSize(12);
            canvas.drawText("Info: " + roadSegment.userId(), (int) (posTheta.x + 16), (int) (posTheta.y + 16), paint);
        }
    }

    private void drawSources(Canvas canvas) {
        paint.reset();
        paint.setStyle(Paint.Style.FILL);
        for (final RoadSegment roadSegment : roadNetwork) {
            final RoadMapping roadMapping = roadSegment.roadMapping();
            assert roadMapping != null;
            final int radius = (int) ((roadMapping.laneCount() + 2) * roadMapping.laneWidth());
            RoadMapping.PosTheta posTheta;

            final TrafficSource trafficSource = roadSegment.getTrafficSource();
            if (trafficSource != null) {
                paint.setColor(Color.WHITE);
                posTheta = roadMapping.startPos();
                canvas.drawCircle((int) posTheta.x, (int) posTheta.y, radius, paint);

                // inflow text
                paint.setColor(Color.BLACK);
                paint.setAntiAlias(true);
                paint.setTextSize(20);
                StringBuilder inflowStringBuilder = new StringBuilder();
                inflowStringBuilder.append("set/target inflow: ");
                inflowStringBuilder.append((int) (Units.INVS_TO_INVH * trafficSource
                        .getTotalInflow(simulationTime())));
                inflowStringBuilder.append("/");
                inflowStringBuilder.append((int) (Units.INVS_TO_INVH * trafficSource.measuredInflow()));
                inflowStringBuilder.append(" veh/h");
                inflowStringBuilder.append(" (");
                inflowStringBuilder.append(trafficSource.getQueueLength());
                inflowStringBuilder.append(")");
                canvas.drawText(inflowStringBuilder.toString(), (int) (posTheta.x) + radius, (int) (posTheta.y)
                        + radius, paint);
            }
        }
    }

    private void drawSinks(Canvas canvas) {
        paint.reset();
        paint.setStyle(Paint.Style.FILL);
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
                String outflowString = "outflow: " + (int) (Units.INVS_TO_INVH * sink.measuredOutflow())
                        + " veh/h";
                // outflow text
                paint.setAntiAlias(true);
                paint.setTextSize(20);
                canvas.drawText(outflowString, (int) (posTheta.x) + radius, (int) (posTheta.y) + radius, paint);
            }
        }
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
        case VELOCITY_COLOR:
            final double v = vehicle.physicalQuantities().getSpeed() * 3.6;
            color = getColorAccordingToSpectrum(0, getVmaxForColorSpectrum(), v);
            break;
        case ACCELERATION_COLOR:
            final double a = vehicle.physicalQuantities().getAcc();
            count = accelerations.length;
            for (int i = 0; i < count; ++i) {
                if (a < accelerations[i])
                    return accelerationColors[i];
            }
            return accelerationColors[accelerationColors.length - 1];
        case EXIT_COLOR:
            color = vehicle.color();
            if (color == 0) {
                color = Colors.randomColor();
                vehicle.setColor(color);
            }
            if (vehicle.exitRoadSegmentId() != Vehicle.ROAD_SEGMENT_ID_NOT_SET) {
                color = Color.WHITE;
            }
            break;
        case VEHICLE_COLOR:
            color = vehicle.color();
            if (color == 0) {
                color = Colors.randomColor();
                vehicle.setColor(color);
            }
            break;
        default:
            color = Color.BLACK;
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
        double f = h * 6 - i;
        double p = v * (1 - s);
        double q = v * (1 - s * f);
        double t = v * (1 - s * (1 - f));

        switch (i) {
        case 0:
            return new int[] { (int) (v * 256), (int) (t * 256), (int) (p * 256) };
        case 1:
            return new int[] { (int) (q * 256), (int) (v * 256), (int) (p * 256) };
        case 2:
            return new int[] { (int) (p * 256), (int) (v * 256), (int) (t * 256) };
        case 3:
            return new int[] { (int) (p * 256), (int) (q * 256), (int) (v * 256) };
        case 4:
            return new int[] { (int) (t * 256), (int) (p * 256), (int) (v * 256) };
        case 5:
            return new int[] { (int) (v * 256), (int) (p * 256), (int) (q * 256) };
        }
        return null;
    }

    protected void initGraphicConfigFieldsFromProperties(Properties properties) {
        setDrawRoadId(Boolean.parseBoolean(properties.getProperty("drawRoadId", "true")));
        setDrawSinks(Boolean.parseBoolean(properties.getProperty("drawSinks", "true")));
        setDrawSources(Boolean.parseBoolean(properties.getProperty("drawSources", "true")));
        setDrawSlopes(Boolean.parseBoolean(properties.getProperty("drawSlopes", "true")));
        setDrawSpeedLimits(Boolean.parseBoolean(properties.getProperty("drawSpeedLimits", "true")));

        backgroundColor = Color.parseColor("#" + properties.getProperty("backgroundColor", "303030"));
        roadColor = Color.parseColor("#" + properties.getProperty("roadColor", "808080"));
        roadEdgeColor = Color.parseColor("#" + properties.getProperty("roadEdgeColor", "222222"));
        roadLineColor = Color.parseColor("#" + properties.getProperty("roadLineColor", "DDDDDD"));
        sourceColor = Color.parseColor("#" + properties.getProperty("sourceColor", "FFFFFF"));
        sinkColor = Color.parseColor("#" + properties.getProperty("sinkColor", "000000"));
        setVehicleColorMode(vehicleColorMode.valueOf(properties.getProperty("vehicleColorMode", "VELOCITY_COLOR")));
        setVmaxForColorSpectrum(Double.parseDouble(properties.getProperty("vmaxForColorSpectrum", "140")));

        lineWidth = Float.parseFloat(properties.getProperty("lineWidth", "1.0"));
        lineLength = Float.parseFloat(properties.getProperty("lineLength", "5.0"));
        gapLength = Float.parseFloat(properties.getProperty("gapLength", "15.0"));
        gapLengthExit = Float.parseFloat(properties.getProperty("gapLengthExit", "6.0"));

        scale = Float.parseFloat(properties.getProperty("initialScale", "0.707106781"));
        setSleepTime(Integer.parseInt(properties.getProperty("initial_sleep_time", "20")));
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
    
    public void setVehicleColorMode(VehicleColorMode vehicleColorMode) {
        this.vehicleColorMode = vehicleColorMode;
    }

    // ============================================================================================
    // Motion event handling
    // ============================================================================================

    /**
     * <p>
     * Touch events are used to drag and resize the view.
     * </p>
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float dx;
        float dy;
        final int ACTION_MASK = MotionEvent.ACTION_MASK;
        final int ACTION_POINTER_UP = MotionEvent.ACTION_POINTER_UP;
        switch (event.getAction() & ACTION_MASK) {
        case MotionEvent.ACTION_DOWN:
            touchMode = TOUCH_MODE_DRAG;
            startDragX = event.getX();
            startDragY = event.getY();
            xOffsetSave = xOffset;
            yOffsetSave = yOffset;
            break;
        case MotionEvent.ACTION_UP:
        case ACTION_POINTER_UP:
            touchMode = TOUCH_MODE_NONE;
            break;
        case MotionEvent.ACTION_POINTER_DOWN:
            dx = event.getX(0) - event.getX(1);
            dy = event.getY(0) - event.getY(1);
            pinchDistance = (float) Math.sqrt(dx * dx + dy * dy);
            if (pinchDistance > touchModeZoomHysteresis) {
                touchMode = TOUCH_MODE_ZOOM;
                scaleSave = scale();
            }
            break;
        case MotionEvent.ACTION_MOVE:
            if (touchMode == TOUCH_MODE_DRAG) {
                final float xOffsetNew = xOffsetSave + (event.getX() - startDragX) / scale;
                final float yOffsetNew = yOffsetSave + (event.getY() - startDragY) / scale;
                if (xOffsetNew != xOffset || yOffsetNew != yOffset) {
                    // the user has dragged the view, so we need to redraw the background bitmap
                    xOffset = xOffsetNew;
                    yOffset = yOffsetNew;
                    setTransform();
                    forceRepaintBackground();
                }
            } else if (touchMode == TOUCH_MODE_ZOOM) {
                dx = event.getX(0) - event.getX(1);
                dy = event.getY(0) - event.getY(1);
                final float distance = FloatMath.sqrt(dx * dx + dy * dy);
                if (pinchDistance > touchModeZoomHysteresis) {
                    final float newScale = distance / pinchDistance * scaleSave;
                    setScale(newScale);
                    // the user has zoomed the view, so we need to redraw the background bitmap
                    forceRepaintBackground();
                }
            }
            break;
        }
        return true;
    }

    /**
     * <p>
     * Standard onTrackballEvent override, just ignore these events.
     * </p>
     */
    @Override
    public boolean onTrackballEvent(MotionEvent event) {
        return false;
    }
}
