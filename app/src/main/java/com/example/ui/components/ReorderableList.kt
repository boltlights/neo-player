package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun rememberReorderableLazyListState(
    lazyListState: LazyListState,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    canDragOver: ((draggedOverIndex: Int) -> Boolean)? = null,
    onDragEnd: (() -> Unit)? = null
): ReorderableLazyListState {
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val state = remember(lazyListState) {
        ReorderableLazyListState(
            lazyListState = lazyListState,
            haptic = haptic,
            coroutineScope = coroutineScope,
            onMoveCallback = onMove,
            canDragOverCallback = canDragOver,
            onDragEndCallback = onDragEnd
        )
    }

    LaunchedEffect(state) {
        state.onMoveCallback = onMove
        state.canDragOverCallback = canDragOver
        state.onDragEndCallback = onDragEnd
    }

    LaunchedEffect(state.isDragging) {
        if (state.isDragging) {
            while (isActive && state.isDragging) {
                state.performAutoScroll()
                kotlinx.coroutines.delay(16L) // ~60fps auto-scroll loop
            }
        }
    }

    return state
}

class ReorderableLazyListState(
    val lazyListState: LazyListState,
    private val haptic: HapticFeedback,
    private val coroutineScope: CoroutineScope,
    var onMoveCallback: (fromIndex: Int, toIndex: Int) -> Unit,
    var canDragOverCallback: ((draggedOverIndex: Int) -> Boolean)? = null,
    var onDragEndCallback: (() -> Unit)? = null
) {
    var draggingIndex by mutableStateOf<Int?>(null)
        private set
    var draggingKey by mutableStateOf<Any?>(null)
        private set
    var draggedDistance by mutableStateOf(0f)
        private set

    val isDragging: Boolean
        get() = draggingIndex != null

    private var initialItemOffset = 0
    private var itemSize = 0

    fun onDragStart(index: Int, key: Any) {
        val visibleItem = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
        if (visibleItem != null) {
            draggingIndex = index
            draggingKey = key
            draggedDistance = 0f
            initialItemOffset = visibleItem.offset
            itemSize = visibleItem.size
            try {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            } catch (_: Exception) {}
        }
    }

    fun onDrag(dragAmount: Float) {
        if (draggingIndex == null) return
        draggedDistance += dragAmount

        val currentIndex = draggingIndex ?: return
        val layoutInfo = lazyListState.layoutInfo
        val currentVisibleItem = layoutInfo.visibleItemsInfo.firstOrNull { it.index == currentIndex } ?: return
        val currentCenterY = currentVisibleItem.offset + draggedDistance + (currentVisibleItem.size / 2f)

        // Find candidate item to swap with
        val targetItem = layoutInfo.visibleItemsInfo.firstOrNull { item ->
            if (item.index == currentIndex) return@firstOrNull false
            if (canDragOverCallback?.invoke(item.index) == false) return@firstOrNull false

            val itemCenter = item.offset + (item.size / 2f)
            if (item.index > currentIndex) {
                // Moving downwards: passed target's midpoint
                currentCenterY > itemCenter
            } else {
                // Moving upwards: passed target's midpoint
                currentCenterY < itemCenter
            }
        }

        if (targetItem != null) {
            val targetIndex = targetItem.index
            val offsetDelta = targetItem.offset - currentVisibleItem.offset
            
            onMoveCallback(currentIndex, targetIndex)
            draggingIndex = targetIndex
            draggedDistance -= offsetDelta

            try {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            } catch (_: Exception) {}
        }
    }

    fun onDragEnd() {
        if (draggingIndex != null) {
            draggingIndex = null
            draggingKey = null
            draggedDistance = 0f
            onDragEndCallback?.invoke()
            try {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            } catch (_: Exception) {}
        }
    }

    fun onDragCancel() {
        draggingIndex = null
        draggingKey = null
        draggedDistance = 0f
    }

    suspend fun performAutoScroll() {
        val currIndex = draggingIndex ?: return
        val layoutInfo = lazyListState.layoutInfo
        val visibleItem = layoutInfo.visibleItemsInfo.firstOrNull { it.index == currIndex } ?: return
        
        val currentTop = visibleItem.offset + draggedDistance
        val currentBottom = currentTop + visibleItem.size
        val viewportStart = layoutInfo.viewportStartOffset.toFloat()
        val viewportEnd = layoutInfo.viewportEndOffset.toFloat()
        val scrollThreshold = 140f // px from edge

        if (currentTop < viewportStart + scrollThreshold) {
            val diff = (viewportStart + scrollThreshold) - currentTop
            val scrollBy = -(diff * 0.35f).coerceIn(4f, 60f)
            val consumed = lazyListState.scrollBy(scrollBy)
            draggedDistance += consumed
        } else if (currentBottom > viewportEnd - scrollThreshold) {
            val diff = currentBottom - (viewportEnd - scrollThreshold)
            val scrollBy = (diff * 0.35f).coerceIn(4f, 60f)
            val consumed = lazyListState.scrollBy(scrollBy)
            draggedDistance += consumed
        }
    }
}

/**
 * Modifier to apply on the item container to handle smooth displacement and elevation during drag
 */
fun Modifier.reorderableItem(
    state: ReorderableLazyListState,
    key: Any
): Modifier = this.then(
    Modifier
        .zIndex(if (state.draggingKey == key) 2f else 0f)
        .graphicsLayer {
            if (state.draggingKey == key) {
                translationY = state.draggedDistance
                scaleX = 1.025f
                scaleY = 1.025f
                shadowElevation = 16f
            } else {
                translationY = 0f
                scaleX = 1f
                scaleY = 1f
                shadowElevation = 0f
            }
        }
)

/**
 * Modifier to attach to the drag handle icon
 */
fun Modifier.reorderDragHandle(
    state: ReorderableLazyListState,
    index: Int,
    key: Any
): Modifier = this.then(
    Modifier.pointerInput(key) {
        detectDragGestures(
            onDragStart = {
                state.onDragStart(index, key)
            },
            onDragEnd = {
                state.onDragEnd()
            },
            onDragCancel = {
                state.onDragCancel()
            },
            onDrag = { change, dragAmount ->
                change.consume()
                state.onDrag(dragAmount.y)
            }
        )
    }
)

/**
 * Modifier to support dragging by long-pressing anywhere on the row item
 */
fun Modifier.reorderLongPressDrag(
    state: ReorderableLazyListState,
    index: Int,
    key: Any,
    enabled: Boolean = true
): Modifier = if (!enabled) this else this.then(
    Modifier.pointerInput(key) {
        detectDragGesturesAfterLongPress(
            onDragStart = {
                state.onDragStart(index, key)
            },
            onDragEnd = {
                state.onDragEnd()
            },
            onDragCancel = {
                state.onDragCancel()
            },
            onDrag = { change, dragAmount ->
                change.consume()
                state.onDrag(dragAmount.y)
            }
        )
    }
)
