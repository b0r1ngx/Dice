package dev.b0r1ngx.dice.ui

import dev.b0r1ngx.dice.die.DieFace
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DiceRollerStateTest {

    @Test
    fun rollSetsRollingAndAdvancesRollId() {
        val state = DiceRollerState(random = Random(0))

        state.roll()

        assertTrue(state.isRolling)
        assertEquals(1L, state.rollId)
        assertTrue(state.face.pips in 1..6)
    }

    @Test
    fun rollIsDeterministicForSeededRandom() {
        val expected = DiceRollerState(random = Random(123)).also { it.roll() }.face
        val actual = DiceRollerState(random = Random(123)).also { it.roll() }.face

        assertEquals(expected, actual)
    }

    @Test
    fun rollIsIgnoredWhileAlreadyRolling() {
        val state = DiceRollerState(random = Random(0))
        state.roll()
        val rollIdAfterFirst = state.rollId
        val faceAfterFirst = state.face

        state.roll()

        assertEquals(rollIdAfterFirst, state.rollId)
        assertEquals(faceAfterFirst, state.face)
    }

    @Test
    fun onSettledClearsRolling() {
        val state = DiceRollerState(random = Random(0))
        state.roll()

        state.onSettled()

        assertFalse(state.isRolling)
    }

    @Test
    fun producesAllFacesOverManyRolls() {
        val state = DiceRollerState()
        val faces = mutableSetOf<DieFace>()

        repeat(2000) {
            state.onSettled()
            state.roll()
            faces += state.face
        }

        assertEquals(DieFace.entries.toSet(), faces)
    }
}