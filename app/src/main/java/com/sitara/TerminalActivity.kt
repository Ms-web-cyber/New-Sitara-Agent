package com.sitara

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.sitara.data.preferences.AuthPreferences
import com.sitara.theme.GeminiTheme
import com.sitara.theme.isSystemInDarkThemeRobust
import com.sitara.ui.components.LocalTerminalDialog

class TerminalActivity : ComponentActivity() {

    private val authPreferences: AuthPreferences by lazy { AuthPreferences(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        com.sitara.data.service.SitaraService.start(this)

        setContent {
            val initialThemeMode = remember { authPreferences.getThemeModeSync() }
            val themeMode by authPreferences.themeMode.collectAsState(initial = initialThemeMode)
            val isSystemDark = isSystemInDarkThemeRobust()
            val useDarkTheme = when (themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemDark
            }

            GeminiTheme(darkTheme = useDarkTheme) {
                LocalTerminalDialog(
                    onDismiss = { finish() }
                )
            }
        }
    }
}
