# «Питомец Финни» — Android-проект (Kotlin + Jetpack Compose)

Порт игрового варианта `game(6).html` (предпоследний, стабильный) в нативное Android-приложение
для Android Studio. Весь контент и механики — из части участника 2: экономика, уход, задания,
сон, адаптивный помощник, голос Финни.

## Как открыть и запустить
1. Android Studio (Hedgehog 2023.1.1 или новее) → **File → Open** → выбрать эту папку (`finni_android`).
2. Дождаться синхронизации Gradle (JDK 17 ставится самой студией: Settings → Build Tools → Gradle → Gradle JDK = 17).
3. Запустить на устройстве/эмуляторе с Android 8.0+ (minSdk 26): кнопка **Run ▶**.
   Интернет приложению не нужен: контент в `assets/json`, графика в `res/drawable-nodpi`,
   шрифт в `res/font`, голос синтезируется на устройстве.

## Структура
```
finni_android/
├── settings.gradle.kts / build.gradle.kts / gradle.properties / gradlew(.bat) / gradle/wrapper/
└── app/
    ├── build.gradle.kts                  # Compose BOM 2024.10, Kotlin 2.0.21, Gson, minSdk 26
    └── src/main/
        ├── AndroidManifest.xml           # одна activity, портрет, офлайн
        ├── assets/json/                  # контент по п. 2.5.14 ТЗ (правки текстов — здесь)
        │   ├── tasks.json  goals.json  periods.json  products.json
        ├── res/
        │   ├── drawable-nodpi/           # 12 спрайтов (3 стадии × 3 настроения + 3 сна),
        │   │                             # лампа-ночник, 6 глиняных иконок, 2 фона комнаты
        │   ├── font/nunito.ttf           # Nunito (вариативный, кириллица)
        │   └── values/                   # темы/строки
        └── java/ru/finny/app/
            ├── MainActivity.kt           # точка входа, setContent { FinnyTheme { GameScreen() } }
            ├── data/
            │   ├── Models.kt             # Task/Goal/Period/Needs/GameState/CareAction/Voice
            │   ├── Content.kt            # загрузка JSON (Gson) + карта спрайтов + действия ухода
            │   └── SaveStore.kt          # локальный профиль: SharedPreferences + Gson (офлайн)
            ├── game/
            │   ├── GameViewModel.kt      # ВСЯ логика: баланс, потребности, дни, рост, сон,
            │   │                         # задания, адаптивные правила, idle-жизнь, частицы
            │   └── BeepSynth.kt          # «голос» Финни: PCM-синтез писков (AudioTrack), без файлов
            └── ui/
                ├── theme/                # палитра (фиолет/оранж/белый), Nunito, MaterialTheme
                └── game/GameScreen.kt    # сцена, спрайт с анимациями, полосы потребностей,
                                          # 6 кнопок ухода, задания, цель, пузырь, частицы
```

## Соответствие HTML → Kotlin
| Было в `game(6).html` | Стало в проекте |
|---|---|
| `state` + `localStorage`-логика | `GameState` + `SaveStore` (SharedPreferences + Gson) |
| `DATA` (json, встроенный base64) | `assets/json/*.json` + `Content.load()` (Gson) |
| `SPRITES` (data-uri) | `res/drawable-nodpi/finny_*.png` + `Content.spriteRes(stage, mood, sleeping)` |
| `ROOMS` (фоны) | `room_day.jpg` / `room_night.jpg` + `animateFloatAsState` для ночной смены |
| `ACTIONS` | `Actions.all` в `Content.kt` (цены, эффекты, обучалки, «голос») |
| `squeak()` (WebAudio) | `BeepSynth.speak()` (AudioTrack, PCM 16-bit, синтез в фоне) |
| CSS-анимации (breathe/jump/wash/munch/idle) | `Animatable` + `graphicsLayer` + `rememberInfiniteTransition` |
| частицы (💜✨🪙🫧💤) | `GameViewModel.Particle` + `ParticleView` (alpha/offset-анимации) |
| `RULES` (prio/money/care/save, 3 уровня) | `ruleTexts` + `fireRule()` в `GameViewModel` |
| зоны касания / гладилка | `pointerInput { detectTapGestures / detectDragGestures }` |
| idle-цикл (`setInterval`) | `viewModelScope.launch { idleLoop() }` |
| онбординг-чипы, пузырь, тосты | `OnbChips`, `AnimatedVisibility`-пузырь в `PetScene` |

## Механики (как в одобренном HTML-варианте)
- **Экономика:** баланс, 6 действий ухода с ценами, проверка «хватает ли монет», копилка с целью
  и прогресс-баром, карманные деньги раз в день (`periods.json`), награды за задания (повтор = +2).
- **Потребности:** сытость/чистота/веселье, затухание −30/−25/−30 за день, пульс-предупреждение < 30.
- **Рост:** очки заботы за день (3 флага ≥50) → стадии малыш → подросший (≥4) → взрослый (≥8);
  9 визуальных комбинаций = 3 стадии × 3 настроения + 3 спящих спрайта.
- **Сон:** лампа-ночник → ночная комната + спящий спрайт + «💤»; «Доброе утро» = новый день.
- **Адаптивность:** обучалки при первом действии, 3 уровня строгости правил, idle-реплики и
  жалобки по потребностям, онбординг-чипы «нажми / купи / реши».
- **Демо-режим:** день завершается кнопкой, без привязки к реальному времени (разрешение ТЗ).
- **Сброс:** двойное нажатие «↺» (без системных confirm — дружелюбно для детей).

## Правки контента
- Тексты заданий/целей/периодов — в `assets/json/*.json` (структура полей — в `Models.kt`).
- Новые спрайты/иконки — PNG с прозрачностью в `res/drawable-nodpi/` + ветка в `Content.spriteRes`.
- Цены/эффекты/реплики действий — `Actions.all` в `Content.kt`.
- Палитра и шрифт — `ui/theme/Color.kt`, `Type.kt`.

## Заметки
- minSdk 26 выбран под вариативный шрифт Nunito; если нужен 24 — замените `res/font/nunito.ttf`
  на статические TTF (Regular/Bold/Black) и поправьте `Type.kt`.
- `products.json` лежит в ассетах про запас (для экранов бюджета/покупок участника 2 из
  `prototype.html`) — игровой экран его не использует, как и в HTML-варианте.
- APK собирается офлайн: `./gradlew assembleDebug` (первый раз скачается только Gradle-distribution).
