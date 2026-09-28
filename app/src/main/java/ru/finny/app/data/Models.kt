package ru.finny.app.data

/** Вариант ответа задания. */
data class TaskOption(val id: String, val text: String)

/** Задание по финграмотности (data/tasks.json). */
data class Task(
    val id: String,
    val theme: String,
    val title: String,
    val story: String,
    val question: String,
    val options: List<TaskOption>,
    val correct: String,
    val hint: String,
    val explain: String,
    val reward: Int,
    val age: String,
)

/** Финансовая цель накоплений (data/goals.json). */
data class Goal(val id: String, val name: String, val target: Int, val icon: String, val desc: String)

/** Игровой период демо-режима (data/periods.json). */
data class Period(val index: Int, val income: Int, val mandatory: List<String>, val event: String)

/** Три потребности питомца (0..100). */
data class Needs(val food: Int = 70, val clean: Int = 70, val joy: Int = 70)

/** Локальный профиль ребёнка (гостевой, без персональных данных) — сохраняется в SharedPreferences. */
data class GameState(
    val balance: Int = 30,
    val day: Int = 1,
    val needs: Needs = Needs(),
    val growth: Int = 0,
    val stage: Int = 1,
    val saved: Int = 0,
    val goalId: String = "g_house",
    val solved: List<String> = emptyList(),
    val taskCoins: Int = 0,
    val dayFeed: Int = 0,
    val dayWash: Int = 0,
    val dayPlay: Int = 0,
    val seen: List<String> = emptyList(),
    val rules: Map<String, Int> = emptyMap(),
    val onbTap: Boolean = false,
    val onbBuy: Boolean = false,
    val onbTask: Boolean = false,
    val sleeping: Boolean = false,
    val muted: Boolean = false,
)

/** Действие ухода: цена, прирост потребностей, анимация, обучающая реплика и реакции. */
data class CareAction(
    val id: String,
    val price: Int,
    val dFood: Int = 0,
    val dClean: Int = 0,
    val dFun: Int = 0,
    val anim: String,
    val teach: String,
    val react: List<String>,
    val voice: Voice,
)

/** Писк-реплика Финни: базовая частота, ступени, длительность ноты (мс). */
data class Voice(val base: Float, val steps: FloatArray, val durMs: Int)
