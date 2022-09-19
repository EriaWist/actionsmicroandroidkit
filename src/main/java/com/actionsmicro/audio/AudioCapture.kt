package com.actionsmicro.audio

import android.media.*
import androidx.annotation.RequiresApi
import android.media.projection.MediaProjection
import android.os.Looper
import com.actionsmicro.airplay.airtunes.IAacEldEncoder
import com.actionsmicro.airplay.airtunes.NativeAacEldEncoder
import android.media.MediaCodec.CodecException
import kotlin.Throws
import android.os.Build
import android.os.Environment
import android.os.Process
import android.util.Log
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.lang.Exception
import java.nio.ByteBuffer

@RequiresApi(api = Build.VERSION_CODES.Q)
class AudioCapture(
    mediaProjection: MediaProjection?,
    private val mAudioDataCallBack: AudioDataCallback?,
    private val mMicStatusListener: RecorderStatusCallback?
) {
    private val BUFFER_SIZE =
        CHANNEL_COUNT * 2 * 480 //AudioRecord.getMinBufferSize(SAMPLING_RATE, CHANNEL_IN_CONFIG, AUDIO_FORMAT);
    private val mAudioRecorder: AudioRecord
    private val mAudioRecorder2: AudioRecord
    private val DUMP_AUDIO = false
    private var pos1 = 0
    private var pos2 = 0

    interface AudioDataCallback {
        fun onAudioDataAvailable(dataBuffer: ByteBuffer?, size: Int)
    }

    interface RecorderStatusCallback {
        fun onStatusChange(status: Boolean)
    }

    fun startRecording(audioRecorder: AudioRecord, statusListener: RecorderStatusCallback? = null) {
        if (AudioRecord.STATE_INITIALIZED == audioRecorder.state && AudioRecord.RECORDSTATE_STOPPED == audioRecorder.recordingState) {
            synchronized(audioRecorder) { audioRecorder.startRecording() }
            statusListener?.onStatusChange(true);
            debugLog("Start recording: $audioRecorder")

        }
    }

    fun stopRecording(audioRecorder: AudioRecord, statusListener: RecorderStatusCallback? = null) {
        if (AudioRecord.STATE_INITIALIZED == audioRecorder.state && AudioRecord.RECORDSTATE_RECORDING == audioRecorder.recordingState) {
            synchronized(audioRecorder) { audioRecorder.stop() }
            statusListener?.onStatusChange(false);
            debugLog("Recording done…")
        }
    }

    fun startRecording() {
        startRecording(mAudioRecorder)
        if (AudioRecord.STATE_INITIALIZED == mAudioRecorder.state && AudioRecord.RECORDSTATE_RECORDING == mAudioRecorder.recordingState) {
            Log.d(TAG, "Already recording")
            object : Thread() {
                var testPCMFile: FileOutputStream? = null
                var testPCMFile2: FileOutputStream? = null
                var testPCMFile3: FileOutputStream? = null
                var testAACFile: FileOutputStream? = null
                var testRawAACFile: FileOutputStream? = null
                override fun run() {
                    if (DUMP_AUDIO) {
                        try {
                            testPCMFile =
                                FileOutputStream(Environment.getExternalStorageDirectory().path + "/Download/pcm.raw")
                            testPCMFile2 =
                                FileOutputStream(Environment.getExternalStorageDirectory().path + "/Download/pcm2.raw")
                            testPCMFile3 =
                                FileOutputStream(Environment.getExternalStorageDirectory().path + "/Download/pcm3.raw")
                            testAACFile =
                                FileOutputStream(Environment.getExternalStorageDirectory().path + "/Download/adtsAAC.aac")
                            testRawAACFile =
                                FileOutputStream(Environment.getExternalStorageDirectory().path + "/Download/rawAAC.aac")
                        } catch (e: FileNotFoundException) {
                            // TODO Auto-generated catch block
                            e.printStackTrace()
                        }
                    }
                    try {
                        val pcmBuffer = ByteArray(BUFFER_SIZE)
                        val pcmBuffer2 = ByteArray(BUFFER_SIZE)
                        val audioTimestamp = AudioTimestamp()
                        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
                        Looper.prepare()
                        val audioRecorder = mAudioRecorder
                        val looper = Looper.myLooper()
                        val aacBuffer = ByteBuffer.allocate(512 * 1024)
                        val codec: IAacEldEncoder = NativeAacEldEncoder()
                        codec.setCallback(object : IAacEldEncoder.Callback() {
                            private val adtsHeader = ByteArray(7)
                            private var seq = 0

                            //   p1      p1n
                            //       p2       p2n
                            //   cur     com
                            override fun onInputBufferAvailable(codec: IAacEldEncoder, index: Int) {
                                val inputBuffer = codec.getInputBuffer(index)
                                inputBuffer.clear()
                                val ret = audioRecorder.read(
                                    pcmBuffer,
                                    pos1,
                                    BUFFER_SIZE - pos1,
                                    AudioRecord.READ_NON_BLOCKING
                                )
                                val ret2 = if (isMicRecording) mAudioRecorder2.read(
                                    pcmBuffer2,
                                    pos2,
                                    BUFFER_SIZE - pos2,
                                    AudioRecord.READ_NON_BLOCKING
                                ) else 0
                                var len = 0
                                debugLog("onInputBufferAvailable audioRecorder.read: p1,p2 = $pos1,$pos2 ret1 = $ret ret2 = $ret2")
                                if (ret < 0) {
                                    Log.e(
                                        TAG,
                                        "onInputBufferAvailable audioRecorder.read failed: $ret"
                                    )
                                    looper!!.quit()
                                } else if (ret2 < 0) {
                                    Log.e(
                                        TAG,
                                        "onInputBufferAvailable audioRecorder.read mic failed: $ret"
                                    )
                                    if (isMicRecording) {
                                        Log.d(TAG, "still recording, should not fail")
                                        looper!!.quit()
                                    }
                                } else if (ret > 0 || ret2 > 0) {
                                    audioRecorder.getTimestamp(
                                        audioTimestamp,
                                        AudioTimestamp.TIMEBASE_MONOTONIC
                                    )
                                    debugLog("onInputBufferAvailable AudioRecord timestamp position: " + audioTimestamp.framePosition + ", delta: " + (System.nanoTime() - audioTimestamp.nanoTime) / 1000000)
                                    if (isMicRecording) {
                                        val left = Math.min(pos1, pos2)
                                        if (testPCMFile != null && ret > 0) {
                                            try {
                                                testPCMFile!!.write(pcmBuffer, pos1, ret)
                                            } catch (e: IOException) {
                                                e.printStackTrace()
                                            }
                                        }
                                        if (testPCMFile2 != null && ret2 > 0) {
                                            try {
                                                testPCMFile2!!.write(pcmBuffer2, pos2, ret2)
                                            } catch (e: IOException) {
                                                e.printStackTrace()
                                            }
                                        }
                                        pos1 += ret
                                        pos2 += ret2
                                        val right = Math.min(pos1, pos2)
                                        // mixed follow
                                        // https://gist.github.com/mpuz/78e9e875df646698243affe1870dda58
                                        len = right - left
                                        val pcmBuffer3 = ByteArray(len)
                                        for (i in left until right) {
                                            val samplef1 = pcmBuffer[i].toFloat()
                                            val samplef2 = pcmBuffer2[i].toFloat()
                                            var mixed = samplef1 + samplef2
                                            // mixed *= 0.8;
                                            // hard clipping
                                            if (mixed > 128.0f) mixed = 128.0f
                                            if (mixed < -128.0f) mixed = -128.0f
                                            val outputSample = mixed.toInt().toByte()
                                            pcmBuffer3[i - left] = outputSample
                                        }
                                        inputBuffer.put(pcmBuffer3, 0, len)
                                        if (testPCMFile3 != null) {
                                            try {
                                                testPCMFile3!!.write(pcmBuffer3, 0, len)
                                            } catch (e: IOException) {
                                                e.printStackTrace()
                                            }
                                        }
                                    } else {
                                        // only media
                                        val left = pos1
                                        len = ret
                                        if (testPCMFile != null && ret > 0) {
                                            try {
                                                testPCMFile!!.write(pcmBuffer, pos1, ret)
                                            } catch (e: IOException) {
                                                e.printStackTrace()
                                            }
                                        }
                                        pos1 += len
                                        inputBuffer.put(pcmBuffer, left, len)
                                    }
                                } else {
                                    debugLog("onInputBufferAvailable AudioRecord has nothing to read")
                                }
                                inputBuffer.rewind()
                                if (pos1 == BUFFER_SIZE) {
                                    if (isMicRecording) {
                                        if (pos2 == BUFFER_SIZE) {
                                            debugLog("queueInputBuffer full p1,p2 $len")
                                            codec.queueInputBuffer(
                                                index,
                                                0,
                                                len,
                                                System.nanoTime() / 1000,
                                                0
                                            )
                                            pos1 = 0
                                            pos2 = 0
                                        } else {
                                            debugLog("queueInputBuffer partial p1,p2 $len")
                                            codec.queueInputBuffer(
                                                index,
                                                0,
                                                len,
                                                System.nanoTime() / 1000,
                                                MediaCodec.BUFFER_FLAG_PARTIAL_FRAME
                                            )
                                        }
                                    } else {
                                        debugLog("queueInputBuffer full p1 $len")
                                        codec.queueInputBuffer(
                                            index,
                                            0,
                                            len,
                                            System.nanoTime() / 1000,
                                            0
                                        )
                                        pos1 = 0
                                    }
                                } else {
                                    debugLog("queueInputBuffer partial since p1/p2 < BUFFER_SIZE $len")
                                    codec.queueInputBuffer(
                                        index,
                                        0,
                                        len,
                                        System.nanoTime() / 1000,
                                        MediaCodec.BUFFER_FLAG_PARTIAL_FRAME
                                    )
                                }
                            }

                            override fun onOutputBufferAvailable(
                                codec: IAacEldEncoder,
                                index: Int,
                                info: MediaCodec.BufferInfo
                            ) {
                                debugLog("onOutputBufferAvailable: " + index + ", offset: " + info.offset + ", size: " + info.size + ", flags: " + info.flags + " ,seq: " + seq)
                                seq++
                                val encodedData = codec.getOutputBuffer(index)
                                encodedData.position(info.offset)
                                aacBuffer.position(0)
                                encodedData[aacBuffer.array(), 0, info.size]
                                if (info.size > 0) {
                                    if (testAACFile != null) {
                                        try {
                                            writeAdtsHeader(testAACFile!!, info.size)
                                            testAACFile!!.write(aacBuffer.array(), 0, info.size)
                                            testRawAACFile!!.write(aacBuffer.array(), 0, info.size)
                                        } catch (e: IOException) {
                                            e.printStackTrace()
                                        }
                                    }
                                    if (info.size > 0) {
                                        mAudioDataCallBack?.onAudioDataAvailable(
                                            aacBuffer,
                                            info.size
                                        )
                                    }
                                }
                                aacBuffer.rewind()
                                codec.releaseOutputBuffer(index, false)
                            }

                            override fun onError(codec: IAacEldEncoder, e: CodecException) {
                                Log.e(TAG, "MediaCodec.onError: $e")
                            }

                            @Throws(IOException::class)
                            private fun writeAdtsHeader(
                                finalTestEncodedFile: FileOutputStream,
                                packetLen: Int
                            ) {
                                var packetLen = packetLen
                                packetLen += 7
                                val profile = 2 //AAC LC
                                //39=MediaCodecInfo.CodecProfileLevel.AACObjectELD;
                                val freqIdx = 4 //44.1KHz
                                val chanCfg = 2 //CPE

                                // fill in ADTS data
                                adtsHeader[0] = 0xFF.toByte()
                                adtsHeader[1] = 0xF9.toByte()
                                adtsHeader[2] =
                                    ((profile - 1 shl 6) + (freqIdx shl 2) + (chanCfg shr 2)).toByte()
                                adtsHeader[3] =
                                    ((chanCfg and 3 shl 6) + (packetLen shr 11)).toByte()
                                adtsHeader[4] = (packetLen and 0x7FF shr 3).toByte()
                                adtsHeader[5] = ((packetLen and 7 shl 5) + 0x1F).toByte()
                                adtsHeader[6] = 0xFC.toByte()
                                finalTestEncodedFile.write(adtsHeader)
                            }

                            override fun onOutputFormatChanged(
                                codec: IAacEldEncoder,
                                format: MediaFormat
                            ) {
                                debugLog("MediaCodec.onOutputFormatChanged: $format")
                            }
                        })
                        codec.start()
                        Looper.loop()
                        codec.stop()
                        if (testPCMFile != null) {
                            testPCMFile!!.close()
                        }
                        if (testAACFile != null) {
                            testAACFile!!.close()
                        }
                        if (testRawAACFile != null) {
                            testRawAACFile!!.close()
                        }
                        debugLog("Thread exit")
                    } catch (e: IOException) {
                        e.printStackTrace()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }.start()
        }
    }

    fun release() {
        stopRecording(mAudioRecorder)
        stopRecording(mAudioRecorder2, mMicStatusListener)
        pos1 = 0
        pos2 = 0
        if (AudioRecord.STATE_INITIALIZED == mAudioRecorder.state) {
            mAudioRecorder.release()
            Log.d(TAG, "Release recorder")
        }
    }

    private fun debugLog(msg: String) {
        if (DEBUG_LOG) {
            Log.d(TAG, msg)
        }
    }

    fun enableMicRecording() {
        Log.d(TAG, "enableMicRecording")
        startRecording(mAudioRecorder2, mMicStatusListener)
        pos2 = pos1
    }

    private val isMicRecording: Boolean
        get() = AudioRecord.STATE_INITIALIZED == mAudioRecorder2.state && AudioRecord.RECORDSTATE_RECORDING == mAudioRecorder2.recordingState

    fun disableMicRecording() {
        Log.d(TAG, "disableMicRecording")
        if (isMicRecording) {
            stopRecording(mAudioRecorder2, mMicStatusListener)
            pos2 = 0
        }
    }

    companion object {
        private const val TAG = "AudioCapture"
        private const val SAMPLING_RATE = 44100
        private const val AUDIO_SOURCE = MediaRecorder.AudioSource.MIC
        private const val CHANNEL_IN_CONFIG = AudioFormat.CHANNEL_IN_STEREO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val CHANNEL_COUNT = 2
        private const val DEBUG_LOG = false
    }

    init {
        val config = AudioPlaybackCaptureConfiguration.Builder(mediaProjection!!)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .addMatchingUsage(AudioAttributes.USAGE_GAME)
            .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
            .build()
        mAudioRecorder = AudioRecord.Builder()
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AUDIO_FORMAT)
                    .setSampleRate(SAMPLING_RATE)
                    .setChannelMask(CHANNEL_IN_CONFIG)
                    .build()
            )
            .setAudioPlaybackCaptureConfig(config)
            .build()
        mAudioRecorder2 = AudioRecord.Builder()
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AUDIO_FORMAT)
                    .setSampleRate(SAMPLING_RATE)
                    .setChannelMask(CHANNEL_IN_CONFIG)
                    .build()
            )
            .setAudioSource(AUDIO_SOURCE)
            .build()
    }
}