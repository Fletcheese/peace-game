package com.fletcher.peace

import android.content.Context
import android.content.SharedPreferences

data class GameScore(val score: Int, val multiplier: Int)

class ScoreManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("peace_scores", Context.MODE_PRIVATE)

    fun saveScore(newScore: Int, newMultiplier: Int) {
        prefs.edit().apply {
            putInt("last_score", newScore)
            putInt("last_multiplier", newMultiplier)
            apply()
        }

        val currentScores = getHighScores().toMutableList()
        currentScores.add(GameScore(newScore, newMultiplier))
        val top3 = currentScores.sortedByDescending { it.score }.take(3)
        
        prefs.edit().apply {
            top3.forEachIndexed { index, gameScore ->
                putInt("score_$index", gameScore.score)
                putInt("multiplier_$index", gameScore.multiplier)
            }
            apply()
        }
    }

    fun getHighScores(): List<GameScore> {
        val scores = mutableListOf<GameScore>()
        for (i in 0..2) {
            val s = prefs.getInt("score_$i", 0)
            val m = prefs.getInt("multiplier_$i", 0)
            if (s > 0) scores.add(GameScore(s, m))
        }
        return scores
    }

    fun getLastScore(): GameScore? {
        val s = prefs.getInt("last_score", 0)
        val m = prefs.getInt("last_multiplier", 0)
        return if (s > 0) GameScore(s, m) else null
    }

    fun isJoystickEnabled(): Boolean = prefs.getBoolean("joystick_enabled", false)
    fun setJoystickEnabled(enabled: Boolean) = prefs.edit().putBoolean("joystick_enabled", enabled).apply()

    fun getShipSpeed(): Float = prefs.getFloat("ship_speed", 1.0f)
    fun setShipSpeed(speed: Float) = prefs.edit().putFloat("ship_speed", speed).apply()
}
