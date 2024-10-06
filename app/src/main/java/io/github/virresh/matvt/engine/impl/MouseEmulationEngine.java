package com.kisa.vmouse.engine.impl;

import static com.kisa.vmouse.helper.Helper.helperContext;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Context;
import android.graphics.Path;
import android.graphics.Point;
import android.content.Context;
import android.graphics.PointF;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.kisa.vmouse.helper.Helper;
import com.kisa.vmouse.view.MouseCursorView;
import com.kisa.vmouse.view.OverlayView;

public class MouseEmulationEngine {

    public void attachGesture (final PointF originPoint, final int direction) {
        if (previousRunnable != null) {
            detachPreviousTimer();
        }
        previousRunnable = new Runnable() {
            @Override
            public void run() {
                mPointerControl.reappear();
                mService.dispatchGesture(createSwipe(originPoint, direction, 20 + momentumStack), null, null);
                //momentumStack += 1;
                timerHandler.postDelayed(this, 30);
            }
        };
        timerHandler.postDelayed(previousRunnable, 0);
    }
    private static boolean DPAD_SELECT_PRESSED = false;
    private static final String LOG_TAG = "MOUSE_EMULATION";

    CountDownTimer waitToChange;

    CountDownTimer disappearTimer;

    private boolean isInScrollMode = false;

    // service which started this engine
    public AccessibilityService mService;

    private final PointerControl mPointerControl;

    public static int stuckAtSide = 0;

    private int momentumStack;

    private boolean isEnabled;

    public static int bossKey;

    public static int scrollSpeed;

    public static boolean isBossKeyDisabled;

    public static boolean isBossKeySetToToggle;

    private Handler timerHandler;

    private Point DPAD_Center_Init_Point = new Point();

    private Runnable previousRunnable;

    // tells which keycodes correspond to which pointer movement in scroll and movement mode
    // scroll directions don't match keycode instruction because that's how swiping works
    private static final Map<Integer, Integer> scrollCodeMap;
    static {
        Map<Integer, Integer> integerMap = new HashMap<>();
        integerMap.put(KeyEvent.KEYCODE_DPAD_UP, com.kisa.vmouse.engine.impl.PointerControl.DOWN);
        integerMap.put(KeyEvent.KEYCODE_DPAD_DOWN, com.kisa.vmouse.engine.impl.PointerControl.UP);
        integerMap.put(KeyEvent.KEYCODE_DPAD_LEFT, com.kisa.vmouse.engine.impl.PointerControl.RIGHT);
        integerMap.put(KeyEvent.KEYCODE_DPAD_RIGHT, com.kisa.vmouse.engine.impl.PointerControl.LEFT);
        integerMap.put(KeyEvent.KEYCODE_BUTTON_4, com.kisa.vmouse.engine.impl.PointerControl.LEFT_DOWN);
        integerMap.put(KeyEvent.KEYCODE_BUTTON_2, com.kisa.vmouse.engine.impl.PointerControl.RIGHT_DOWN);
        integerMap.put(KeyEvent.KEYCODE_BUTTON_3, com.kisa.vmouse.engine.impl.PointerControl.LEFT_UP);
        integerMap.put(KeyEvent.KEYCODE_BUTTON_1, com.kisa.vmouse.engine.impl.PointerControl.RIGHT_UP);
        /*integerMap.put(KeyEvent.KEYCODE_PROG_GREEN, PointerControl.DOWN);
        integerMap.put(KeyEvent.KEYCODE_PROG_RED, PointerControl.UP);
        integerMap.put(KeyEvent.KEYCODE_PROG_BLUE, PointerControl.RIGHT);
        integerMap.put(KeyEvent.KEYCODE_PROG_YELLOW, PointerControl.LEFT);*/
        //only for Android TVs
        scrollCodeMap = Collections.unmodifiableMap(integerMap);
    }

