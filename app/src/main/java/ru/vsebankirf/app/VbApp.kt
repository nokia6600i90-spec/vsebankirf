package ru.vsebankirf.app

import android.app.Application
import android.content.Intent
import android.os.Process
import java.io.PrintWriter
import java.io.StringWriter

class VbApp : Application() {

override fun onCreate() {
super.onCreate()
Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
try {
val sw = StringWriter()
throwable.printStackTrace(PrintWriter(sw))
val intent = Intent(this, CrashActivity::class.java)
intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
intent.putExtra(CrashActivity.EXTRA_TRACE, sw.toString())
startActivity(intent)
} catch (e: Exception) {
}
Process.killProcess(Process.myPid())
}
}}
