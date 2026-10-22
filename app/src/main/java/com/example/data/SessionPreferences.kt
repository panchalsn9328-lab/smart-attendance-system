package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.UserRole

/**
 * Local SharedPreferences manager for Smart Attendance application.
 * Manages onboarding completion flag and persistent session cache.
 */
object SessionPreferences {
    private const val PREFS_NAME = "smart_attendance_prefs"
    private const val KEY_HAS_SEEN_ONBOARDING = "has_seen_onboarding"
    private const val KEY_SAVED_ROLE = "saved_user_role"
    private const val KEY_SAVED_USER_ID = "saved_user_id"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun hasSeenOnboarding(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_HAS_SEEN_ONBOARDING, false)
    }

    fun setHasSeenOnboarding(context: Context, hasSeen: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_HAS_SEEN_ONBOARDING, hasSeen).apply()
    }

    fun getSavedRole(context: Context): UserRole? {
        val roleStr = getPrefs(context).getString(KEY_SAVED_ROLE, null) ?: return null
        return try {
            UserRole.valueOf(roleStr.uppercase())
        } catch (e: Exception) {
            null
        }
    }

    fun setSavedRole(context: Context, role: UserRole?) {
        if (role == null) {
            getPrefs(context).edit().remove(KEY_SAVED_ROLE).apply()
        } else {
            getPrefs(context).edit().putString(KEY_SAVED_ROLE, role.name).apply()
        }
    }

    fun getSavedUserId(context: Context): String? {
        return getPrefs(context).getString(KEY_SAVED_USER_ID, null)
    }

    fun setSavedUserId(context: Context, userId: String?) {
        if (userId == null) {
            getPrefs(context).edit().remove(KEY_SAVED_USER_ID).apply()
        } else {
            getPrefs(context).edit().putString(KEY_SAVED_USER_ID, userId).apply()
        }
    }

    fun clearSession(context: Context) {
        getPrefs(context).edit()
            .remove(KEY_SAVED_ROLE)
            .remove(KEY_SAVED_USER_ID)
            .apply()
    }
}
