import re

with open('app/src/main/java/com/example/ui/screens/LibraryScreen.kt', 'r') as f:
    content = f.read()

drag_handle_code = """            if (showDragHandle) {
                Icon(
                    Icons.Default.DragHandle,
                    contentDescription = "Reorder",
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .then(reorderModifier)
                )
            }"""

content = content.replace(drag_handle_code, '')

new_left_code = """            if (isMultiSelectActive) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelection() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = accentColor,
                        uncheckedColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            if (showDragHandle) {
                Icon(
                    Icons.Default.DragHandle,
                    contentDescription = "Reorder",
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .then(reorderModifier)
                )
            }"""

content = content.replace("""            if (isMultiSelectActive) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelection() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = accentColor,
                        uncheckedColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.padding(end = 8.dp)
                )
            }""", new_left_code)

with open('app/src/main/java/com/example/ui/screens/LibraryScreen.kt', 'w') as f:
    f.write(content)
