import re
with open('app/src/main/java/com/example/ui/screens/FocusedNowPlayingScreen.kt', 'r') as f:
    content = f.read()

# 1. Update swipe direction to only left
content = re.sub(
    r'''                                val dismissState = androidx\.compose\.material3\.rememberSwipeToDismissBoxState\(\n\s*confirmValueChange = \{ value ->\n\s*if \(value != androidx\.compose\.material3\.SwipeToDismissBoxValue\.Settled\) \{''',
    r'''                                val dismissState = androidx.compose.material3.rememberSwipeToDismissBoxState(\n                                    confirmValueChange = { value ->\n                                        if (value == androidx.compose.material3.SwipeToDismissBoxValue.EndToStart) {''',
    content
)

# 2. Add enableDismissFromStartToEnd to SwipeToDismissBox
if 'enableDismissFromStartToEnd = false' not in content:
    content = re.sub(
        r'''androidx\.compose\.material3\.SwipeToDismissBox\(\n\s*state = dismissState,\n\s*modifier = Modifier\.reorderableItem\(state = reorderState, key = qTrack\.id\),''',
        r'''androidx.compose.material3.SwipeToDismissBox(\n                                    state = dismissState,\n                                    modifier = Modifier.reorderableItem(state = reorderState, key = qTrack.id),\n                                    enableDismissFromStartToEnd = false,''',
        content
    )

# 3. Remove the Delete Icon and the padding
content = re.sub(
    r'''\s*\.padding\(horizontal = 20\.dp\),\n\s*contentAlignment = Alignment\.CenterEnd\n\s*\) \{\n\s*Icon\(\n\s*Icons\.Default\.Delete,\n\s*contentDescription = "Delete",\n\s*tint = Color\.White\n\s*\)\n\s*\}''',
    r'''\n                                        ) {\n                                        }''',
    content
)


with open('app/src/main/java/com/example/ui/screens/FocusedNowPlayingScreen.kt', 'w') as f:
    f.write(content)
