package `is`.xyz.mpv

import android.content.SharedPreferences
import android.util.Log
import java.io.File

/**
 * Manager for GLSL shaders.
 * Scans the shaders directory recursively, allows toggling shaders on/off,
 * and applies them via mpv commands with higher priority than mpv.conf settings.
 *
 * Priority mechanism:
 * - When user toggles a shader ON, it's added to "enabled" set and removed from "disabled" set.
 * - When user toggles a shader OFF, it's added to "disabled" set and removed from "enabled" set.
 * - On each file load (START_FILE), enabled shaders are added and disabled shaders are removed.
 * - This ensures user toggles override any shader settings in mpv.conf.
 * - Shaders the user hasn't touched remain controlled by mpv.conf.
 */
object ShaderManager {
    private const val TAG = "mpv-shader"
    private const val PREFS_KEY_ENABLED = "shader_manager_enabled"
    private const val PREFS_KEY_DISABLED = "shader_manager_disabled"
    private val SHADER_EXTENSIONS = setOf("glsl", "hook")

    /**
     * Represents a single shader file.
     */
    data class ShaderInfo(
        val name: String,
        val path: String,
        val relativePath: String
    )

    /**
     * Get the shaders directory path.
     */
    fun getShadersDir(configDir: String): File {
        return File(configDir, "shaders")
    }

    /**
     * Scan the shaders directory recursively for .glsl and .hook files.
     */
    fun scanShaders(configDir: String): List<ShaderInfo> {
        val shadersDir = getShadersDir(configDir)
        if (!shadersDir.exists()) {
            Log.i(TAG, "Shaders directory does not exist: ${shadersDir.absolutePath}")
            return emptyList()
        }
        if (!shadersDir.isDirectory) {
            Log.w(TAG, "Shaders path is not a directory: ${shadersDir.absolutePath}")
            return emptyList()
        }

        val result = mutableListOf<ShaderInfo>()
        val basePath = shadersDir.absolutePath

        shadersDir.walkTopDown().forEach { file ->
            if (file.isFile && file.extension.lowercase() in SHADER_EXTENSIONS) {
                val relPath = file.absolutePath.removePrefix(basePath).removePrefix("/")
                result.add(ShaderInfo(
                    name = file.name,
                    path = file.absolutePath,
                    relativePath = relPath
                ))
            }
        }

        Log.d(TAG, "Found ${result.size} shader(s) in ${shadersDir.absolutePath}")
        return result.sortedBy { it.relativePath }
    }

    /**
     * Get the set of enabled shader paths from preferences.
     */
    fun getEnabledShaders(prefs: SharedPreferences): Set<String> {
        return prefs.getStringSet(PREFS_KEY_ENABLED, emptySet()) ?: emptySet()
    }

    /**
     * Get the set of disabled shader paths from preferences.
     */
    fun getDisabledShaders(prefs: SharedPreferences): Set<String> {
        return prefs.getStringSet(PREFS_KEY_DISABLED, emptySet()) ?: emptySet()
    }

    /**
     * Check if a specific shader is enabled.
     */
    fun isShaderEnabled(prefs: SharedPreferences, path: String): Boolean {
        return getEnabledShaders(prefs).contains(path)
    }

    /**
     * Toggle a shader on or off.
     * Returns true if the shader is now enabled, false if disabled.
     */
    fun toggleShader(prefs: SharedPreferences, path: String, enable: Boolean): Boolean {
        val enabled = getEnabledShaders(prefs).toMutableSet()
        val disabled = getDisabledShaders(prefs).toMutableSet()
        if (enable) {
            enabled.add(path)
            disabled.remove(path)
        } else {
            disabled.add(path)
            enabled.remove(path)
        }
        prefs.edit()
            .putStringSet(PREFS_KEY_ENABLED, enabled)
            .putStringSet(PREFS_KEY_DISABLED, disabled)
            .apply()
        return enable
    }

    /**
     * Apply a single shader by adding it to the shader chain.
     * Removes first then adds to avoid duplicates (handles case where mpv.conf
     * already loaded the same shader).
     */
    fun addShader(path: String) {
        Log.d(TAG, "Adding shader (remove+add): $path")
        // Remove first to avoid duplicates from mpv.conf, then add
        MPVLib.command(arrayOf("glsl-shader", "remove", path))
        MPVLib.command(arrayOf("glsl-shader", "add", path))
    }

    /**
     * Remove a single shader from the shader chain.
     */
    fun removeShader(path: String) {
        Log.d(TAG, "Removing shader: $path")
        MPVLib.command(arrayOf("glsl-shader", "remove", path))
    }

    /**
     * Sync all managed shaders to mpv.
     * Called after a new file starts playing (after mpv.conf is loaded).
     *
     * Priority mechanism:
     * 1. Remove all disabled shaders (overrides mpv.conf)
     * 2. Add all enabled shaders (remove+add to avoid duplicates)
     *
     * Shaders not managed by the user (never toggled) are left as-is
     * from mpv.conf.
     */
    fun syncManagedShaders(prefs: SharedPreferences, configDir: String) {
        val enabled = getEnabledShaders(prefs)
        val disabled = getDisabledShaders(prefs)

        // First remove disabled shaders (overrides mpv.conf if it loaded them)
        for (path in disabled) {
            if (File(path).exists()) {
                removeShader(path)
            }
        }

        // Then add enabled shaders (remove+add avoids duplication with mpv.conf)
        for (path in enabled) {
            if (File(path).exists()) {
                addShader(path)
            } else {
                // Clean up stale entries - shader file was deleted
                Log.w(TAG, "Enabled shader no longer exists: $path")
                toggleShader(prefs, path, false)
            }
        }

        if (enabled.isNotEmpty() || disabled.isNotEmpty()) {
            Log.d(TAG, "Synced shaders: +${enabled.size} enabled, -${disabled.size} disabled")
        }
    }

    /**
     * Clear all shaders managed by us (not mpv.conf's).
     * Called when exiting or before re-syncing.
     */
    fun clearManagedShaders(prefs: SharedPreferences) {
        val enabled = getEnabledShaders(prefs)
        for (path in enabled) {
            removeShader(path)
        }
    }

    /**
     * Count enabled shaders.
     */
    fun getEnabledCount(prefs: SharedPreferences): Int {
        return getEnabledShaders(prefs).size
    }
}
