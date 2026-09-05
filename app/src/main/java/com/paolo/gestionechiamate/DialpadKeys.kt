package com.paolo.gestionechiamate

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.widget.GridLayout
import android.widget.TextView

object DialpadKeys {

    private val LAYOUT = listOf(
        "1" to "", "2" to "ABC", "3" to "DEF",
        "4" to "GHI", "5" to "JKL", "6" to "MNO",
        "7" to "PQRS", "8" to "TUV", "9" to "WXYZ",
        "*" to "", "0" to "+", "#" to ""
    )

    fun build(
        context: Context,
        grid: GridLayout,
        keySizeDp: Int = 64,
        textSizeSp: Float = 22f,
        stileScuro: Boolean = false,
        onKeyPressed: (String) -> Unit
    ) {
        grid.removeAllViews()
        grid.columnCount = 3
        grid.rowCount = 4
        val density = context.resources.displayMetrics.density
        val sizePx = (keySizeDp * density).toInt()
        val marginPx = (6 * density).toInt()

        for ((digit, letters) in LAYOUT) {
            val key = TextView(context).apply {
                text = if (letters.isEmpty()) digit else "$digit\n$letters"
                gravity = Gravity.CENTER
                textSize = if (letters.isEmpty()) textSizeSp else textSizeSp * 0.7f
                if (stileScuro) {
                    setTextColor(Color.WHITE)
                    setBackgroundResource(R.drawable.bg_dialpad_key_scuro)
                } else {
                    setTextColor(Color.parseColor("#1B1B1B"))
                    setBackgroundResource(R.drawable.bg_dialpad_key)
                }
                isClickable = true
                isFocusable = true
                val outValue = android.util.TypedValue()
                context.theme.resolveAttribute(
                    android.R.attr.selectableItemBackground, outValue, true
                )
            }
            val params = GridLayout.LayoutParams().apply {
                width = sizePx
                height = sizePx
                setMargins(marginPx, marginPx, marginPx, marginPx)
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            }
            key.layoutParams = params
            key.setOnClickListener { onKeyPressed(digit) }
            grid.addView(key)
        }
    }
}
