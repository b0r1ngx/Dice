package dev.b0r1ngx.dice.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.b0r1ngx.dice.die.DieFace
import dev.b0r1ngx.dice.die.rollD6
import kotlin.math.ceil
import kotlin.random.Random

internal const val SPINS = 3
internal const val ROLL_DURATION_MS = 1_200
internal const val FACE_STEP_DEG = 60f

/**
 * UI-layer state holder for the dice roller. Owns the roll animation so the
 * [DiceRollerScreen] composable stays declarative.
 *
 * The rolled *result* is decided by the Service layer ([rollD6] in `core`);
 * only animation/derived-display state lives here.
 *
 * Two independent quantities are driven by a single progress value so that the
 * idle -> rolling -> settled states form one continuous curve:
 *  - [spinDeg]: the cube's visual rotation. Always `0` at rest (cube upright,
 *    result face on top), spins forward and returns upright at the end.
 *  - [displayFace]: a pure function of the continuous face phase, so there is
 *    no snap between the resting face and the rolling face.
 */
class DiceRollerState(
    private val random: Random = Random.Default,
) {
    private val progress = Animatable(0f)
    private var restPhase by mutableStateOf(0f)
    private var rollSpan by mutableStateOf(0f)

    val spinDeg by derivedStateOf { progress.value * SPINS * 360f }

    val displayFace by derivedStateOf {
        val n = DieFace.entries.size
        val step = (restPhase + progress.value * rollSpan) / FACE_STEP_DEG
        DieFace.entries[((step.toInt() % n) + n) % n]
    }

    var face: DieFace by mutableStateOf(DieFace.ONE)
        private set

    var isRolling: Boolean by mutableStateOf(false)
        private set

    var rollId: Long by mutableStateOf(0L)
        private set

    fun roll() {
        if (isRolling) return
        face = rollD6(random)
        isRolling = true
        rollId += 1L
    }

    suspend fun runRollAnimation() {
        val from = restPhase
        val target = nextPhaseTarget(from, face)
        rollSpan = target - from
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = ROLL_DURATION_MS, easing = FastOutSlowInEasing),
        )
        restPhase = target
        progress.snapTo(0f)
        onSettled()
    }

    private fun nextPhaseTarget(from: Float, targetFace: DieFace): Float {
        val n = DieFace.entries.size
        val minSteps = ceil((from + SPINS * 360f) / FACE_STEP_DEG).toInt()
        var steps = minSteps
        while (((steps % n) + n) % n != targetFace.ordinal) steps++
        return steps * FACE_STEP_DEG
    }

    fun onSettled() {
        isRolling = false
    }
}