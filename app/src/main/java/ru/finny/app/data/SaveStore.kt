package ru.finny.app.data

import android.content.Context
import com.google.gson.Gson

/**
 * Локальное хранение профиля (ТЗ: только офлайн, SharedPreferences + Gson).
 * Никаких имён, телефонов, почт — гостевой режим.
 */
object SaveStore {
    private const val PREF = "finny_save"
    private const val KEY = "game_state"
    private val gson = Gson()

    fun save(context: Context, state: GameState) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putString(KEY, gson.toJson(state)).apply()
    }

    fun load(context: Context): GameState? = try {
        val json = context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY, null)
        if (json == null) null else gson.fromJson(json, GameState::class.java)
    } catch (e: Exception) {
        null
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().remove(KEY).apply()
    }
}
