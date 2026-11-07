with open('app/src/main/java/com/example/ui/screens/LibraryScreen.kt', 'r') as f:
    content = f.read()

scrollbar_code = """                                FastScrollbar(
                                    listState = songsListState,
                                    itemCount = filteredTracks.size,
                                    accentColor = accentColor,
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .fillMaxHeight()
                                        .padding(top = totalTopPadding + 16.dp, bottom = 16.dp, end = 2.dp)
                                )"""

new_scrollbar_code = """                                if (!isMultiSelectActive) {
                                    FastScrollbar(
                                        listState = songsListState,
                                        itemCount = filteredTracks.size,
                                        accentColor = accentColor,
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .fillMaxHeight()
                                            .padding(top = totalTopPadding + 16.dp, bottom = 16.dp, end = 2.dp)
                                    )
                                }"""

content = content.replace(scrollbar_code, new_scrollbar_code)

with open('app/src/main/java/com/example/ui/screens/LibraryScreen.kt', 'w') as f:
    f.write(content)
