package ru.finny.app.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.finny.app.R
import ru.finny.app.data.Actions
import ru.finny.app.data.CareAction
import ru.finny.app.data.Content
import ru.finny.app.data.GameState
import ru.finny.app.game.GameViewModel
import ru.finny.app.ui.theme.Bg
import ru.finny.app.ui.theme.CardBg
import ru.finny.app.ui.theme.Line
import ru.finny.app.ui.theme.Muted
import ru.finny.app.ui.theme.Night
import ru.finny.app.ui.theme.Orange
import ru.finny.app.ui.theme.OrangeDark
import ru.finny.app.ui.theme.OrangeLight
import ru.finny.app.ui.theme.Purple
import ru.finny.app.ui.theme.PurpleDark
import ru.finny.app.ui.theme.PurpleLight
import ru.finny.app.ui.theme.TextMain

private val stageNames = mapOf(1 to "малыш", 2 to "подросший", 3 to "взрослый")

@Composable
fun GameScreen(vm: GameViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    val bubble by vm.bubble.collectAsState()
    val particles by vm.particles.collectAsState()
    val anim by vm.anim.collectAsState()
    val tasksOpen by vm.tasksOpen.collectAsState()
    val currentTaskId by vm.currentTask.collectAsState()
    val taskFb by vm.taskFb.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 16.dp),
    ) {
        TopBar(state, vm)
        OnbChips(state)
        PetScene(state, bubble, particles, anim, vm)
        Spacer(Modifier.height(10.dp))
        NeedsBars(state)
        Spacer(Modifier.height(10.dp))
        ActionsGrid(state, vm)
        DayRow(state, vm)
        if (tasksOpen) {
            Spacer(Modifier.height(10.dp))
            TasksCard(currentTaskId, taskFb, vm)
        }
        Spacer(Modifier.height(10.dp))
        GoalCard(state)
    }
}

// ================= Шапка =================
@Composable
private fun TopBar(state: GameState, vm: GameViewModel) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Комната Финни 🐹", style = androidx.compose.material3.MaterialTheme.typography.headlineSmall, color = Purple)
            Text("Ухаживай, покупай, зарабатывай — Финни растёт", style = androidx.compose.material3.MaterialTheme.typography.bodySmall, color = Muted)
            Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Chip("${state.balance} 🪙")
                Chip("день ${state.day}", orange = true)
                Chip("стадия: ${stageNames[state.stage]}")
            }
        }
        IconButton(onClick = { vm.toggleMute() }) {
            Text(if (state.muted) "🔇" else "🔊", fontSize = 20.sp)
        }
        IconButton(onClick = { vm.resetRequest() }) {
            Text("↺", fontSize = 20.sp, color = Purple)
        }
    }
}

@Composable
private fun Chip(text: String, orange: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (orange) OrangeLight else CardBg,
        border = BorderStroke(1.dp, if (orange) Line else Line),
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (orange) OrangeDark else Purple,
        )
    }
}

@Composable
private fun OnbChips(state: GameState) {
    if (state.onbTap && state.onbBuy && state.onbTask) return
    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        OnbChip("Нажми на Финни", state.onbTap)
        OnbChip("Купи уход", state.onbBuy)
        OnbChip("Реши задание", state.onbTask)
    }
}

@Composable
private fun OnbChip(text: String, done: Boolean) {
    Surface(shape = RoundedCornerShape(50), color = if (done) PurpleLight else OrangeLight) {
        Text(
            (if (done) "✔ " else "○ ") + text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (done) Purple else OrangeDark,
        )
    }
}

