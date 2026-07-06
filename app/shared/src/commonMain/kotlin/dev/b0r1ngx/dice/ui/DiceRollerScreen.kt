package dev.b0r1ngx.dice.ui

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

@Composable
fun DiceRollerScreen(modifier: Modifier = Modifier) {
    val state = remember { DiceRollerState() }

    LaunchedEffect(state.rollId) {
        if (state.rollId == 0L) return@LaunchedEffect
        state.runRollAnimation()
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
                rotationMatrix = state.rotationMatrix,
                modifier = Modifier.size(220.dp),
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = "Total: ${state.topFace.pips}",
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = state::roll, enabled = !state.isRolling) {
                Text("Roll")
            }
        }
    }
}