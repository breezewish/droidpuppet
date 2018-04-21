package org.breeswish.droidpuppet

import android.util.Log
import org.breeswish.droidpuppet.media.ScreenEncoder
import org.breeswish.droidpuppet.network.Connection
import java.nio.ByteBuffer

class Main: ScreenEncoder.FrameEncodeListener, Connection.CloseListener {
    private var connection: Connection? = null
    private var screenEncoder: ScreenEncoder? = null

    override fun onFrame(buffer: ByteBuffer, len: Int, presentationTimeUs: Long) {
        connection!!.writeVideoFrame(buffer, len, presentationTimeUs)
    }

    override fun onFrameSizeWillChange() {
        connection!!.writeStreamRestartFrame()
    }

    private fun run(token: String) {
        Log.i(TAG, "run")

        connection = Connection.open(CONNECTION_ADDRESS, token, this)

        val streamRequest = connection!!.receiveStreamRequest()
        Log.i(TAG, String.format("Receive stream request: %s", streamRequest.toString()))

        screenEncoder = ScreenEncoder(
                streamRequest.encoder,
                streamRequest.format,
                streamRequest.videoWidthInPortrait,
                streamRequest.videoHeightInPortrait,
                streamRequest.frameRate,
                streamRequest.kBitRate,
                this
        )
        screenEncoder!!.run()

        connection!!.close()
    }

    override fun onConnectionClose() {
        if (screenEncoder != null) {
            screenEncoder!!.stop()
        }
    }

    companion object {
        private const val TAG = "Main"
        private const val CONNECTION_ADDRESS = "droidpuppet_endpoint"

        @JvmStatic
        fun main(args: Array<String>) {
            Thread.setDefaultUncaughtExceptionHandler { t, e -> e.printStackTrace() }
            assert(args.isNotEmpty())
            val token = args[0]
            Main().run(token)
        }
    }
}