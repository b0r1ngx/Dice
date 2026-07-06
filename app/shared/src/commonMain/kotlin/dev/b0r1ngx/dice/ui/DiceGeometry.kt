package dev.b0r1ngx.dice.ui

import dev.b0r1ngx.dice.die.DieFace
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

internal const val COS_30 = 0.8660254f
internal const val SIN_30 = 0.5f

internal val PIP_FRAC = floatArrayOf(0.24f, 0.5f, 0.76f)

internal const val CUBE_SCALE_FRACTION = 0.25f

internal val UP = Vec3(0f, 1f, 0f)

internal val VIEW_DIR = Vec3(1f, 1f, 1f).normalized()

internal val TUMBLE_AXIS = Vec3(1f, 2f, 1f).normalized()

internal data class Vec3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(o: Vec3) = Vec3(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: Vec3) = Vec3(x - o.x, y - o.y, z - o.z)
    operator fun times(s: Float) = Vec3(x * s, y * s, z * s)
    fun dot(o: Vec3) = x * o.x + y * o.y + z * o.z
    fun cross(o: Vec3) = Vec3(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x)
    fun length() = sqrt(dot(this))
    fun normalized(): Vec3 {
        val l = length()
        return if (l < 1e-9f) this else this * (1f / l)
    }
}

internal data class Vec2(val x: Float, val y: Float) {
    operator fun plus(o: Vec2) = Vec2(x + o.x, y + o.y)
    operator fun minus(o: Vec2) = Vec2(x - o.x, y - o.y)
    operator fun times(s: Float) = Vec2(x * s, y * s)
    fun dot(o: Vec2) = x * o.x + y * o.y
    fun length() = sqrt(x * x + y * y)
}

internal class Mat3(val r0: Vec3, val r1: Vec3, val r2: Vec3) {
    fun apply(v: Vec3) = Vec3(r0.dot(v), r1.dot(v), r2.dot(v))
}

internal data class Quat(val w: Float, val x: Float, val y: Float, val z: Float) {
    fun normalized(): Quat {
        val l = sqrt(w * w + x * x + y * y + z * z)
        return if (l < 1e-9f) this else Quat(w / l, x / l, y / l, z / l)
    }

    operator fun times(o: Quat): Quat = Quat(
        w * o.w - x * o.x - y * o.y - z * o.z,
        w * o.x + x * o.w + y * o.z - z * o.y,
        w * o.y - x * o.z + y * o.w + z * o.x,
        w * o.z + x * o.y - y * o.x + z * o.w,
    )

    fun toMat3(): Mat3 {
        val xx = x * x
        val yy = y * y
        val zz = z * z
        val xy = x * y
        val xz = x * z
        val yz = y * z
        val wx = w * x
        val wy = w * y
        val wz = w * z
        return Mat3(
            Vec3(1 - 2 * (yy + zz), 2 * (xy - wz), 2 * (xz + wy)),
            Vec3(2 * (xy + wz), 1 - 2 * (xx + zz), 2 * (yz - wx)),
            Vec3(2 * (xz - wy), 2 * (yz + wx), 1 - 2 * (xx + yy)),
        )
    }

    companion object {
        fun identity() = Quat(1f, 0f, 0f, 0f)

        fun fromAxisAngle(axis: Vec3, angleRad: Float): Quat {
            val a = axis.normalized()
            val h = angleRad / 2f
            val s = sin(h)
            return Quat(cos(h), a.x * s, a.y * s, a.z * s)
        }

        fun fromTo(from: Vec3, to: Vec3): Quat {
            val f = from.normalized()
            val t = to.normalized()
            val d = f.dot(t).coerceIn(-1f, 1f)
            if (d >= 1f - 1e-6f) return identity()
            if (d <= -1f + 1e-6f) {
                val axis = (if (abs(f.x) < 0.9f) Vec3(1f, 0f, 0f) else Vec3(0f, 1f, 0f))
                    .cross(f)
                    .normalized()
                return fromAxisAngle(axis, PI_FLOAT)
            }
            return fromAxisAngle(f.cross(t), acos(d))
        }
    }
}

private const val PI_FLOAT = 3.1415927f

internal fun slerp(a: Quat, b: Quat, t: Float): Quat {
    var dot = a.w * b.w + a.x * b.x + a.y * b.y + a.z * b.z
    var rhs = b
    if (dot < 0f) {
        rhs = Quat(-b.w, -b.x, -b.y, -b.z)
        dot = -dot
    }
    if (dot > 0.9995f) {
        return Quat(
            a.w + (rhs.w - a.w) * t,
            a.x + (rhs.x - a.x) * t,
            a.y + (rhs.y - a.y) * t,
            a.z + (rhs.z - a.z) * t,
        ).normalized()
    }
    val theta0 = acos(dot.coerceIn(-1f, 1f))
    val theta = theta0 * t
    val s0 = cos(theta) - dot * sin(theta) / sin(theta0)
    val s1 = sin(theta) / sin(theta0)
    return Quat(
        a.w * s0 + rhs.w * s1,
        a.x * s0 + rhs.x * s1,
        a.y * s0 + rhs.y * s1,
        a.z * s0 + rhs.z * s1,
    )
}

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