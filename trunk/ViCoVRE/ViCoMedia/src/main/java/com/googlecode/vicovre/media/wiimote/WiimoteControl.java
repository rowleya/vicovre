/**
 * Copyright (c) 2009, University of Manchester
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1) Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2) Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3) Neither the name of the and the University of Manchester nor the names of
 *    its contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 */

package com.googlecode.vicovre.media.wiimote;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.LinkedList;
import java.util.Vector;

import javax.swing.Timer;

import com.googlecode.vicovre.media.ui.FullScreenFrame;

import wiiusej.WiiUseApiManager;
import wiiusej.Wiimote;
import wiiusej.wiiusejevents.physicalevents.ExpansionEvent;
import wiiusej.wiiusejevents.physicalevents.IREvent;
import wiiusej.wiiusejevents.physicalevents.MotionSensingEvent;
import wiiusej.wiiusejevents.physicalevents.WiimoteButtonsEvent;
import wiiusej.wiiusejevents.utils.WiimoteListener;
import wiiusej.wiiusejevents.wiiuseapievents.ClassicControllerInsertedEvent;
import wiiusej.wiiusejevents.wiiuseapievents.ClassicControllerRemovedEvent;
import wiiusej.wiiusejevents.wiiuseapievents.DisconnectionEvent;
import wiiusej.wiiusejevents.wiiuseapievents.GuitarHeroInsertedEvent;
import wiiusej.wiiusejevents.wiiuseapievents.GuitarHeroRemovedEvent;
import wiiusej.wiiusejevents.wiiuseapievents.NunchukInsertedEvent;
import wiiusej.wiiusejevents.wiiuseapievents.NunchukRemovedEvent;
import wiiusej.wiiusejevents.wiiuseapievents.StatusEvent;

public class WiimoteControl implements WiimoteListener, ActionListener {

    private static final int POINT_TIMEOUT = 500;

    private boolean buttonHeld = false;

    private Point currentPoint = null;

    private LinkedList<Point> points = new LinkedList<Point>();

    private LinkedList<Timer> timers = new LinkedList<Timer>();

    private LinkedList<Timer> reusableTimer = new LinkedList<Timer>();

    private LinkedList<PointsListener> listeners =
        new LinkedList<PointsListener>();

    private Wiimote wiimote = null;

    private Integer wiimoteSync = new Integer(0);

    private boolean cancelled = false;

    private Integer cancelSync = new Integer(0);

    private boolean connecting = false;

    private Integer connectSync = new Integer(0);

    private int width = 0;

    private int height = 0;

    private boolean aboveScreen = true;

    private int sensitivity = 3;

    private WiimoteCalibrationPanel calibrationPanel =
        new WiimoteCalibrationPanel();

    private FullScreenFrame calibrationFrame = new FullScreenFrame();

    private boolean isCalibrating = false;

    public WiimoteControl() {
        calibrationFrame.setComponent(calibrationPanel);
        calibrationFrame.addComponentListener(new ComponentAdapter() {
            public void componentShown(ComponentEvent e) {
                System.err.println("Calibrating!");
                isCalibrating = true;
            }

            public void componentHidden(ComponentEvent e) {
                System.err.println("Finished calibrating");
                isCalibrating = false;
            }
        });
        calibrationPanel.setSensitivity(sensitivity);
        calibrationPanel.setSensorBarTop();
        calibrationPanel.setDisconnected();
    }

    public void setSize(int width, int height) {
        synchronized (wiimoteSync) {
            this.width = width;
            this.height = height;
            if (wiimote != null) {
                wiimote.setVirtualResolution(width, height);
            }
        }
    }

    public void setAboveScreen(boolean aboveScreen) {
        synchronized (wiimoteSync) {
            this.aboveScreen = aboveScreen;
            if (wiimote != null) {
                if (aboveScreen) {
                    wiimote.setSensorBarAboveScreen();
                    calibrationPanel.setSensorBarTop();
                } else {
                    wiimote.setSensorBarBelowScreen();
                    calibrationPanel.setSensorBarBottom();
                }
            }
        }
    }

    public void setSensitivity(int sensitivity) {
        synchronized (wiimoteSync) {
            this.sensitivity = sensitivity;
            if (wiimote != null) {
                wiimote.setIrSensitivity(sensitivity);
                calibrationPanel.setSensitivity(sensitivity);
            }
        }
    }

    public void addPointsListener(PointsListener listener) {
        listeners.add(listener);
    }

    public void removePointsListener(PointsListener listener) {
        listeners.remove(listener);
    }

