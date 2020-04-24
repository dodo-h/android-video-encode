package com.android.ffmpegproject

/**
 *
 * Author：dodo
 * Date：2020/4/17
 * Description：
 *
 */
class RecorderParams {
    var cameraType = 0
    var videoPath = ""

    var videoWidth = 0
    var videoHeight = 0
    var videoBitRate = 0
    var videoFrameRate = 0

    var audioBitRate = 0
    var audioSampleRate = 0
    override fun toString(): String {
        return "videoWidth=$videoWidth, videoHeight=$videoHeight, videoBitRate=$videoBitRate, videoFrameRate=$videoFrameRate, audioBitRate=$audioBitRate, audioSampleRate=$audioSampleRate"
    }


}