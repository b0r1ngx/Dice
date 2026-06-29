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
import androidx.compose.ui.graphics.drawscope.rotate
import dev.b0r1ngx.dice.die.DieFace
import kotlin.math.min

private const val COS_30 = 0.8660254f
private const val SIN_30 = 0.5f

private val PIP_FRAC = floatArrayOf(0.24f, 0.5f, 0.76f)

private class Quad(val a: Offset, val b: Offset, val c: Offset, val d: Offset)

@Composable
fun DiceCanvas(
    rotationDeg: Float,
    topFace: DieFace,
    modifier: Modifier = Modifier,
) {
    val topColor = MaterialTheme.colorScheme.surface
    val sideColor = MaterialTheme.colorScheme.surfaceVariant
    val pipColor = MaterialTheme.colorScheme.onSurface
    val edgeColor = MaterialTheme.colorScheme.outlineVariant

    val leftFace = DieFace.entries[(topFace.ordinal + 1) % DieFace.entries.size]
    val rightFace = DieFace.entries[(topFace.ordinal + 2) % DieFace.entries.size]

    Canvas(modifier = modifier) {
        val edge = min(size.width, size.height) * 0.26f
        val right = Offset(edge * COS_30, edge * SIN_30)
        val left = Offset(-edge * COS_30, edge * SIN_30)
        val up = Offset(0f, -edge)
        val center = Offset(size.width / 2f, size.height / 2f)
        val base = Offset(
            center.x - (right.x + left.x + up.x) / 2f,
            center.y - (right.y + left.y + up.y) / 2f,
        )
        fun corner(i: Int, j: Int, k: Int): Offset =
            Offset(base.x + right.x * i + left.x * j + up.x * k,
                   base.y + right.y * i + left.y * j + up.y * k)

        val top = Quad(corner(0, 0, 1), corner(1, 0, 1), corner(1, 1, 1), corner(0, 1, 1))
        val leftSide = Quad(corner(0, 1, 0), corner(1, 1, 0), corner(1, 1, 1), corner(0, 1, 1))
        val rightSide = Quad(corner(1, 0, 0), corner(1, 1, 0), corner(1, 1, 1), corner(1, 0, 1))

        val pipRadius = edge * 0.085f
        val stroke = Stroke(width = edge * 0.04f)

        rotate(rotationDeg, pivot = center) {
            drawQuad(leftSide, sideColor, edgeColor, stroke)
            drawQuad(rightSide, sideColor, edgeColor, stroke)
            drawQuad(top, topColor, edgeColor, stroke)

            drawPips(pipPositions(leftSide.a, right, up), leftFace, pipColor, pipRadius)
            drawPips(pipPositions(rightSide.a, left, up), rightFace, pipColor, pipRadius)
            drawPips(pipPositions(top.a, right, left), topFace, pipColor, pipRadius)
        }
    }
}

private fun pipPositions(origin: Offset, axisA: Offset, axisB: Offset): List<Offset> =
    listOf(
        origin + axisA * PIP_FRAC[0] + axisB * PIP_FRAC[0],
        origin + axisA * PIP_FRAC[0] + axisB * PIP_FRAC[1],
        origin + axisA * PIP_FRAC[0] + axisB * PIP_FRAC[2],
        origin + axisA * PIP_FRAC[1] + axisB * PIP_FRAC[0],
        origin + axisA * PIP_FRAC[1] + axisB * PIP_FRAC[1],
        origin + axisA * PIP_FRAC[1] + axisB * PIP_FRAC[2],
        origin + axisA * PIP_FRAC[2] + axisB * PIP_FRAC[0],
        origin + axisA * PIP_FRAC[2] + axisB * PIP_FRAC[1],
        origin + axisA * PIP_FRAC[2] + axisB * PIP_FRAC[2],
    )

private fun DrawScope.drawQuad(quad: Quad, fill: Color, stroke: Color, strokeStyle: Stroke) {
    val path = Path().apply {
        moveTo(quad.a.x, quad.a.y)
        lineTo(quad.b.x, quad.b.y)
        lineTo(quad.c.x, quad.c.y)
        lineTo(quad.d.x, quad.d.y)
        close()
    }
    drawPath(path, fill)
    drawPath(path, stroke, style = strokeStyle)
}

private fun DrawScope.drawPips(positions: List<Offset>, face: DieFace, color: Color, radius: Float) {
    val cells = pipCells(face)
    for (index in cells) {
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