with open('app/src/main/java/com/example/ui/screens/LibraryScreen.kt', 'r') as f:
    content = f.read()

# For Albums:
content = content.replace(
'''                                    }
                                }
                            }
                            FastScrollbar(
                                listState = albumsListState,''',
'''                                    }
                                }
                            }
                            FastScrollbar(
                                listState = albumsListState,''')
# Wait, this is getting confusing. Let me just do regex on FastScrollbar to wrap the box.
import re
def fix_fastscroll(match):
    return '} // closes LazyColumn\n                            FastScrollbar(' + match.group(1) + ')\n                        } // closes Box'

content = re.sub(r'\}[\s\n]*FastScrollbar\((.*?)\)', fix_fastscroll, content, flags=re.DOTALL)
