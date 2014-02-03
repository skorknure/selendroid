/*
 * Copyright 2011 Selenium committers
 * 
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package io.selendroid.android;

import android.app.Instrumentation;
import android.content.Context;
import android.os.PowerManager;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.AbsListView;
import android.widget.ScrollView;
import io.selendroid.ServerInstrumentation;
import io.selendroid.android.internal.Point;
import io.selendroid.exceptions.SelendroidException;
import io.selendroid.server.model.TouchScreen;
import io.selendroid.server.model.interactions.Coordinates;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements touch capabilities of a device.
 * 
 */
public class AndroidTouchScreen implements TouchScreen {

  private final ServerInstrumentation instrumentation;
  private final MotionSender motions;

  public AndroidTouchScreen(ServerInstrumentation instrumentation, MotionSender motions) {
    this.instrumentation = instrumentation;
    this.motions = motions;
  }

  public void singleTap(Coordinates where) {
    Point toTap = where.getLocationOnScreen();
    List<MotionEvent> motionEvents = new ArrayList<MotionEvent>();
    long downTime = SystemClock.uptimeMillis();
    motionEvents.add(getMotionEvent(downTime, downTime, MotionEvent.ACTION_DOWN, toTap));
    motionEvents.add(getMotionEvent(downTime, downTime, MotionEvent.ACTION_UP, toTap));
    motions.send(motionEvents);
  }

  public void down(int x, int y) {
    List<MotionEvent> event = new ArrayList<MotionEvent>();
    long downTime = SystemClock.uptimeMillis();
    Point coords = new Point(x, y);
    event.add(getMotionEvent(downTime, downTime, MotionEvent.ACTION_DOWN, coords));
    motions.send(event);
  }

  public void up(int x, int y) {
    List<MotionEvent> event = new ArrayList<MotionEvent>();
    long downTime = SystemClock.uptimeMillis();
    Point coords = new Point(x, y);
    event.add(getMotionEvent(downTime, downTime, MotionEvent.ACTION_UP, coords));
    motions.send(event);
  }

  public void move(int x, int y) {
    List<MotionEvent> event = new ArrayList<MotionEvent>();
    long downTime = SystemClock.uptimeMillis();
    Point coords = new Point(x, y);
    event.add(getMotionEvent(downTime, downTime, MotionEvent.ACTION_MOVE, coords));
    motions.send(event);
  }

  public void scroll(Coordinates where, int xOffset, int yOffset) {
    long downTime = SystemClock.uptimeMillis();
    List<MotionEvent> motionEvents = new ArrayList<MotionEvent>();
    Point origin = where.getLocationOnScreen();
    Point destination = new Point(origin.x + xOffset, origin.y + yOffset);
    motionEvents.add(getMotionEvent(downTime, downTime, MotionEvent.ACTION_DOWN, origin));

    Scroll scroll = new Scroll(origin, destination, downTime);
    // Initial acceleration from origin to reference point
    motionEvents.addAll(getMoveEvents(downTime, downTime, origin, scroll.getDecelerationPoint(),
        Scroll.INITIAL_STEPS, Scroll.TIME_BETWEEN_EVENTS));
    // Deceleration phase from reference point to destination
    motionEvents.addAll(getMoveEvents(downTime, scroll.getEventTimeForReferencePoint(),
        scroll.getDecelerationPoint(), destination, Scroll.DECELERATION_STEPS,
        Scroll.TIME_BETWEEN_EVENTS));

    motionEvents.add(getMotionEvent(downTime,
        (downTime + scroll.getEventTimeForDestinationPoint()), MotionEvent.ACTION_UP, destination));
    motions.send(motionEvents);
  }

  public void doubleTap(Coordinates where) {
    Point toDoubleTap = where.getLocationOnScreen();
    List<MotionEvent> motionEvents = new ArrayList<MotionEvent>();
    long downTime = SystemClock.uptimeMillis();
    motionEvents.add(getMotionEvent(downTime, downTime, MotionEvent.ACTION_DOWN, toDoubleTap));
    motionEvents.add(getMotionEvent(downTime, downTime, MotionEvent.ACTION_UP, toDoubleTap));
    motionEvents.add(getMotionEvent(downTime, downTime, MotionEvent.ACTION_DOWN, toDoubleTap));
    motionEvents.add(getMotionEvent(downTime, downTime, MotionEvent.ACTION_UP, toDoubleTap));
    motions.send(motionEvents);
  }

