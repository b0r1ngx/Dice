package dev.b0r1ngx.dice.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import dev.b0r1ngx.dice.die.DieFace
import dev.b0r1ngx.dice.die.rollD6
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.random.Random
import kotlinx.coroutines.isActive
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch

internal const val SPINS = 10
// with this we can control visible speed of die roll in air
internal const val ROLL_DURATION_MS = 1_000

internal const val RESTITUTION = 0.5f
internal const val MAX_SQUASH = 0.25f
// Apex height as a fraction of the die-space travel (kept < 1 so the arc never
// punches the ceiling — the die always lands on the floor at [ROLL_DURATION_MS]).
internal const val APEX_FRACTION = 0.85f
internal const val SQUASH_DECAY_PER_S = 9f
internal const val SIDE_DRIFT_FRACTION = 0.18f
internal const val IMPACT_REF_SPEED_PX_PER_S = 1500f

/**
 * Compose state holder driving the dice roll: tumble orientation (quaternion
 * slerp + spins) and the gravity/bounce/squash physics loop. The rolled
 * [face] is decided by `core`'s [rollD6]; only animation state lives here.
 */
class DiceRollerState(
    private val random: Random = Random.Default,
) {
    private val progress = Animatable(0f)
    private var startQuat by mutableStateOf(Quat.identity())
    private var targetQuat by mutableStateOf(Quat.identity())
    private var restQuat by mutableStateOf(Quat.identity())

    private var container by mutableStateOf(Vec2(0f, 0f))

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

    /** Cube-center offset (px) from the die-space center; animated during a roll. */
    internal var position by mutableStateOf(Vec2(0f, 0f))
        private set

    /** Per-axis scale (x, y) for squash & stretch; (1, 1) is rest. */
    internal var squash by mutableStateOf(Vec2(1f, 1f))
        private set

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

    fun setContainerSize(widthPx: Float, heightPx: Float) {
        if (widthPx <= 0f || heightPx <= 0f) return
        val firstLayout = container.x <= 0f || container.y <= 0f
        container = Vec2(widthPx, heightPx)
        if (firstLayout && !isRolling) {
            position = restPosition()
            squash = Vec2(1f, 1f)
        }
    }

    suspend fun runRollAnimation() {
        val cw = container.x
        val ch = container.y
        startQuat = restQuat
        targetQuat = orientationForFaceOnTop(face)
        progress.snapTo(0f)

        coroutineScope {
            launch {
                progress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = ROLL_DURATION_MS, easing = FastOutSlowInEasing),
                )
            }
            if (cw > 0f && ch > 0f) {
                launch { runPhysicsLoop(cw, ch) }
            }
        }

        restQuat = targetQuat
        progress.snapTo(0f)
        squash = Vec2(1f, 1f)
        onSettled()
    }

    private suspend fun runPhysicsLoop(cw: Float, ch: Float) {
        val scale = min(cw, ch) * CUBE_SCALE_FRACTION
        val containerRect = Rect2d(0f, cw, 0f, ch)

        val startSil = projectedSilhouette(visibleFaces(startQuat.toMat3(), scale, cw / 2f, ch / 2f))
        val halfH = (startSil.maxY - startSil.minY) * 0.5f
        val floorY = ch / 2f - halfH
        val ceilingY = -ch / 2f + halfH
        val travel = (floorY - ceilingY).coerceAtLeast(1f)
        val apex = travel * APEX_FRACTION
        val airTimeS = ROLL_DURATION_MS / 1000f
        val gravity = 8f * apex / (airTimeS * airTimeS)
        val launchSpeed = 4f * apex / airTimeS
        val dirX = if (random.nextBoolean()) 1f else -1f
        var velocity = Vec2(dirX * launchSpeed * SIDE_DRIFT_FRACTION, -launchSpeed)

        position = Vec2(position.x, floorY)
        squash = Vec2(1f, 1f)

        var prevNanos = -1L

        while (currentCoroutineContext().isActive) {
            val now = withFrameNanos { it }
            if (prevNanos < 0L) {
                prevNanos = now
                continue
            }
            val dt = ((now - prevNanos) / 1_000_000_000f).coerceIn(0f, 1f / 30f)
            prevNanos = now

            velocity += Vec2(0f, gravity * dt)
            position += velocity * dt

            val cx = cw / 2f + position.x
            val cy = ch / 2f + position.y
            val t = progress.value
            val m = (spinQuat(t) * slerp(startQuat, targetQuat, t)).toMat3()
            val sil = projectedSilhouette(visibleFaces(m, scale, cx, cy))

            val impact = detectWallImpact(sil, containerRect)
            if (impact != null) {
                val incoming = (-velocity.dot(impact.normal)).coerceAtLeast(0f)
                position += impact.normal * impact.depth
                if (impact.normal.y < 0f) break // landed on the floor: rest here, no teleport
                velocity = bounceVelocity(velocity, impact.normal, RESTITUTION)
                val intensity = (incoming / IMPACT_REF_SPEED_PX_PER_S).coerceIn(0f, 1f)
                squash = squashForImpact(impact.normal, intensity, MAX_SQUASH)
            }
            decaySquash(dt)
        }
    }

    private fun decaySquash(dt: Float) {
        val decay = 1f - exp(-SQUASH_DECAY_PER_S * dt)
        squash = Vec2(
            1f + (squash.x - 1f) * (1f - decay),
            1f + (squash.y - 1f) * (1f - decay),
        )
    }

    private fun restPosition(): Vec2 {
        val cw = container.x
        val ch = container.y
        if (cw <= 0f || ch <= 0f) return Vec2(0f, 0f)
        val scale = min(cw, ch) * CUBE_SCALE_FRACTION
        val faces = visibleFaces(restQuat.toMat3(), scale, cw / 2f, ch / 2f)
        val sil = projectedSilhouette(faces)
        val halfH = (sil.maxY - sil.minY) * 0.5f
        return Vec2(0f, ch / 2f - halfH)
    }

    private fun spinQuat(t: Float): Quat =
        Quat.fromAxisAngle(TUMBLE_AXIS, (SPINS * 2f * PI * t).toFloat())

    fun onSettled() {
        isRolling = false
    }
}