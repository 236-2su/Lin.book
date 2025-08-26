package com.example.myapplication

import android.content.Context
import android.content.SharedPreferences

object UserManager {
    private const val PREFS_NAME = "app_prefs"
    private const val KEY_USER_PK = "user_pk"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"

    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // user_pk 저장
    fun saveUserPk(context: Context, userPk: Int) {
        val prefs = getSharedPreferences(context)
        prefs.edit()
            .putInt(KEY_USER_PK, userPk)
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .apply()
    }

    // user_pk 가져오기
    fun getUserPk(context: Context): Int? {
        val prefs = getSharedPreferences(context)
        val userPk = prefs.getInt(KEY_USER_PK, -1)
        return if (userPk != -1) userPk else null
    }

    // 로그인 상태 확인
    fun isLoggedIn(context: Context): Boolean {
        val prefs = getSharedPreferences(context)
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }



    // user_pk가 있는지 확인
    fun hasUserPk(context: Context): Boolean {
        return getUserPk(context) != null
    }
}
