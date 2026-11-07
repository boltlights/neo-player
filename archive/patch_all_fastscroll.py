import re

with open('app/src/main/java/com/example/ui/screens/LibraryScreen.kt', 'r') as f:
    content = f.read()

# Albums
content = re.sub(
r'''                        \} else \{
                            LazyColumn\(
                                modifier = Modifier
                                    \.fillMaxSize\(\)
                                    \.padding\(start = 8\.dp, end = 2\.dp\),
                                verticalArrangement = Arrangement\.spacedBy\(0\.dp\),
                                contentPadding = PaddingValues\(top = totalTopPadding \+ 32\.dp, bottom = 80\.dp\)
                            \) \{''',
'''                        } else {
                            val albumsListState = rememberLazyListState()
                            Box(modifier = Modifier.fillMaxSize()) {
                                LazyColumn(
                                    state = albumsListState,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(start = 8.dp, end = 24.dp),
                                    verticalArrangement = Arrangement.spacedBy(0.dp),
                                    contentPadding = PaddingValues(top = totalTopPadding + 32.dp, bottom = 80.dp)
                                ) {''', content)

content = content.replace(
'''                                    }
                                }
                            }
                        }
                    }
                    "Artists" -> {''',
'''                                    }
                                }
                            }
                            FastScrollbar(
                                listState = albumsListState,
                                itemCount = albumsMap.size,
                                accentColor = accentColor,
                                popupText = { index -> albumsMap.getOrNull(index)?.first?.firstOrNull()?.uppercase() ?: "" },
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .fillMaxHeight()
                                    .padding(top = totalTopPadding + 16.dp, bottom = 16.dp, end = 2.dp)
                            )
                        }
                    }
                    "Artists" -> {''')


# Artists
content = re.sub(
r'''                        \} else \{
                            LazyColumn\(
                                modifier = Modifier
                                    \.fillMaxSize\(\)
                                    \.padding\(start = 8\.dp, end = 2\.dp\),
                                verticalArrangement = Arrangement\.spacedBy\(0\.dp\),
                                contentPadding = PaddingValues\(top = totalTopPadding \+ 32\.dp, bottom = 80\.dp\)
                            \) \{''',
'''                        } else {
                            val artistsListState = rememberLazyListState()
                            Box(modifier = Modifier.fillMaxSize()) {
                                LazyColumn(
                                    state = artistsListState,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(start = 8.dp, end = 24.dp),
                                    verticalArrangement = Arrangement.spacedBy(0.dp),
                                    contentPadding = PaddingValues(top = totalTopPadding + 32.dp, bottom = 80.dp)
                                ) {''', content)

content = content.replace(
'''                                    }
                                }
                            }
                        }
                    }
                    "Genres" -> {''',
'''                                    }
                                }
                            }
                            FastScrollbar(
                                listState = artistsListState,
                                itemCount = artistsMap.size,
                                accentColor = accentColor,
                                popupText = { index -> artistsMap.getOrNull(index)?.first?.firstOrNull()?.uppercase() ?: "" },
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .fillMaxHeight()
                                    .padding(top = totalTopPadding + 16.dp, bottom = 16.dp, end = 2.dp)
                            )
                        }
                    }
                    "Genres" -> {''')


# Genres
content = re.sub(
r'''                        \} else \{
                            LazyColumn\(
                                modifier = Modifier
                                    \.fillMaxSize\(\)
                                    \.padding\(start = 8\.dp, end = 2\.dp\),
                                verticalArrangement = Arrangement\.spacedBy\(0\.dp\),
                                contentPadding = PaddingValues\(top = totalTopPadding \+ 32\.dp, bottom = 80\.dp\)
                            \) \{''',
'''                        } else {
                            val genresListState = rememberLazyListState()
                            Box(modifier = Modifier.fillMaxSize()) {
                                LazyColumn(
                                    state = genresListState,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(start = 8.dp, end = 24.dp),
                                    verticalArrangement = Arrangement.spacedBy(0.dp),
                                    contentPadding = PaddingValues(top = totalTopPadding + 32.dp, bottom = 80.dp)
                                ) {''', content)

content = content.replace(
'''                                    }
                                }
                            }
                        }
                    }
                    "Playlists" -> {''',
'''                                    }
                                }
                            }
                            FastScrollbar(
                                listState = genresListState,
                                itemCount = genresMap.size,
                                accentColor = accentColor,
                                popupText = { index -> genresMap.getOrNull(index)?.first?.firstOrNull()?.uppercase() ?: "" },
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .fillMaxHeight()
                                    .padding(top = totalTopPadding + 16.dp, bottom = 16.dp, end = 2.dp)
                            )
                        }
                    }
                    "Playlists" -> {''')

# Playlists
# Need to check how Playlists is structured
with open('app/src/main/java/com/example/ui/screens/LibraryScreen.kt', 'w') as f:
    f.write(content)
