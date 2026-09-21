package ru.vsebankirf.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class CrashActivity : AppCompatActivity() {

override fun onCreate(savedInstanceState: Bundle?) {
super.onCreate(savedInstanceState)
val trace = intent.getStringExtra(EXTRA_TRACE) ?: "no data"

val root = LinearLayout(this)
root.orientation = LinearLayout.VERTICAL
root.setPadding(32, 96, 32, 32)

val title = TextView(this)
title.text = "App crashed. Copy the text below and send it to the developer:"
title.textSize = 16f
title.setPadding(0, 0, 0, 24)
root.addView(title)

val copyButton = Button(this)
copyButton.text = "Copy error text"
copyButton.setOnClickListener {
val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
cm.setPrimaryClip(ClipData.newPlainText("crash", trace))
Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
}
root.addView(copyButton)

val closeButton = Button(this)
closeButton.text = "Close"
closeButton.setOnClickListener { finishAffinity() }
root.addView(closeButton)

val traceView = TextView(this)
traceView.text = trace
traceView.setTextIsSelectable(true)
traceView.setPadding(0, 24, 0, 0)
root.addView(traceView)

val scroll = ScrollView(this)
scroll.addView(root)
setContentView(scroll)
}

companion object {
const val EXTRA_TRACE = "trace"
}
}
