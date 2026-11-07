with open('app/src/main/java/com/example/ui/screens/LibraryScreen.kt', 'r') as f:
    lines = f.readlines()

artists_replacement = """                                }
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
"""

playlists_replacement = """                                }
                            }
                            FastScrollbar(
                                listState = playlistsListState,
                                itemCount = playlistsMap.size,
                                accentColor = accentColor,
                                popupText = { index -> playlistsMap.getOrNull(index)?.first?.name?.firstOrNull()?.uppercase() ?: "" },
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .fillMaxHeight()
                                    .padding(top = totalTopPadding + 16.dp, bottom = 16.dp, end = 2.dp)
                            )
                        }
"""

lines[978:981] = [artists_replacement]

# We need to recalculate index for playlists_replacement because we just changed the number of lines.
# Instead of doing that, let's write to a string and use replace.
with open('app/src/main/java/com/example/ui/screens/LibraryScreen.kt', 'r') as f:
    content = f.read()

content = content.replace(
'''                                    }
                                }
                            }
                        }
                    }
                    "Genre" -> {''',
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
                    "Genre" -> {'''
)

content = content.replace(
'''                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        }
    }

    if (showPlaylistSelectDialogForGroupAdd) {''',
'''                                    }
                                }
                            }
                            FastScrollbar(
                                listState = playlistsListState,
                                itemCount = playlistsMap.size,
                                accentColor = accentColor,
                                popupText = { index -> playlistsMap.getOrNull(index)?.first?.name?.firstOrNull()?.uppercase() ?: "" },
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .fillMaxHeight()
                                    .padding(top = totalTopPadding + 16.dp, bottom = 16.dp, end = 2.dp)
                            )
                        }
                    }
                }
            }
        }
        }
    }

    if (showPlaylistSelectDialogForGroupAdd) {'''
)

with open('app/src/main/java/com/example/ui/screens/LibraryScreen.kt', 'w') as f:
    f.write(content)

