//package com.starry.greenstash.ui.screens.recognition.composables
//
//import com.benasher44.uuid.uuid4
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.withContext
//import timber.log.Timber
//import java.io.File
//
//class LameUtils {
//
//    suspend fun convertPcmToMp3(pcmFilePath: String, mp3FilePath: String) {
//        withContext(Dispatchers.IO) {
//            try {
//                val pcmFile = File(pcmFilePath)
//                val mp3File = File(mp3FilePath)
//                com.benasher44.kotlinx.mp3.encoder.encodePcmToMp3(
//                    pcmFile = pcmFile,
//                    mp3File = mp3File,
//                    samplingRate = 44100, //  根据你的录音设置调整采样率
//                    channels = 2, //  根据你的录音设置调整声道数
//                    bitrate = 128000 //  设置比特率
//                )
//            } catch (e: Exception) {
//                Timber.e(e, "PCM 转 MP3 失败")
//            }
//        }
//    }
//
//
//}