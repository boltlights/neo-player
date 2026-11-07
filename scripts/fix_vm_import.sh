#!/bin/bash
sed -i 's/kotlinx.coroutines.flow.asSharedFlow()/asSharedFlow()/g' app/src/main/java/com/example/MusicViewModel.kt
sed -i '1i import kotlinx.coroutines.flow.asSharedFlow' app/src/main/java/com/example/MusicViewModel.kt
