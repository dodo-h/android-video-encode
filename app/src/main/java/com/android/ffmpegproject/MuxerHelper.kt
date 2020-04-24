package com.android.ffmpegproject

import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import java.nio.ByteBuffer

/**
 *
 * Author：dodo
 * Date：2020/4/17
 * Description：
 *
 */
class MuxerHelper(path: String, format: Int, cameraType: Int) {
    companion object{
        const val TAG = "MuxerHelper"
    }

    private val muxer: MediaMuxer = MediaMuxer(path, format)
    private var videoTrack = 0
    private var audioTrack = 0
    private var isStop = false
    private var trackCount = 0

    private var audioPts = 0L
    private var videoPts = 0L

    init {
        muxer.setOrientationHint(if (cameraType == ECameraB.TYPE_BACK) 90 else 270)
    }

    fun writeData(outBuffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo, isVideo: Boolean){
//        Log.i(TAG, "write data")
        synchronized(this){
            if(trackCount > 1){
                val track = if (isVideo) videoTrack else audioTrack
                muxer.writeSampleData(track, outBuffer, bufferInfo)
            }
        }
    }

    fun addTrack(mediaForamt: MediaFormat, isVideo: Boolean){
        synchronized(this){
            val trackId = muxer.addTrack(mediaForamt)

            if(isVideo){
                videoTrack = trackId
            }else{
                audioTrack = trackId
            }

            ++trackCount

            if(trackCount > 1){
                muxer.start()
            }
            Log.i(TAG, "add track:isVideo=$isVideo,trackId=$trackId")
        }
    }

    fun stop(){
        synchronized(this){
            if(isStop){
                return
            }

            Log.i(TAG, "stop")
            isStop = true
            muxer.stop()
            muxer.release()
        }
    }

    fun updateVideoPts(pts: Long){
        videoPts = pts
    }

    fun updateAudioPts(pts: Long){
        audioPts = pts
    }

    fun getVideoPts() = videoPts

    fun getAudioPts() = audioPts
}