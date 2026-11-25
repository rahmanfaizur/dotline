package dev.rahmanfaizur.dotline

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import dev.rahmanfaizur.dotline.data.AppEntry
import dev.rahmanfaizur.dotline.ui.LocalIconLoader
import dev.rahmanfaizur.dotline.ui.drawer.DrawerScreen
import dev.rahmanfaizur.dotline.ui.home.HomeScreen
import dev.rahmanfaizur.dotline.ui.theme.Dot

class MainActivity : ComponentActivity() {

    /** Bumped every time Home is pressed while we're already the home screen. */
    private var homePresses by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Light status/nav bar icons: the whole launcher sits on black.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        val app = application as DotlineApp
        setContent {
            CompositionLocalProvider(LocalIconLoader provides app.icons) {
                Launcher(app, homePresses)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) homePresses++
    }
}

@Composable
private fun Launcher(app: DotlineApp, homePresses: Int) {
    val apps by app.apps.apps.collectAsState()
    var drawerOpen by rememberSaveable { mutableStateOf(false) }
    var focusSearch by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    val focus = LocalFocusManager.current

    fun closeDrawer() {
        drawerOpen = false
        focusSearch = false
        query = ""
        focus.clearFocus()
    }

    fun launch(entry: AppEntry) {
        app.apps.launch(entry)
        closeDrawer()
    }

    // Home button: always come back to the clean home screen.
    LaunchedEffect(homePresses) { if (homePresses > 0) closeDrawer() }
    // A launcher sits at the bottom of the back stack; Back only closes the drawer.
    BackHandler { if (drawerOpen) closeDrawer() }

    Box(Modifier.fillMaxSize().background(Dot.Black)) {
        HomeScreen(onOpenApps = { withSearch ->
            focusSearch = withSearch
            drawerOpen = true
        })
        AnimatedVisibility(
            visible = drawerOpen,
            enter = fadeIn(tween(160)) + slideInVertically(tween(220)) { it / 8 },
            exit = fadeOut(tween(140)) + slideOutVertically(tween(180)) { it / 8 },
        ) {
            DrawerScreen(
                apps = apps,
                query = query,
                onQueryChange = { query = it },
                focusSearch = focusSearch,
                onLaunch = ::launch,
            )
        }
    }
}
