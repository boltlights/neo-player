with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

content = content.replace(
'''            val customColorScheme = if (themeStyle == MusicThemeStyle.DYNAMIC_SYSTEM && isSAndAbove) {
                if (isDark) {
                    val scheme = dynamicDarkColorScheme(context)
                    if (useBlackTheme) scheme.copy(background = Color.Black, surface = Color(0xFF111111)) else scheme
                } else {
                    dynamicLightColorScheme(context)
                }
            } else {
                val baseBg = Color(themeStyle.background)
                val bg = if (isDark && useBlackTheme) Color.Black else baseBg
                val surf = if (isDark && useBlackTheme) Color(0xFF111111) else Color(themeStyle.surface)
                val prim = Color(themeStyle.primary)
                val onBg = Color(themeStyle.onBackground)
                darkColorScheme(
                    background = bg,
                    surface = surf,
                    primary = prim,
                    onBackground = onBg,
                    onSurface = onBg,
                    secondary = Color(themeStyle.accent)
                )
            }''',
'''            val customColorScheme = if (themeStyle == MusicThemeStyle.DYNAMIC_SYSTEM && isSAndAbove) {
                if (isDark) {
                    val scheme = dynamicDarkColorScheme(context)
                    if (useBlackTheme) scheme.copy(background = Color.Black, surface = Color(0xFF000000)) else scheme
                } else {
                    dynamicLightColorScheme(context)
                }
            } else {
                val baseBg = Color(themeStyle.background)
                val bg = if (useBlackTheme) Color.Black else baseBg
                val surf = if (useBlackTheme) Color.Black else Color(themeStyle.surface)
                val prim = Color(themeStyle.primary)
                val onBg = Color(themeStyle.onBackground)
                darkColorScheme(
                    background = bg,
                    surface = surf,
                    primary = prim,
                    onBackground = onBg,
                    onSurface = onBg,
                    secondary = Color(themeStyle.accent)
                )
            }'''
)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
