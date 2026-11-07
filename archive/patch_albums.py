import re

with open('app/src/main/java/com/example/ui/screens/LibraryScreen.kt', 'r') as f:
    content = f.read()

albums_target = r'''                        } else {
                            LazyColumn\(
                                modifier = Modifier
                                    \.fillMaxSize\(\)
                                    \.padding\(start = 8\.dp, end = 2\.dp\),
                                verticalArrangement = Arrangement\.spacedBy\(0\.dp\),
                                contentPadding = PaddingValues\(top = totalTopPadding \+ 32\.dp, bottom = 80\.dp\)
                            \) \{'''

albums_replacement = r'''                        } else {
                            val albumsListState = rememberLazyListState()
                            Box(modifier = Modifier.fillMaxSize()) {
                                LazyColumn(
                                    state = albumsListState,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(start = 8.dp, end = 2.dp),
                                    verticalArrangement = Arrangement.spacedBy(0.dp),
                                    contentPadding = PaddingValues(top = totalTopPadding + 32.dp, bottom = 80.dp)
                                ) {'''

# we also need to close the Box and add FastScrollbar at the end of the LazyColumn.
# Let's find the end of the `LazyColumn` for `Albums`.
