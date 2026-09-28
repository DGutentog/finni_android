package ru.finny.app.game

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.finny.app.data.Actions
import ru.finny.app.data.Content
import ru.finny.app.data.GameState
import ru.finny.app.data.Needs
import ru.finny.app.data.SaveStore
import ru.finny.app.data.Voice
import kotlin.random.Random

/**
 * Вся игровая логика «Комнаты Финни» (участник 2: экономика + игровой слой):
 * баланс, потребности, дни, рост, сон, задания, адаптивный помощник, голос.
 * Состояние сохраняется локально после каждого действия (SharedPreferences + Gson).
 */
class GameViewModel(app: Application) : AndroidViewModel(app) {

    data class Particle(val id: Int, val emoji: String, val x: Float, val y: Float)
    data class AnimEvent(val token: Int, val name: String)

    private val _state = MutableStateFlow(SaveStore.load(app) ?: GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()

    private val _bubble = MutableStateFlow<String?>(null)
    val bubble: StateFlow<String?> = _bubble.asStateFlow()

    private val _particles = MutableStateFlow<List<Particle>>(emptyList())
    val particles: StateFlow<List<Particle>> = _particles.asStateFlow()

    private val _anim = MutableStateFlow(AnimEvent(0, ""))
    val anim: StateFlow<AnimEvent> = _anim.asStateFlow()

    private val _tasksOpen = MutableStateFlow(false)
    val tasksOpen: StateFlow<Boolean> = _tasksOpen.asStateFlow()

    private val _currentTask = MutableStateFlow<String?>(null)
    val currentTask: StateFlow<String?> = _currentTask.asStateFlow()

    /** pair: kind ("ok" | "no") -> текст обратной связи */
    private val _taskFb = MutableStateFlow<Pair<String, String>?>(null)
    val taskFb: StateFlow<Pair<String, String>?> = _taskFb.asStateFlow()

    private var particleId = 0
    private var animToken = 0
    private var idleCd = 8
    private var petLast = 0L
    private var resetArmed = false

    init {
        Content.load(app)
        viewModelScope.launch { idleLoop() }
        say("Привет! Я Финни. Нажми на меня, ухаживай кнопками ниже и зарабатывай монетки в заданиях!", 5200)
    }

    private val s: GameState get() = _state.value

    private fun commit(next: GameState) {
        _state.value = next
        SaveStore.save(getApplication(), next)
    }

    fun say(text: String, ms: Long = 3400) {
        _bubble.value = text
        viewModelScope.launch {
            delay(ms)
            _bubble.value = null
        }
    }

    // ================= Адаптивный помощник =================
    private val ruleTexts = mapOf(
        "prio" to listOf(
            "Эй-эй! Сначала корм и мытьё, потом игры — иначе у меня урчит животик.",
            "Правило: обязательные траты (еда, гигиена) идут ПЕРЕД хотелками. Посмотри на полоски потребностей.",
            "В экономике приложения обязательные покупки — приоритет №1: без них питомец грустит и не растёт, а хотелки планируются по остатку.",
        ),
        "money" to listOf(
            "Монеток не хватает! Заработай: нажми «⭐ Задания».",
            "Доход бывает двух видов: карманные деньги раз в день и награды за задания. Задания — твой заработок!",
            "Бюджет всегда ограничен доходом. Потратить больше, чем есть, нельзя — это правило настоящего бюджета.",
        ),
        "care" to listOf(
            "Мне грустно… проверь полоски: сытость, чистота, веселье.",
            "Если полоска ниже 30 — питомцу плохо, и он растёт медленно. Ухаживай каждый день, а не разово.",
            "Рост питомца = метрика заботы за день: сытость≥50, чистота≥50, веселье≥50. Три балла в день — и Финни быстро вырастет.",
        ),
        "save" to listOf(
            "Все мои потребности закрыты! Самое время отложить монетки в копилку.",
            "Правило накоплений: сначала закрой потребности, потом отложи регулярный взнос — цель приближают маленькие, но частые взносы.",
            "Регулярность важнее размера: взнос каждый период приносит цель быстрее, чем редкие крупные. Копилка защищена от трат.",
        ),
    )

    private fun fireRule(id: String) {
        val texts = ruleTexts[id] ?: return
        val lvl = minOf(s.rules[id] ?: 0, texts.lastIndex)
        commit(s.copy(rules = s.rules + (id to (lvl + 1))))
        say(texts[lvl], 4200)
    }

    // ================= Касания (зоны как у «Говорящего Тома») =================
    fun tapPet(zone: String) {
        if (s.sleeping) {
            say("Финни улыбается во сне… тсс, не буди раньше времени 💤")
            return
        }
        commit(s.copy(onbTap = true))
        when (zone) {
            "belly" -> {
                anim("wiggle"); spawn("heart", 5)
                BeepSynth.speak(Voice(620f, floatArrayOf(1f, 1.3f, 1.6f, 1.2f), 70), s.muted)
                say("Хи-хи-хи! Пузо щекотное!")
            }
            "paws" -> {
                anim("tap"); spawn("spark", 4)
                BeepSynth.speak(Voice(400f, floatArrayOf(0.8f, 1.1f), 80), s.muted)
                say("Ой! Лапки щекочутся!")
            }
            else -> {
                anim("tap"); spawn("heart", 4)
                BeepSynth.speak(Voice(520f, floatArrayOf(1f, 1.25f), 80), s.muted)
                say(listOf("Хи-хи! Щекотно!", "Мур-р… то есть хрум!", "Я Финни, твой питомец!", "Погладил — настроение +1!").random())
            }
        }
    }

    fun petMove() {
        if (s.sleeping) return
        val now = System.currentTimeMillis()
        if (now - petLast < 140) return
        petLast = now
        spawn("heart", 1)
        BeepSynth.speak(Voice(700f + Random.nextFloat() * 200f, floatArrayOf(1f, 1.15f), 60), s.muted)
    }

    private fun anim(name: String) {
        animToken++
        _anim.value = AnimEvent(animToken, name)
    }

    private fun spawn(kind: String, n: Int) {
        val emoji = when (kind) {
            "heart" -> "💜"; "spark" -> "✨"; "coin" -> "🪙"; "crumb" -> "🍪"; "drop" -> "🫧"; else -> "✨"
        }
        val list = _particles.value.toMutableList()
        repeat(n) { list.add(Particle(particleId++, emoji, Random.nextFloat(), Random.nextFloat())) }
        _particles.value = list
    }

    fun removeParticle(id: Int) {
        _particles.value = _particles.value.filterNot { it.id == id }
    }

    // ================= Действия ухода и покупки =================
    fun action(id: String) {
        val a = Actions.byId(id) ?: return
        if (s.sleeping) {
            say("Тсс… Финни спит. Покупки подождут до утра!")
            return
        }
        if ((id == "play" || id == "treat") && s.needs.food < 35) fireRule("prio")
        if (a.price > s.balance) {
            fireRule("money")
            return
        }
        val hadSeen = s.seen.contains(id)
        val n = s.needs
        val next = s.copy(
            balance = s.balance - a.price,
            needs = Needs(
                food = clamp(n.food + a.dFood),
                clean = clamp(n.clean + a.dClean),
                joy = clamp(n.joy + a.dFun),
            ),
            dayFeed = s.dayFeed + if (id == "feed") 1 else 0,
            dayWash = s.dayWash + if (id == "wash") 1 else 0,
            dayPlay = s.dayPlay + if (id == "play") 1 else 0,
            onbBuy = true,
            seen = if (hadSeen) s.seen else s.seen + id,
        )
        BeepSynth.speak(a.voice, s.muted)
        if (id == "save") {
            val goal = Content.goal(next.goalId)
            val saved = next.saved + a.price
            commit(next.copy(saved = saved))
            spawn("coin", 5)
            if (saved >= goal.target) say("УРА! Цель «${goal.name}» достигнута! 🎉 Я тобой горжусь.", 5000)
            else say("${a.react.random()} Накоплено $saved/${goal.target}.")
        } else {
            commit(next)
            anim(a.anim)
            when (id) {
                "wash" -> spawn("drop", 6)
                "play" -> spawn("spark", 4)
                "feed" -> spawn("crumb", 5)
                "treat", "vita" -> spawn("spark", 4)
            }
            if (!hadSeen) say(a.teach) else say(a.react.random())
        }
    }

    // ================= Сон и смена дня =================
    fun toggleSleep() {
        if (s.sleeping) {
            wake()
            return
        }
        commit(s.copy(sleeping = true))
        BeepSynth.speak(Voice(140f, floatArrayOf(1f, 0.8f), 280), s.muted)
        say("Псс… Финни уснул в своей спальне. Нажми «Доброе утро» или лампу, чтобы начать новый день.", 4600)
    }

    fun wake() {
        if (!s.sleeping) return
        commit(s.copy(sleeping = false))
        nextDay()
        say("Доброе утро! Финни выспался и готов к новому дню. Баланс: ${s.balance} 🪙", 4200)
    }

    fun nextDay() {
        val old = s
        val n = old.needs
        val pts = (if (n.food >= 50) 1 else 0) + (if (n.clean >= 50) 1 else 0) + (if (n.joy >= 50) 1 else 0)
        val growth = old.growth + pts
        val stage = if (growth >= 8) 3 else if (growth >= 4) 2 else 1
        val day = old.day + 1
        val income = Content.incomeFor(day)
        val needs = Needs(clamp(n.food - 30), clamp(n.clean - 25), clamp(n.joy - 30))
        commit(
            old.copy(
                growth = growth, stage = stage, day = day,
                balance = old.balance + income, needs = needs,
                dayFeed = 0, dayWash = 0, dayPlay = 0,
            )
        )
        val msg = "День ${old.day} итог: кормлений ${old.dayFeed}, мытья ${old.dayWash}, игр ${old.dayPlay}. " +
            "Очки роста +$pts. Карманные деньги +$income 🪙"
        if (minOf(needs.food, needs.clean, needs.joy) < 30) fireRule("care")
        else if (needs.food >= 70 && needs.clean >= 70 && needs.joy >= 70 && old.balance >= 20) fireRule("save")
        else say(msg, 4200)
        if (stage != old.stage) {
            val names = mapOf(1 to "малыш", 2 to "подросший", 3 to "взрослый")
            viewModelScope.launch {
                delay(4400)
                say("Ого! Я ВЫРОС! Теперь я ${names[stage]} — это твоя заслуга, ты хорошо планируешь! 🌟", 5000)
                spawn("spark", 12)
            }
        }
    }

    // ================= Задания (заработок) =================
    fun toggleTasks() {
        val open = !_tasksOpen.value
        _tasksOpen.value = open
        _taskFb.value = null
        if (open && _currentTask.value == null) pickTask()
    }

    private fun pickTask() {
        val unsolved = Content.tasks.filter { !s.solved.contains(it.id) }
        _currentTask.value = (unsolved.firstOrNull() ?: Content.tasks.random()).id
        _taskFb.value = null
    }

    fun answer(optId: String) {
        val t = Content.tasks.find { it.id == _currentTask.value } ?: return
        commit(s.copy(onbTask = true))
        if (optId == t.correct) {
            val first = !s.solved.contains(t.id)
            val reward = if (first) t.reward else PRACTICE_REWARD
            commit(
                s.copy(
                    solved = if (first) s.solved + t.id else s.solved,
                    balance = s.balance + reward,
                    taskCoins = s.taskCoins + reward,
                )
            )
            _taskFb.value = "ok" to (t.explain + " Награда: +$reward 🪙" + if (first) "" else " (тренировка)")
            spawn("coin", 6)
            BeepSynth.speak(Voice(880f, floatArrayOf(1f, 1.25f, 1.5f), 80), s.muted)
        } else {
            _taskFb.value = "no" to t.hint
        }
    }

    fun nextTask() = pickTask()

    // ================= Прочее =================
    fun toggleMute() {
        commit(s.copy(muted = !s.muted))
        if (!s.muted) BeepSynth.speak(Voice(600f, floatArrayOf(1f, 1.3f), 80), false)
    }

    /** Двойное нажатие вместо системного confirm (дружелюбно для детей). */
    fun resetRequest() {
        if (!resetArmed) {
            resetArmed = true
            say("Точно сбросить профиль? Нажми ещё раз.", 2500)
            viewModelScope.launch {
                delay(2500)
                resetArmed = false
            }
            return
        }
        resetArmed = false
        SaveStore.clear(getApplication())
        commit(GameState())
        _tasksOpen.value = false
        _currentTask.value = null
        _taskFb.value = null
        say("Привет! Я Финни. Нажми на меня, ухаживай кнопками ниже и зарабатывай монетки в заданиях!", 5200)
    }

    // ================= Idle-жизнь =================
    private suspend fun idleLoop() {
        while (true) {
            delay(1000)
            idleTick()
        }
    }

    private fun idleTick() {
        if (s.sleeping) return
        idleCd--
        if (idleCd > 0) return
        idleCd = 7 + Random.nextInt(6)
        val grumbles = mutableListOf<String>()
        if (s.needs.food < 45) grumbles += "У меня урчит в животике… покормишь? 🍎"
        if (s.needs.clean < 45) grumbles += "Мои иголки запылились… пора мыться 🧼"
        if (s.needs.joy < 45) grumbles += "Скучно-скучно… поиграем в мячик? 🎾"
        if (grumbles.isNotEmpty() && Random.nextFloat() < 0.6f) {
            say(grumbles.random())
            return
        }
        when (listOf("look", "yawn", "sneeze", "wiggle").random()) {
            "look" -> {
                anim("look")
                BeepSynth.speak(Voice(560f, floatArrayOf(1.1f, 0.95f), 70), s.muted)
            }
            "yawn" -> {
                anim("yawn")
                say("Ааа-а… то есть хи-хи!", 2200)
            }
            "sneeze" -> {
                anim("sneeze")
                viewModelScope.launch {
                    delay(550)
                    spawn("drop", 4)
                }
                BeepSynth.speak(Voice(300f, floatArrayOf(0.7f, 1.4f), 90), s.muted)
            }
            else -> anim("wiggle")
        }
    }

    private fun clamp(v: Int) = v.coerceIn(0, 100)

    fun mood(): String {
        val m = minOf(s.needs.food, s.needs.clean, s.needs.joy)
        return if (m >= 60) "happy" else if (m >= 30) "ok" else "sad"
    }

    companion object {
        const val PRACTICE_REWARD = 2
    }
}
