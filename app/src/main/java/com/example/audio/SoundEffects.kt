package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

enum class SfxType {
    TAP,
    UPGRADE,
    OVERDRIVE,
    GACHA,
    BATTLE_HIT,
    SKILL,
    VICTORY,
    REWARD,
    BLIZZARD
}

object SoundEffects {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    @Volatile
    var enabled: Boolean = true

    fun play(type: SfxType) {
        if (!enabled) return
        scope.launch {
            runCatching {
                when (type) {
                    SfxType.TAP -> playTone(580.0, 45, 0.16)
                    SfxType.UPGRADE -> playArpeggio(listOf(440.0, 554.37, 659.25), 70, 0.22)
                    SfxType.OVERDRIVE -> playSweep(140.0, 340.0, 220, 0.28)
                    SfxType.GACHA -> playArpeggio(listOf(523.25, 659.25, 783.99, 1046.5), 85, 0.26)
                    SfxType.BATTLE_HIT -> playSweep(220.0, 90.0, 75, 0.22)
                    SfxType.SKILL -> playSweep(350.0, 880.0, 160, 0.25)
                    SfxType.VICTORY -> playArpeggio(listOf(523.25, 659.25, 783.99, 1046.5, 1318.5), 95, 0.25)
                    SfxType.REWARD -> playArpeggio(listOf(659.25, 880.0), 65, 0.22)
                    SfxType.BLIZZARD -> playSweep(320.0, 120.0, 350, 0.22)
                }
            }
        }
    }

    private fun playTone(freqHz: Double, durationMs: Int, volume: Double) {
        val sampleRate = 22050
        val count = (sampleRate * durationMs) / 1000
        val samples = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toDouble() / sampleRate
            val env = exp(-4.5 * i / count.toDouble())
            samples[i] = (sin(2.0 * PI * freqHz * t) * env * volume * Short.MAX_VALUE).toInt().toShort()
        }
        writePcm(samples, sampleRate)
    }

    private fun playSweep(startHz: Double, endHz: Double, durationMs: Int, volume: Double) {
        val sampleRate = 22050
        val count = (sampleRate * durationMs) / 1000
        val samples = ShortArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            val frac = i.toDouble() / count
            val freq = startHz + (endHz - startHz) * frac
            phase += 2.0 * PI * freq / sampleRate
            val env = (1.0 - frac) * (if (frac < 0.08) frac / 0.08 else 1.0)
            samples[i] = (sin(phase) * env * volume * Short.MAX_VALUE).toInt().toShort()
        }
        writePcm(samples, sampleRate)
    }

    private fun playArpeggio(notes: List<Double>, noteMs: Int, volume: Double) {
        val sampleRate = 22050
        val perNote = (sampleRate * noteMs) / 1000
        val samples = ShortArray(perNote * notes.size)
        notes.forEachIndexed { idx, freq ->
            for (i in 0 until perNote) {
                val t = i.toDouble() / sampleRate
                val env = exp(-3.5 * i / perNote.toDouble())
                samples[idx * perNote + i] =
                    (sin(2.0 * PI * freq * t) * env * volume * Short.MAX_VALUE).toInt().toShort()
            }
        }
        writePcm(samples, sampleRate)
    }

    private fun writePcm(samples: ShortArray, sampleRate: Int) {
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferBytes = maxOf(minBuf, samples.size * 2)
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(bufferBytes)
            .build()

        track.write(samples, 0, samples.size)
        track.play()
        val sleepMs = (samples.size * 1000L / sampleRate) + 40L
        Thread.sleep(sleepMs)
        track.stop()
        track.release()
    }
}