    private static final Map<Integer, Integer> movementCodeMap;
    static {
        Map<Integer, Integer> integerMap = new HashMap<>();
        integerMap.put(KeyEvent.KEYCODE_DPAD_UP, com.kisa.vmouse.engine.impl.PointerControl.UP);
        integerMap.put(KeyEvent.KEYCODE_DPAD_DOWN, com.kisa.vmouse.engine.impl.PointerControl.DOWN);
        integerMap.put(KeyEvent.KEYCODE_DPAD_LEFT, com.kisa.vmouse.engine.impl.PointerControl.LEFT);
        integerMap.put(KeyEvent.KEYCODE_DPAD_RIGHT, com.kisa.vmouse.engine.impl.PointerControl.RIGHT);
        integerMap.put(KeyEvent.KEYCODE_BUTTON_4, com.kisa.vmouse.engine.impl.PointerControl.LEFT_DOWN);
        integerMap.put(KeyEvent.KEYCODE_BUTTON_2, com.kisa.vmouse.engine.impl.PointerControl.RIGHT_DOWN);
        integerMap.put(KeyEvent.KEYCODE_BUTTON_3, com.kisa.vmouse.engine.impl.PointerControl.LEFT_UP);
        integerMap.put(KeyEvent.KEYCODE_BUTTON_1, com.kisa.vmouse.engine.impl.PointerControl.RIGHT_UP);
        movementCodeMap = Collections.unmodifiableMap(integerMap);
    }

    private static final Set<Integer> actionableKeyMap;
    static {
        Set<Integer> integerSet = new HashSet<>();
        integerSet.add(KeyEvent.KEYCODE_DPAD_UP);
        integerSet.add(KeyEvent.KEYCODE_DPAD_DOWN);
        integerSet.add(KeyEvent.KEYCODE_DPAD_LEFT);
        integerSet.add(KeyEvent.KEYCODE_DPAD_RIGHT);
        integerSet.add(KeyEvent.KEYCODE_BUTTON_4);
        integerSet.add(KeyEvent.KEYCODE_BUTTON_2);
        integerSet.add(KeyEvent.KEYCODE_BUTTON_3);
        integerSet.add(KeyEvent.KEYCODE_BUTTON_1);
        actionableKeyMap = Collections.unmodifiableSet(integerSet);
    }

    private static final Set<Integer> colorSet;
    static {
        Set<Integer> integerSet = new HashSet<>();
        integerSet.add(KeyEvent.KEYCODE_PROG_GREEN);
        integerSet.add(KeyEvent.KEYCODE_PROG_YELLOW);
        integerSet.add(KeyEvent.KEYCODE_PROG_BLUE);
        integerSet.add(KeyEvent.KEYCODE_PROG_RED);
        colorSet = Collections.unmodifiableSet(integerSet);
    }

    public MouseEmulationEngine (Context c, OverlayView ov) {
        momentumStack = 0;
        // overlay view for drawing mouse
        MouseCursorView mCursorView = new MouseCursorView(c);
        ov.addFullScreenLayer(mCursorView);
        mPointerControl = new PointerControl(ov, mCursorView);
        mPointerControl.disappear();
        Log.i(LOG_TAG, "X, Y: " + mPointerControl.getPointerLocation().x + ", " + mPointerControl.getPointerLocation().y);
    }

    public void init(@NonNull AccessibilityService s) {
        this.mService = s;
        mPointerControl.reset();
        timerHandler = new Handler();
        isEnabled = false;
    }

    private void attachTimer (final int direction, KeyEvent keyEvent) {
        if (previousRunnable != null) {
            detachPreviousTimer();
        }

        previousRunnable = new Runnable() {
            @Override
            public void run() {
                mPointerControl.reappear();
                mPointerControl.move(direction, momentumStack, keyEvent);
                momentumStack += 1;
                timerHandler.postDelayed(this, 30);
            }
        };
        timerHandler.postDelayed(previousRunnable, 0);
    }

    /**
     * Send input via Android's gestureAPI
     * Only sends swipes
     * see {@link MouseEmulationEngine#createClick(PointF, long)} for clicking at a point
     * @param originPoint
     * @param direction
     */


    private void createSwipeForSingle (final PointF originPoint, final int direction) {
        if (previousRunnable != null) {
            detachPreviousTimer();
        }
        previousRunnable = new Runnable() {
            @Override
            public void run() {
                mPointerControl.reappear();
                mService.dispatchGesture(createSwipe(originPoint, direction, 20), null, null);
                //momentumStack += 1;
                timerHandler.postDelayed(this, 30);
            }
        };
        timerHandler.postDelayed(previousRunnable, 0);
    }


    /**
     * Auto Disappear mouse after some duration and reset momentum
     */
    private void detachPreviousTimer () {
        if (disappearTimer != null) {
            disappearTimer.cancel();
        }
        if (previousRunnable != null) {
            timerHandler.removeCallbacks(previousRunnable);
            momentumStack = 0;
            disappearTimer = new CountDownTimer(10000, 10000) {
                @Override
                public void onTick(long l) { }

                @Override
                public void onFinish() {
                    mPointerControl.disappear();
                }
            };
            disappearTimer.start();
        }
    }

