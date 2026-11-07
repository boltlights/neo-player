with open('app/src/main/java/com/example/ui/screens/FocusedNowPlayingScreen.kt', 'r') as f:
    lines = f.readlines()

new_lines = []
for line in lines:
    if line.strip() == '@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)' or line.strip() == 'package com.example.ui.screens' or line.strip() == 'import androidx.compose.ui.draw.alpha':
        continue
    new_lines.append(line)

final_lines = [
    "@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)\n",
    "package com.example.ui.screens\n",
    "\n",
    "import androidx.compose.ui.draw.alpha\n"
] + new_lines

with open('app/src/main/java/com/example/ui/screens/FocusedNowPlayingScreen.kt', 'w') as f:
    f.writelines(final_lines)
