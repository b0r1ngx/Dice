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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.sp

private const val ROLL_TEXT_EM_WIDTH = 10f
private const val ROLL_TEXT_HEIGHT_DIVISOR = 5f
private const val TOTAL_TEXT_EM_WIDTH = 7f
private const val TOTAL_TEXT_HEIGHT_DIVISOR = 3f
private const val BUTTON_HEIGHT_FRACTION = 0.2f

@Composable
fun DiceRollerScreen(modifier: Modifier = Modifier) {
    val state = remember { DiceRollerState() }

    LaunchedEffect(state.rollId) {
        if (state.rollId == 0L) return@LaunchedEffect
        state.runRollAnimation()
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize().safeContentPadding()) {
        val buttonWidth = maxWidth * (1f / 3f)
        val buttonHeight = minOf(maxWidth, maxHeight) * BUTTON_HEIGHT_FRACTION
        val density = LocalDensity.current
        val rollFontSize = with(density) {
            val widthSp = buttonWidth.toSp().value
            val heightSp = buttonHeight.toSp().value
            minOf(widthSp / ROLL_TEXT_EM_WIDTH, heightSp / ROLL_TEXT_HEIGHT_DIVISOR).sp
        }
        val totalSlotWidth = maxWidth - buttonWidth
        val totalFontSize = with(density) {
            val widthSp = totalSlotWidth.toSp().value
            val heightSp = buttonHeight.toSp().value
            minOf(widthSp / TOTAL_TEXT_EM_WIDTH, heightSp / TOTAL_TEXT_HEIGHT_DIVISOR).sp
        }

        Column(Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
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
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = state::roll,
                    enabled = !state.isRolling,
                    modifier = Modifier.size(width = buttonWidth, height = buttonHeight),
                ) {
                    Text(text = "Roll", fontSize = rollFontSize, maxLines = 1)
                }

                Text(
                    text = "Total: ${state.topFace.pips}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontSize = totalFontSize,
                    maxLines = 1,
                    modifier = Modifier
                        .weight(1f)
                        .wrapContentWidth(Alignment.CenterHorizontally),
                )
            }
        }
    }
}