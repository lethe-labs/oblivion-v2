package oblivion.v2.core.log

import android.util.Log
import oblivion.v2.BuildConfig

/**
 * Debug/info/verbose logging compiles away in release builds, so a release APK
 * leaves nothing in logcat for a forensic tool to pick up.
 *
 * Error logging is kept in release to make crash reports usable -- which means
 * error messages must NEVER carry a PIN, keyword, keyphrase or phone number.
 */
object SecLog {
    inline fun d(tag: String, msg: String) {
        if (BuildConfig.DEBUG) Log.d(tag, msg)
    }

    inline fun i(tag: String, msg: String) {
        if (BuildConfig.DEBUG) Log.i(tag, msg)
    }

    inline fun v(tag: String, msg: String) {
        if (BuildConfig.DEBUG) Log.v(tag, msg)
    }

    inline fun w(tag: String, msg: String) {
        if (BuildConfig.DEBUG) Log.w(tag, msg)
    }

    inline fun w(tag: String, msg: String, t: Throwable) {
        if (BuildConfig.DEBUG) Log.w(tag, msg, t)
    }

    inline fun e(tag: String, msg: String) {
        Log.e(tag, msg)
    }

    inline fun e(tag: String, msg: String, t: Throwable) {
        Log.e(tag, msg, t)
    }
}
