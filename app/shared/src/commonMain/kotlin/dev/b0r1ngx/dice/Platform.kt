package dev.b0r1ngx.dice

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform