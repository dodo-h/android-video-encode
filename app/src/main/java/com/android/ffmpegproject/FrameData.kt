package com.android.ffmpegproject

/**
 *
 * Author：dodo
 * Date：2020/4/21
 * Description：
 *
 */
class FrameData(dt: ByteArray, p: Long) {
    var data = dt
    var pts = p
}