// ================= Сцена с питомцем =================
@Composable
private fun PetScene(
    state: GameState,
    bubble: String?,
    particles: List<GameViewModel.Particle>,
    anim: GameViewModel.AnimEvent,
    vm: GameViewModel,
) {
    val sleeping = state.sleeping
    val nightAlpha by animateFloatAsState(if (sleeping) 1f else 0f, tween(700), label = "night")
    val petSize by animateFloatAsState(if (sleeping) 160f else 300f, tween(500), label = "petsize")

    // трансформации спрайта: дыхание + событийные анимации
    val inf = rememberInfiniteTransition(label = "breathe")
    val breathe by inf.animateFloat(1f, 1.015f, infiniteRepeatable(tween(1600), RepeatMode.Reverse), label = "breathe")
    val sx = remember { Animatable(1f) }
    val sy = remember { Animatable(1f) }
    val oy = remember { Animatable(0f) }
    val rot = remember { Animatable(0f) }

    LaunchedEffect(anim.token) {
        when (anim.name) {
            "tap", "play", "save" -> {
                oy.animateTo(-16f, tween(250)); oy.animateTo(0f, tween(220))
                sy.animateTo(0.95f, tween(120)); sy.animateTo(1f, tween(180))
            }
            "wash" -> {
                rot.animateTo(-4f, tween(200)); rot.animateTo(4f, tween(200))
                rot.animateTo(-3f, tween(200)); rot.animateTo(0f, tween(200))
            }
            "feed" -> {
                sy.animateTo(0.93f, tween(150)); sy.animateTo(1.04f, tween(150))
                sy.animateTo(0.95f, tween(150)); sy.animateTo(1f, tween(150))
            }
            "look" -> {
                rot.animateTo(-3f, tween(300)); rot.animateTo(3f, tween(350)); rot.animateTo(0f, tween(300))
            }
            "yawn" -> {
                sy.animateTo(1.1f, tween(350)); sy.animateTo(0.96f, tween(300)); sy.animateTo(1f, tween(250))
            }
            "sneeze" -> {
                rot.animateTo(-6f, tween(180)); rot.animateTo(4f, tween(160)); rot.animateTo(0f, tween(200))
            }
            "wiggle" -> {
                rot.animateTo(-5f, tween(180)); rot.animateTo(4f, tween(180))
                rot.animateTo(-3f, tween(180)); rot.animateTo(0f, tween(180))
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(430.dp)
            .clip(RoundedCornerShape(20.dp)),
    ) {
        // комната: днём гостиная, ночью спальня
        androidx.compose.foundation.Image(
            painter = painterResource(if (sleeping) R.drawable.room_night else R.drawable.room_day),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        // ночное затемнение
        Box(Modifier.matchParentSize().background(Night.copy(alpha = Night.alpha * nightAlpha)))

        // реплика-пузырь
        AnimatedVisibility(
            visible = bubble != null,
            enter = fadeIn(tween(200)) + scaleIn(initialScale = 0.7f, animationSpec = tween(200)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
        ) {
            Surface(shape = RoundedCornerShape(14.dp), color = CardBg, border = BorderStroke(2.dp, Purple)) {
                Text(
                    bubble.orEmpty(),
                    modifier = Modifier.padding(12.dp).width(240.dp),
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = PurpleDark,
                )
            }
        }

        // контактная тень
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (sleeping) 96.dp else 26.dp)
                .size(150.dp, 20.dp)
                .graphicsLayer { alpha = if (sleeping) 0f else 0.3f }
                .background(PurpleDark, RoundedCornerShape(50)),
        )

        // Финни: зоны касания + гладилка
        androidx.compose.foundation.Image(
            painter = painterResource(Content.spriteRes(state.stage, vm.mood(), sleeping)),
            contentDescription = "Финни",
            modifier = Modifier
                .align(if (sleeping) Alignment.Center else Alignment.BottomCenter)
                .padding(bottom = if (sleeping) 110.dp else 18.dp)
                .size(petSize.dp)
                .graphicsLayer {
                    scaleX = sx.value * breathe
                    scaleY = sy.value * breathe
                    translationY = oy.value
                    rotationZ = rot.value
                }
                .pointerInput(Unit) {
                    detectTapGestures { off ->
                        val zone = when {
                            off.y < size.height * 0.34f -> "head"
                            off.y < size.height * 0.7f -> "belly"
                            else -> "paws"
                        }
                        vm.tapPet(zone)
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { _, _ -> vm.petMove() }
                },
        )

        // частицы (сердечки, монетки, пузыри, zzz)
        particles.forEach { p -> ParticleView(p, vm) }

        // лампа-ночник
        IconButton(
            onClick = { vm.toggleSleep() },
            modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp).size(72.dp),
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(R.drawable.finny_lamp),
                contentDescription = "Лампа-ночник",
                modifier = Modifier.size(72.dp).graphicsLayer { alpha = if (sleeping) 0.55f else 1f },
            )
        }

        // кнопка пробуждения
        if (sleeping) {
            Button(
                onClick = { vm.wake() },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Orange),
            ) { Text("☀️ Доброе утро!") }
        }
    }
}

