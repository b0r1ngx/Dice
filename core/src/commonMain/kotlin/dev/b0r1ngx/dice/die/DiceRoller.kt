package dev.b0r1ngx.dice.die

import kotlin.random.Random

/**
 * Rolls a fair six-sided die. Each face has an equal, unbiased chance.
 *
 * [random] is injectable so tests can use a seeded [Random] for deterministic results.
 */
fun rollD6(random: Random = Random.Default): DieFace =
    DieFace.entries[random.nextInt(DieFace.entries.size)]