    private static GestureDescription createClick (PointF clickPoint, long duration) {
        final int DURATION = 1 + (int) duration;
        Log.i(LOG_TAG, "Actual Duration used -- " + DURATION);
        Path clickPath = new Path();
        clickPath.moveTo(clickPoint.x, clickPoint.y);
        GestureDescription.StrokeDescription clickStroke =
                new GestureDescription.StrokeDescription(clickPath, 0, DURATION);
        GestureDescription.Builder clickBuilder = new GestureDescription.Builder();
        clickBuilder.addStroke(clickStroke);
        return clickBuilder.build();
    }

    private static GestureDescription createSwipe (PointF originPoint, int direction, int momentum) {
        final int DURATION = scrollSpeed + 8;
        Path clickPath = new Path();
        PointF lineDirection = new PointF(originPoint.x + momentum * PointerControl.dirX[direction], originPoint.y + momentum * PointerControl.dirY[direction]);
        clickPath.moveTo(originPoint.x, originPoint.y);
        clickPath.lineTo(lineDirection.x, lineDirection.y);
        GestureDescription.StrokeDescription clickStroke =
                new GestureDescription.StrokeDescription(clickPath, 0, DURATION);
        GestureDescription.Builder clickBuilder = new GestureDescription.Builder();
        clickBuilder.addStroke(clickStroke);
        return clickBuilder.build();
    }

