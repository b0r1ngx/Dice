package dev.b0r1ngx.dice.ui

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Vec2 / Vec3 / Mat3 / Quat primitives and quaternion slerp. */

private const val PI = 3.1415927f
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
                val axis = (
                        if (abs(f.x) < 0.9f) Vec3(1f, 0f, 0f)
                        else Vec3(0f, 1f, 0f)
                    )
                    .cross(f)
                    .normalized()

                return fromAxisAngle(axis, PI)
            }

            return fromAxisAngle(f.cross(t), acos(d))
        }
    }
}

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
