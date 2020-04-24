package com.android.ffmpegproject

import android.media.*
import android.util.Log
import java.util.concurrent.LinkedBlockingQueue

/**
 *
 * Author：dodo
 * Date：2020/4/15
 * Description：video encoder,use media codec
 *
 */
class VideoEncoder(muxer: MuxerHelper){
    private val mMimeType = MediaFormat.MIMETYPE_VIDEO_AVC
    private lateinit var mRecorderParams: RecorderParams
    private lateinit var mMediaCodec: MediaCodec
    private lateinit var mOutputTask: OutputTask
    private lateinit var mInputTask: InputTask
    private var mMediaMuxer = muxer

    private var frameIndex = 0L

    companion object{
        const val TAG = "video_encoder"
    }

    fun start(params: RecorderParams){
        mRecorderParams = params
        frameIndex = 0L
//        val count = MediaCodecList.getCodecCount();
//        for(i in 0 until  count - 1){
//            val codecInfo = MediaCodecList.getCodecInfoAt(i)
//            if(codecInfo.isEncoder){
//                val types = codecInfo.supportedTypes
//                Log.i(TAG, "${codecInfo.name}:${Arrays.toString(types)}")
//            }
//        }

        try {
            mMediaCodec = MediaCodec.createEncoderByType(mMimeType)
        } catch (e: Exception) {
            Log.i(TAG, "init error:$e")
            return
        }

        val format = MediaFormat.createVideoFormat(mMimeType, mRecorderParams.videoWidth, mRecorderParams.videoHeight)
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar)
        format.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR)
        format.setInteger(MediaFormat.KEY_BIT_RATE, mRecorderParams.videoBitRate)
        format.setInteger(MediaFormat.KEY_FRAME_RATE, mRecorderParams.videoFrameRate)
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)

        mMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        mMediaCodec.start()

        mInputTask = InputTask(mMediaCodec)
        mOutputTask = OutputTask(mMediaCodec)

        mInputTask.start()
        mOutputTask.start()
        Log.i(TAG, "init end.")
    }

    fun encode(data: ByteArray){
        if(mMediaMuxer.getVideoPts() + 500000 < mMediaMuxer.getAudioPts()){
            frameIndex += 5
        }else{
            ++frameIndex
        }

        val pts = frameIndex * 1000000 / mRecorderParams.videoFrameRate
        mMediaMuxer.updateVideoPts(pts)

        mInputTask.writeData(FrameData(data, pts))
    }

    fun stop(){
        mInputTask.stop()
        mOutputTask.stop()
    }

    internal inner class InputTask(codec: MediaCodec): Runnable{
        private val mediaCodec = codec
        private var taskThread: Thread? = null
        private val inputQueue: LinkedBlockingQueue<FrameData> = LinkedBlockingQueue()
        private var isInputEnd = false
        private var isRun = false

        fun start(){
            taskThread = Thread(this)
            taskThread?.start()
            isRun = true
            isInputEnd = false
        }

        fun stop(){
            isRun = false
        }

        fun writeData(data: FrameData){
            inputQueue.offer(data)
        }

        fun isInputEnd(): Boolean{
            return isInputEnd
        }

        override fun run() {
            Log.i(TAG, "input task start.")
            while(true){
                var frame = inputQueue.peek()

                if(frame != null){
                    val inputIndex = mediaCodec.dequeueInputBuffer(5000)//5ms
                    if(inputIndex >= 0){
                        val data = nv21toNV12(inputQueue.poll())
                        val inputBuffer = mediaCodec.getInputBuffer(inputIndex)
                        inputBuffer.clear()
                        inputBuffer.put(data)

                        mediaCodec.queueInputBuffer(inputIndex, 0, data.size, frame.pts, 0)
                    }
                }

                if(!isRun && inputQueue.size < 1){
                    Log.i(TAG, "input task end.")
                    isInputEnd = true
                    break
                }
            }

        }
    }

    internal inner class OutputTask(codec: MediaCodec): Runnable{
        private val mediaCodec = codec
        private var taskThread: Thread? = null

        fun start(){
            taskThread = Thread(this)
            taskThread?.start()
        }

        fun stop(){

        }

        override fun run() {
            Log.i(TAG, "output task start.")
            while(true){
                val bufferInfo = MediaCodec.BufferInfo()
                val outputIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 5000)//5ms

                if(outputIndex >= 0){
                    val outputBuffer = mediaCodec.getOutputBuffer(outputIndex)
                    if(outputBuffer != null){
                        val data = ByteArray(bufferInfo.size)
                        outputBuffer.get(data)
                        outputBuffer.position(bufferInfo.offset)
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                        mMediaMuxer.writeData(outputBuffer, bufferInfo, true)

                        mediaCodec.releaseOutputBuffer(outputIndex, false)
                    }
                }else if(outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
                    Log.i(TAG, "####################################info output format changed.")
                    mMediaMuxer.addTrack(mediaCodec.outputFormat, true)
                }

                val isEnd = mInputTask.isInputEnd()
                if(isEnd == null || isEnd){
                    Log.i(TAG, "output task end.")
                    mMediaMuxer.stop()
                    break
                }
            }
        }
    }


    fun nv21toNV12(frame: FrameData): ByteArray{
        val nv21 = frame.data
        val size = nv21.size
        val nv12 = ByteArray(size)
        val len = size * 4 / 6
        System.arraycopy(nv21, 0, nv12, 0, len)

        var i = len
        while(i < size - 1){
            nv12[i] = nv21[i + 1]
            nv12[i + 1] = nv21[i]
            i += 2
        }

        return nv12
    }
}