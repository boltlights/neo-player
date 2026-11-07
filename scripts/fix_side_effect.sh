#!/bin/bash
cat << 'INNER_EOF' > script.awk
/if \(!view.isInEditMode\) \{/ {
    print
    print "        val topColor = if (isPlayerExpanded) {"
    print "            if (liquidPaletteEnabled) playerBgColor else currentBg"
    print "        } else {"
    print "            currentBg"
    print "        }"
    print "        LaunchedEffect(topColor, isPlayerExpanded, liquidPaletteEnabled, currentBg) {"
    print "            var ctx = view.context"
    print "            while (ctx is android.content.ContextWrapper) {"
    print "                if (ctx is android.app.Activity) break"
    print "                ctx = ctx.baseContext"
    print "            }"
    print "            val window = (ctx as? android.app.Activity)?.window ?: return@LaunchedEffect"
    print "            val luminance = topColor.luminance()"
    print "            val useDarkIcons = luminance > 0.5f"
    print "            window.statusBarColor = android.graphics.Color.TRANSPARENT"
    print "            window.navigationBarColor = android.graphics.Color.TRANSPARENT"
    print "            androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)"
    print "            val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, view)"
    print "            insetsController.isAppearanceLightStatusBars = useDarkIcons"
    print "            insetsController.isAppearanceLightNavigationBars = useDarkIcons"
    print "        }"
    skip = 1
    next
}
skip && /insetsController.isAppearanceLightNavigationBars = useDarkIcons/ {
    skip = 0
    getline
    next
}
skip { next }
{ print }
INNER_EOF
awk -f script.awk app/src/main/java/com/example/MainActivity.kt > tmp.kt && mv tmp.kt app/src/main/java/com/example/MainActivity.kt