  public void longPress(Coordinates where) {
    long downTime = SystemClock.uptimeMillis();
    long eventTime = SystemClock.uptimeMillis();
    Point point = where.getLocationOnScreen();
    // List<MotionEvent> motionEvents = new ArrayList<MotionEvent>();
    //
    // motionEvents.add(getMotionEvent(downTime, downTime, MotionEvent.ACTION_DOWN, point));
    // motionEvents.add(getMotionEvent(downTime, (downTime + 3000), MotionEvent.ACTION_UP, point));
    // sendMotionEvents(motionEvents);
    Instrumentation inst = instrumentation;


    MotionEvent event = null;
    boolean successfull = false;
    int retry = 0;
    while (!successfull && retry < 10) {
      try {
        if (event == null) {
          event =
              MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_DOWN, point.x, point.y, 0);
        }
        System.out.println("trying to send pointer");
        inst.sendPointerSync(event);
        successfull = true;
      } catch (SecurityException e) {
        System.out.println("failed: " + retry);
        // activityUtils.hideSoftKeyboard(null, false, true);
        retry++;
      }
    }
    if (!successfull) {
      throw new SelendroidException("Click can not be completed!");
    }
    inst.sendPointerSync(event);
    inst.waitForIdleSync();

    eventTime = SystemClock.uptimeMillis();
    final int touchSlop = ViewConfiguration.get(inst.getTargetContext()).getScaledTouchSlop();
    event =
        MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_MOVE, point.x + touchSlop / 2,
            point.y + touchSlop / 2, 0);
    inst.sendPointerSync(event);
    inst.waitForIdleSync();

    try {
      Thread.sleep((long) (ViewConfiguration.getLongPressTimeout() * 1.5f));
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    eventTime = SystemClock.uptimeMillis();
    event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_UP, point.x, point.y, 0);
    inst.sendPointerSync(event);
    inst.waitForIdleSync();
  }

  public void scroll(final int xOffset, final int yOffset) {
    List<View> scrollableContainer =
        ViewHierarchyAnalyzer.getDefaultInstance().findScrollableContainer();

    if (scrollableContainer == null) {
      // nothing to do
      return;
    }
    for (View view : scrollableContainer) {
      if (view instanceof AbsListView) {
        final AbsListView absListView = (AbsListView) view;
        instrumentation.getCurrentActivity().runOnUiThread(new Runnable() {
          public void run() {
            absListView.scrollBy(xOffset, yOffset);
          }
        });
      } else if (view instanceof ScrollView) {
        final ScrollView scrollView = (ScrollView) view;
        instrumentation.getCurrentActivity().runOnUiThread(new Runnable() {
          public void run() {
            scrollView.scrollBy(xOffset, yOffset);
          }
        });
      } else if (view instanceof WebView) {
        final WebView webView = (WebView) view;
        instrumentation.getCurrentActivity().runOnUiThread(new Runnable() {
          public void run() {
            webView.scrollBy(xOffset, yOffset);
          }
        });
      }
    }
  }

  public void flick(final int speedX, final int speedY) {
    List<View> scrollableContainer =
        ViewHierarchyAnalyzer.getDefaultInstance().findScrollableContainer();

    if (scrollableContainer == null) {
      // nothing to do
      return;
    }
    for (View view : scrollableContainer) {
      if (view instanceof AbsListView) {
        // ignore
      } else if (view instanceof ScrollView) {
        final ScrollView scrollView = (ScrollView) view;
        instrumentation.getCurrentActivity().runOnUiThread(new Runnable() {
          public void run() {
            scrollView.fling(speedY);
          }
        });
      } else if (view instanceof WebView) {
        final WebView webView = (WebView) view;
        instrumentation.getCurrentActivity().runOnUiThread(new Runnable() {
          public void run() {
            webView.flingScroll(speedX, speedY);
          }
        });
      }
    }
  }

  public void flick(Coordinates where, int xOffset, int yOffset, int speed) {
    long downTime = SystemClock.uptimeMillis();
    List<MotionEvent> motionEvents = new ArrayList<MotionEvent>();
    Point origin = where.getLocationOnScreen();
    Point destination = new Point(origin.x + xOffset, origin.y + yOffset);
    Flick flick = new Flick(speed);
    motionEvents.add(getMotionEvent(downTime, downTime, MotionEvent.ACTION_DOWN, origin));
    motionEvents.addAll(getMoveEvents(downTime, downTime, origin, destination, Flick.STEPS,
        flick.getTimeBetweenEvents()));
    motionEvents.add(getMotionEvent(downTime, flick.getTimeForDestinationPoint(downTime),
        MotionEvent.ACTION_UP, destination));
    motions.send(motionEvents);
  }

  private MotionEvent getMotionEvent(long start, long eventTime, int action, Point coords) {
    return MotionEvent.obtain(start, eventTime, action, coords.x, coords.y, 0);
  }

  private List<MotionEvent> getMoveEvents(long downTime, long startingEVentTime, Point origin,
      Point destination, int steps, long timeBetweenEvents) {
    List<MotionEvent> move = new ArrayList<MotionEvent>();
    MotionEvent event;

    float xStep = (destination.x - origin.x) / steps;
    float yStep = (destination.y - origin.y) / steps;
    float x = origin.x;
    float y = origin.y;
    long eventTime = startingEVentTime;

    for (int i = 0; i < steps - 1; i++) {
      x += xStep;
      y += yStep;
      eventTime += timeBetweenEvents;
      event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_MOVE, x, y, 0);
      move.add(event);
    }

    eventTime += timeBetweenEvents;
    move.add(getMotionEvent(downTime, eventTime, MotionEvent.ACTION_MOVE, destination));
    return move;
  }

  @Override
  public float getBrightness() {
    PowerManager powerManager = (PowerManager) instrumentation.getContext().getSystemService(Context.POWER_SERVICE);

    if (!powerManager.isScreenOn()) {
      return 0f;
    } else {
      WindowManager.LayoutParams attributes = instrumentation.getCurrentActivity().getWindow().getAttributes();
      return attributes.screenBrightness;
    }
  }

  @Override
  public void setBrightness(float brightness) {
    if (brightness < 0) {
      brightness = 0;
    }
    if (brightness > 1) {
      brightness = 1;
    }

    PowerManager powerManager = (PowerManager) instrumentation.getContext().getSystemService(Context.POWER_SERVICE);
    final Window window = instrumentation.getCurrentActivity().getWindow();
    final WindowManager.LayoutParams attributes = window.getAttributes();
    PowerManager.WakeLock wakeLock = null;
    if (brightness != 0) {
      // Turn on display
      if (!powerManager.isScreenOn()) {
        wakeLock = powerManager.newWakeLock(
            PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, "Selendroid screen wake");
      }
      // Now set the brightness
      attributes.screenBrightness = brightness;
    } else {
      // Turn off the display. Oh boy. This is derived from a reading of the PowerManager SDK docs.
      // http://developer.android.com/reference/android/os/PowerManager.html
      attributes.screenBrightness = 0;
      wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Selendroid screen sleep");
    }

    instrumentation.getCurrentActivity().runOnUiThread(
        new Runnable() {
          @Override public void run() {
            window.setAttributes(attributes);
          }
        }
    );
    instrumentation.waitForIdleSync();

    if (wakeLock != null) {
      try {
        wakeLock.acquire();
        wakeLock.release();
      } catch (SecurityException ignored) {
        // We can only turn off the screen if the AUT has the android.permission.WAKE_LOCK permission.
      }
    }
  }

  final class Scroll {

    private Point origin;
    private Point destination;
    private long downTime;
    // A regular scroll usually has 15 gestures, where the last 5 are used for deceleration
    final static int INITIAL_STEPS = 10;
    final static int DECELERATION_STEPS = 5;
    final int TOTAL_STEPS = INITIAL_STEPS + DECELERATION_STEPS;
    // Time in milliseconds to provide a speed similar to scroll
    final static long TIME_BETWEEN_EVENTS = 50;

    public Scroll(Point origin, Point destination, long downTime) {
      this.origin = origin;
      this.destination = destination;
      this.downTime = downTime;
    }

    // This method is used to calculate the point where the deceleration will start at 20% of the
    // distance to the destination point
    private Point getDecelerationPoint() {
      int deltaX = (destination.x - origin.x);
      int deltaY = (destination.y - origin.y);
      // Coordinates of reference point where deceleration should start for scroll gesture, on the
      // last 20% of the total distance to scroll
      int xRef = (int) (deltaX * 0.8);
      int yRef = (int) (deltaY * 0.8);
      return new Point(origin.x + xRef, origin.y + yRef);
    }

    private long getEventTimeForReferencePoint() {
      return (downTime + INITIAL_STEPS * TIME_BETWEEN_EVENTS);
    }

    private long getEventTimeForDestinationPoint() {
      return (downTime + TOTAL_STEPS * TIME_BETWEEN_EVENTS);
    }
  }

  final class Flick {

    private final int SPEED_NORMAL = 0;
    private final int SPEED_FAST = 1;
    // A regular scroll usually has 4 gestures
    private final static int STEPS = 4;
    private int speed;

    public Flick(int speed) {
      this.speed = speed;
    }

    private long getTimeBetweenEvents() {
      if (speed == SPEED_NORMAL) {
        return 25; // Time in milliseconds to provide a speed similar to normal flick
      } else if (speed == SPEED_FAST) {
        return 9; // Time in milliseconds to provide a speed similar to fast flick
      }
      return 0;
    }

    private long getTimeForDestinationPoint(long downTime) {
      return (downTime + STEPS * getTimeBetweenEvents());
    }
  }
}
