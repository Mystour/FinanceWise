package com.starry.greenstash.ui.screens.recognition.composables

import android.media.MediaRecorder
import java.io.File
import java.io.IOException

class AudioRecorder {
    private var mediaRecorder: MediaRecorder? = null
    private lateinit var audioFile: File

    fun startRecording() {
        try {
            audioFile = File.createTempFile("audio_record", ".3gp") // 创建临时文件

            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(audioFile.absolutePath)
                prepare()
                start()
            }

        } catch (e: IOException) {
            // 处理错误
        }
    }


    fun stopRecording() {
        mediaRecorder?.apply {
            stop()
            release()
        }
        mediaRecorder = null
    }

    fun getAudioFile(): File? {
        return if (::audioFile.isInitialized) audioFile else null //  确保 audioFile 已初始化
    }
}