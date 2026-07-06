package dev.b0r1ngx.dice.ui

import dev.b0r1ngx.dice.die.DieFace
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DiceGeometryTest {

    @Test
    fun oppositeFacesSumToSeven() {
        for (face in CUBE_FACES) {
            val opposite = CUBE_FACES.first { it.normal.dot(face.normal) < -0.999f }
            assertEquals(7, face.value.pips + opposite.value.pips)
        }
    }

    @Test
    fun identityOrientationShowsOneOnTop() {
        assertEquals(DieFace.ONE, topFaceOf(Quat.identity().toMat3()))
    }

    @Test
    fun orientationForFaceOnTopPutsThatFaceUp() {
        for (face in DieFace.entries) {
            val m = orientationForFaceOnTop(face).toMat3()
            assertEquals(face, topFaceOf(m), "face $face not on top")
        }
    }

    @Test
    fun identityShowsExactlyThreeVisibleFaces() {
        val faces = visibleFaces(Quat.identity().toMat3(), scale = 100f, cx = 0f, cy = 0f)
        assertEquals(3, faces.size)
        assertTrue(faces.map { it.value }.contains(DieFace.ONE))
    }

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