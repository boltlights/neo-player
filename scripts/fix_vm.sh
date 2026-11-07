#!/bin/bash
sed -i '1d' app/src/main/java/com/example/MusicViewModel.kt
sed -i '/package com.example/a import kotlinx.coroutines.flow.asSharedFlow' app/src/main/java/com/example/MusicViewModel.kt