    public PointF getScreenCenter() {
        // Get the display metrics
        DisplayMetrics metrics = new DisplayMetrics();
        ((WindowManager) mService.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getMetrics(metrics);

        // Calculate the center point
        return new PointF(metrics.widthPixels / 2f, metrics.heightPixels / 2f);
    }

    private static final long LONG_PRESS_THRESHOLD = 500;
    private long bossKeyPressTime = 0;

    public boolean perform (KeyEvent keyEvent) {

        // toggle mouse mode if going via bossKey
        //it wasn't working
        /*if (keyEvent.getKeyCode() == bossKey && !isBossKeyDisabled && !isBossKeySetToToggle) {
            if (keyEvent.getAction() == KeyEvent.ACTION_UP) {
                if (waitToChange != null) {
                    // cancel change countdown
                    waitToChange.cancel();
                    if (isEnabled) return true;
                }
            }
            if (keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                waitToChange();
                if (isEnabled){
                    isInScrollMode = !isInScrollMode;
                    Toast.makeText(mService, isInScrollMode ? "Scroll Mode: Enabled" : "Scroll Mode: Disabled",
                            Toast.LENGTH_SHORT).show();
                    return true;
                }
            }
        }*/
        /*else*/
            if (keyEvent.getKeyCode() == bossKey && !isBossKeyDisabled) {

                //start counter for longpress, previous code was not working
                if (keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                    bossKeyPressTime = System.currentTimeMillis();
                    return true;
                }

                // keep a three way toggle. Dpad Mode -> Mouse Mode -> Scroll Mode -> Dpad Mode
                else if (keyEvent.getAction() == KeyEvent.ACTION_UP) {
                    //check longpress duration and switch if sufficient
                    long keyPressDuration = System.currentTimeMillis() - bossKeyPressTime;
                    if (keyPressDuration >= LONG_PRESS_THRESHOLD) {
                        if (isEnabled) {
                            // Mouse Mode -> D-pad mode
                            setMouseModeEnabled(false);
                            //isInScrollMode = false;  //v. comment $1
                        }
                        //$1 - Ex-Scroll mode, it is now handled by bordered window

                /*else if (isEnabled && !isInScrollMode) {
                    // Mouse Mode -> Scroll Mode
                    Toast.makeText(mService, "Scroll Mode", Toast.LENGTH_SHORT).show();
                    isInScrollMode = true;
                } */

                        else if (!isEnabled) {
                            // Dpad mode -> Mouse mode
                            setMouseModeEnabled(true);
                            //isInScrollMode = false; //v. comment $1
                        }
                    }
                }
            // bossKey is enabled. Handle this here itself and don't let it reach system
            return true;
        }
        else if (keyEvent.getKeyCode() == bossKey && isBossKeyDisabled) {
            // bossKey is set to disabled, let system do it's thing
            return false;
        }
        // keep full functionality on full size remotes
        if (keyEvent.getAction() == KeyEvent.ACTION_DOWN && keyEvent.getKeyCode() == KeyEvent.KEYCODE_INFO) {
            if (this.isEnabled) {
                // mouse already enabled, disable it and make it go away
                this.isEnabled = false;
                mPointerControl.disappear();
                Toast.makeText(mService, "Dpad Mode", Toast.LENGTH_SHORT).show();
                return true;
            } else {
                // mouse is disabled, enable it, reset it and show it
                this.isEnabled = true;
                mPointerControl.reset();
                mPointerControl.reappear();
                Toast.makeText(mService, "Mouse/Scroll", Toast.LENGTH_SHORT).show();
            }
        }

        if (!isEnabled) {
            // mouse is disabled, don't do anything and let the system consume this event
            return false;
        }
        boolean consumed = false;
        if (keyEvent.getAction() == KeyEvent.ACTION_DOWN){
            if (scrollCodeMap.containsKey(keyEvent.getKeyCode())) {

                if (isInScrollMode || colorSet.contains(keyEvent.getKeyCode()))
                    attachGesture(mPointerControl.getPointerLocation(), scrollCodeMap.get(keyEvent.getKeyCode()));

                else if (!isInScrollMode && stuckAtSide != 0 && keyEvent.getKeyCode() == stuckAtSide)
                    attachGesture(getScreenCenter(), scrollCodeMap.get(keyEvent.getKeyCode()));

                else if (movementCodeMap.containsKey(keyEvent.getKeyCode()))
                    attachTimer(movementCodeMap.get(keyEvent.getKeyCode()), keyEvent);
                    consumed = true;
            }
            else if(keyEvent.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER) {
                // just consume this event to prevent propagation
                DPAD_Center_Init_Point = new Point((int) mPointerControl.getPointerLocation().x, (int) mPointerControl.getPointerLocation().y);
                DPAD_SELECT_PRESSED = true;
                consumed = true;
            }
        }
        else if (keyEvent.getAction() == KeyEvent.ACTION_UP) {
            // key released, cancel any ongoing effects and clean-up
            // since bossKey is also now a part of this stuff, consume it if events enabled
            if (actionableKeyMap.contains(keyEvent.getKeyCode())
                    || keyEvent.getKeyCode() == bossKey) {
                detachPreviousTimer();
                consumed = true;
            }
            else if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER) {
                DPAD_SELECT_PRESSED = false;
                detachPreviousTimer();
//                if (keyEvent.getEventTime() - keyEvent.getDownTime() > 500) {
                    // unreliable long click event if button was pressed for more than 500 ms
                int action = AccessibilityNodeInfo.ACTION_CLICK;
                Point pInt = new Point((int) mPointerControl.getPointerLocation().x, (int) mPointerControl.getPointerLocation().y);
                if (DPAD_Center_Init_Point.equals(pInt)) {
                    List<AccessibilityWindowInfo> windowList = mService.getWindows();
                    boolean wasIME = false, focused = false;
                    for (AccessibilityWindowInfo window : windowList) {
                        if (consumed || wasIME) {
                            break;
                        }
                        List<AccessibilityNodeInfo> nodeHierarchy = findNode(window.getRoot(), action, pInt);
                        for (int i = nodeHierarchy.size() - 1; i >= 0; i--) {
                            if (consumed || focused) {
                                break;
                            }
                            AccessibilityNodeInfo hitNode = nodeHierarchy.get(i);
                            List<AccessibilityNodeInfo.AccessibilityAction> availableActions = hitNode.getActionList();
                            if (availableActions.contains(AccessibilityNodeInfo.AccessibilityAction.ACTION_ACCESSIBILITY_FOCUS)) {
                                focused = hitNode.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS);
                            }
                            if (hitNode.isFocused() && availableActions.contains(AccessibilityNodeInfo.AccessibilityAction.ACTION_SELECT)) {
                                hitNode.performAction(AccessibilityNodeInfo.ACTION_SELECT);
                            }
                            if (hitNode.isFocused() && availableActions.contains(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK)) {
                                consumed = hitNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            }
                            if (window.getType() == AccessibilityWindowInfo.TYPE_INPUT_METHOD && !(hitNode.getPackageName()).toString().contains("leankeyboard")) {
                                if (hitNode.getPackageName().equals("com.amazon.tv.ime") && keyEvent.getKeyCode() == KeyEvent.KEYCODE_BACK && helperContext != null) {
                                    InputMethodManager imm = (InputMethodManager) helperContext.getSystemService(Context.INPUT_METHOD_SERVICE);
                                    imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
                                    consumed = wasIME = true;
                                } else {
                                    wasIME = true;
                                    consumed = hitNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                }
                                break;
                            }

                            if ((hitNode.getPackageName().equals("com.google.android.tvlauncher")
                                    && availableActions.contains(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK))) {
                                if (hitNode.isFocusable()) {
                                    focused = hitNode.performAction(AccessibilityNodeInfo.FOCUS_INPUT);
                                }
                                consumed = hitNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            }
                        }
                    }
                    if (!consumed && !wasIME) {
                        mService.dispatchGesture(createClick(mPointerControl.getPointerLocation(), keyEvent.getEventTime() - keyEvent.getDownTime()), null, null);
                    }
                }
                else{
                    //Implement Drag Function here
                }
            }
        }
        return consumed;
    }

    private void setMouseModeEnabled(boolean enable) {
        if (enable) {
            // Enable Mouse Mode
            this.isEnabled = true;
            isInScrollMode = false;
            mPointerControl.reset();
            mPointerControl.reappear();
            //Toast.makeText(mService, "Mouse Mode", Toast.LENGTH_SHORT).show();
        }
        else {
            // Disable Mouse Mode
            this.isEnabled = false;
            mPointerControl.disappear();
            //Toast.makeText(mService, "D-Pad Mode", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Simple count down timer for checking keypress duration
     */
    private void waitToChange() {
        waitToChange = new CountDownTimer(800, 800) {
            @Override
            public void onTick(long l) { }
            @Override
            public void onFinish() {
                setMouseModeEnabled(!isEnabled);
            }
        };
        waitToChange.start();
    }

    //// below code is for supporting legacy devices as per my understanding of evia face cam source
    //// this is only used for long clicks here and isn't exactly something reliable
    //// leaving it in for reference just in case needed in future, because looking up face cam
    //// app's source might be a daunting task

    private List<AccessibilityNodeInfo> findNode (AccessibilityNodeInfo node, int action, Point pInt) {
        if (node == null) {
            node = mService.getRootInActiveWindow();
        }
        if (node == null) {
            Log.i(LOG_TAG, "Root Node ======>>>>>" + ((node != null) ? node.toString() : "null"));
        }
        List<AccessibilityNodeInfo> nodeInfos = new ArrayList<>();
        Log.i(LOG_TAG, "Node found ?" + ((node != null) ? node.toString() : "null"));
        node = findNodeHelper(node, action, pInt, nodeInfos);
        Log.i(LOG_TAG, "Node found ?" + ((node != null) ? node.toString() : "null"));
        Log.i(LOG_TAG, "Number of Nodes ?=>>>>> " + nodeInfos.size());
        return nodeInfos;
    }

    private AccessibilityNodeInfo findNodeHelper (AccessibilityNodeInfo node, int action, Point pInt, List<AccessibilityNodeInfo> nodeList) {
        if (node == null) {
            return null;
        }
        Rect tmp = new Rect();
        node.getBoundsInScreen(tmp);
        if (!tmp.contains(pInt.x, pInt.y)) {
            // node doesn't contain cursor
            return null;
        }
        // node contains cursor, add to node hierarchy
        nodeList.add(node);
        AccessibilityNodeInfo result = null;
        result = node;
//        if ((node.getActions() & action) != 0 && node != null) {
//            // possible to use this one, but keep searching children as well
//            nodeList.add(node);
//        }
        int childCount = node.getChildCount();
        for (int i=0; i<childCount; i++) {
            AccessibilityNodeInfo child = findNodeHelper(node.getChild(i), action, pInt, nodeList);
            if (child != null) {
                // always picks the last innermost clickable child
                result = child;
            }
        }
        return result;
    }

    /** Not used
     * Letting this stay here just in case the code needs porting back to an obsolete version
     * sometime in future
     //    private void attachActionable (final int action, final AccessibilityNodeInfo node) {
     //        if (previousRunnable != null) {
     //            detachPreviousTimer();
     //        }
     //        previousRunnable = new Runnable() {
     //            @Override
     //            public void run() {
     //                mPointerControl.reappear();
     //                node.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
     //                node.performAction(action);
     //                node.performAction(AccessibilityNodeInfo.ACTION_CLEAR_FOCUS);
     //                timerHandler.postDelayed(this, 30);
     //            }
     //        };
     //        timerHandler.postDelayed(previousRunnable, 0);
     //    }
     **/
}
