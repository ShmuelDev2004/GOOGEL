package app.sederyom.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import app.sederyom.SederYomApplication
import app.sederyom.di.AppContainer

@Composable
fun rememberAppContainer(): AppContainer {
    val application = LocalContext.current.applicationContext as SederYomApplication
    return application.container
}
