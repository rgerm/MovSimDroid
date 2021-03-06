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

import java.util.Iterator;

import org.movsim.roadmappings.RoadMappingArc;
import org.movsim.roadmappings.RoadMappingBezier;
import org.movsim.roadmappings.RoadMappingCircle;
import org.movsim.roadmappings.RoadMappingLine;
import org.movsim.roadmappings.RoadMappingPolyBezier;
import org.movsim.roadmappings.RoadMappingPolyLine;
import org.movsim.roadmappings.RoadMappingU;
import org.movsim.simulator.roadnetwork.RoadMapping;

import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Region;

/**
 * Optimized drawing of RoadSegments based on the type of their RoadMapping
 * 
 */
public class DrawRoadMapping {
    static public Path drawRoadMapping(Path roadPath, RoadMapping roadMapping, double lateralOffset) {

        assert roadMapping != null;

        RoadMapping.PosTheta posTheta;
        final double roadLength = roadMapping.roadLength();
        final Class<? extends RoadMapping> roadMappingClass = roadMapping.getClass();

        if (roadMappingClass == RoadMappingLine.class) {
            posTheta = roadMapping.startPos(lateralOffset);
            roadPath.moveTo((float) posTheta.x, (float) posTheta.y);
            posTheta = roadMapping.endPos(lateralOffset);
            roadPath.lineTo((float) posTheta.x, (float) posTheta.y);
            return roadPath;
        } else if (roadMappingClass == RoadMappingCircle.class) {
            final RoadMappingCircle mappingCircle = (RoadMappingCircle) roadMapping;
            roadPath.addCircle((float) mappingCircle.centerX(), (float) mappingCircle.centerY(),
                    (float) (mappingCircle.radius() + lateralOffset), Path.Direction.CCW);
            return roadPath;
        } else if (roadMappingClass == RoadMappingU.class) {
            final RoadMappingU mappingU = (RoadMappingU) roadMapping;
            final double straightLength = mappingU.straightLength();
            // draw the first straight
            posTheta = roadMapping.startPos(lateralOffset);
            roadPath.moveTo((float) posTheta.x, (float) posTheta.y);
            posTheta = roadMapping.map(straightLength, lateralOffset);
            roadPath.lineTo((float) posTheta.x, (float) posTheta.y);
            // draw the U
            final RectF rect = new RectF();
            rect.top = (float) posTheta.y;
            rect.right = (float) (posTheta.x + mappingU.radius() + lateralOffset);
            rect.left = (float) (posTheta.x - mappingU.radius() - lateralOffset);
            posTheta = roadMapping.map(roadLength - straightLength, lateralOffset);
            rect.bottom = (float) posTheta.y;
            roadPath.addArc(rect, 90.0f, 180.0f);
            // draw the second straight
            roadPath.moveTo((float) posTheta.x, (float) posTheta.y);
            posTheta = roadMapping.endPos(lateralOffset);
            roadPath.lineTo((float) posTheta.x, (float) posTheta.y);
            return roadPath;
        } else if (roadMappingClass == RoadMappingArc.class) {
            final RoadMappingArc arc = (RoadMappingArc) roadMapping;
            final double arcSweep;
            final double arcStart;
            if (arc.clockwise()) {
                arcSweep = -arc.arcAngle();
                arcStart = -arc.startAngle() - 0.5 * Math.PI;
            } else {
                arcSweep = arc.arcAngle();
                arcStart = arc.startAngle() + 0.5 * Math.PI;
            }
            final double radius = arc.radius();
            final RectF rect = new RectF();
            rect.top = (float) (arc.centerY() - radius - lateralOffset);
            rect.bottom = (float) (arc.centerY() + radius + lateralOffset);
            rect.left = (float) (arc.centerX() - radius - lateralOffset);
            rect.right = (float) (arc.centerX() + radius + lateralOffset);
            roadPath.addArc(rect, (float) Math.toDegrees(arcStart), (float) Math.toDegrees(arcSweep));
            return roadPath;
        } else if (roadMappingClass == RoadMappingPolyLine.class) {
            final RoadMappingPolyLine polyLine = (RoadMappingPolyLine) roadMapping;
            final Iterator<RoadMappingLine> iterator = polyLine.iterator();
            if (!iterator.hasNext()) {
                return null;
            }
            RoadMappingLine line = iterator.next();
            posTheta = line.startPos(lateralOffset);
            roadPath.moveTo((float) posTheta.x, (float) posTheta.y);
            posTheta = roadMapping.endPos(lateralOffset);
            roadPath.lineTo((float) posTheta.x, (float) posTheta.y);
            while (iterator.hasNext()) {
                line = iterator.next();
                posTheta = line.endPos(lateralOffset);
                roadPath.lineTo((float) posTheta.x, (float) posTheta.y);
            }
            return roadPath;
        } else if (roadMappingClass == RoadMappingPolyBezier.class) {
            final RoadMappingPolyBezier polyBezier = (RoadMappingPolyBezier) roadMapping;
            final Iterator<RoadMappingBezier> iterator = polyBezier.iterator();
            if (!iterator.hasNext()) {
                return null;
            }
            RoadMappingBezier bezier = iterator.next();
            posTheta = bezier.startPos(lateralOffset);
            roadPath.moveTo((float) posTheta.x, (float) posTheta.y);
            posTheta = bezier.endPos(lateralOffset);
            roadPath.quadTo((float) bezier.controlX(lateralOffset), (float) bezier.controlY(lateralOffset),
                    (float) posTheta.x, (float) posTheta.y);
            while (iterator.hasNext()) {
                bezier = iterator.next();
                posTheta = bezier.endPos(lateralOffset);
                roadPath.quadTo((float) bezier.controlX(lateralOffset), (float) bezier.controlY(lateralOffset),
                        (float) posTheta.x, (float) posTheta.y);
            }
            return roadPath;
        }
        // default drawing splits the road into line sections and draws those
        final double sectionLength = 20; // draw the road in sections 20 meters long
        double roadPos = 0.0;
        posTheta = roadMapping.map(roadPos, lateralOffset);
        roadPath.moveTo((float) posTheta.x, (float) posTheta.y);
        while (roadPos < roadLength) {
            roadPos += sectionLength;
            posTheta = roadMapping.map(roadPos, lateralOffset);
            roadPath.lineTo((float) posTheta.x, (float) posTheta.y);
        }
        return roadPath;
    }

