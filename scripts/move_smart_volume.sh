#!/bin/bash
# Remove from OthersSettingsScreen
cat << 'INNER_EOF' > script1.awk
/Spacer\(modifier = Modifier.height\(16.dp\)\)/ {
    if (in_smart == 0 && count_spacer == 0) {
        count_spacer = 1
        print
        next
    }
}
/Surface\(/ {
    if (count_spacer == 1 && in_smart == 0) {
        in_smart = 1
        buffer = $0 "\n"
        next
    }
}
/contentDescription = "Smart Volume Driver Mode"/ {
    if (in_smart == 1) {
        is_smart_block = 1
    }
}
{
    if (in_smart == 1) {
        buffer = buffer $0 "\n"
        if ($0 ~ /^        }$/) {
            in_smart = 0
            if (is_smart_block == 1) {
                is_smart_block = 0
                count_spacer = 0
                next
            } else {
                printf "%s", buffer
                buffer = ""
                count_spacer = 0
            }
        }
        next
    }
    
    if (is_smart_block == 0 && $0 ~ /Spacer\(modifier = Modifier.height\(16.dp\)\)/ && count_spacer == 1) {
        count_spacer = 0
        next
    }
    print
}
INNER_EOF

# Well, doing this cleanly with AWK might be tricky because of braces.
# Let's just use sed to delete the specific lines from OthersSettingsScreen.
