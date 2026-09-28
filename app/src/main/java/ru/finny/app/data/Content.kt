package ru.finny.app.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import ru.finny.app.R

/**
 * Контент вынесен в assets/json (требование ТЗ п. 2.5.14 — контент отдельно от кода).
 * Спрайты и иконки лежат в res/drawable-nodpi.
 */
object Content {
    lateinit var tasks: List<Task>
        private set
    lateinit var goals: List<Goal>
        private set
    lateinit var periods: List<Period>
        private set

    private var loaded = false

    fun load(context: Context) {
        if (loaded) return
        val gson = Gson()
        tasks = gson.fromJson(
            stripMeta(readAsset(context, "json/tasks.json"), "tasks"),
            object : TypeToken<List<Task>>() {}.type
        )
        goals = gson.fromJson(
            stripMeta(readAsset(context, "json/goals.json"), "goals"),
            object : TypeToken<List<Goal>>() {}.type
        )
        periods = gson.fromJson(
            stripMeta(readAsset(context, "json/periods.json"), "periods"),
            object : TypeToken<List<Period>>() {}.type
        )
        loaded = true
    }

    private fun stripMeta(json: String, key: String): String {
        val root = com.google.gson.JsonParser.parseString(json).asJsonObject
        return root.getAsJsonArray(key).toString()
    }

    private fun readAsset(context: Context, path: String): String =
        context.assets.open(path).bufferedReader().use { it.readText() }

    fun goal(id: String): Goal = goals.first { it.id == id }

    fun incomeFor(day: Int): Int = periods[(day - 1) % periods.size].income

    /** Спрайт Финни: стадия (1..3) x настроение (happy|ok|sad) + сон. */
    fun spriteRes(stage: Int, mood: String, sleeping: Boolean): Int = when {
        sleeping -> when (stage) {
            1 -> R.drawable.finny_s1_sleep
            2 -> R.drawable.finny_s2_sleep
            else -> R.drawable.finny_s3_sleep
        }
        else -> when (stage) {
            1 -> when (mood) {
                "happy" -> R.drawable.finny_s1_happy
                "sad" -> R.drawable.finny_s1_sad
                else -> R.drawable.finny_s1_ok
            }
            2 -> when (mood) {
                "happy" -> R.drawable.finny_s2_happy
                "sad" -> R.drawable.finny_s2_sad
                else -> R.drawable.finny_s2_ok
            }
            else -> when (mood) {
                "happy" -> R.drawable.finny_s3_happy
                "sad" -> R.drawable.finny_s3_sad
                else -> R.drawable.finny_s3_ok
            }
        }
    }
}

/** Действия ухода — экономики частью участника 2 (цены/эффекты как в прототипе). */
object Actions {
    val all: List<CareAction> = listOf(
        CareAction(
            "feed", 25, dFood = 40, anim = "feed",
            teach = "Это КОРМ — обязательная трата: без еды я грустнею. Сытость +40!",
            react = listOf("Ням-ням! Спасибо!", "Вкусно-вкусно!", "Хрум-хрум… добавочки?"),
            voice = Voice(300f, floatArrayOf(1f, 1.2f, 1.45f), 90),
        ),
        CareAction(
            "wash", 10, dClean = 40, anim = "wash",
            teach = "Это ГИГИЕНА — тоже обязательная трата, но дешёвая. Чистота +40!",
            react = listOf("Пузырьки! Щекотно!", "Теперь я чистюля!", "Пшик-пшик!"),
            voice = Voice(700f, floatArrayOf(1.4f, 1.15f, 0.9f), 80),
        ),
        CareAction(
            "play", 10, dFun = 35, anim = "play",
            teach = "Это ИГРА — желаемая трата: приятно, но не жизненно важно. Веселье +35!",
            react = listOf("Ура, мячик!", "Прыг-скок! Ещё!", "Класс играем!"),
            voice = Voice(440f, floatArrayOf(1f, 1.25f, 1.5f, 1.9f), 90),
        ),
        CareAction(
            "treat", 5, dFood = 5, dFun = 15, anim = "tap",
            teach = "ВКУСНЯШКА — маленькая хотелка за 5 монеток. Много не полезных!",
            react = listOf("Медовое блаженство…", "Сладко-сладко!", "Ммм!"),
            voice = Voice(500f, floatArrayOf(1.3f, 1.6f), 80),
        ),
        CareAction(
            "vita", 10, dFood = 10, dClean = 10, dFun = 10, anim = "tap",
            teach = "ВИТАМИНЫ — трата «понемногу на всё»: +10 ко всем потребностям.",
            react = listOf("Хм, полезно и вкусно!", "Ягодки! Спасибо!", "Чувствую силу!"),
            voice = Voice(520f, floatArrayOf(1f, 1.2f), 80),
        ),
        CareAction(
            "save", 10, anim = "save",
            teach = "ОТЛОЖИТЬ — не трата! Монетки уходят в копилку ради большой цели.",
            react = listOf("Копилка звенит! Цель ближе!", "Отложить — это мудро!", "Моя копилка растёт!"),
            voice = Voice(900f, floatArrayOf(1f, 1.5f), 70),
        ),
    )

    fun byId(id: String): CareAction? = all.find { it.id == id }
}
