package dev.b0r1ngx.dice

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Dice",
    ) {
        App()
    }
}