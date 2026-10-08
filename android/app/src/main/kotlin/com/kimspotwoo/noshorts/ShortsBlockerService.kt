package com.kimspotwoo.noshorts

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent

/**
 * 숏폼 화면을 감지해 막는 접근성 서비스.
 * - 가림막: 쇼츠/릴스 탭 버튼 위에 터치를 막는 오버레이를 띄운다.
 * - 경고 화면: 숏폼 화면에 들어가거나 진입 버튼을 누르면 화면 전체를 덮는 경고를 띄운다.
 */
class ShortsBlockerService : AccessibilityService() {
    private lateinit var settings: BlockerSettings
    private lateinit var overlay: OverlayController

    /** "돌아가기" 직후 화면이 바뀌는 동안 경고가 다시 뜨지 않게 하는 시각. */
    private var suppressUntil = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        settings = BlockerSettings(this)
        overlay = OverlayController(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || !::overlay.isInitialized) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return // 우리 오버레이에서 나온 이벤트

        if (!ShortsDetector.isTarget(pkg)) {
            // 다른 앱이나 홈 화면으로 넘어가면 모두 걷어낸다.
            if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) overlay.hideAll()
            return
        }
        if (settings.isAllowedNow()) {
            overlay.hideAll()
            return
        }

        val root = rootInActiveWindow ?: return
        if (root.packageName?.toString() != pkg) return

        if (settings.warningEnabled && SystemClock.uptimeMillis() >= suppressUntil) {
            val clickedEntry = event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED &&
                event.source?.let { ShortsDetector.isEntryNode(pkg, it) } == true
            if (clickedEntry || ShortsDetector.isShortsScreen(pkg, root)) {
                showWarning(pkg)
                return
            }
        }
        if (overlay.isWarningShowing) return

        if (settings.maskEnabled) {
            overlay.showMasks(ShortsDetector.findEntryBounds(pkg, root))
        } else {
            overlay.hideMasks()
        }
    }

    private fun showWarning(pkg: String) {
        overlay.showWarning(
            onLeave = {
                suppressUntil = SystemClock.uptimeMillis() + LEAVE_GRACE_MS
                val wholeApp = ShortsDetector.rules[pkg]?.wholeAppIsShortForm == true
                performGlobalAction(if (wholeApp) GLOBAL_ACTION_HOME else GLOBAL_ACTION_BACK)
            },
            onAllow = {
                settings.allowUntil = System.currentTimeMillis() + OverlayController.ALLOW_MINUTES * 60_000L
            },
        )
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        if (::overlay.isInitialized) overlay.hideAll()
        super.onDestroy()
    }

    companion object {
        private const val LEAVE_GRACE_MS = 1000L
    }
}
