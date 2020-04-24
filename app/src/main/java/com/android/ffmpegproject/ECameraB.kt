package com.android.ffmpegproject

import CameraSize
import android.content.Context
import android.content.res.Configuration
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.hardware.camera2.CameraCharacteristics
import android.util.Log
import android.view.TextureView
import java.util.*


/**
 *
 * Author：dodo
 * Date：2019/11/5
 * Description：camera
 *
 */
class ECameraB(context: Context, cameraType: Int) {
    companion object{
        const val TYPE_FRONT = CameraCharacteristics.LENS_FACING_FRONT
        const val TYPE_BACK = CameraCharacteristics.LENS_FACING_BACK
        private val TAG = ECameraB::class.java.simpleName
    }

    private val mContext = context
    private val mCameraType = cameraType
    private var mCamera: Camera? = null
    private val mCameraId: Int
    private var isCameraOpened: Boolean = false
    private var mVideoSize: Camera.Size? = null
    private var mFrameRate = 15
    private var mTextureView: TextureView? = null

    init {
        mCameraId = if(cameraType == TYPE_FRONT) Camera.CameraInfo.CAMERA_FACING_FRONT else Camera.CameraInfo.CAMERA_FACING_BACK
    }

    fun openCamera(): Boolean{
        try {
            mCamera = Camera.open(mCameraId)
            val params = mCamera?.parameters
            params?.previewFormat = ImageFormat.NV21

            val videoSizes = params?.supportedVideoSizes
            val videoSize = videoSizes?.let { chooseVideoSize(it) }
            mVideoSize = videoSize
            params?.setPreviewSize(videoSize?.width!!, videoSize.height)
            params?.setPreviewFpsRange(mFrameRate * 1000, mFrameRate * 1000)
            mCamera?.parameters = params

            mCamera?.setPreviewCallback(mPreviewCallback)
            mCamera?.setErrorCallback(mErrorCallback)

            val orientation = mContext.resources.configuration.orientation
            if(orientation == Configuration.ORIENTATION_LANDSCAPE){
                mCamera?.setDisplayOrientation(0)
            }else{
                mCamera?.setDisplayOrientation(90)
            }

            val surface = mTextureView?.surfaceTexture
            mCamera?.setPreviewTexture(surface)
            mCamera?.startPreview()
            isCameraOpened = true
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            val error = e.toString()
            Log.e(TAG, "open camera:$mCameraType,$error")
            return false
        }
    }

    fun setVideoFrameRate(frameRate: Int){
        mFrameRate = frameRate;
    }

    fun setTextureView(textureView: TextureView){
        mTextureView = textureView
    }

    private var videoEncoder: VideoEncoder? = null
    fun setEncoder(encoder: VideoEncoder){
        videoEncoder = encoder
    }

    private val mPreviewCallback = object : Camera.PreviewCallback{
        override fun onPreviewFrame(data: ByteArray?, camera: Camera?) {
//            Log.i(TAG, "onPreviewFrame:$mCameraType")
            if (data != null) {
                encodeVideo(data)
            }
        }
    }

    private val mErrorCallback = object : Camera.ErrorCallback{
        override fun onError(error: Int, camera: Camera?) {
            Log.e(TAG, "onError:$error,camera=$mCameraType")
        }
    }

    fun closeCamera(){
        if(!isCameraOpened){
            return
        }

        mCamera?.setPreviewTexture(null)
        mCamera?.setPreviewCallback(null)
        isCameraOpened = false
        mCamera?.stopPreview()
        mCamera?.release()
    }

    fun isOpened(): Boolean{
        return isCameraOpened
    }

    fun getOutputVideoSize(): CameraSize {
        if(mVideoSize != null){
            return CameraSize(mVideoSize!!.width, mVideoSize!!.height)
        }

        return CameraSize(1280, 720)
    }

    fun chooseVideoSize(outputSizes: List<Camera.Size>): Camera.Size{
        Collections.sort(outputSizes, CompareSizesByArea())

        //select a size of 720p
        val minWidth = 1280
        val minHeight = 720
        val count = outputSizes.size
//        for (i in 0 until count) {
//            val outSize = outputSizes[i]
//            if (outSize.height >= minHeight && outSize.width >= minWidth) {
//                return outSize
//            }
////            Log.i(TAG, "width=${outSize.width},height=${outSize.height}")
//        }

//        return outputSizes[count / 2]
        return outputSizes[count -3]
    }

    internal class CompareSizesByArea :
        Comparator<Camera.Size> {
        override fun compare(
            lhs: Camera.Size,
            rhs: Camera.Size
        ): Int {
            return java.lang.Long.signum(
                lhs.width.toLong() * lhs.height -
                        rhs.width.toLong() * rhs.height
            )
        }
    }

    private fun encodeVideo(buffer: ByteArray){
//        encode(buffer)
        videoEncoder?.encode(buffer)
    }

}