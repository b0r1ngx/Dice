package dev.b0r1ngx.dice.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class PhysicsTest {

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

    private companion object {
        const val TOL = 1e-4f
    }
}