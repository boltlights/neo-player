with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

content = content.replace(
'''            val themeStyle by viewModel.themeStyle.collectAsStateWithLifecycle()''',
'''            val themeStyle by viewModel.themeStyle.collectAsStateWithLifecycle()
            val useBlackTheme by viewModel.useBlackTheme.collectAsStateWithLifecycle()'''
)

content = content.replace(
'''            val customColorScheme = if (themeStyle == MusicThemeStyle.DYNAMIC_SYSTEM && isSAndAbove) {
                if (isDark) {
                    dynamicDarkColorScheme(context)
                } else {
                    dynamicLightColorScheme(context)
                }
            } else {
                val bg = Color(themeStyle.background)
                val surf = Color(themeStyle.surface)
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
            }'''
)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
