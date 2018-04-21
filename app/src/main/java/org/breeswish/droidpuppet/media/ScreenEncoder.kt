package org.breeswish.droidpuppet.media

import android.graphics.Point
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.util.Log
import android.media.MediaFormat
import android.os.Build
import android.view.IRotationWatcher
import android.view.Surface
import org.breeswish.droidpuppet.device.Display
import org.breeswish.droidpuppet.device.VirtualDisplayFactory
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

class ScreenEncoder(
        private val codecName: String,
        private val format: String,
        private val videoWidthInPortrait: Int,
        private val videoHeightInPortrait: Int,
        private val frameRate: Int,
        private val kBitRate: Int,
        private val listener: FrameEncodeListener
): IRotationWatcher.Stub() {
    companion object {
        private const val TAG = "ScreenEncoder"
    }

    private val hasPendingRotationChange = AtomicBoolean(false)
    private val keepRunning = AtomicBoolean(true)

    interface FrameEncodeListener {
        fun onFrame(buffer: ByteBuffer, len: Int, presentationTimeUs: Long)
        fun onFrameSizeWillChange()
    }

    override fun onRotationChanged(rotation: Int) {
        Log.i(TAG, String.format("onRotationChanged, rotation = %d", rotation))
        hasPendingRotationChange.set(true)
    }

    private fun consumeRotationChange(): Boolean {
        return hasPendingRotationChange.getAndSet(false)
    }

    /**
     * Get the size of the output video
     */
    private fun getVideoSize(): Point {
        val size = Point(videoWidthInPortrait, videoHeightInPortrait)
        val rotation = Display.getRotation()
        if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) {
            val swap = size.x
            size.x = size.y
            size.y = swap
        }
        return size
    }

    fun run() {
        Display.registerRotationWatcher(this)
        try {
            var shouldRestart = true
            while (shouldRestart) {
                val videoSize = getVideoSize()
                Log.i(TAG, String.format("Creating new MediaCodec with %dx%d", videoSize.x, videoSize.y))
                val codec = MediaCodec.createByCodecName(codecName)
                val mediaFormat = createEncoderFormat(videoSize)
                codec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                val surface = codec.createInputSurface()
                val virtualDisplay = VirtualDisplayFactory.createVirtualDisplay(
                        "droidpuppet",
                        videoSize,
                        surface
                )
                codec.start()
                try {
                    shouldRestart = encode(codec, listener)
                } finally {
                    codec.stop()
                    virtualDisplay.release()
                    codec.release()
                    surface.release()
                }
            }
        } finally {
            Display.removeRotationWatcher(this)
        }
    }

    fun stop() {
        keepRunning.set(false)
    }

    private fun encode(codec: MediaCodec, listener: FrameEncodeListener): Boolean {
        val buf = ByteArray(kBitRate * 1000 / 8)
        val bufferInfo = MediaCodec.BufferInfo()
        var isEOS = false

        while (!isEOS && keepRunning.get()) {
            val outputBufferId = codec.dequeueOutputBuffer(bufferInfo, -1)
            try {
                isEOS = bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                if (consumeRotationChange()) {
                    // restart encoder. discard remaining outputs.
                    listener.onFrameSizeWillChange()
                    break
                }
                if (outputBufferId >= 0) {
                    val outputBuffer = codec.getOutputBuffer(outputBufferId)
                    while (outputBuffer!!.hasRemaining()) {
                        val remaining = outputBuffer.remaining()
                        val len = Math.min(buf.size, remaining)
                        listener.onFrame(outputBuffer, len, bufferInfo.presentationTimeUs)
                    }
                }
            } finally {
                if (outputBufferId >= 0) {
                    codec.releaseOutputBuffer(outputBufferId, false)
                }
            }
        }

        return keepRunning.get() && !isEOS
    }

    private fun createEncoderFormat(videoSize: Point): MediaFormat {
        val mediaFormat = MediaFormat.createVideoFormat(format, videoSize.x, videoSize.y)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mediaFormat.setInteger(MediaFormat.KEY_LATENCY, 0)
        }
        mediaFormat.setLong(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, (1000000 / frameRate).toLong())
        // mediaFormat.setInteger(MediaFormat.KEY_OUTPUT_REORDER_DEPTH, 0)
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, kBitRate * 1000)
        mediaFormat.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR)
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        return mediaFormat
    }
}