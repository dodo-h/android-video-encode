package com.android.ffmpegproject

import android.media.*
import android.util.Log
import java.util.concurrent.LinkedBlockingQueue

/**
 *
 * Author：dodo
 * Date：2020/4/17
 * Description：
 *
 */
class AudioEncoder(muxer: MuxerHelper) {
    private val mMimeType = MediaFormat.MIMETYPE_AUDIO_AAC
    private lateinit var mRecorderParams: RecorderParams
    private lateinit var mMediaCodec: MediaCodec
    private lateinit var mEncodeTask: EncodeTask
    private var mMediaMuxer = muxer
    private val mPcmEncoding = AudioFormat.ENCODING_PCM_16BIT
    private var sampleSize = 0L

    companion object{
        const val TAG = "audio_encoder"
    }

    fun start(params: RecorderParams){
        mRecorderParams = params
        sampleSize = 0L

        try {
            mMediaCodec = MediaCodec.createEncoderByType(mMimeType)
        } catch (e: Exception) {
            Log.e(TAG, "init error:$e")
            return
        }

        val format = MediaFormat.createAudioFormat(mMimeType, mRecorderParams.audioSampleRate, 1)
        format.setInteger(MediaFormat.KEY_BIT_RATE, mRecorderParams.audioBitRate)
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 100 * 1024)
        format.setInteger(MediaFormat.KEY_PCM_ENCODING, mPcmEncoding)

        mMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mMediaCodec.start()

        mEncodeTask = EncodeTask(mMediaCodec)

        mEncodeTask.start()
        Log.i(TAG, "init end.")
    }

    fun encode(data: ByteArray){
        sampleSize += data.size / mPcmEncoding
        val pts = sampleSize * 1000000 / mRecorderParams.audioSampleRate
        mMediaMuxer.updateAudioPts(pts)

        mEncodeTask.writeData(FrameData(data, pts))
    }

    fun stop(){
        mEncodeTask.stop()
    }

    private inner class EncodeTask(codec: MediaCodec): Runnable{
        private var encodeThread: Thread? = null
        private val mediaCodec = codec
        private val encodeQueue = LinkedBlockingQueue<FrameData>()
        private var isRun = false

        fun start(){
            encodeThread = Thread(this)
            encodeThread?.start()
            isRun = true
        }

        fun writeData(data: FrameData){
            encodeQueue.offer(data)
        }

        fun stop(){
            isRun = false
        }

        override fun run() {
            Log.i(TAG, "encode task start.")
            while(true){
                var data = encodeQueue.peek()
                if(data != null){
                    val inputIndex = mediaCodec.dequeueInputBuffer(5000)//5ms
                    if(inputIndex >= 0){
                        data = encodeQueue.poll()
                        val inputBuffer = mediaCodec.getInputBuffer(inputIndex)
                        inputBuffer.clear()
                        inputBuffer.put(data.data)

                        mediaCodec.queueInputBuffer(inputIndex, 0, data.data.size, data.pts, 0)

                        val bufferInfo = MediaCodec.BufferInfo()
                        val outputIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, -1)

                        if(outputIndex >= 0){
                            val outputBuffer = mediaCodec.getOutputBuffer(outputIndex)
                            if(outputBuffer != null){
                                val data = ByteArray(bufferInfo.size)
                                outputBuffer.get(data)
                                outputBuffer.position(bufferInfo.offset)
                                outputBuffer.limit(bufferInfo.offset + bufferInfo.size)

                                mMediaMuxer.writeData(outputBuffer, bufferInfo, false)
                                mediaCodec.releaseOutputBuffer(outputIndex, false)
                            }
                        }else if(outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
                            Log.i(TAG, "####################################info output format changed.")
                            mMediaMuxer.addTrack(mediaCodec.outputFormat, false)
                        }
                    }
                }

                if(!isRun && encodeQueue.size < 1){
                    mMediaMuxer.stop()
                    Log.i(TAG, "encode task end.")
                    break
                }
            }
        }
    }
}