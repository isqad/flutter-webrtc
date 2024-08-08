package com.cloudwebrtc.webrtc.video

import android.content.Context;
import android.graphics.Bitmap
import android.graphics.BitmapFactory

import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.YuvImage
import android.graphics.Rect
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
import java.io.ByteArrayOutputStream

import com.pixpark.gpupixel.GPUPixel
import com.pixpark.gpupixel.GPUPixelSourceImage
import com.pixpark.gpupixel.filter.LipstickFilter

internal class BeautifyVideoProcessor constructor(val context: Context) : VideoProcessor {
    companion object {
        private const val TAG = "BeautifyVideoProcessor"
    }

    private var videoSink: VideoSink? = null

    private val t: LooperThread = LooperThread()

    private var frameCounter: Int = 0

    private val lipstickFilter: LipstickFilter = LipstickFilter()

    override fun setSink(sink: VideoSink?) {
        Log.d(TAG, "Set sink $sink")
        videoSink = sink
    }

    override fun onFrameCaptured(frame: VideoFrame?) {
        if (frame == null) {
            return
        }
        frame.retain()

        val stream = ByteArrayOutputStream()

        val buffer = frame.buffer
        val i420Buffer = buffer.toI420()
        val rotation = frame.rotation

        val image = i420ToYUVImage(i420Buffer)
        if (image == null) {
            Log.d(TAG, "YUV IMAGE IS NULL!!!")
        } else {
            val rect = Rect(0, 0, image.width, image.height)
            image.compressToJpeg(rect, 100, stream)
            val imageBytes: ByteArray = stream.toByteArray()
            var bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            Log.d(TAG, "Bitmap: ${bitmap.height} ${bitmap.byteCount}")

            val matrix = Matrix()
            matrix.postRotate(rotation.toFloat())

            bitmap = Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width, bitmap.height, matrix,
                true
            )

            val source = GPUPixelSourceImage(bitmap)
            source.addTarget(lipstickFilter)
        }

        frame.release()

        stream.close()

        videoSink?.onFrame(frame)
    }

    /** Notify if the capturer have been started successfully or not.  */
    override fun onCapturerStarted(success: Boolean) {
        Log.d(TAG, "On Capture started $success")

        if (!success) {
          return
        }

        GPUPixel.setContext(context);
        t.start()
    }

    /** Notify that the capturer has been stopped.  */
    override fun onCapturerStopped() {
        Log.d(TAG, "On Capture stopped ")

        t.mHandler?.looper?.quit()
    }

    private fun i420ToYUVImage(i420Buffer: VideoFrame.I420Buffer?): YuvImage? {
        if (i420Buffer == null) return null

        val y = i420Buffer.dataY
        val u = i420Buffer.dataU
        val v = i420Buffer.dataV
        val width = i420Buffer.width
        val height = i420Buffer.height
        val strides = intArrayOf(
             i420Buffer.strideY,
             i420Buffer.strideU,
             i420Buffer.strideV
         )
         val chromaWidth = (width + 1) / 2
         val chromaHeight = (height + 1) / 2
         val minSize = width * height + chromaWidth * chromaHeight * 2

         val yuvBuffer = ByteBuffer.allocateDirect(minSize)

         // NV21 is the same as NV12, only that V and U are stored in the reverse oder
         // NV21 (YYYYYYYYY:VUVU)
         // NV12 (YYYYYYYYY:UVUV)
         // Therefore we can use the NV12 helper, but swap the U and V input buffers
         YuvHelper.I420ToNV12(
             y,
             strides[0],
             v,
             strides[2],
             u,
             strides[1],
             yuvBuffer,
             width,
             height
         )

        return YuvImage(yuvBuffer.array(), ImageFormat.NV21, width, height, null)
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
