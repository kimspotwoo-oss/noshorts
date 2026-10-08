package com.kimspotwoo.noshorts

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo

/**
 * 앱별로 숏폼 화면과 숏폼 진입 버튼을 찾는다.
 * 각 앱의 화면 구조(view id, 버튼 라벨)에 의존하므로 앱이 업데이트되면 규칙을 고쳐야 할 수 있다.
 */
object ShortsDetector {
    data class AppRule(
        /** 숏폼 플레이어 화면에만 있는 view id (패키지 접두사 제외). */
        val screenViewIds: List<String>,
        /** 숏폼 탭 버튼의 content description / 텍스트. */
        val entryLabels: List<String>,
        /** 앱 전체가 숏폼이면 true (TikTok). */
        val wholeAppIsShortForm: Boolean = false,
    )

    val rules: Map<String, AppRule> = mapOf(
        "com.google.android.youtube" to AppRule(
            screenViewIds = listOf("reel_player_page_container", "reel_recycler", "reel_watch_player"),
            entryLabels = listOf("Shorts", "쇼츠"),
        ),
        "com.instagram.android" to AppRule(
            screenViewIds = listOf("clips_viewer_view_pager", "clips_viewer_container"),
            entryLabels = listOf("Reels", "릴스"),
        ),
        "com.zhiliaoapp.musically" to AppRule(emptyList(), emptyList(), wholeAppIsShortForm = true),
        "com.ss.android.ugc.trill" to AppRule(emptyList(), emptyList(), wholeAppIsShortForm = true),
    )

    fun isTarget(packageName: String?) = packageName != null && rules.containsKey(packageName)

    /** 지금 화면이 숏폼 플레이어인지. */
    fun isShortsScreen(packageName: String, root: AccessibilityNodeInfo): Boolean {
        val rule = rules[packageName] ?: return false
        if (rule.wholeAppIsShortForm) return true
        return rule.screenViewIds.any { id ->
            root.findAccessibilityNodeInfosByViewId("$packageName:id/$id")
                .any { it.isVisibleToUser }
        }
    }

    /** 이 노드가 숏폼 진입 버튼인지. */
    fun isEntryNode(packageName: String, node: AccessibilityNodeInfo): Boolean {
        val rule = rules[packageName] ?: return false
        val label = (node.contentDescription ?: node.text)?.toString()?.trim() ?: return false
        return rule.entryLabels.any { label.equals(it, ignoreCase = true) }
    }

    /** 화면에 보이는 숏폼 진입 버튼들의 위치. */
    fun findEntryBounds(packageName: String, root: AccessibilityNodeInfo): List<Rect> {
        val rule = rules[packageName] ?: return emptyList()
        if (rule.entryLabels.isEmpty()) return emptyList()
        val result = mutableListOf<Rect>()
        val queue = ArrayDeque<AccessibilityNodeInfo>().apply { add(root) }
        var visited = 0
        while (queue.isNotEmpty() && visited < MAX_NODES) {
            val node = queue.removeFirst()
            visited++
            if (node.isVisibleToUser && isEntryNode(packageName, node)) {
                // 라벨이 붙은 노드가 작은 아이콘일 수 있어서, 눌리는 부모까지 올라가 영역을 잡는다.
                val target = clickableAncestor(node) ?: node
                val rect = Rect().also { target.getBoundsInScreen(it) }
                if (!rect.isEmpty && result.none { it == rect }) result.add(rect)
                continue
            }
            for (i in 0 until node.childCount) node.getChild(i)?.let(queue::add)
        }
        return result
    }

    private fun clickableAncestor(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var current: AccessibilityNodeInfo? = node
        repeat(3) {
            if (current?.isClickable == true) return current
            current = current?.parent
        }
        return null
    }

    private const val MAX_NODES = 3000
}
