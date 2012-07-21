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

import org.movsim.simulator.SimulationRunnable;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

/**
 * <p>
 * TrafficView abstract base class.
 * </p>
 * <p>
 * Contains the SimulationRunnable which runs the simulation in a separate thread.
 * </p>
 * <p>
 * The base class handles:
 * <ul>
 * <li>synchronization between the simulation and UI threads.</li>
 * <li>starting, stopping, pausing and resuming of the simulation.</li>
 * <li>zooming and panning</li>
 * <li>standard motion events (including dragging and pinch zooming)</li>
 * </ul>
 * </p>
 * 
 * <p>
 * That is the base class handles the "policy free" aspects of the view. Other aspects of the view (colors, setting up the simulation,
 * drawing the foreground and background) are deferred to a subclass.
 * </p>
 */
public abstract class ViewBase extends View {

    //private static final String TAG = "TrafficViewBase";//$NON-NLS-1$

    protected final SimulationRunnable simulationRunnable;
    protected long totalAnimationTime;

    // drawing support
    private Bitmap backgroundBitmap;
    protected int backgroundColor;
    private final ShapeDrawable mDrawable;

    // pre-allocate Paint object
    protected final Paint paint = new Paint();

    // scale factor pixels/m, smaller value means smaller looking roads
    protected float scale = 1.0f;
    // canvas offset to support dragging etc
    private float xOffset;
    private float yOffset;
    private Matrix transform = new Matrix();

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
    protected Integer androidVersion;

    /**
     * <p>
     * Abstract function to allow the view to draw the simulation background, normally this is everything that does not move.
     * </p>
     */
    protected abstract void drawBackground(Canvas canvas);

    /**
     * <p>
     * Abstract function to allow the view to draw the simulation foreground, normally this is everything that moves.
     * </p>
     */
    protected abstract void drawForeground(Canvas canvas);

    /**
     * <p>
     * Constructor.
     * </p>
     * 
     * @param context
     * @param simulationRunnable
     */
    @SuppressLint("NewApi")
    public ViewBase(Context context, SimulationRunnable simulationRunnable) {
        super(context);
        this.simulationRunnable = simulationRunnable;
        backgroundBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);

        // TODO better solution to paint background
        mDrawable = new ShapeDrawable(new RectShape());
        mDrawable.getPaint().setColor(0xff303030);
        final int x = 0;
        final int y = 0;
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        int width;
        int height;
        androidVersion = Integer.valueOf(android.os.Build.VERSION.SDK_INT);
        if (androidVersion >= 13) {
            // since API 13:
            Point size = new Point();
            display.getSize(size);
            width = size.x;
            height = size.y;
        } else {
            width = display.getWidth(); // deprecated
            height = display.getHeight(); // deprecated
        }
        
