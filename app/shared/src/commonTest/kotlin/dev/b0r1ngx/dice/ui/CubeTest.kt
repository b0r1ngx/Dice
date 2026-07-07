package dev.b0r1ngx.dice.ui

import dev.b0r1ngx.dice.die.DieFace
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CubeTest {

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

    private companion object {
        const val TOL = 1e-4f
    }
}