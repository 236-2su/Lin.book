package com.example.myapplication

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.view.View
import android.view.ViewGroup

class App : Application(), Application.ActivityLifecycleCallbacks {

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(this)
    }

    private fun disableScrollbarsInActivity(activity: Activity) {
        val root = activity.window?.decorView?.findViewById<ViewGroup>(android.R.id.content) ?: return
        root.post { disableScrollbarsRecursively(root) }
    }

    private fun disableScrollbarsRecursively(view: View) {
        view.isVerticalScrollBarEnabled = false
        view.isHorizontalScrollBarEnabled = false
        if (view is ViewGroup) {
            var i = 0
            val childCount = view.childCount
            while (i < childCount) {
                disableScrollbarsRecursively(view.getChildAt(i))
                i++
            }
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        disableScrollbarsInActivity(activity)
    }

    override fun onActivityStarted(activity: Activity) {
        disableScrollbarsInActivity(activity)
    }

    override fun onActivityResumed(activity: Activity) {
        disableScrollbarsInActivity(activity)
    }

    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}


