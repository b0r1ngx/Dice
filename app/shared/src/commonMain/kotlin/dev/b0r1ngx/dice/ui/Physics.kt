package dev.b0r1ngx.dice.ui

/** Wall-collision detection, bounce, and impact-squash for the tumble loop. */

internal data class Rect2d(val minX: Float, val maxX: Float, val minY: Float, val maxY: Float)

internal data class Impact(val normal: Vec2, val depth: Float)

internal fun projectedSilhouette(faces: List<ProjectedFace>): Rect2d {
    var minX = Float.POSITIVE_INFINITY
    var maxX = Float.NEGATIVE_INFINITY
    var minY = Float.POSITIVE_INFINITY
    var maxY = Float.NEGATIVE_INFINITY
    for (face in faces) {
        for (c in face.corners) {
            if (c.x < minX) minX = c.x
            if (c.x > maxX) maxX = c.x
            if (c.y < minY) minY = c.y
            if (c.y > maxY) maxY = c.y
        }
    }
    return Rect2d(minX, maxX, minY, maxY)
}

internal fun detectWallImpact(sil: Rect2d, container: Rect2d): Impact? {
    val candidates = listOf(
        Vec2(0f, 1f) to (container.minY - sil.minY),
        Vec2(0f, -1f) to (sil.maxY - container.maxY),
        Vec2(1f, 0f) to (container.minX - sil.minX),
        Vec2(-1f, 0f) to (sil.maxX - container.maxX),
    )
    val (normal, depth) = candidates.maxBy { it.second }
    return if (depth <= 0f) null else Impact(normal, depth)
}

internal fun bounceVelocity(velocity: Vec2, normal: Vec2, restitution: Float): Vec2 {
    val vn = velocity.dot(normal)
    if (vn >= 0f) return velocity
    return velocity - normal * ((1f + restitution) * vn)
}

internal fun squashForImpact(normal: Vec2, intensity: Float, maxSquash: Float): Vec2 {
    val k = intensity.coerceIn(0f, 1f) * maxSquash
    val compress = 1f - k
    val stretch = 1f + k
    return if (normal.x != 0f) Vec2(compress, stretch) else Vec2(stretch, compress)
}
