package com.fletcheese.peace

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.media.MediaPlayer
import android.view.animation.LinearInterpolator

object MusicManager {
    private var mediaPlayer: MediaPlayer? = null
    private var currentVolume: Float = 1.0f
    private var userVolume: Float = 1.0f
    private var fadeAnimator: ValueAnimator? = null
    private var currentTrackResId: Int = 0

    fun playMenuMusic(context: Context) {
        val resId = R.raw.bg_main
        if (currentTrackResId == resId && mediaPlayer?.isPlaying == true) return
        
        transitionTo(context, resId, true)
    }

    fun playGameMusic(context: Context) {
        val introResId = R.raw.bg_init
        val loopResId = R.raw.bg_loop
        
        if (currentTrackResId == introResId || currentTrackResId == loopResId) {
             if (mediaPlayer?.isPlaying == true) return
        }
        
        transitionTo(context, introResId, false) {
            startNewPlayer(context, loopResId, true, null)
        }
    }

    private fun transitionTo(
        context: Context, 
        resId: Int, 
        loop: Boolean, 
        onComplete: (() -> Unit)? = null
    ) {
        if (mediaPlayer != null && mediaPlayer?.isPlaying == true) {
            fadeAnimator?.cancel()
            fadeAnimator = ValueAnimator.ofFloat(currentVolume, 0f).apply {
                duration = 1000
                interpolator = LinearInterpolator()
                addUpdateListener { animator ->
                    val vol = animator.animatedValue as Float
                    mediaPlayer?.setVolume(vol, vol)
                }
                addListener(object : Animator.AnimatorListener {
                    override fun onAnimationStart(animation: Animator) {}
                    override fun onAnimationEnd(animation: Animator) {
                        stop()
                        startNewPlayer(context, resId, loop, onComplete)
                    }
                    override fun onAnimationCancel(animation: Animator) {}
                    override fun onAnimationRepeat(animation: Animator) {}
                })
                start()
            }
        } else {
            startNewPlayer(context, resId, loop, onComplete)
        }
    }

    private fun startNewPlayer(
        context: Context, 
        resId: Int, 
        loop: Boolean, 
        onComplete: (() -> Unit)?
    ) {
        stop()
        currentTrackResId = resId
        mediaPlayer = MediaPlayer.create(context.applicationContext, resId).apply {
            isLooping = loop
            setVolume(0f, 0f)
            setOnCompletionListener {
                if (!loop) {
                    onComplete?.invoke()
                }
            }
            start()
        }

        fadeAnimator?.cancel()
        fadeAnimator = ValueAnimator.ofFloat(0f, userVolume).apply {
            duration = 1000
            interpolator = LinearInterpolator()
            addUpdateListener { animator ->
                currentVolume = animator.animatedValue as Float
                mediaPlayer?.setVolume(currentVolume, currentVolume)
            }
            start()
        }
    }

    fun setVolume(volume: Float) {
        userVolume = volume
        if (fadeAnimator?.isRunning != true) {
            currentVolume = volume
            mediaPlayer?.setVolume(volume, volume)
        }
    }

    fun pause() {
        mediaPlayer?.pause()
        fadeAnimator?.pause()
    }

    fun start(context: Context) {
        if (mediaPlayer == null) {
            playMenuMusic(context)
        } else if (mediaPlayer?.isPlaying == false) {
            mediaPlayer?.start()
            fadeAnimator?.resume()
        }
    }

    fun stop() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        currentTrackResId = 0
        fadeAnimator?.cancel()
    }
}
