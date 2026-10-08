package com.kimspotwoo.noshorts

import android.content.Context

/** Flutter 화면과 접근성 서비스가 함께 쓰는 설정. */
class BlockerSettings(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** 쇼츠/릴스 진입 버튼 위에 가림막을 띄운다. */
    var maskEnabled: Boolean
        get() = prefs.getBoolean(KEY_MASK, true)
        set(value) = prefs.edit().putBoolean(KEY_MASK, value).apply()

    /** 숏폼 화면에 들어가면 경고 화면을 띄운다. */
    var warningEnabled: Boolean
        get() = prefs.getBoolean(KEY_WARNING, true)
        set(value) = prefs.edit().putBoolean(KEY_WARNING, value).apply()

    /** "잠깐만 보기"를 고른 경우, 이 시각(ms)까지 차단을 멈춘다. */
    var allowUntil: Long
        get() = prefs.getLong(KEY_ALLOW_UNTIL, 0L)
        set(value) = prefs.edit().putLong(KEY_ALLOW_UNTIL, value).apply()

    fun isAllowedNow(now: Long = System.currentTimeMillis()) = now < allowUntil

    fun toMap(): Map<String, Any> = mapOf(
        KEY_MASK to maskEnabled,
        KEY_WARNING to warningEnabled,
        KEY_ALLOW_UNTIL to allowUntil,
    )

    companion object {
        private const val PREFS_NAME = "noshorts_settings"
        const val KEY_MASK = "maskEnabled"
        const val KEY_WARNING = "warningEnabled"
        const val KEY_ALLOW_UNTIL = "allowUntil"
    }
}
