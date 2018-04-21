package org.breeswish.droidpuppet.device

import android.graphics.Point
import android.hardware.display.DisplayManagerGlobal
import android.view.Display
import android.view.IRotationWatcher
import android.view.Surface
import android.view.WindowManagerGlobal

class Display {
    companion object {
        fun getRotation(): Int {
            return DisplayManagerGlobal.getInstance().getDisplayInfo(Display.DEFAULT_DISPLAY).rotation
        }

        /**
         * Get display size.
         */
        fun getDisplaySize(applyRotation: Boolean): Point {
            val displaySize = Point()
            val wm = WindowManagerGlobal.getWindowManagerService()
            wm.getBaseDisplaySize(Display.DEFAULT_DISPLAY, displaySize)
            if (applyRotation) {
                val rotation = getRotation()
                if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) {
                    val swap = displaySize.x
                    displaySize.x = displaySize.y
                    displaySize.y = swap
                }
            }
            return displaySize
        }

        fun registerRotationWatcher(watcher: IRotationWatcher) {
            val wm = WindowManagerGlobal.getWindowManagerService()
            wm.watchRotation(watcher, 0)
        }

        fun removeRotationWatcher(watcher: IRotationWatcher) {
            val wm = WindowManagerGlobal.getWindowManagerService()
            wm.removeRotationWatcher(watcher)
        }
    }
}
