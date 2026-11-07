package com.example

enum class AppThemeMode(
    val displayName: String,
    val description: String
) {
    NEO_CLASSIC(
        displayName = "Neo Classic",
        description = "Modern sleek audio player with fluid CD album art"
    ),
    NEO_RETRO(
        displayName = "Neo Retro",
        description = "Vintage tape cassette deck with rotating spindles & tactile controls"
    )
}
