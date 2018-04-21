package org.breeswish.droidpuppet.device

import android.hardware.input.InputManager
import android.os.SystemClock
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent

class InputEventInjector {
    private val inputManager = InputManager.getInstance()

    private fun asyncInputInputEvent(event: KeyEvent) {
        inputManager.injectInputEvent(event, InputManager.INJECT_INPUT_EVENT_MODE_ASYNC)
    }

    private fun injectMotionEvent(time: Long, action: Int, x: Float, y: Float) {
        val event = MotionEvent.obtain(time, time, action, x, y, 1.0f, 1.0f, 0, 1.0f, 1.0f, -1, 0)
        event.source = InputDevice.SOURCE_TOUCHSCREEN
        inputManager.injectInputEvent(event, InputManager.INJECT_INPUT_EVENT_MODE_ASYNC)
    }

    private fun injectKeyEvent(time: Long, action: Int, code: Int) {
        asyncInputInputEvent(KeyEvent.obtain(time, time, action, code, 0, 0, -1, 0, 0, InputDevice.SOURCE_KEYBOARD, null))
    }

    fun touchDown(x: Float, y: Float) {
        val time = SystemClock.uptimeMillis()
        injectMotionEvent(time, MotionEvent.ACTION_DOWN, x, y)
    }

    fun touchMove(x: Float, y: Float) {
        val time = SystemClock.uptimeMillis()
        injectMotionEvent(time, MotionEvent.ACTION_MOVE, x, y)
    }

    fun touchUp(x: Float, y: Float) {
        val time = SystemClock.uptimeMillis()
        injectMotionEvent(time, MotionEvent.ACTION_UP, x, y)
    }

    fun tap(x: Float, y: Float) {
        val time = SystemClock.uptimeMillis()
        injectMotionEvent(time, MotionEvent.ACTION_DOWN, x, y)
        injectMotionEvent(time, MotionEvent.ACTION_UP, x, y)
    }

    fun keyDownUp(code: Int) {
        val time = SystemClock.uptimeMillis()
        injectKeyEvent(time, KeyEvent.ACTION_DOWN, code)
        injectKeyEvent(time, KeyEvent.ACTION_UP, code)
    }
}