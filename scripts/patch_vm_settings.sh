#!/bin/bash
sed -i 's/fun addIncludedFolder(uri: String) {/fun addIncludedFolder(uri: String) { viewModelScope.launch {/g' app/src/main/java/com/example/MusicViewModel.kt
sed -i 's/settingsRepository.addIncludedFolder(uri)/settingsRepository.addIncludedFolder(uri) }/g' app/src/main/java/com/example/MusicViewModel.kt
sed -i 's/fun removeIncludedFolder(uri: String) {/fun removeIncludedFolder(uri: String) { viewModelScope.launch {/g' app/src/main/java/com/example/MusicViewModel.kt
sed -i 's/settingsRepository.removeIncludedFolder(uri)/settingsRepository.removeIncludedFolder(uri) }/g' app/src/main/java/com/example/MusicViewModel.kt
sed -i 's/fun addExcludedFolder(uri: String) {/fun addExcludedFolder(uri: String) { viewModelScope.launch {/g' app/src/main/java/com/example/MusicViewModel.kt
sed -i 's/settingsRepository.addExcludedFolder(uri)/settingsRepository.addExcludedFolder(uri) }/g' app/src/main/java/com/example/MusicViewModel.kt
sed -i 's/fun removeExcludedFolder(uri: String) {/fun removeExcludedFolder(uri: String) { viewModelScope.launch {/g' app/src/main/java/com/example/MusicViewModel.kt
sed -i 's/settingsRepository.removeExcludedFolder(uri)/settingsRepository.removeExcludedFolder(uri) }/g' app/src/main/java/com/example/MusicViewModel.kt
