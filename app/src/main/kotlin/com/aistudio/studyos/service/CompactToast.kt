package com.aistudio.studyos.service

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.TextView
import android.widget.Toast

object CompactToast {
    fun show(context: Context, message: CharSequence, duration: Int = Toast.LENGTH_SHORT) {
        val textView = TextView(context).apply {
            text = message
            textSize = 11f
            setTextColor(Color.WHITE)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            maxLines = 2
            setPadding(dp(context, 12), dp(context, 7), dp(context, 12), dp(context, 7))
            background = GradientDrawable().apply {
                cornerRadius = dp(context, 16).toFloat()
                setColor(Color.rgb(72, 72, 72))
            }
        }

        Toast(context).apply {
            duration = duration
            view = textView
            setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, dp(context, 76))
        }.show()
    }

    private fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()
}