    static public void clipPath(Canvas canvas, Path clipPath, RoadMapping roadMapping) {
        if (roadMapping.clippingPolygons() == null) {
            canvas.clipPath(clipPath);
        } else {
            clipPath.reset();
            for (RoadMapping.PolygonFloat polygon : roadMapping.clippingPolygons()) {
                clipPath.moveTo(polygon.xPoints[0], polygon.yPoints[0]);
                clipPath.lineTo(polygon.xPoints[1], polygon.yPoints[1]);
                clipPath.lineTo(polygon.xPoints[2], polygon.yPoints[2]);
                clipPath.lineTo(polygon.xPoints[3], polygon.yPoints[3]);
                clipPath.lineTo(polygon.xPoints[0], polygon.yPoints[0]);
                clipPath.close();
                canvas.clipPath(clipPath, Region.Op.DIFFERENCE);
            }
            // add the outer region (encloses whole road), so that everything outside the clip
            // region is drawn
            RoadMapping.PolygonFloat polygon = roadMapping.outsideClippingPolygon();
            clipPath.reset();
            clipPath.moveTo(polygon.xPoints[0], polygon.yPoints[0]);
            clipPath.lineTo(polygon.xPoints[1], polygon.yPoints[1]);
            clipPath.lineTo(polygon.xPoints[2], polygon.yPoints[2]);
            clipPath.lineTo(polygon.xPoints[3], polygon.yPoints[3]);
            clipPath.lineTo(polygon.xPoints[0], polygon.yPoints[0]);
            clipPath.close();
            canvas.clipPath(clipPath, Region.Op.REPLACE);
            // add the clip region
        }
    }
}