    public void connectToWiimote() {
        synchronized (connectSync) {
            if (connecting) {
                while ((wiimote == null) && !cancelled) {
                    try {
                        connectSync.wait();
                    } catch (InterruptedException e) {
                        // Do Nothing
                    }
                }
                return;
            }
            connecting = true;
        }

        synchronized (cancelSync) {
            cancelled = false;
            while ((wiimote == null) && !cancelled) {
                if (!cancelled) {
                    Wiimote[] wiimotes = WiiUseApiManager.getWiimotes(1, false);
                    if (wiimotes != null && wiimotes.length > 0) {
                        synchronized (wiimoteSync) {
                            wiimote = wiimotes[0];
                            wiimote.setLeds(true, false, false, false);
                            wiimote.activateIRTRacking();
                            wiimote.activateMotionSensing();
                            if (aboveScreen) {
                                wiimote.setSensorBarAboveScreen();
                            } else {
                                wiimote.setSensorBarBelowScreen();
                            }
                            wiimote.setVirtualResolution(width, height);
                            wiimote.setIrSensitivity(sensitivity);
                            wiimote.addWiiMoteEventListeners(this);
                            calibrationPanel.setConnected();
                            System.err.println("Wiimote connected!");
                        }
                    }
                }
                try {
                    cancelSync.wait(1000);
                } catch (InterruptedException e) {
                    // Does Nothing
                }
            }
        }

        synchronized (connectSync) {
            connecting = false;
            connectSync.notifyAll();
        }
    }

    public void cancelConnect() {
        synchronized (cancelSync) {
            cancelled = true;
            cancelSync.notifyAll();
        }
    }

    public void onButtonsEvent(WiimoteButtonsEvent event) {
        buttonHeld = event.isButtonAHeld() || event.isButtonBHeld();
        if (isCalibrating) {
            if (event.isButtonHomeJustPressed()) {
                calibrationFrame.setVisible(false);
            } else if (event.isButtonUpJustPressed()) {
                setAboveScreen(true);
            } else if (event.isButtonDownJustPressed()) {
                setAboveScreen(false);
            } else if (event.isButtonPlusJustPressed()) {
                if (sensitivity < 5) {
                    setSensitivity(sensitivity + 1);
                }
            } else if (event.isButtonMinusJustPressed()) {
                if (sensitivity > 1) {
                    setSensitivity(sensitivity - 1);
                }
            }
        }
    }

    public void onDisconnectionEvent(DisconnectionEvent event) {
        synchronized (wiimoteSync) {
            if (event.getWiimoteId() == wiimote.getId()) {
                wiimote = null;
                connectToWiimote();
            }
        }
    }

    public void onIrEvent(IREvent event) {
        if (isCalibrating) {
            calibrationPanel.setPoints(event.getIRPoints());
        }
        if (event.getX() > 0 && event.getY() > 0) {
            if (buttonHeld) {
                synchronized (points) {
                    currentPoint = null;
                    int x = event.getX();
                    int y = event.getY();
                    Point point = new Point(x, y);
                    points.addLast(point);
                    Timer timer = null;
                    if (!reusableTimer.isEmpty()) {
                        timer = reusableTimer.removeFirst();
                    } else {
                        timer = new Timer(POINT_TIMEOUT, this);
                    }
                    timers.addLast(timer);
                    timer.start();
                    for (PointsListener listener : listeners) {
                        listener.updatePoints(new Vector<Point>(points),
                                currentPoint);
                    }
                }
            } else {
                synchronized (points) {
                    currentPoint = new Point(event.getX(), event.getY());
                    for (PointsListener listener : listeners) {
                        listener.updatePoints(new Vector<Point>(points),
                                currentPoint);
                    }
                }
            }
        } else {
            currentPoint = null;
            for (PointsListener listener : listeners) {
                listener.updatePoints(new Vector<Point>(points),
                        currentPoint);
            }
        }
    }

    public void actionPerformed(ActionEvent e) {
        synchronized (points) {
            if (!points.isEmpty()) {
                points.removeFirst();
                Timer timer = timers.removeFirst();
                timer.stop();
                reusableTimer.addLast(timer);
            }
            for (PointsListener listener : listeners) {
                listener.updatePoints(new Vector<Point>(points),
                        currentPoint);
            }
        }
    }

    public FullScreenFrame getCalibrationFrame() {
        return calibrationFrame;
    }

    public int getSensitivity() {
        return sensitivity;
    }

    public boolean isAboveScreen() {
        return aboveScreen;
    }

    public void onMotionSensingEvent(MotionSensingEvent event) {
        // Does Nothing

    }

    public void onClassicControllerInsertedEvent(
            ClassicControllerInsertedEvent event) {
        // Does Nothing
    }

    public void onClassicControllerRemovedEvent(
            ClassicControllerRemovedEvent event) {
        // Does Nothing
    }

    public void onExpansionEvent(ExpansionEvent event) {
        // Does Nothing
    }

    public void onGuitarHeroInsertedEvent(GuitarHeroInsertedEvent event) {
        // Does Nothing
    }

    public void onGuitarHeroRemovedEvent(GuitarHeroRemovedEvent event) {
        // Does Nothing
    }

    public void onNunchukInsertedEvent(NunchukInsertedEvent event) {
        // Does Nothing
    }

    public void onNunchukRemovedEvent(NunchukRemovedEvent event) {
        // Does Nothing
    }

    public void onStatusEvent(StatusEvent event) {
        // Does Nothing
    }

}