        // rotation hack
        if (width > height) {
            height = width;
        } else {
            width = height;
        }
        mDrawable.setBounds(x, y, width+50, height);
    }

    protected void reset() {
        resetScaleAndOffset();
        simulationRunnable.reset();
    }

    protected void setTransform() {
        transform.reset();
        transform.postScale(scale, scale);
        transform.postTranslate(xOffset, yOffset);
    }

    /**
     * <p>
     * Set the view size and redraw the background bitmap.
     * </p>
     * 
     * @param width
     * @param height
     */
    private void setSize(int width, int height) {
        backgroundBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        drawBackgroundBitmap();
    }

    /**
     * <p>
     * Standard override for onSizeChanged.
     * </p>
     */
    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        setSize(width, height);
        setScale(scale);
    }

    protected void setScale(float scale) {
        xOffset -= 0.5 * getWidth() * (1.0 / this.scale - 1.0 / scale);
        yOffset -= 0.5 * getHeight() * (1.0 / this.scale - 1.0 / scale);
        this.scale = scale;
        setTransform();
    }

    protected float scale() {
        return scale;
    }

    protected void setOffset(float xOffset, float yOffset) {
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        setTransform();
    }

    protected float xOffset() {
        return xOffset;
    }

    protected float yOffset() {
        return yOffset;
    }

    protected void setCenter(float centerX, float centerY) {
        this.xOffset = centerX - getWidth() / 2;
        this.yOffset = centerY - getHeight() / 2;
        setTransform();
    }

    protected float centerX() {
        return xOffset + getWidth() / 2;
    }

    protected float centerY() {
        return yOffset + getHeight() / 2;
    }

    public void resetScaleAndOffset() {
        scale = 1.0f;
        xOffset = 0;
        yOffset = 0;
        setTransform();
    }

    protected void forceRepaintBackground() {
        drawBackgroundBitmap();
        invalidate();
    }

    /**
     * <p>
     * Draw the view by blitting the previously drawn background bitmap and then drawing the foreground.
     * </p>
     * 
     * @param canvas
     */
    @Override
    protected void onDraw(Canvas canvas) {
        assert backgroundBitmap != null;
        // blit the previously drawn background bitmap
        canvas.drawBitmap(backgroundBitmap, 0, 0, paint);
        // and then draw the simulation
        canvas.scale(scale, scale);
        canvas.translate(xOffset, yOffset);
        drawForeground(canvas);
    }

    /**
     * <p>
     * Draw the background bitmap for the view.
     * </p>
     * <p>
     * For efficiency the background bitmap is not drawn every animation step, rather it is only drawn when it changes:
     * <ul>
     * <li>The view size or scale has changed.</li>
     * <li>The view center has changed.</li>
     * <li>The underlying road network has changed.</li>
     * </ul>
     * </p>
     */
    protected void drawBackgroundBitmap() {
        assert backgroundBitmap != null;
        final Canvas bitmapCanvas = new Canvas(backgroundBitmap);
        // must clear the background before transforms
        bitmapCanvas.drawColor(backgroundColor);
        mDrawable.getPaint().setColor(backgroundColor);
        mDrawable.draw(bitmapCanvas);
        bitmapCanvas.scale(scale, scale);
        bitmapCanvas.translate(xOffset, yOffset);
        drawBackground(bitmapCanvas);
    }

    /**
     * <p>
     * Standard window-focus override: notice focus lost so we can pause on focus lost.
     * </p>
     */
    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        // if (!hasWindowFocus) pause();
    }

    // ============================================================================================
    // SimulationRunnable related functions
    // ============================================================================================

    /**
     * <p>
     * Set the thread sleep time, this controls the animation speed.
     * </p>
     * 
     * @param sleepTime_ms
     *            the sleep time in milliseconds
     * 
     */
    public void setSleepTime(int sleepTime_ms) {
        simulationRunnable.setSleepTime(sleepTime_ms);
    }

    /**
     * <p>
     * Returns the sleep time
     * </p>
     * 
     * @return the sleep time in milliseconds
     */
    public int sleepTime() {
        return simulationRunnable.sleepTime();
    }

    /**
     * <p>
     * Returns the time for which the simulation has been running.
     * </p>
     * <p>
     * This is the logical time in the simulation (that is the sum of the timesteps), not the amount of real time that has been required to
     * do the simulation calculations.
     * </p>
     * 
     * @return the simulation time
     */
    public double simulationTime() {
        return simulationRunnable.simulationTime();
    }

    /**
     * <p>
     * Starts the animation.
     * </p>
     */
    public void start() {
        totalAnimationTime = 0;
        simulationRunnable.start();
    }

    /**
     * <p>
     * Returns true if the animation is stopped.
     * </p>
     * 
     * @return true if the animation is stopped
     */
    public boolean isStopped() {
        return simulationRunnable.isStopped();
    }

    /**
     * <p>
     * Stops the animation.
     * </p>
     */
    public void stop() {
        simulationRunnable.stop();
    }

    /**
     * <p>
     * Returns true if the animation is paused.
     * </p>
     * 
     * @return true if the animation is paused
     */
    public boolean isPaused() {
        return simulationRunnable.isPaused();
    }

    /**
     * <p>
     * Pauses the animation.
     * </p>
     */
    public void pause() {
        simulationRunnable.pause();
    }

    /**
     * <p>
     * Resumes the animation after a pause.
     * </p>
     */
    public void resume() {
        simulationRunnable.resume();
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
        // define constants to allow building for android-3 target
        // final int MASK = 0x000000ff;
        // final int POINTER_UP = 0x00000006;
        final int ACTION_MASK = MotionEvent.ACTION_MASK;
        final int ACTION_POINTER_UP = MotionEvent.ACTION_POINTER_UP;
        switch (event.getAction() & ACTION_MASK) {
        case MotionEvent.ACTION_DOWN:
            touchMode = TOUCH_MODE_DRAG;
            // pause();
            startDragX = event.getX();
            startDragY = event.getY();
            xOffsetSave = xOffset;
            yOffsetSave = yOffset;
            break;
        case MotionEvent.ACTION_UP:
        case ACTION_POINTER_UP:
            touchMode = TOUCH_MODE_NONE;
            // resume();
            break;
        case MotionEvent.ACTION_POINTER_DOWN:
            dx = event.getX(0) - event.getX(1);
            dy = event.getY(0) - event.getY(1);
            pinchDistance = (float) Math.sqrt(dx * dx + dy * dy);
            if (pinchDistance > touchModeZoomHysteresis) {
                // pinchMidpointX = (event.getX(0) + event.getX(1)) / 2;
                // pinchMidpointY = (event.getY(0) + event.getY(1)) / 2;
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
                    // xOffsetSave = xOffset;
                    // yOffsetSave = yOffset;
                    setTransform();
                    forceRepaintBackground();
                }
            } else if (touchMode == TOUCH_MODE_ZOOM) {
                dx = event.getX(0) - event.getX(1);
                dy = event.getY(0) - event.getY(1);
                final float distance = (float) Math.sqrt(dx * dx + dy * dy);
                if (pinchDistance > touchModeZoomHysteresis) {
                    final float newScale = distance / pinchDistance * scaleSave;
                    setScale(newScale);
                    // xOffset += (pinchMidpointX - getWidth() / 2) / scale;
                    // yOffset += (pinchMidpointY - getHeight() / 2) / scale;
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
