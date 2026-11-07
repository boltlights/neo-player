#!/bin/bash
cat << 'INNER_EOF' > script.awk
/fun OthersSettingsScreen\(/ {
    print "fun OthersSettingsScreen("
    print "    viewModel: MusicViewModel,"
    print "    accentColor: Color,"
    print "    surfaceColor: Color,"
    print "    onBgColor: Color,"
    print "    onBack: () -> Unit,"
    print "    onSmartVolumeClick: () -> Unit"
    print ") {"
    skip = 1
    next
}
/onBack: \(\) -> Unit/ && skip == 1 {
    skip = 0
    next
}
/fun SettingsScreen\(/ {
    in_settings = 1
}
/var currentSubScreen by remember {/ && in_settings == 1 {
    print
    print "    var showSmartVolumeScreen by remember { mutableStateOf(false) }"
    print "    if (showSmartVolumeScreen) {"
    print "        com.example.ui.screens.SmartVolumeScreen("
    print "            viewModel = viewModel,"
    print "            accentColor = accentColor,"
    print "            surfaceColor = surfaceColor,"
    print "            onBack = { showSmartVolumeScreen = false }"
    print "        )"
    print "        return"
    print "    }"
    next
}
/OthersSettingsScreen\(/ && in_settings == 1 {
    in_others = 1
    print
    next
}
/onBack = { currentSubScreen = "Main" }/ && in_others == 1 {
    print
    print "            ,"
    print "            onSmartVolumeClick = { showSmartVolumeScreen = true }"
    in_others = 0
    next
}
/Hide sample music button/ {
    print "            Spacer(modifier = Modifier.height(16.dp))"
    print "            Surface("
    print "                shape = RoundedCornerShape(20.dp),"
    print "                color = surfaceColor,"
    print "                modifier = Modifier"
    print "                    .fillMaxWidth()"
    print "                    .clickable { onSmartVolumeClick() }"
    print "            ) {"
    print "                Row("
    print "                    modifier = Modifier.padding(16.dp),"
    print "                    verticalAlignment = Alignment.CenterVertically"
    print "                ) {"
    print "                    Icon("
    print "                        Icons.Default.DriveEta,"
    print "                        contentDescription = \"Smart Volume Driver Mode\","
    print "                        tint = Color.White,"
    print "                        modifier = Modifier.size(32.dp)"
    print "                    )"
    print "                    Spacer(modifier = Modifier.width(16.dp))"
    print "                    Column {"
    print "                        Text("
    print "                            \"Smart Volume Driver Mode\","
    print "                            fontSize = 18.sp,"
    print "                            fontWeight = FontWeight.Bold,"
    print "                            fontFamily = FontFamily.Monospace,"
    print "                            color = Color.White"
    print "                        )"
    print "                        Text("
    print "                            \"Location-based volume adjustment\","
    print "                            fontSize = 12.sp,"
    print "                            fontFamily = FontFamily.Monospace,"
    print "                            color = Color.White.copy(alpha = 0.6f)"
    print "                        )"
    print "                    }"
    print "                }"
    print "            }"
    print "            Spacer(modifier = Modifier.height(16.dp))"
    print
    next
}
{
    if (skip == 1) {
        if ($0 ~ /\) {/) {
            skip = 0
            next
        }
        next
    }
    print
}
INNER_EOF
awk -f script.awk app/src/main/java/com/example/ui/screens/SettingsScreen.kt > tmp.kt && mv tmp.kt app/src/main/java/com/example/ui/screens/SettingsScreen.kt
