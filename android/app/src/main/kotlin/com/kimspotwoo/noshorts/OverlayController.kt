package com.kimspotwoo.noshorts

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

/**
 * 접근성 오버레이(TYPE_ACCESSIBILITY_OVERLAY)로 가림막과 경고 화면을 띄운다.
 * 접근성 서비스 컨텍스트에서 쓰면 "다른 앱 위에 표시" 권한이 따로 필요 없다.
 */
class OverlayController(private val context: Context) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val handler = Handler(Looper.getMainLooper())

    private val masks = mutableListOf<View>()
    private var maskRects: List<Rect> = emptyList()
    private var warningView: View? = null

    val isWarningShowing get() = warningView != null

    // ---- 가림막 ----

    /**
     * 주어진 영역들 위에 터치를 막는 가림막을 띄운다.
     * 창을 지웠다 다시 만들면 깜빡이고 느려서, 이미 있는 창은 위치만 옮긴다.
     */
    fun showMasks(rects: List<Rect>) {
        if (rects == maskRects) return
        maskRects = rects
        while (masks.size > rects.size) removeSafely(masks.removeAt(masks.lastIndex))
        rects.forEachIndexed { i, rect ->
            val params = maskParams(rect)
            if (i < masks.size) {
                windowManager.updateViewLayout(masks[i], params)
            } else {
                val view = createMaskView()
                windowManager.addView(view, params)
                masks.add(view)
            }
        }
    }

    fun hideMasks() = showMasks(emptyList())

    private fun createMaskView() = TextView(context).apply {
        text = "🚫 숏폼 가림"
        gravity = Gravity.CENTER
        setTextColor(Color.argb(200, 255, 255, 255))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        background = GradientDrawable().apply {
            setColor(Color.argb(245, 30, 30, 30))
            cornerRadius = dp(12f)
        }
        // 터치를 소비해서 아래 버튼이 눌리지 않게 한다.
        setOnClickListener {
            Toast.makeText(context, "NoShorts가 숏폼을 막고 있어요", Toast.LENGTH_SHORT).show()
        }
    }

    private fun maskParams(rect: Rect) = baseParams(rect.width(), rect.height()).apply {
        gravity = Gravity.TOP or Gravity.START
        x = rect.left
        y = rect.top
    }

    // ---- 경고 화면 ----

    /**
     * 화면 전체를 덮는 경고 화면을 띄운다.
     * @param onLeave "돌아가기"를 눌렀을 때
     * @param onAllow 대기 시간이 지난 뒤 "N분만 보기"를 눌렀을 때
     */
    @SuppressLint("SetTextI18n")
    fun showWarning(onLeave: () -> Unit, onAllow: () -> Unit) {
        if (warningView != null) return
        hideMasks()

        val title = TextView(context).apply {
            text = "숏폼을 보려고 해요"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 26f)
            gravity = Gravity.CENTER
        }
        val subtitle = TextView(context).apply {
            text = "지금 정말 보고 싶은 건가요?\n잠깐 멈추고 생각해봐요."
            setTextColor(Color.argb(200, 255, 255, 255))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            gravity = Gravity.CENTER
            setPadding(0, dp(12f).toInt(), 0, dp(40f).toInt())
        }
        val leave = Button(context).apply {
            text = "돌아가기"
            setOnClickListener {
                hideWarning()
                onLeave()
            }
        }
        val allow = Button(context).apply {
            isEnabled = false
            setOnClickListener {
                hideWarning()
                onAllow()
            }
        }
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.argb(245, 20, 20, 20))
            val pad = dp(32f).toInt()
            setPadding(pad, pad, pad, pad)
            isClickable = true // 아래 화면으로 터치가 새지 않게 한다.
            addView(title)
            addView(subtitle)
            addView(leave, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
            addView(allow, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        }

        // "보기" 버튼은 몇 초 기다려야 눌린다. 충동적으로 바로 넘어가지 않게 하는 장치.
        var remaining = ALLOW_DELAY_SECONDS
        val tick = object : Runnable {
            override fun run() {
                if (warningView !== root) return
                if (remaining > 0) {
                    allow.text = "${ALLOW_MINUTES}분만 보기 (${remaining}초)"
                    remaining--
                    handler.postDelayed(this, 1000)
                } else {
                    allow.text = "${ALLOW_MINUTES}분만 보기"
                    allow.isEnabled = true
                }
            }
        }

        val params = baseParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
        windowManager.addView(root, params)
        warningView = root
        tick.run()
    }

    fun hideWarning() {
        warningView?.let { removeSafely(it) }
        warningView = null
    }

    fun hideAll() {
        hideMasks()
        hideWarning()
    }

    // ----

    private fun baseParams(width: Int, height: Int) = WindowManager.LayoutParams(
        width,
        height,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT,
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }

    private fun removeSafely(view: View) {
        try {
            windowManager.removeView(view)
        } catch (_: IllegalArgumentException) {
            // 이미 제거된 뷰
        }
    }

    private fun dp(value: Float) =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, context.resources.displayMetrics)

    companion object {
        const val ALLOW_MINUTES = 5
        const val ALLOW_DELAY_SECONDS = 5
    }
}
