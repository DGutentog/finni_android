package ru.finny.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ru.finny.app.ui.game.GameScreen
import ru.finny.app.ui.theme.FinnyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FinnyTheme {
                GameScreen()
            }
        }
    }
}
