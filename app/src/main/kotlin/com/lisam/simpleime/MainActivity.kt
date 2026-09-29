package com.lisam.simpleime

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val padding = dp(16)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(Color.BLACK)
            setPadding(padding, padding * 2, padding, padding)
        }

        val description = TextView(this).apply {
            text = "請到 Android 系統設定 > 鍵盤與輸入法 > 啟用 SimpleIME"
            setTextColor(Color.GREEN)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
        }

        val openSettingsButton = Button(this).apply {
            text = "開啟輸入法設定"
            setTextColor(Color.GREEN)
            setBackgroundColor(Color.rgb(30, 30, 30))
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
            }
        }

        root.addView(
            description,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        root.addView(
            openSettingsButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(20)
            }
        )

        setContentView(root)
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
