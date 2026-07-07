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
import kotlin.math.sqrt
import kotlin.random.Random
import kotlinx.coroutines.isActive
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch

internal const val SPINS = 3
internal const val ROLL_DURATION_MS = 1_200

internal const val GRAVITY_PX_PER_S2 = 4000f
internal const val RESTITUTION = 0.5f
internal const val MAX_SQUASH = 0.25f
internal const val APEX_FRACTION = 1.1f
internal const val SQUASH_DECAY_PER_S = 9f
internal const val SETTLE_SPEED_PX_PER_S = 40f
internal const val SIDE_DRIFT_FRACTION = 0.18f
internal const val IMPACT_REF_SPEED_PX_PER_S = 1500f
internal const val SETTLE_SAFETY_MS = 3_000
internal const val HORIZONTAL_DRAG_PER_S = 1.0f

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
        val travelH = (floorY - ceilingY).coerceAtLeast(1f) * APEX_FRACTION
        val v0 = sqrt(2f * GRAVITY_PX_PER_S2 * travelH)
        val dirX = if (random.nextBoolean()) 1f else -1f
        var velocity = Vec2(dirX * v0 * SIDE_DRIFT_FRACTION, -v0)

        position = Vec2(position.x, floorY)
        squash = Vec2(1f, 1f)

        val maxNanos = SETTLE_SAFETY_MS * 1_000_000L
        var startNanos = 0L
        var prevNanos = -1L

        while (currentCoroutineContext().isActive) {
            val now = withFrameNanos { it }
            if (prevNanos < 0L) {
                startNanos = now
                prevNanos = now
                continue
            }
            val dt = ((now - prevNanos) / 1_000_000_000f).coerceIn(0f, 1f / 30f)
            prevNanos = now

            velocity += Vec2(0f, GRAVITY_PX_PER_S2 * dt)
            position += velocity * dt
            velocity = Vec2(velocity.x * exp(-HORIZONTAL_DRAG_PER_S * dt), velocity.y)

            val cx = cw / 2f + position.x
            val cy = ch / 2f + position.y
            val t = progress.value
            val m = (spinQuat(t) * slerp(startQuat, targetQuat, t)).toMat3()
            val sil = projectedSilhouette(visibleFaces(m, scale, cx, cy))

            velocity = resolveImpact(sil, containerRect, velocity)
            decaySquash(dt)

            if (hasSettled(velocity, sil, ch)) break
            if (now - startNanos > maxNanos) {
                position = Vec2(position.x, ch / 2f - (sil.maxY - sil.minY) * 0.5f)
                velocity = Vec2(0f, 0f)
                squash = Vec2(1f, 1f)
                break
            }
        }
    }

    private fun resolveImpact(sil: Rect2d, containerRect: Rect2d, velocity: Vec2): Vec2 {
        val impact = detectWallImpact(sil, containerRect) ?: return velocity
        val incoming = (-velocity.dot(impact.normal)).coerceAtLeast(0f)
        position += impact.normal * impact.depth
        val bounced = bounceVelocity(velocity, impact.normal, RESTITUTION)
        val intensity = (incoming / IMPACT_REF_SPEED_PX_PER_S).coerceIn(0f, 1f)
        squash = squashForImpact(impact.normal, intensity, MAX_SQUASH)
        return bounced
    }

    private fun decaySquash(dt: Float) {
        val decay = 1f - exp(-SQUASH_DECAY_PER_S * dt)
        squash = Vec2(
            1f + (squash.x - 1f) * (1f - decay),
            1f + (squash.y - 1f) * (1f - decay),
        )
    }

    private fun hasSettled(velocity: Vec2, sil: Rect2d, ch: Float): Boolean {
        val currentHalfH = (sil.maxY - sil.minY) * 0.5f
        val currentFloorY = ch / 2f - currentHalfH
        return velocity.length() < SETTLE_SPEED_PX_PER_S && position.y >= currentFloorY - 1f
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