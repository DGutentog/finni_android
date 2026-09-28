package ru.finny.app.game

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import ru.finny.app.data.Voice
import kotlin.math.sin
import kotlin.concurrent.thread

/**
 * «Голос» Финни: писки-реплики синтезируются на лету (AudioTrack, PCM 16-bit),
 * без аудиофайлов в ассетах — как WebAudio-писки в HTML-прототипе.
 */
object BeepSynth {
    private const val SR = 22050

    fun speak(voice: Voice, muted: Boolean) {
        if (muted) return
        thread(name = "finny-beep") {
            try {
                play(voice)
            } catch (_: Exception) {
                // звук не критичен для игры
            }
        }
    }

    private fun play(voice: Voice) {
        val perNote = SR * voice.durMs / 1000
        val total = perNote * voice.steps.size
        val buf = ShortArray(total)
        for ((i, step) in voice.steps.withIndex()) {
            val f0 = voice.base * step
            val f1 = f0 * 1.22f
            for (t in 0 until perNote) {
                val p = t.toFloat() / perNote
                val freq = f0 + (f1 - f0) * p
                // огибающая: быстрая атака, экспоненциальный спад
                val env = if (p < 0.1f) p / 0.1f else (1f - (p - 0.1f) / 0.9f).coerceAtLeast(0f)
                val sample = (sin(2.0 * Math.PI * freq * t / SR) * env * 0.35 * Short.MAX_VALUE).toInt()
                buf[i * perNote + t] = sample.toShort()
            }
        }
        val minSize = AudioTrack.getMinBufferSize(
            SR, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SR)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(maxOf(minSize, buf.size * 2))
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        track.write(buf, 0, buf.size)
        track.play()
        Thread.sleep((voice.durMs * voice.steps.size + 120).toLong())
        track.stop()
        track.release()
    }
}
