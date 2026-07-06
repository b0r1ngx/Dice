package dev.b0r1ngx.dice.ui

import dev.b0r1ngx.dice.die.DieFace
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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

    @Test
    fun projectedSilhouetteIsSymmetricForIdentity() {
        val faces = visibleFaces(Quat.identity().toMat3(), scale = 100f, cx = 0f, cy = 0f)
        val sil = projectedSilhouette(faces)
        assertEquals(-sil.maxX, sil.minX, TOL)
        assertEquals(-sil.maxY, sil.minY, TOL)
    }

    @Test
    fun detectWallImpactReturnsNullWhenInside() {
        val sil = Rect2d(-10f, 10f, -10f, 10f)
        val container = Rect2d(-50f, 50f, -50f, 50f)
        assertNull(detectWallImpact(sil, container))
    }

    @Test
    fun detectWallImpactCeilingHasInwardDownNormal() {
        val sil = Rect2d(-10f, 10f, -55f, -35f)
        val container = Rect2d(-50f, 50f, -50f, 50f)
        val impact = detectWallImpact(sil, container)
        assertNotNull(impact)
        assertEquals(Vec2(0f, 1f), impact.normal)
        assertEquals(5f, impact.depth, TOL)
    }

    @Test
    fun detectWallImpactFloorHasInwardUpNormal() {
        val sil = Rect2d(-10f, 10f, 35f, 55f)
        val container = Rect2d(-50f, 50f, -50f, 50f)
        val impact = detectWallImpact(sil, container)
        assertNotNull(impact)
        assertEquals(Vec2(0f, -1f), impact.normal)
        assertEquals(5f, impact.depth, TOL)
    }

    @Test
    fun bounceVelocityReflectsCeilingHitWithRestitution() {
        val v = bounceVelocity(Vec2(0f, -10f), Vec2(0f, 1f), 0.5f)
        assertEquals(0f, v.x, TOL)
        assertEquals(5f, v.y, TOL)
    }

    @Test
    fun bounceVelocityLeavesVelocityWhenMovingAway() {
        val v = bounceVelocity(Vec2(0f, 10f), Vec2(0f, 1f), 0.5f)
        assertEquals(0f, v.x, TOL)
        assertEquals(10f, v.y, TOL)
    }

    @Test
    fun squashForImpactCeilingCompressesVertically() {
        val s = squashForImpact(Vec2(0f, 1f), 1f, 0.25f)
        assertEquals(1.25f, s.x, TOL)
        assertEquals(0.75f, s.y, TOL)
    }

    @Test
    fun squashForImpactZeroIntensityIsIdentity() {
        val s = squashForImpact(Vec2(0f, 1f), 0f, 0.25f)
        assertEquals(1f, s.x, TOL)
        assertEquals(1f, s.y, TOL)
    }

    @Test
    fun applySquashIdentityLeavesCorners() {
        val faces = visibleFaces(Quat.identity().toMat3(), 100f, 0f, 0f)
        val squashed = applySquash(faces, Vec2(1f, 1f), 0f, 0f)
        assertEquals(faces.size, squashed.size)
        for (i in faces.indices) {
            for (j in faces[i].corners.indices) {
                assertEquals(faces[i].corners[j].x, squashed[i].corners[j].x, TOL)
                assertEquals(faces[i].corners[j].y, squashed[i].corners[j].y, TOL)
            }
        }
    }

    @Test
    fun applySquashScalesXAboutCenter() {
        val faces = visibleFaces(Quat.identity().toMat3(), 100f, 0f, 0f)
        val squashed = applySquash(faces, Vec2(2f, 1f), 0f, 0f)
        for (i in faces.indices) {
            for (j in faces[i].corners.indices) {
                assertEquals(faces[i].corners[j].x * 2f, squashed[i].corners[j].x, TOL)
                assertEquals(faces[i].corners[j].y, squashed[i].corners[j].y, TOL)
            }
        }
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