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
import kotlin.math.PI
import kotlin.random.Random

internal const val SPINS = 3
internal const val ROLL_DURATION_MS = 1_200

/**
 * UI-layer state holder for the dice roller. Owns the roll animation so the
 * [DiceRollerScreen] composable stays declarative.
 *
 * The rolled *result* is decided by the Service layer ([rollD6] in `core`);
 * only animation/derived-display state lives here. The 3D math itself is
 * Compose-free and lives in [DiceGeometry].
 *
 * The die is a real rigid 3D body: each face has a fixed pip value, and the
 * visible faces are a pure function of the cube's orientation ([orientation]).
 * A roll animates the orientation from "previous result on top" to "new result
 * on top" via quaternion slerp, plus extra full spins around a tumble axis so
 * the idle -> rolling -> settled states form one continuous motion with no
 * snaps and no value regeneration.
 */
class DiceRollerState(
    private val random: Random = Random.Default,
) {
    private val progress = Animatable(0f)
    private var startQuat by mutableStateOf(Quat.identity())
    private var targetQuat by mutableStateOf(Quat.identity())
    private var restQuat by mutableStateOf(Quat.identity())

    /** Current 3D orientation of the cube, animated during a roll. */
    private val orientation: Quat by derivedStateOf {
        val t = progress.value
        if (t <= 0f) restQuat
        else spinQuat(t) * slerp(startQuat, targetQuat, t)
    }

    /** Rotation matrix derived from [orientation]; what the canvas renders. */
    internal val rotationMatrix: Mat3 by derivedStateOf { orientation.toMat3() }

    /** The face currently on top of the cube (live, even while tumbling). */
    val topFace: DieFace by derivedStateOf { topFaceOf(rotationMatrix) }

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
        startQuat = restQuat
        targetQuat = orientationForFaceOnTop(face)
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = ROLL_DURATION_MS, easing = FastOutSlowInEasing),
        )
        restQuat = targetQuat
        progress.snapTo(0f)
        onSettled()
    }

    private fun spinQuat(t: Float): Quat =
        Quat.fromAxisAngle(TUMBLE_AXIS, (SPINS * 2f * PI * t).toFloat())

    fun onSettled() {
        isRolling = false
    }
}