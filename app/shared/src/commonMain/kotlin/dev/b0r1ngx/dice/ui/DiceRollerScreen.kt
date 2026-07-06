package dev.b0r1ngx.dice.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.onSizeChanged

@Composable
fun DiceRollerScreen(modifier: Modifier = Modifier) {
    val state = remember { DiceRollerState() }

    LaunchedEffect(state.rollId) {
        if (state.rollId == 0L) return@LaunchedEffect
        state.runRollAnimation()
    }

    MaterialTheme {
        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .safeContentPadding(),
        ) {
            val buttonWidth = maxWidth * (1f / 3f)
            val buttonHeight = maxHeight * (1f / 3f)

            Column(Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(2f)
                        .fillMaxWidth()
                        .clipToBounds()
                        .onSizeChanged { state.setContainerSize(it.width.toFloat(), it.height.toFloat()) },
                ) {
                    DiceCanvas(
                        rotationMatrix = state.rotationMatrix,
                        modifier = Modifier.fillMaxSize(),
                        position = state.position,
                        squash = state.squash,
                    )
                }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        onClick = state::roll,
                        enabled = !state.isRolling,
                        modifier = Modifier.size(width = buttonWidth, height = buttonHeight),
                    ) {
                        Text("Roll")
                    }
                    Text(
                        text = "Total: ${state.topFace.pips}",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier
                            .weight(1f)
                            .wrapContentWidth(Alignment.CenterHorizontally),
                    )
                }
            }
        }
    }
}