@Composable
private fun ParticleView(p: GameViewModel.Particle, vm: GameViewModel) {
    val alphaAnim = remember { Animatable(1f) }
    val dy = remember { Animatable(0f) }
    LaunchedEffect(p.id) {
        launch { alphaAnim.animateTo(0f, tween(1200)) }
        launch { dy.animateTo(-90f, tween(1200)) }
        vm.removeParticle(p.id)
    }
    Text(
        p.emoji,
        fontSize = 20.sp,
        modifier = Modifier
            .offset(x = (30f + p.x * 260f).dp, y = (140f + p.y * 80f + dy.value).dp)
            .graphicsLayer { alpha = alphaAnim.value },
    )
}

// ================= Потребности =================
@Composable
private fun NeedsBars(state: GameState) {
    Row(Modifier.padding(horizontal = 2.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NeedBar(R.drawable.ic_feed, "сытость", state.needs.food, Modifier.weight(1f))
        NeedBar(R.drawable.ic_wash, "чистота", state.needs.clean, Modifier.weight(1f))
        NeedBar(R.drawable.ic_play, "веселье", state.needs.joy, Modifier.weight(1f))
    }
}

@Composable
private fun NeedBar(icon: Int, label: String, value: Int, modifier: Modifier = Modifier) {
    val low = value < 30
    val pulse = rememberInfiniteTransition(label = "pulse")
    val pa by pulse.animateFloat(1f, 0.55f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "pulse")
    Surface(
        shape = RoundedCornerShape(12.dp), color = CardBg,
        border = BorderStroke(1.dp, Line), modifier = modifier,
    ) {
        Column(Modifier.padding(horizontal = 9.dp, vertical = 7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.foundation.Image(
                    painter = painterResource(icon), contentDescription = null,
                    modifier = Modifier.size(15.dp),
                )
                Text(" $label", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Muted)
                Text(" $value", fontSize = 11.sp, fontWeight = FontWeight.Black, color = if (low) OrangeDark else Purple)
            }
            Box(Modifier.padding(top = 5.dp).height(7.dp).fillMaxWidth().clip(RoundedCornerShape(50)).background(PurpleLight)) {
                Box(
                    Modifier
                        .height(7.dp)
                        .fillMaxWidth(value / 100f)
                        .clip(RoundedCornerShape(50))
                        .graphicsLayer { alpha = if (low) pa else 1f }
                        .background(Brush.horizontalGradient(listOf(Orange, androidx.compose.ui.graphics.Color(0xFFFFA24D)))),
                )
            }
        }
    }
}

// ================= Действия =================
@Composable
private fun ActionsGrid(state: GameState, vm: GameViewModel) {
    val disabled = state.sleeping
    val rows = Actions.all.chunked(3)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { a ->
                    ActionTile(a, disabled, vm, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ActionTile(a: CareAction, disabled: Boolean, vm: GameViewModel, modifier: Modifier = Modifier) {
    val save = a.id == "save"
    Surface(
        shape = RoundedCornerShape(14.dp), color = CardBg,
        border = BorderStroke(1.5.dp, Line),
        modifier = modifier.graphicsLayer { alpha = if (disabled) 0.35f else 1f }
            .clickable(enabled = !disabled) { vm.action(a.id) },
    ) {
        Column(
            Modifier.padding(vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.size(56.dp).clip(RoundedCornerShape(16.dp))
                    .background(if (save) PurpleLight else OrangeLight),
                contentAlignment = Alignment.Center,
            ) {
                val icon = when (a.id) {
                    "feed" -> R.drawable.ic_feed
                    "wash" -> R.drawable.ic_wash
                    "play" -> R.drawable.ic_play
                    "treat" -> R.drawable.ic_treat
                    "vita" -> R.drawable.ic_vita
                    else -> R.drawable.ic_save
                }
                androidx.compose.foundation.Image(
                    painter = painterResource(icon), contentDescription = a.id,
                    modifier = Modifier.size(40.dp),
                )
            }
            Text(
                when (a.id) {
                    "feed" -> "Корм"; "wash" -> "Мыть"; "play" -> "Играть"
                    "treat" -> "Вкусняшка"; "vita" -> "Витамины"; else -> "Отложить"
                },
                fontSize = 11.sp, fontWeight = FontWeight.Black, color = TextMain,
            )
            Text("${a.price} 🪙", fontSize = 11.sp, fontWeight = FontWeight.Black, color = if (save) Purple else OrangeDark)
        }
    }
}

// ================= День и задания =================
@Composable
private fun DayRow(state: GameState, vm: GameViewModel) {
    Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { vm.toggleTasks() }, modifier = Modifier.weight(1f)) {
            Text("⭐ Задания")
        }
        if (!state.sleeping) {
            Button(onClick = { vm.nextDay() }, modifier = Modifier.weight(1f)) {
                Text("Следующий день →")
            }
        }
    }
}

@Composable
private fun TasksCard(currentTaskId: String?, fb: Pair<String, String>?, vm: GameViewModel) {
    val task = Content.tasks.find { it.id == currentTaskId } ?: return
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = CardBg)) {
        Column(Modifier.padding(14.dp)) {
            Row {
                Text(task.title, style = androidx.compose.material3.MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text("+${task.reward} 🪙", color = OrangeDark, fontWeight = FontWeight.Black, fontSize = 13.sp)
            }
            Text(task.story, style = androidx.compose.material3.MaterialTheme.typography.bodySmall, color = Muted)
            Text(task.question, style = androidx.compose.material3.MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
            Spacer(Modifier.height(8.dp))
            task.options.forEach { o ->
                Surface(
                    shape = RoundedCornerShape(12.dp), color = CardBg,
                    border = BorderStroke(1.5.dp, Line),
                    modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth()
                        .clickable { vm.answer(o.id) },
                ) {
                    Text(o.text, modifier = Modifier.padding(12.dp), style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
                }
            }
            fb?.let { (kind, text) ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (kind == "ok") PurpleLight else OrangeLight,
                    modifier = Modifier.padding(top = 6.dp).fillMaxWidth(),
                ) {
                    Text(
                        (if (kind == "ok") "Верно! 🎉 " else "Подсказка: ") + text,
                        modifier = Modifier.padding(12.dp), fontSize = 13.sp,
                        color = if (kind == "ok") PurpleDark else OrangeDark,
                    )
                }
                if (kind == "ok") {
                    Button(onClick = { vm.nextTask() }, modifier = Modifier.padding(top = 8.dp).fillMaxWidth()) {
                        Text("Следующее задание →")
                    }
                }
            }
        }
    }
}

// ================= Цель накоплений =================
@Composable
private fun GoalCard(state: GameState) {
    val goal = Content.goal(state.goalId)
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = CardBg)) {
        Column(Modifier.padding(14.dp)) {
            Text("${goal.icon} Цель: ${goal.name}", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            Text("Накоплено ${state.saved} из ${goal.target} 🪙", style = androidx.compose.material3.MaterialTheme.typography.bodySmall, color = Muted)
            Box(Modifier.padding(top = 8.dp).height(8.dp).fillMaxWidth().clip(RoundedCornerShape(50)).background(PurpleLight)) {
                Box(
                    Modifier.height(8.dp).fillMaxWidth(minOf(1f, state.saved.toFloat() / goal.target))
                        .clip(RoundedCornerShape(50))
                        .background(Brush.horizontalGradient(listOf(Orange, androidx.compose.ui.graphics.Color(0xFFFFA24D)))),
                )
            }
        }
    }
}
