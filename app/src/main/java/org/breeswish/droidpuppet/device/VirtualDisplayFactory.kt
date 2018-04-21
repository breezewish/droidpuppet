package org.breeswish.droidpuppet.device

import android.graphics.Point
import android.graphics.Rect
import android.view.*

class VirtualDisplayFactory {
    companion object {
        fun createVirtualDisplay(name: String, size: Point, surface: Surface): VirtualDisplay {
            val token = SurfaceControl.createDisplay(name, false)
            val projectionSize = Rect(0, 0, size.x, size.y)
            val displaySize = Display.getDisplaySize(true)
            val layerStackRect = Rect(0, 0, displaySize.x, displaySize.y)

            SurfaceControl.openTransaction()
            try {
                SurfaceControl.setDisplaySurface(token, surface)
                SurfaceControl.setDisplayProjection(token, 0, layerStackRect, projectionSize)
                SurfaceControl.setDisplayLayerStack(token, 0)
            } finally {
                SurfaceControl.closeTransaction()
            }

            return object : VirtualDisplay {
                override fun release() {
                    SurfaceControl.destroyDisplay(token)
                }
            }
        }
    }
}
