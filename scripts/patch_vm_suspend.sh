#!/bin/bash
sed -i 's/fun addIncludedFolder(uri: String) { viewModelScope.launch {/fun addIncludedFolder(uri: String) { viewModelScope.launch { settingsRepository.addIncludedFolder(uri) } }/g' app/src/main/java/com/example/MusicViewModel.kt
sed -i 's/settingsRepository.addIncludedFolder(uri) }//g' app/src/main/java/com/example/MusicViewModel.kt

sed -i 's/if (!includedFolders.value.contains(uriStr)) {/if (!includedFolders.value.contains(uriStr)) {\n            viewModelScope.launch {\n                settingsRepository.addIncludedFolder(uriStr)\n                scanMediaStore(context, onCompleted)\n            }\n            return\n        }/g' app/src/main/java/com/example/MusicViewModel.kt
