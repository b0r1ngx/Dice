package dev.b0r1ngx.dice.die

import kotlin.random.Random

/** Fair d6 roll; [random] is injectable for deterministic tests. */
fun rollD6(random: Random = Random.Default): DieFace =
    DieFace.entries[random.nextInt(DieFace.entries.size)]
