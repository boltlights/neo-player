#!/bin/bash
cat << 'INNER_EOF' > script.awk
/@Composable/ {
    if (in_row_item == 1) {
        in_row_item = 0
    }
}
/fun PlaylistRowItem\(/ {
    in_row_item = 1
}
in_row_item == 1 { next }
{ print }
INNER_EOF
awk -f script.awk app/src/main/java/com/example/ui/screens/PlaylistsScreen.kt > tmp.kt && mv tmp.kt app/src/main/java/com/example/ui/screens/PlaylistsScreen.kt
