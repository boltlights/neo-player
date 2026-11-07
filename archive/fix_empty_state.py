import re

with open('app/src/main/java/com/example/ui/screens/LibraryScreen.kt', 'r') as f:
    content = f.read()

target = r'''                                    Image\(
                                        painter = painterResource\(id = R\.drawable\.empty_state_music_note_[0-9]+\),
                                        contentDescription = null,
                                        contentScale = androidx\.compose\.ui\.layout\.ContentScale\.Crop,
                                        modifier = Modifier
                                            \.size\(112\.dp\)
                                            \.clip\(com\.example\.ui\.components\.CookieShape\(\)\)
                                    \)'''

replacement = r'''                                    Icon(
                                        imageVector = androidx.compose.material.icons.Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = androidx.compose.ui.graphics.Color.White,
                                        modifier = Modifier
                                            .size(96.dp)
                                            .background(
                                                color = androidx.compose.ui.graphics.Color.Black,
                                                shape = com.example.ui.components.CookieShape()
                                            )
                                            .padding(24.dp)
                                    )'''

new_content = re.sub(target, replacement, content)

with open('app/src/main/java/com/example/ui/screens/LibraryScreen.kt', 'w') as f:
    f.write(new_content)
