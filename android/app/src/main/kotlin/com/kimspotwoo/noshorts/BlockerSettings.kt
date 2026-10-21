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

    /** "N분만 보기"를 누르면 확인 경고를 한 번 더 띄운다. */
    var extraConfirmEnabled: Boolean
        get() = prefs.getBoolean(KEY_EXTRA_CONFIRM, false)
        set(value) = prefs.edit().putBoolean(KEY_EXTRA_CONFIRM, value).apply()

    /** 추가 확인 경고를 몇 번 띄울지. (1~MAX_EXTRA_CONFIRMS) */
    var extraConfirmCount: Int
        get() = prefs.getInt(KEY_EXTRA_CONFIRM_COUNT, 1).coerceIn(1, MAX_EXTRA_CONFIRMS)
        set(value) = prefs.edit().putInt(KEY_EXTRA_CONFIRM_COUNT, value.coerceIn(1, MAX_EXTRA_CONFIRMS)).apply()

    /** 지금 설정에서 첫 경고 뒤에 더 띄울 확인 경고 수. */
    val extraConfirms get() = if (extraConfirmEnabled) extraConfirmCount else 0

    /** "잠깐만 보기"를 고른 경우, 이 시각(ms)까지 차단을 멈춘다. */
    var allowUntil: Long
        get() = prefs.getLong(KEY_ALLOW_UNTIL, 0L)
        set(value) = prefs.edit().putLong(KEY_ALLOW_UNTIL, value).apply()

    fun isAllowedNow(now: Long = System.currentTimeMillis()) = now < allowUntil

    fun toMap(): Map<String, Any> = mapOf(
        KEY_MASK to maskEnabled,
        KEY_WARNING to warningEnabled,
        KEY_EXTRA_CONFIRM to extraConfirmEnabled,
        KEY_EXTRA_CONFIRM_COUNT to extraConfirmCount,
        KEY_ALLOW_UNTIL to allowUntil,
    )

    companion object {
        private const val PREFS_NAME = "noshorts_settings"
        const val KEY_MASK = "maskEnabled"
        const val KEY_WARNING = "warningEnabled"
        const val KEY_EXTRA_CONFIRM = "extraConfirmEnabled"
        const val KEY_EXTRA_CONFIRM_COUNT = "extraConfirmCount"
        const val KEY_ALLOW_UNTIL = "allowUntil"
        const val MAX_EXTRA_CONFIRMS = 5
    }
}
