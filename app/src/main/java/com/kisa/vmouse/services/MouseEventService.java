package com.kisa.vmouse.services;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

import com.kisa.vmouse.engine.impl.MouseEmulationEngine;
import com.kisa.vmouse.engine.impl.PointerControl;
import com.kisa.vmouse.helper.Helper;
import com.kisa.vmouse.helper.KeyDetection;
import com.kisa.vmouse.view.OverlayView;

import static com.kisa.vmouse.engine.impl.MouseEmulationEngine.bossKey;
import static com.kisa.vmouse.engine.impl.MouseEmulationEngine.scrollSpeed;

public class MouseEventService extends AccessibilityService {

    private MouseEmulationEngine mEngine;
    private static String TAG_NAME = "V-Mouse_SERVICE";

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {}

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        super.onKeyEvent(event);
        new KeyDetection(event);
        Log.i(TAG_NAME, "Received Key => " + event.getKeyCode() + ", Action => " + event.getAction() + ", Repetition value => " + event.getRepeatCount() + ", Scan code => " + event.getScanCode());
        if (Helper.isAnotherServiceInstalled(this) &&
                event.getKeyCode() == KeyEvent.KEYCODE_HOME) return true;
        if (Helper.isOverlayDisabled(this)) return false;
        return mEngine.perform(event);
    }

    @Override
    public void onInterrupt() {}

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        bossKey = KeyEvent.KEYCODE_DPAD_CENTER;
        PointerControl.isBordered = Helper.getMouseBordered(this);
        scrollSpeed = Helper.getScrollSpeed(this);
        MouseEmulationEngine.isBossKeyDisabled = Helper.isBossKeyDisabled(this);
        MouseEmulationEngine.isBossKeySetToToggle = Helper.isBossKeySetToToggle(this);
        if (Helper.isOverriding(this)) bossKey = Helper.getBossKeyValue(this);
        if (Settings.canDrawOverlays(this)) init();
    }

    private void init() {
        if (Helper.helperContext != null) Helper.helperContext = this;
        OverlayView mOverlayView = new OverlayView(this);
        AccessibilityServiceInfo asi = this.getServiceInfo();
        if (asi != null) {
            asi.flags |= AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
            this.setServiceInfo(asi);
        }
        Log.i(TAG_NAME, "Configuration -- Scroll Speed " + scrollSpeed);
        Log.i(TAG_NAME, "Configuration -- Boss Key Disabled " + MouseEmulationEngine.isBossKeyDisabled);
        Log.i(TAG_NAME, "Configuration -- Boss Key Toggleable " + MouseEmulationEngine.isBossKeySetToToggle);
        Log.i(TAG_NAME, "Configuration -- Is Bordered " + PointerControl.isBordered);
        Log.i(TAG_NAME, "Configuration -- Boss Key value " + bossKey);

        mEngine = new MouseEmulationEngine(this, mOverlayView);
        mEngine.init(this);
    }
}
