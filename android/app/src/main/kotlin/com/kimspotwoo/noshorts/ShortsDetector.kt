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
        /** 피드 안 숏폼 묶음 제목에 들어가는 단어. ("추천 릴스" 처럼 앞뒤에 다른 말이 붙을 수 있다) */
        val feedHeaderWords: List<String> = emptyList(),
        /** 앱 전체가 숏폼이면 true (TikTok). */
        val wholeAppIsShortForm: Boolean = false,
    )

    val rules: Map<String, AppRule> = mapOf(
        "com.google.android.youtube" to AppRule(
            screenViewIds = listOf("reel_player_page_container", "reel_recycler", "reel_watch_player"),
            entryLabels = listOf("Shorts", "쇼츠"),
            feedHeaderWords = listOf("Shorts", "쇼츠"),
        ),
        "com.instagram.android" to AppRule(
            screenViewIds = listOf("clips_viewer_view_pager", "clips_viewer_container"),
            entryLabels = listOf("Reels", "릴스"),
            feedHeaderWords = listOf("Reels", "릴스"),
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

    /** 피드 안 숏폼 묶음의 제목("Shorts", "추천 릴스" 등)인지. 영상 제목에 걸리지 않게 짧은 글자만 본다. */
    private fun isFeedHeader(packageName: String, node: AccessibilityNodeInfo): Boolean {
        val rule = rules[packageName] ?: return false
        val text = node.text?.toString()?.trim() ?: return false
        if (text.length > MAX_HEADER_LENGTH) return false
        return rule.feedHeaderWords.any { text.contains(it, ignoreCase = true) }
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
            val entry = isEntryNode(packageName, node)
            if (entry || isFeedHeader(packageName, node)) {
                for (target in targetsFor(node, screen, allowNavButton = entry)) {
                    if (result.none { it.rect == target.rect }) result.add(target)
                }
                continue
            }
            for (i in 0 until node.childCount) node.getChild(i)?.let(queue::add)
        }
        return result
    }

    private fun targetsFor(node: AccessibilityNodeInfo, screen: Rect, allowNavButton: Boolean): List<MaskTarget> {
        val bounds = Rect().also { node.getBoundsInScreen(it) }
        if (bounds.isEmpty) return emptyList()

        // 화면 아래쪽 20% 안에 있는 작은 버튼은 하단 탭 바 버튼으로 본다.
        val inBottomBar = bounds.top >= screen.bottom - screen.height() / 5
        if (inBottomBar && allowNavButton) {
            if (!node.isVisibleToUser) return emptyList()
            val target = clickableAncestor(node) ?: node
            val rect = Rect().also { target.getBoundsInScreen(it) }
            return if (rect.isEmpty) emptyList() else listOf(MaskTarget(rect, MaskKind.NAV_BUTTON))
        }
        return shelfItems(node, screen).mapNotNull { item ->
            val rect = Rect().also { item.getBoundsInScreen(it) }
            if (!item.isVisibleToUser || !rect.intersect(screen) || rect.isEmpty) return@mapNotNull null
            if (rect.height() > screen.height() * MAX_SHELF_HEIGHT_PERCENT / 100) return@mapNotNull null
            MaskTarget(rect, MaskKind.FEED_SHELF)
        }
    }

    /**
     * 제목 노드에서 부모로 올라가다가 세로로 스크롤되는 피드 목록을 만나면,
     * 그 목록에서 제목이 든 항목과, 바로 뒤에 이어지는 쇼츠 썸네일 항목들을 돌려준다.
     * (YouTube는 제목 줄과 썸네일 줄이 피드 목록의 별도 항목이다)
     */
    private fun shelfItems(node: AccessibilityNodeInfo, screen: Rect): List<AccessibilityNodeInfo> {
        var child = node
        repeat(MAX_SHELF_DEPTH) {
            val list = child.parent ?: return emptyList()
            if (list.isScrollable) {
                // 다른 탭으로 넘어가 숨겨진 화면의 노드도 트리에 남아 있어서, 보이는지 꼭 확인한다.
                if (!list.isVisibleToUser) return emptyList()
                return listOf(child) + followingShortsCards(list, child, screen)
            }
            child = list
        }
        return emptyList()
    }

    private fun followingShortsCards(
        list: AccessibilityNodeInfo,
        header: AccessibilityNodeInfo,
        screen: Rect,
    ): List<AccessibilityNodeInfo> {
        val headerBounds = Rect().also { header.getBoundsInScreen(it) }
        val result = mutableListOf<AccessibilityNodeInfo>()
        var afterHeader = false
        for (i in 0 until list.childCount) {
            val item = list.getChild(i) ?: continue
            if (!afterHeader) {
                val r = Rect().also { item.getBoundsInScreen(it) }
                if (item == header || r == headerBounds) afterHeader = true
                continue
            }
            if (result.size >= MAX_FOLLOWING_CARDS || !containsPortraitCard(item, screen)) break
            result.add(item)
        }
        return result
    }

    /** 세로로 긴 썸네일 카드(쇼츠/릴스 모양)를 담고 있는지. 일반 영상은 가로로 긴 썸네일이다. */
    private fun containsPortraitCard(node: AccessibilityNodeInfo, screen: Rect): Boolean {
        val queue = ArrayDeque<Pair<AccessibilityNodeInfo, Int>>().apply { add(node to 0) }
        val r = Rect()
        while (queue.isNotEmpty()) {
            val (n, depth) = queue.removeFirst()
            n.getBoundsInScreen(r)
            if (r.width() < screen.width() * 6 / 10 &&
                r.height() > r.width() * 13 / 10 &&
                r.height() > screen.height() * 15 / 100
            ) {
                return true
            }
            if (depth < PORTRAIT_SEARCH_DEPTH) {
                for (i in 0 until n.childCount) n.getChild(i)?.let { queue.add(it to depth + 1) }
            }
        }
        return false
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
    private const val MAX_HEADER_LENGTH = 12
    private const val MAX_FOLLOWING_CARDS = 6
    private const val PORTRAIT_SEARCH_DEPTH = 4
}
