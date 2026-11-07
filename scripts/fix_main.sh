#!/bin/bash
cat << 'INNER_EOF' > script.awk
/if \(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU\)/ {
    print "            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {"
    print "                val permissionLauncher = rememberLauncherForActivityResult("
    print "                    contract = ActivityResultContracts.RequestPermission()"
    print "                ) { _ -> }"
    print "                LaunchedEffect(Unit) {"
    print "                    if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {"
    print "                        permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)"
    print "                    }"
    print "                }"
    print "            }"
    print "            val deleteLauncher = rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult()) { result ->"
    print "                if (result.resultCode == android.app.Activity.RESULT_OK) {"
    print "                    // The user granted permission, the file is usually deleted automatically or we can retry"
    print "                    viewModel.performAutoRefresh()"
    print "                }"
    print "            }"
    print "            LaunchedEffect(viewModel) {"
    print "                viewModel.deleteIntentSender.collect { intentSender ->"
    print "                    deleteLauncher.launch("
    print "                        androidx.activity.result.IntentSenderRequest.Builder(intentSender).build()"
    print "                    )"
    print "                }"
    print "            }"
    
    skip = 1
    next
}
skip && /val themeStyle/ {
    skip = 0
}
skip { next }
{ print }
INNER_EOF
awk -f script.awk app/src/main/java/com/example/MainActivity.kt > tmp.kt && mv tmp.kt app/src/main/java/com/example/MainActivity.kt
