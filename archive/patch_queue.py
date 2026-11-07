import re
with open('app/src/main/java/com/example/ui/screens/FocusedNowPlayingScreen.kt', 'r') as f:
    content = f.read()

target = r'''                                val dismissState = androidx\.compose\.material3\.rememberSwipeToDismissBoxState\(
                                    confirmValueChange = \{ value ->
                                        if \(value != androidx\.compose\.material3\.SwipeToDismissBoxValue\.Settled\) \{
                                            viewModel\.playbackController\.removeTrackFromQueue\(qTrack\.id\)
                                            true
                                        \} else \{
                                            false
                                        \}
                                    \}
                                \)
                                androidx\.compose\.material3\.SwipeToDismissBox\(
                                    state = dismissState,
                                    modifier = Modifier\.reorderableItem\(state = reorderState, key = qTrack\.id\),
                                    backgroundContent = \{
                                        val color = androidx\.compose\.ui\.graphics\.Color\.Red\.copy\(alpha = 0\.8f\)
                                        Box\(
                                            modifier = Modifier
                                                \.fillMaxSize\(\)
                                                \.clip\(RoundedCornerShape\(12\.dp\)\)
                                                \.background\(color\)
                                                \.padding\(horizontal = 20\.dp\),
                                            contentAlignment = Alignment\.CenterEnd
                                        \) \{
                                            Icon\(
                                                Icons\.Default\.Delete,
                                                contentDescription = "Delete",
                                                tint = Color\.White
                                            \)
                                        \}
                                    \},
                                    content = \{'''

repl = r'''                                val dismissState = androidx.compose.material3.rememberSwipeToDismissBoxState(
                                    confirmValueChange = { value ->
                                        if (value == androidx.compose.material3.SwipeToDismissBoxValue.EndToStart) {
                                            viewModel.playbackController.removeTrackFromQueue(qTrack.id)
                                            true
                                        } else {
                                            false
                                        }
                                    }
                                )
                                androidx.compose.material3.SwipeToDismissBox(
                                    state = dismissState,
                                    modifier = Modifier.reorderableItem(state = reorderState, key = qTrack.id),
                                    enableDismissFromStartToEnd = false,
                                    backgroundContent = {
                                        val color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f)
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(color)
                                        )
                                    },
                                    content = {'''

content = re.sub(target, repl, content)

with open('app/src/main/java/com/example/ui/screens/FocusedNowPlayingScreen.kt', 'w') as f:
    f.write(content)
