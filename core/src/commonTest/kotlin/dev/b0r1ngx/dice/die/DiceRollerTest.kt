package dev.b0r1ngx.dice.die

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DiceRollerTest {

    @Test
    fun rollD6AlwaysReturnsAValidFace() {
        repeat(1_000) {
            val face = rollD6()
            assertTrue(face.pips in 1..6, "face out of range: ${face.pips}")
        }
    }

    @Test
    fun rollD6IsDeterministicForASeededRandom() {
        val expected = rollD6(Random(1234))
        val actual = rollD6(Random(1234))
        assertEquals(expected, actual)
    }

    @Test
    fun rollD6ProducesEveryFaceOverManyRolls() {
        val seen = mutableSetOf<DieFace>()
        repeat(2_000) { seen += rollD6() }
        assertEquals(DieFace.entries.toSet(), seen)
    }
}