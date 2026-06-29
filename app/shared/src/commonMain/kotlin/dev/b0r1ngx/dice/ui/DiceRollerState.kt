package dev.b0r1ngx.dice.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.b0r1ngx.dice.die.DieFace
import dev.b0r1ngx.dice.die.rollD6
import kotlin.random.Random

class DiceRollerState(
    private val random: Random = Random.Default,
) {
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

    fun onSettled() {
        isRolling = false
    }
}