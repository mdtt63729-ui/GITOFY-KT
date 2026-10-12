package com.gitofy.app.security

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The danger klaxon that starts with the "unofficial app" popup. The web app
 * synthesised a cinematic multi-layer sound with Web Audio; natively we build a
 * two-tone klaxon with a sub rumble directly as PCM — no audio assets to ship.
 */
@Singleton
class DangerSound @Inject constructor() {

    suspend fun play(durationMs: Int = 1800) = withContext(Dispatchers.Default) {
        val sampleRate = 44100
        val total = sampleRate * durationMs / 1000
        val buf = ShortArray(total)
        for (i in 0 until total) {
            val t = i.toDouble() / sampleRate
            // two-tone klaxon alternating every 300 ms + 60 Hz sub rumble
            val tone = if (((i / (sampleRate * 3 / 10)) % 2) == 0) 660.0 else 880.0
            val klaxon = kotlin.math.sin(2 * Math.PI * tone * t)
            val sub = 0.4 * kotlin.math.sin(2 * Math.PI * 60.0 * t)
            val env = fade(i, total)
            val sample = (0.35 * klaxon + sub) * env
            buf[i] = (sample.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
        }
        runCatching {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(buf.size * 2)
                .build()
            track.write(buf, 0, buf.size)
            track.play()
            kotlinx.coroutines.delay(durationMs.toLong() + 100)
            track.stop()
            track.release()
        }
    }

    private fun fade(i: Int, total: Int): Double {
        val fadeLen = 2000
        val a = (i.toDouble() / fadeLen).coerceAtMost(1.0)
        val b = ((total - i).toDouble() / fadeLen).coerceAtMost(1.0)
        return minOf(a, b)
    }
}
