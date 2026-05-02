package com.fletcheese.peace

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import java.util.UUID

@Serializable
data class ModProfile(
    val id: String = UUID.randomUUID().toString(),
    var label: String,
    val isEditable: Boolean = true,
    
    // Enemy Params
    var enemyColor: Int = Color.Red.toArgb(),
    var enemyMaxSpeed: Float = 4f,
    var enemyAcceleration: Float = 0.2f,
    var enemySize: Float = 15f,
    var enemySpawnRateMod: Float = 1.0f,
    var enemySpawnCount: Int = 3,

    // Player Params
    var playerMaxSpeed: Float = 12f,
    var playerAcceleration: Float = 0.0035f,
    var playerSize: Float = 37.5f,

    // Gate Params
    var gateColor: Int = Color(0xFFFFA500).toArgb(),
    var gateMaxSpeed: Float = 2f,
    var gateAcceleration: Float = 0.01f,
    var gateLength: Float = 250f,
    var gateEndZoneSize: Float = 24f,
    var gateExplosionRadius: Float = 350f,
    var gateSpawnRateMod: Float = 1.0f
)

class ModManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("peace_mods", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    object DefaultValues {
        const val ENEMY_SPEED = 4f
        const val ENEMY_ACCEL = 0.2f
        const val ENEMY_SIZE = 15f
        const val ENEMY_SPAWN_RATE = 1.0f
        const val ENEMY_COUNT = 3f
        
        const val PLAYER_SPEED = 12f
        const val PLAYER_ACCEL = 0.0035f
        const val PLAYER_SIZE = 37.5f
        
        const val GATE_SPEED = 2f
        const val GATE_ACCEL = 0.01f
        const val GATE_LENGTH = 250f
        const val GATE_END_ZONE = 24f
        const val GATE_EXPLOSION = 350f
        const val GATE_SPAWN_RATE = 1.0f
    }

    private val defaultProfiles = listOf(
        ModProfile(id = "standard", label = "Standard", isEditable = false),
        ModProfile(
            id = "turbo", label = "Turbo", isEditable = false,
            enemyMaxSpeed = 8f, enemySpawnRateMod = 2.0f, playerMaxSpeed = 20f, gateMaxSpeed = 5f
        ),
        ModProfile(
            id = "sloth", label = "Sloth", isEditable = false,
            enemyMaxSpeed = 1f, enemySpawnRateMod = 0.5f, playerMaxSpeed = 6f, gateMaxSpeed = 0.5f
        )
    )

    fun getSelectedProfileId(): String = prefs.getString("selected_profile_id", "standard") ?: "standard"
    fun setSelectedProfileId(id: String) = prefs.edit().putString("selected_profile_id", id).apply()

    fun getProfiles(): List<ModProfile> {
        val customJson = prefs.getString("custom_profiles", "[]") ?: "[]"
        val customProfiles = try {
            json.decodeFromString<List<ModProfile>>(customJson)
        } catch (e: Exception) {
            emptyList()
        }
        return defaultProfiles + customProfiles
    }

    fun saveProfiles(profiles: List<ModProfile>) {
        val customProfiles = profiles.filter { it.isEditable }
        val customJson = json.encodeToString(customProfiles)
        prefs.edit().putString("custom_profiles", customJson).apply()
    }

    fun getActiveProfile(): ModProfile {
        val id = getSelectedProfileId()
        return getProfiles().find { it.id == id } ?: defaultProfiles[0]
    }

    fun createCustomProfile(): ModProfile {
        val current = getActiveProfile()
        val profiles = getProfiles()
        val customCount = profiles.count { it.label.startsWith("Custom ") } + 1
        val newProfile = current.copy(
            id = UUID.randomUUID().toString(),
            label = "Custom $customCount",
            isEditable = true
        )
        val updated = profiles.filter { it.isEditable }.toMutableList()
        updated.add(newProfile)
        saveProfiles(updated)
        setSelectedProfileId(newProfile.id)
        return newProfile
    }

    fun deleteProfile(id: String) {
        val updated = getProfiles().filter { it.isEditable && it.id != id }
        saveProfiles(updated)
        setSelectedProfileId("standard")
    }
}
