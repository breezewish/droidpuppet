package org.breeswish.droidpuppet.network

import android.net.LocalSocket
import android.net.LocalSocketAddress
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.Log
import com.google.protobuf.ByteString
import com.google.protobuf.MessageLite
import com.jaredrummler.android.device.DeviceName
import org.breeswish.droidpuppet.ProtocolProtos
import org.breeswish.droidpuppet.device.Display
import org.breeswish.droidpuppet.media.Codecs
import java.io.Closeable
import java.nio.ByteBuffer

class Connection(
        private val socket: LocalSocket,
        connectionToken: String,
        private val closeListener: CloseListener
): Closeable {
    interface CloseListener {
        fun onConnectionClose()
    }

    private var outputHandlerThread = HandlerThread("ConnectionOutputThread")
    private var outputHandler: Handler

    init {
        outputHandlerThread.start()
        outputHandler = Handler(outputHandlerThread.looper, object : Handler.Callback {
            override fun handleMessage(msg: Message?): Boolean {
                if (msg != null) {
                    try {
                        val protoMessage = msg.obj as MessageLite
                        protoMessage.writeDelimitedTo(socket.outputStream)
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                        close()
                        return true
                    }
                }
                return false
            }
        })

        // Once connection established, write greeting
        val greetingBuilder = ProtocolProtos.ClientGreeting.newBuilder()
        val displaySize = Display.getDisplaySize(false)
        greetingBuilder.connectionToken = connectionToken
        greetingBuilder.widthInPortrait = displaySize.x
        greetingBuilder.heightInPortrait = displaySize.y
        greetingBuilder.systemSdkVersion = Build.VERSION.SDK_INT
        greetingBuilder.systemReleaseVersion = Build.VERSION.RELEASE
        greetingBuilder.deviceName = DeviceName.getDeviceName()
        val encoders = Codecs.getVideoEncoders()
        for (pair in encoders) {
            val encodersBuilder = ProtocolProtos.Encoders
                    .newBuilder()
            for (info in pair.value) {
                encodersBuilder.addName(info.name)
            }
            greetingBuilder.putVideoEncoders(pair.key, encodersBuilder.build())
        }
        val greeting = greetingBuilder.build()

        Log.i(TAG, String.format("Sending greeting: %s", greeting.toString()))
        asyncWrite(greeting)
    }

    override fun close() {
        outputHandlerThread.quit()
        socket.shutdownInput()
        socket.shutdownOutput()
        socket.close()
        closeListener.onConnectionClose()
    }

    fun receiveStreamRequest(): ProtocolProtos.StreamRequest {
        val streamRequest = ProtocolProtos.StreamRequest.parseDelimitedFrom(socket.inputStream)
        Log.i(TAG, String.format("Receive stream request: %s", streamRequest.toString()))
        return streamRequest
    }

    fun receiveControlRequest() {
        TODO()
    }

    fun writeVideoFrame(buffer: ByteBuffer, len: Int, presentationTimeUs: Long) {
        val builder = ProtocolProtos.Frame.newBuilder()
        builder.restart = false
        builder.timeUs = presentationTimeUs
        builder.data = ByteString.copyFrom(buffer, len)

        asyncWrite(builder.build())
    }

    fun writeStreamRestartFrame() {
        val builder = ProtocolProtos.Frame.newBuilder()
        builder.restart = true

        asyncWrite(builder.build())
    }

    /**
     * Post message into the writer thread
     */
    private fun asyncWrite(message: MessageLite) {
        outputHandler.obtainMessage(0, message).sendToTarget()
    }

    companion object {
        private const val TAG = "Connection"

        fun open(localAddress: String, connectionToken: String, closeListener: CloseListener): Connection {
            val socket = LocalSocket(LocalSocket.SOCKET_STREAM)
            val address = LocalSocketAddress(localAddress, LocalSocketAddress.Namespace.ABSTRACT)
            socket.connect(address)
            Log.i(TAG, String.format("Connected to localabstract:%s", localAddress))
            return Connection(socket, connectionToken, closeListener)
        }
    }
}