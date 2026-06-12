package com.fletcheese.peace

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

object SoundManager {
    private var soundPool: SoundPool? = null
    private val sounds = mutableMapOf<Int, Int>()
    private var currentVolume: Float = 1.0f

    fun init(context: Context) {
        if (soundPool == null) {
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            
            soundPool = SoundPool.Builder()
                .setMaxStreams(5)
                .setAudioAttributes(attributes)
                .build()
        }
    }

    fun setVolume(volume: Float) {
        currentVolume = volume
    }

    fun load(context: Context, resId: Int) {
        if (soundPool != null && !sounds.containsKey(resId)) {
            sounds[resId] = soundPool!!.load(context, resId, 1)
        }
    }

    fun play(resId: Int) {
        sounds[resId]?.let { soundId ->
            soundPool?.play(soundId, currentVolume, currentVolume, 0, 0, 1f)
        }
    }

    fun release() {
        soundPool?.release()
        soundPool = null
        sounds.clear()
    }
}
