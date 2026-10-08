package com.kimspotwoo.noshorts

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent

/**
 * 숏폼 화면을 감지해 차단하는 접근성 서비스.
 * 감시 대상 앱은 res/xml/shorts_blocker_service.xml 의 packageNames 로 제한된다.
 * 실제 감지/차단 로직은 2단계에서 구현한다.
 */
class ShortsBlockerService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        Log.d(TAG, "event=${event.eventType} package=${event.packageName} class=${event.className}")
    }

    override fun onInterrupt() {}

    companion object {
        private const val TAG = "ShortsBlocker"
    }
}
