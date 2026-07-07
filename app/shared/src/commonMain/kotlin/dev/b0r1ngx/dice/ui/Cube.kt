package dev.b0r1ngx.dice.ui

import dev.b0r1ngx.dice.die.DieFace

/** Static cube topology, isometric projection, and face-orientation helpers. */

internal const val COS_30 = 0.8660254f
internal const val SIN_30 = 0.5f
internal const val CUBE_SCALE_FRACTION = 0.25f

private const val H = 0.5f

internal data class CubeFace(val normal: Vec3, val corners: List<Int>, val value: DieFace)

internal val CUBE_VERTICES: List<Vec3> = listOf(
    Vec3(-H, -H, -H),
    Vec3(H, -H, -H),
    Vec3(H, H, -H),
    Vec3(-H, H, -H),
    Vec3(-H, -H, H),
    Vec3(H, -H, H),
    Vec3(H, H, H),
    Vec3(-H, H, H),
)

internal val CUBE_FACES: List<CubeFace> = listOf(
    CubeFace(Vec3(0f, 1f, 0f), listOf(3, 2, 6, 7), DieFace.ONE),
    CubeFace(Vec3(0f, -1f, 0f), listOf(4, 5, 1, 0), DieFace.SIX),
    CubeFace(Vec3(0f, 0f, 1f), listOf(4, 5, 6, 7), DieFace.TWO),
    CubeFace(Vec3(0f, 0f, -1f), listOf(1, 0, 3, 2), DieFace.FIVE),
    CubeFace(Vec3(1f, 0f, 0f), listOf(5, 1, 2, 6), DieFace.THREE),
    CubeFace(Vec3(-1f, 0f, 0f), listOf(0, 4, 7, 3), DieFace.FOUR),
)

internal fun faceNormalOf(face: DieFace): Vec3 =
    CUBE_FACES.first { it.value == face }.normal

internal fun orientationForFaceOnTop(face: DieFace): Quat =
    Quat.fromTo(faceNormalOf(face), UP)

internal fun topFaceOf(m: Mat3): DieFace =
    CUBE_FACES.maxBy { m.apply(it.normal).y }.value

internal data class ProjectedFace(
    val depth: Float,
    val corners: List<Vec2>,
    val edgeLen: Float,
    val isTop: Boolean,
    val value: DieFace,
)

internal fun project(p: Vec3, scale: Float, cx: Float, cy: Float): Vec2 = Vec2(
    cx + COS_30 * (p.x - p.z) * scale,
    cy + (SIN_30 * (p.x + p.z) - p.y) * scale,
)

internal fun visibleFaces(
    m: Mat3,
    scale: Float,
    cx: Float,
    cy: Float,
): List<ProjectedFace> {
    val topFaceIndex = CUBE_FACES.indices.maxBy { m.apply(CUBE_FACES[it].normal).y }
    val result = mutableListOf<ProjectedFace>()
    for (i in CUBE_FACES.indices) {
        val face = CUBE_FACES[i]
        val rotNormal = m.apply(face.normal)
        if (rotNormal.dot(VIEW_DIR) <= 0f) continue
        val rotCorners = face.corners.map { m.apply(CUBE_VERTICES[it]) }
        val projected = rotCorners.map { project(it, scale, cx, cy) }
        val center = rotCorners.reduce { acc, v -> acc + v } * (1f / rotCorners.size)
        val edgeLen = (projected[1] - projected[0]).length()
        result.add(
            ProjectedFace(
                depth = center.dot(VIEW_DIR),
                corners = projected,
                edgeLen = edgeLen,
                isTop = i == topFaceIndex,
                value = face.value,
            )
        )
    }
    return result.sortedBy { it.depth }
}

internal fun applySquash(
    faces: List<ProjectedFace>,
    squash: Vec2,
    cx: Float,
    cy: Float,
): List<ProjectedFace> = faces.map { face ->
    val scaled = face.corners.map { c ->
        Vec2(cx + (c.x - cx) * squash.x, cy + (c.y - cy) * squash.y)
    }
    face.copy(corners = scaled)
}