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

    enum class MaskKind {
        /** 하단 탭 바의 쇼츠/릴스 버튼. 화면에 고정돼 있다. */
        NAV_BUTTON,

        /** 피드 안의 쇼츠 묶음(제목 + 썸네일). 스크롤하면 움직인다. */
        FEED_SHELF,
    }

    data class MaskTarget(val rect: Rect, val kind: MaskKind)

    /** 가림막을 띄울 숏폼 진입 영역들. */
    fun findMaskTargets(packageName: String, root: AccessibilityNodeInfo): List<MaskTarget> {
        val rule = rules[packageName] ?: return emptyList()
        if (rule.entryLabels.isEmpty()) return emptyList()
        val screen = Rect().also { root.getBoundsInScreen(it) }
        if (screen.isEmpty) return emptyList()

        val result = mutableListOf<MaskTarget>()
        val queue = ArrayDeque<AccessibilityNodeInfo>().apply { add(root) }
        var visited = 0
        while (queue.isNotEmpty() && visited < MAX_NODES) {
            val node = queue.removeFirst()
            visited++
            if (isEntryNode(packageName, node)) {
                targetFor(node, screen)?.let { target ->
                    if (result.none { it.rect == target.rect }) result.add(target)
                }
                continue
            }
            for (i in 0 until node.childCount) node.getChild(i)?.let(queue::add)
        }
        return result
    }

    private fun targetFor(node: AccessibilityNodeInfo, screen: Rect): MaskTarget? {
        val bounds = Rect().also { node.getBoundsInScreen(it) }
        if (bounds.isEmpty) return null

        // 화면 아래쪽 20% 안에 있는 작은 버튼은 하단 탭 바 버튼으로 본다.
        val inBottomBar = bounds.top >= screen.bottom - screen.height() / 5
        if (inBottomBar) {
            if (!node.isVisibleToUser) return null
            val target = clickableAncestor(node) ?: node
            val rect = Rect().also { target.getBoundsInScreen(it) }
            return if (rect.isEmpty) null else MaskTarget(rect, MaskKind.NAV_BUTTON)
        }

        // 피드 안의 "Shorts" 제목이면, 썸네일까지 포함하는 묶음 전체를 가린다.
        val shelf = shelfItem(node) ?: return null
        // 다른 탭으로 넘어가 숨겨진 화면의 노드도 트리에 남아 있어서, 보이는지 꼭 확인한다.
        if (!shelf.isVisibleToUser) return null
        val rect = Rect().also { shelf.getBoundsInScreen(it) }
        if (!rect.intersect(screen) || rect.isEmpty) return null
        if (rect.height() > screen.height() * MAX_SHELF_HEIGHT_PERCENT / 100) return null
        return MaskTarget(rect, MaskKind.FEED_SHELF)
    }

    /**
     * 제목 노드에서 부모로 올라가다가 세로로 스크롤되는 피드 목록을 만나면,
     * 그 목록의 바로 아래 항목(= 제목과 썸네일 줄을 담은 쇼츠 묶음)을 돌려준다.
     */
    private fun shelfItem(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var child = node
        repeat(MAX_SHELF_DEPTH) {
            val parent = child.parent ?: return null
            if (parent.isScrollable) return child
            child = parent
        }
        return null
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
    private const val MAX_SHELF_DEPTH = 10
    private const val MAX_SHELF_HEIGHT_PERCENT = 70
}
