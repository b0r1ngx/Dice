package dev.b0r1ngx.dice.ui

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import dev.b0r1ngx.dice.die.DieFace
import kotlin.math.min

private val PIP_FRAC = floatArrayOf(0.24f, 0.5f, 0.76f)

/** Isometric 3D cube renderer; pips are baked into the visible faces. */
@Composable
internal fun DiceCanvas(
    rotationMatrix: Mat3,
    modifier: Modifier = Modifier,
    position: Vec2 = Vec2(0f, 0f),
    squash: Vec2 = Vec2(1f, 1f),
) {
    val topColor = MaterialTheme.colorScheme.surface
    val sideColor = MaterialTheme.colorScheme.surfaceVariant
    val pipColor = MaterialTheme.colorScheme.onSurface
    val edgeColor = MaterialTheme.colorScheme.outlineVariant

    Canvas(modifier = modifier) {
        val scale = min(size.width, size.height) * CUBE_SCALE_FRACTION
        val cx = size.width / 2f + position.x
        val cy = size.height / 2f + position.y

        val faces = applySquash(visibleFaces(rotationMatrix, scale, cx, cy), squash, cx, cy)

        for (face in faces) {
            val fill = if (face.isTop) topColor else sideColor
            val strokeW = face.edgeLen * 0.04f
            val stroke = Stroke(width = strokeW)
            val corners = face.corners.map { Offset(it.x, it.y) }

            drawQuad(corners, fill, edgeColor, stroke)

            val pipRadius = face.edgeLen * 0.085f
            drawPips(pipPositions(corners), face.value, pipColor, pipRadius)
        }
    }
}

private fun pipPositions(corners: List<Offset>): List<Offset> {
    val ax = corners[0].x
    val ay = corners[0].y
    val e1x = corners[1].x - corners[0].x
    val e1y = corners[1].y - corners[0].y
    val e2x = corners[3].x - corners[0].x
    val e2y = corners[3].y - corners[0].y
    return List(9) { index ->
        val row = index / 3
        val col = index % 3
        Offset(
            x = ax + e1x * PIP_FRAC[col] + e2x * PIP_FRAC[row],
            y = ay + e1y * PIP_FRAC[col] + e2y * PIP_FRAC[row],
        )
    }
}

private fun DrawScope.drawQuad(corners: List<Offset>, fill: Color, stroke: Color, strokeStyle: Stroke) {
    val path = Path().apply {
        moveTo(corners[0].x, corners[0].y)
        lineTo(corners[1].x, corners[1].y)
        lineTo(corners[2].x, corners[2].y)
        lineTo(corners[3].x, corners[3].y)
        close()
    }
    drawPath(path, fill)
    drawPath(path, stroke, style = strokeStyle)
}

private fun DrawScope.drawPips(positions: List<Offset>, face: DieFace, color: Color, radius: Float) {
    for (index in pipCells(face)) {
        drawCircle(color = color, radius = radius, center = positions[index])
    }
}

private fun pipCells(face: DieFace): List<Int> = when (face) {
    DieFace.ONE -> listOf(4)
    DieFace.TWO -> listOf(0, 8)
    DieFace.THREE -> listOf(0, 4, 8)
    DieFace.FOUR -> listOf(0, 2, 6, 8)
    DieFace.FIVE -> listOf(0, 2, 4, 6, 8)
    DieFace.SIX -> listOf(0, 2, 3, 5, 6, 8)
}
