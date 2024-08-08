package com.cloudwebrtc.webrtc.video

import android.graphics.ImageFormat
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import org.webrtc.VideoFrame
import org.webrtc.VideoProcessor
import org.webrtc.VideoSink
import org.webrtc.YuvHelper
import java.nio.ByteBuffer
import java.util.*

internal class BeautifyVideoProcessor : VideoProcessor {
    companion object {
        private const val TAG = "BeautifyVideoProcessor"
        //private const val WRAPAROUND_COUNTER_VAL = 30
        //private const val DETECT_FACE_EVERY_FRAME = 5
    }

    private var videoSink: VideoSink? = null

    private val t: LooperThread = LooperThread()

    private var frameCounter: Int = 0

    override fun setSink(sink: VideoSink?) {
        Log.d(TAG, "Set sink $sink")
        videoSink = sink
    }

    override fun onFrameCaptured(frame: VideoFrame?) {
        if (frame == null) {
            return
        }
        frame.retain()

        val buffer = frame.buffer
        val i420Buffer = buffer.toI420()
        val rotation = frame.rotation

        t.mHandler?.post {
            try {
                // if (i420Buffer != null) {
                //     val image = inputImage(i420Buffer, rotation)
                //     faceDetector?.process(image)
                //         ?.addOnSuccessListener { faces ->
                //             if (faces.isEmpty()) {
                //                 noFacesDetectedCallback()
                //             } else {
                //                 faceDetectedCallback()
                //             }
                //         }
                //         ?.addOnFailureListener { e ->
                //             e.printStackTrace()
                //         }
                // }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            } finally {
                i420Buffer?.release()
            }
        }
        frame.release()

        videoSink?.onFrame(frame)
    }

    /** Notify if the capturer have been started successfully or not.  */
    override fun onCapturerStarted(success: Boolean) {
        Log.d(TAG, "On Capture started $success")

        if (!success) {
          return
        }
        //t.start()
    }

    /** Notify that the capturer has been stopped.  */
    override fun onCapturerStopped() {
        Log.d(TAG, "On Capture stopped ")

        // t.mHandler?.looper?.quit()
    }

    // private fun inputImage(i420Buffer: VideoFrame.I420Buffer, rotation: Int) : InputImage {
    //     val y = i420Buffer.dataY
    //     val u = i420Buffer.dataU
    //     val v = i420Buffer.dataV
    //     val width = i420Buffer.width
    //     val height = i420Buffer.height
    //     val strides = intArrayOf(
    //         i420Buffer.strideY,
    //         i420Buffer.strideU,
    //         i420Buffer.strideV
    //     )
    //     val chromaWidth = (width + 1) / 2
    //     val chromaHeight = (height + 1) / 2
    //     val minSize = width * height + chromaWidth * chromaHeight * 2

    //     val yuvBuffer = ByteBuffer.allocateDirect(minSize)
    //     // NV21 is the same as NV12, only that V and U are stored in the reverse oder
    //     // NV21 (YYYYYYYYY:VUVU)
    //     // NV12 (YYYYYYYYY:UVUV)
    //     // Therefore we can use the NV12 helper, but swap the U and V input buffers
    //     YuvHelper.I420ToNV12(
    //         y,
    //         strides[0],
    //         v,
    //         strides[2],
    //         u,
    //         strides[1],
    //         yuvBuffer,
    //         width,
    //         height
    //     )

    //     // For some reason the ByteBuffer may have leading 0. We remove them as
    //     // otherwise the
    //     // image will be shifted
    //     val cleanedArray =
    //         Arrays.copyOfRange(yuvBuffer.array(), yuvBuffer.arrayOffset(), minSize)

    //     return InputImage.fromByteArray(cleanedArray, width, height, rotation,
    //         ImageFormat.NV21)
    // }

    class LooperThread : Thread() {
        var mHandler: Handler? = null

        override fun run() {
            super.run()

            Looper.prepare()
            mHandler = FrameHandler()
            Looper.loop()
        }
    }

    class FrameHandler: Handler() {
        override fun handleMessage(msg: Message) {
            // process incoming messages here
        }
    }
}
