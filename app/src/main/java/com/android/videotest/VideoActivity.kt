package com.android.videotest

import android.os.Bundle
import android.util.Log
import android.view.TextureView
import android.view.View
import android.widget.Toast
import com.android.ffmpegproject.ECameraB
import com.android.ffmpegproject.VideoRecorder

/**
 *
 * Author：hdd
 * Date：2020/4/24
 * Description：
 *
 */
class VideoActivity: PermissionActivity() {
    private var mRecorder: VideoRecorder? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkPermission()
    }

    override fun onCheckPermissionResult(success: Boolean, failPermissions: List<String>?) {
        if (success) {
            val videoPath = "/mnt/sdcard/video_capture_path_test.mp4"
            mRecorder = VideoRecorder(this, ECameraB.TYPE_BACK, videoPath)
            val textureView = findViewById<TextureView>(R.id.texture_view)
            mRecorder!!.setTextureView(textureView)
            findViewById<View>(R.id.start_record).setOnClickListener { mRecorder!!.startVideoRecord() }
            findViewById<View>(R.id.stop_record).setOnClickListener { mRecorder!!.stopVideoRecord() }
            return
        }
        Toast.makeText(this, "permission denied", Toast.LENGTH_SHORT).show()
        for (fail in failPermissions!!) {
            Log.e("MainActivity", "permission denied:$fail")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mRecorder?.stopVideoRecord()
    }
}