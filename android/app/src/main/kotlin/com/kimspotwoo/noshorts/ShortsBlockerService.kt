package com.kimspotwoo.noshorts

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
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

    /**
     * 스크롤 중인 동안(마지막 스크롤 이벤트 후 SCROLL_SETTLE_MS까지)은 피드 가림막을 숨긴다.
     * 스크롤 이벤트는 초당 10번 정도만 와서 가림막이 썸네일을 매끄럽게 따라갈 수 없다.
     */
    private var scrollingUntil = 0L
    private val handler = Handler(Looper.getMainLooper())
    private val refreshMasks = Runnable { updateMasks() }
    private var lastTargets: List<ShortsDetector.MaskTarget> = emptyList()

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
            if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) hideAll()
            return
        }
        if (settings.isAllowedNow()) {
            hideAll()
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

        if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            if (!isScrolling()) {
                // 스크롤이 시작되면 피드 가림막만 걷고, 고정된 하단 버튼 가림막은 그대로 둔다.
                showMasks(lastTargets.filter { it.kind == ShortsDetector.MaskKind.NAV_BUTTON })
            }
            scrollingUntil = SystemClock.uptimeMillis() + SCROLL_SETTLE_MS
            // 스크롤이 멈추면 화면을 다시 읽어 피드 가림막을 그린다.
            handler.removeCallbacks(refreshMasks)
            handler.postDelayed(refreshMasks, SCROLL_SETTLE_MS)
            return
        }
        // 스크롤 중에는 화면 전체를 다시 읽지 않는다. (버벅임의 원인)
        if (isScrolling()) return
        updateMasks()
    }

    private fun hideAll() {
        lastTargets = emptyList()
        overlay.hideAll()
    }

    private fun isScrolling() = SystemClock.uptimeMillis() < scrollingUntil

    private fun showMasks(targets: List<ShortsDetector.MaskTarget>) {
        lastTargets = targets
        overlay.showMasks(targets.map { it.rect })
    }

    private fun updateMasks() {
        if (!settings.maskEnabled || settings.isAllowedNow() || overlay.isWarningShowing) {
            showMasks(emptyList())
            return
        }
        val root = rootInActiveWindow ?: return
        val pkg = root.packageName?.toString()
        if (pkg == null || !ShortsDetector.isTarget(pkg)) return
        showMasks(ShortsDetector.findMaskTargets(pkg, root))
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
        handler.removeCallbacks(refreshMasks)
        if (::overlay.isInitialized) overlay.hideAll()
        super.onDestroy()
    }

    companion object {
        private const val LEAVE_GRACE_MS = 1000L
        private const val SCROLL_SETTLE_MS = 300L
    }
}
