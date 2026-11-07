import re

with open('app/src/main/java/com/example/ui/components/TrackRowItem.kt', 'r') as f:
    content = f.read()

# Add callbacks to signature
sig_target = r'onDelete: \(\) -> Unit,\n    onShowProperties: \(\) -> Unit\n\)'
sig_repl = r'onDelete: () -> Unit,\n    onShowProperties: () -> Unit,\n    onPlayArtist: (String) -> Unit = {},\n    onPlayGenre: (String) -> Unit = {}\n)'
content = re.sub(sig_target, sig_repl, content)

# Add Dropdown items
menu_target = r'''                    DropdownMenuItem\(
                        text = \{ Text\("Properties"\) \},
                        onClick = \{ showMenu = false; onShowProperties\(\) \}
                    \)
                \}'''

menu_repl = r'''                    DropdownMenuItem(
                        text = { Text("Properties") },
                        onClick = { showMenu = false; onShowProperties() }
                    )
                    DropdownMenuItem(
                        text = { Text("Play Artist") },
                        onClick = { 
                            showMenu = false
                            val artists = track.artist.split(Regex("[,&;/]")).map { it.trim() }.filter { it.isNotEmpty() }
                            if (artists.size > 1) {
                                artistPickerOptions = artists
                                showArtistPicker = true
                            } else {
                                onPlayArtist(artists.firstOrNull() ?: track.artist)
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Play Genre") },
                        onClick = { 
                            showMenu = false
                            val genres = (track.genre ?: "Unknown").split(Regex("[,&;/]")).map { it.trim() }.filter { it.isNotEmpty() }
                            if (genres.size > 1) {
                                genrePickerOptions = genres
                                showGenrePicker = true
                            } else {
                                onPlayGenre(genres.firstOrNull() ?: track.genre ?: "Unknown")
                            }
                        }
                    )
                }'''

content = re.sub(menu_target, menu_repl, content)

# Add state variables
state_target = r'var showMenu by remember \{ mutableStateOf\(false\) \}'
state_repl = r'''var showMenu by remember { mutableStateOf(false) }
    var showArtistPicker by remember { mutableStateOf(false) }
    var showGenrePicker by remember { mutableStateOf(false) }
    var artistPickerOptions by remember { mutableStateOf<List<String>>(emptyList()) }
    var genrePickerOptions by remember { mutableStateOf<List<String>>(emptyList()) }'''
content = re.sub(state_target, state_repl, content)

# Add bottom sheets
bs_target = r'        }\n    }\n}'
bs_repl = r'''        }
    }

    if (showArtistPicker && artistPickerOptions.isNotEmpty()) {
        @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
        androidx.compose.material3.ModalBottomSheet(onDismissRequest = { showArtistPicker = false }) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                androidx.compose.material3.Text(
                    "Play from Artist",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                androidx.compose.foundation.lazy.LazyColumn {
                    items(artistPickerOptions.size) { index ->
                        val artist = artistPickerOptions[index]
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showArtistPicker = false
                                    onPlayArtist(artist)
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(androidx.compose.material.icons.Icons.Default.Person, contentDescription = null, tint = accentColor)
                            Spacer(modifier = Modifier.width(16.dp))
                            androidx.compose.material3.Text(artist, fontSize = 16.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showGenrePicker && genrePickerOptions.isNotEmpty()) {
        @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
        androidx.compose.material3.ModalBottomSheet(onDismissRequest = { showGenrePicker = false }) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                androidx.compose.material3.Text(
                    "Play from Genre",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                androidx.compose.foundation.lazy.LazyColumn {
                    items(genrePickerOptions.size) { index ->
                        val genre = genrePickerOptions[index]
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showGenrePicker = false
                                    onPlayGenre(genre)
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(androidx.compose.material.icons.Icons.Default.Category, contentDescription = null, tint = accentColor)
                            Spacer(modifier = Modifier.width(16.dp))
                            androidx.compose.material3.Text(genre, fontSize = 16.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}'''
content = re.sub(bs_target, bs_repl, content)

with open('app/src/main/java/com/example/ui/components/TrackRowItem.kt', 'w') as f:
    f.write(content)
