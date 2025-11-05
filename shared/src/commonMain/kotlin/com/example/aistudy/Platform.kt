package com.example.aistudy

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform