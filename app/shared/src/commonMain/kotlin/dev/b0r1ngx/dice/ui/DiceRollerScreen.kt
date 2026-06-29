package dev.b0r1ngx.dice.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.b0r1ngx.dice.die.DieFace

private const val SPINS = 3
private const val ROLL_DURATION_MS = 1_200
private const val FACE_STEP_DEG = 60f

@Composable
fun DiceRollerScreen(modifier: Modifier = Modifier) {
    val state = remember { DiceRollerState() }
    val rotation = remember { Animatable(0f) }

    LaunchedEffect(state.rollId) {
        if (state.rollId == 0L) return@LaunchedEffect
        rotation.snapTo(0f)
        rotation.animateTo(
            targetValue = SPINS * 360f,
            animationSpec = tween(durationMillis = ROLL_DURATION_MS, easing = FastOutSlowInEasing),
        )
        state.onSettled()
    }

    val currentRotation = rotation.value
    val shownFace = if (state.isRolling) {
        DieFace.entries[((currentRotation / FACE_STEP_DEG).toInt() % DieFace.entries.size)]
    } else {
        state.face
    }

    MaterialTheme {
        Column(
            modifier = modifier
                .fillMaxSize()
                .safeContentPadding(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            DiceCanvas(
                rotationDeg = currentRotation,
                topFace = shownFace,
                modifier = Modifier.size(220.dp),
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = "Total: ${if (state.isRolling) "?" else state.face.pips}",
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = state::roll, enabled = !state.isRolling) {
                Text("Roll")
            }
        }
    }
}