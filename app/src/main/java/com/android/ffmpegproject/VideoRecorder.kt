package com.android.ffmpegproject

import android.content.Context
import android.media.MediaMuxer
import android.os.Environment
import android.util.Log
import android.view.TextureView

/**
 *
 * Author：dodo
 * Date：2019/10/24
 * Description：audio encoder
 *
 */
class VideoRecorder(context: Context, cameraType: Int, path: String) {
    private val mContext: Context = context
    private val mCameraType = cameraType
    private val mCamera: ECameraB
    private val mAudio: EAudio
    private val mVideoEncoder: VideoEncoder
    private val mAudioEncoder: AudioEncoder
    private val mMuxerHelper: MuxerHelper
    private var isRecordStarted = false
    private val mVideoPath = path

    init {
        mMuxerHelper = MuxerHelper(mVideoPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4, cameraType)

        mVideoEncoder = VideoEncoder(mMuxerHelper)
        mAudioEncoder = AudioEncoder(mMuxerHelper)

        mCamera = ECameraB(mContext, mCameraType)
        mCamera.setVideoFrameRate(VIDEO_FRAME_RATE)
        mCamera.setEncoder(mVideoEncoder)

        mAudio = EAudio()
        mAudio.setEncoder(mAudioEncoder)
    }

    fun setTextureView(textureView: TextureView){
        mCamera.setTextureView(textureView)
    }

    fun startVideoRecord(){
        val state = Environment.getExternalStorageState()
        if(Environment.MEDIA_MOUNTED != state){
            Log.e(TAG, "media mounted state error:$state")
            return
        }

        startRecordMedia()
    }

    fun stopVideoRecord(){
        if(!isRecordStarted){
            Log.i(TAG, "recorder is stopped...")
            return
        }

        isRecordStarted = false

        mCamera.closeCamera()
        mAudio.stopRecord()

        mVideoEncoder.stop()
        mAudioEncoder.stop()
    }

    private fun startRecordMedia(){
        if(isRecordStarted){
            Log.i(TAG, "recorder is started...")
            return
        }

        mCamera.openCamera()
        if(!mCamera.isOpened()){
            Log.e(TAG, "camera open fail:cameraType=$mCameraType")
            return
        }

        //open camera success
        isRecordStarted = true

        val videoSize = mCamera.getOutputVideoSize()//video size
        val videoBitRate = videoSize.width * videoSize.height * 2//video bit rate

        val params = RecorderParams()
        params.videoPath = mVideoPath
        params.videoWidth = videoSize.width
        params.videoHeight = videoSize.height
        params.videoBitRate = videoBitRate
        params.videoFrameRate = VIDEO_FRAME_RATE
        mVideoEncoder.start(params)

        mAudio.startRecord()
        params.audioBitRate = AUDIO_BIT_RATE
        params.audioSampleRate = AUDIO_SAMPLE_RATE
        mAudioEncoder.start(params)
        Log.i(TAG, "init:$params")
    }

    companion object{
        private val TAG = VideoRecorder::class.java.simpleName
        private const val VIDEO_FRAME_RATE = 30//20fps

        private const val AUDIO_BIT_RATE = 64000
        private const val AUDIO_SAMPLE_RATE = 44100
    }
}