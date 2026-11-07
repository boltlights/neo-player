package com.example.data

sealed interface IndexingState {
    data class Indexing(val progress: Float) : IndexingState
    data class Completed(val error: Exception?) : IndexingState
}
