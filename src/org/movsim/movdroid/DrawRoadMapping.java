/*
 * Copyright (C) 2010 Martin Budden.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.movsim.movdroid;
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
            roadPath.moveTo((float)posTheta.x, (float)posTheta.y);
            posTheta = roadMapping.endPos(lateralOffset);
            roadPath.lineTo((float)posTheta.x, (float)posTheta.y);
            return roadPath;
        } else if (roadMappingClass == RoadMappingCircle.class) {
            final RoadMappingCircle mappingCircle = (RoadMappingCircle)roadMapping;
            roadPath.addCircle((float)mappingCircle.centerX(), (float)mappingCircle.centerY(),
                    (float)(mappingCircle.radius() + lateralOffset), Path.Direction.CCW);
            return roadPath;
        } else if (roadMappingClass == RoadMappingU.class) {
            final RoadMappingU mappingU = (RoadMappingU)roadMapping;
            final double straightLength = mappingU.straightLength();
            // draw the first straight
            posTheta = roadMapping.startPos(lateralOffset);
            roadPath.moveTo((float)posTheta.x, (float)posTheta.y);
            posTheta = roadMapping.map(straightLength, lateralOffset);
            roadPath.lineTo((float)posTheta.x, (float)posTheta.y);
            // draw the U
            final RectF rect = new RectF();
            rect.top = (float)posTheta.y;
            rect.right = (float)(posTheta.x + mappingU.radius() + lateralOffset);
            rect.left = (float)(posTheta.x - mappingU.radius() - lateralOffset);
            posTheta = roadMapping.map(roadLength - straightLength, lateralOffset);
            rect.bottom = (float)posTheta.y;
            roadPath.addArc(rect, 90.0f, 180.0f);
            // draw the second straight
            roadPath.moveTo((float)posTheta.x, (float)posTheta.y);
            posTheta = roadMapping.endPos(lateralOffset);
            roadPath.lineTo((float)posTheta.x, (float)posTheta.y);
            return roadPath;
        } else if (roadMappingClass == RoadMappingArc.class) {
            final RoadMappingArc arc = (RoadMappingArc)roadMapping;
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
            rect.top = (float)(arc.centerY() - radius);
            rect.bottom = (float)(arc.centerY() + radius);
            rect.left = (float)(arc.centerX() - radius);
            rect.right = (float)(arc.centerX() + radius);
            roadPath.addArc(rect, (float)Math.toDegrees(arcStart), (float)Math.toDegrees(arcSweep));
            return roadPath;
        } else if (roadMappingClass == RoadMappingPolyLine.class) {
            final RoadMappingPolyLine polyLine = (RoadMappingPolyLine)roadMapping;
            final Iterator<RoadMappingLine> iterator = polyLine.iterator();
            if (!iterator.hasNext()) {
                return null;
            }
            RoadMappingLine line = iterator.next();
            posTheta = line.startPos(lateralOffset);
            roadPath.moveTo((float)posTheta.x, (float)posTheta.y);
            posTheta = roadMapping.endPos(lateralOffset);
            roadPath.lineTo((float)posTheta.x, (float)posTheta.y);
            while (iterator.hasNext()) {
                line = iterator.next();
                posTheta = line.endPos(lateralOffset);
                roadPath.lineTo((float)posTheta.x, (float)posTheta.y);
            }
            return roadPath;
        } else if (roadMappingClass == RoadMappingPolyBezier.class) {
            final RoadMappingPolyBezier polyBezier = (RoadMappingPolyBezier)roadMapping;
            final Iterator<RoadMappingBezier> iterator = polyBezier.iterator();
            if (!iterator.hasNext()) {
                return null;
            }
            RoadMappingBezier bezier = iterator.next();
            posTheta = bezier.startPos(lateralOffset);
            roadPath.moveTo((float)posTheta.x, (float)posTheta.y);
            posTheta = bezier.endPos(lateralOffset);
            roadPath.quadTo((float)bezier.controlX(lateralOffset),
                    (float)bezier.controlY(lateralOffset), (float)posTheta.x, (float)posTheta.y);
            while (iterator.hasNext()) {
                bezier = iterator.next();
                posTheta = bezier.endPos(lateralOffset);
                roadPath.quadTo((float)bezier.controlX(lateralOffset),
                        (float)bezier.controlY(lateralOffset), (float)posTheta.x, (float)posTheta.y);
            }
            return roadPath;
        }
        // default drawing splits the road into line sections and draws those
        final double sectionLength = 20; // draw the road in sections 20 meters long
        double roadPos = 0.0;
        posTheta = roadMapping.map(roadPos, lateralOffset);
        roadPath.moveTo((float)posTheta.x, (float)posTheta.y);
        while (roadPos < roadLength) {
            roadPos += sectionLength;
            posTheta = roadMapping.map(roadPos, lateralOffset);
            roadPath.lineTo((float)posTheta.x, (float)posTheta.y);
        }
        return roadPath;
    }

    static public void clipPath(Canvas canvas, Path clipPath, RoadMapping roadMapping) {
        if (roadMapping.clippingPolygons() == null) {
            canvas.clipPath(null);
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