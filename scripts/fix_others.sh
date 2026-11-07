#!/bin/bash
cat << 'INNER_EOF' > script.awk
/Text\(/ && /Spacer\(modifier = Modifier.height\(16.dp\)\)/ {
    skip = 1
}
INNER_EOF
# wait, I'll just rewrite OthersSettingsScreen.
