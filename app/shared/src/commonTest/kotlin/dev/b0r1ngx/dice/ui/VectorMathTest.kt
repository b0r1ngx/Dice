package dev.b0r1ngx.dice.ui

import dev.b0r1ngx.dice.die.DieFace
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VectorMathTest {

    @Test
    fun slerpEndpointsMatchInputs() {
        val a = Quat.identity()
        val b = orientationForFaceOnTop(DieFace.SIX)
        assertQuatEquals(a, slerp(a, b, 0f))
        assertQuatEquals(b, slerp(a, b, 1f))
    }

    @Test
    fun fromToRotatesFromVectorToToVector() {
        val q = Quat.fromTo(Vec3(0f, 1f, 0f), Vec3(0f, 0f, 1f))
        val rotated = q.toMat3().apply(Vec3(0f, 1f, 0f))
        assertEquals(0f, rotated.x, TOL)
        assertEquals(0f, rotated.y, TOL)
        assertEquals(1f, rotated.z, TOL)
    }

    private fun assertQuatEquals(expected: Quat, actual: Quat) {
        // Quaternions q and -q represent the same rotation; accept either sign.
        val same = abs(expected.w - actual.w) < TOL &&
            abs(expected.x - actual.x) < TOL &&
            abs(expected.y - actual.y) < TOL &&
            abs(expected.z - actual.z) < TOL
        val flipped = abs(expected.w + actual.w) < TOL &&
            abs(expected.x + actual.x) < TOL &&
            abs(expected.y + actual.y) < TOL &&
            abs(expected.z + actual.z) < TOL
        assertTrue(same || flipped, "quaternions differ: $expected vs $actual")
    }

    private companion object {
        const val TOL = 1e-4f
    }
}