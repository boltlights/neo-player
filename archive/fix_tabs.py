with open('app/src/main/java/com/example/ui/screens/LibraryScreen.kt', 'r') as f:
    lines = f.readlines()

lines[929] = lines[929].replace("albumsListState", "artistsListState")
lines[932] = lines[932].replace("albumsListState", "artistsListState")

lines[999] = lines[999].replace("albumsListState", "genresListState")
lines[1002] = lines[1002].replace("albumsListState", "genresListState")

lines[1135] = lines[1135].replace("albumsListState", "playlistsListState")
lines[1138] = lines[1138].replace("albumsListState", "playlistsListState")

with open('app/src/main/java/com/example/ui/screens/LibraryScreen.kt', 'w') as f:
    f.writelines(lines)
