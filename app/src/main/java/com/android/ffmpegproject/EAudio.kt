package com.android.ffmpegproject

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log

/**
 *
 * Author：dodo
 * Date：2019/9/23
 * Description：
 *
 */
class EAudio{
    private val TAG = EAudio::class.simpleName
    private val mAudioSource = MediaRecorder.AudioSource.MIC
    private val mSampleRateInHz = 44100
    private val mChannelConfig = AudioFormat.CHANNEL_IN_MONO
    private val mAudioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val mBufferSize = 2048
    private var mBufferSizeInBytes = 0
    private var mAudioRecord: AudioRecord? = null
    private var isRecording = false
    private var mAudioEncoder: AudioEncoder? = null

    fun startRecord(){
        if(isRecording){
            return
        }

        isRecording = true
        mBufferSizeInBytes = AudioRecord.getMinBufferSize(mSampleRateInHz, mChannelConfig, mAudioFormat)
        mAudioRecord = AudioRecord(mAudioSource, mSampleRateInHz, mChannelConfig, mAudioFormat, mBufferSizeInBytes)
        if(AudioRecord.ERROR_BAD_VALUE == mBufferSizeInBytes
            || AudioRecord.ERROR == mBufferSizeInBytes){
            Log.e(TAG, "unable to getMinBufferSize")
            isRecording = false
            return
        }

        if(mAudioRecord?.state == AudioRecord.STATE_UNINITIALIZED){
            Log.e(TAG, "AudioRecord is uninitialized")
            isRecording = false
            return
        }

        val thread = Thread(RecordTask())
        thread.start()
    }

    fun stopRecord(){
        if(!isRecording){
            return
        }

        isRecording = false
        mAudioRecord?.stop()
        mAudioRecord?.release()
    }

    fun setEncoder(encoder: AudioEncoder){
        mAudioEncoder = encoder
    }

    inner class RecordTask: Runnable{
        override fun run() {
            mAudioRecord?.startRecording()
            val buffer = ByteArray(mBufferSize)
            while(isRecording && mAudioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING){
                val result = mAudioRecord?.read(buffer, 0, mBufferSize)
                if(result!! >  0){
                    encodeAudio(buffer)
                }
            }
        }
    }

    fun encodeAudio(data: ByteArray){
        mAudioEncoder?.encode(data)
    }

}