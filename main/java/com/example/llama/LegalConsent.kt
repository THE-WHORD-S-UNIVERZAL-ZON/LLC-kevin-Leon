package com.example.llama

import android.content.Context

object LegalConsent {
    const val TERMS_VERSION = "2026-10-09-v1"
    const val PRIVACY_VERSION = "2026-10-09-v1"

    private const val PREFS = "universal_zon_legal_consent"
    private const val TERMS_ACCEPTED_VERSION = "terms_version"
    private const val PRIVACY_ACCEPTED_VERSION = "privacy_version"

    fun hasCurrentConsent(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString(TERMS_ACCEPTED_VERSION, null) == TERMS_VERSION &&
            prefs.getString(PRIVACY_ACCEPTED_VERSION, null) == PRIVACY_VERSION
    }

    fun saveConsent(context: Context) {
        val now = System.currentTimeMillis()
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(TERMS_ACCEPTED_VERSION, TERMS_VERSION)
            .putString(PRIVACY_ACCEPTED_VERSION, PRIVACY_VERSION)
            .putLong("terms_accepted_at", now)
            .putLong("privacy_accepted_at", now)
            .putLong("consent_recorded_at", now)
            .apply()
    }
}
