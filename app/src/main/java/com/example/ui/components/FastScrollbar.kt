package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun FastScrollbar(
    modifier: Modifier = Modifier,
    listState: LazyListState,
    itemCount: Int,
    accentColor: Color,
    popupText: ((Int) -> String)? = null
) {
    if (itemCount <= 1) return

    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val thumbHeightDp = 36.dp
    val thumbHeightPx = with(density) { thumbHeightDp.toPx() }

    var scrollbarHeight by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var dragY by remember { mutableFloatStateOf(0f) }

    // Fading logic: visible when scrolling or dragging, fades out when idle
    var isVisibleByScroll by remember { mutableStateOf(false) }
    val isListScrollInProgress = listState.isScrollInProgress

    LaunchedEffect(isListScrollInProgress, isDragging) {
        if (isListScrollInProgress || isDragging) {
            isVisibleByScroll = true
        } else {
            // Keep visible briefly after scrolling stops, then fade out
            delay(1500)
            isVisibleByScroll = false
        }
    }

    val alphaAnim by animateFloatAsState(
        targetValue = if (isVisibleByScroll || isDragging) 1f else 0f,
        animationSpec = tween(durationMillis = 350),
        label = "scrollbar_alpha"
    )

    // Calculate current thumb position based on list scroll
    val thumbY = remember {
        derivedStateOf {
            if (itemCount <= 0 || scrollbarHeight <= thumbHeightPx) return@derivedStateOf 0f
            val visibleInfo = listState.layoutInfo.visibleItemsInfo
            if (visibleInfo.isEmpty()) return@derivedStateOf 0f

            val firstVisible = listState.firstVisibleItemIndex
            val offset = listState.firstVisibleItemScrollOffset
            val firstItemSize = visibleInfo.first().size.toFloat().coerceAtLeast(1f)

            // Smooth progress through the current visible item
            val itemProgress = (offset.toFloat() / firstItemSize).coerceIn(0f, 1f)
            val currentScroll = firstVisible + itemProgress

            val visibleCount = visibleInfo.size
            val maxScrollable = (itemCount - visibleCount + 1).coerceAtLeast(1).toFloat()
            val proportion = (currentScroll / maxScrollable).coerceIn(0f, 1f)

            val maxTravel = (scrollbarHeight - thumbHeightPx).coerceAtLeast(0f)
            proportion * maxTravel
        }
    }

    val currentItemIndex = remember {
        derivedStateOf {
            if (itemCount <= 0 || scrollbarHeight <= thumbHeightPx) return@derivedStateOf 0
            val maxTravel = (scrollbarHeight - thumbHeightPx).coerceAtLeast(1f)
            val currentY = if (isDragging) dragY else thumbY.value
            val proportion = (currentY / maxTravel).coerceIn(0f, 1f)
            (proportion * (itemCount - 1)).toInt().coerceIn(0, itemCount - 1)
        }
    }

    if (alphaAnim <= 0f && !isDragging && !isVisibleByScroll) {
        return
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(32.dp)
            .graphicsLayer { alpha = alphaAnim }
            .onGloballyPositioned { scrollbarHeight = it.size.height.toFloat() }
            .pointerInput(itemCount, scrollbarHeight, thumbHeightPx) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        val maxTravel = (scrollbarHeight - thumbHeightPx).coerceAtLeast(1f)
                        dragY = (offset.y - thumbHeightPx / 2f).coerceIn(0f, maxTravel)
                        val proportion = (dragY / maxTravel).coerceIn(0f, 1f)
                        val targetIndex = (proportion * (itemCount - 1)).toInt().coerceIn(0, itemCount - 1)
                        coroutineScope.launch { listState.scrollToItem(targetIndex) }
                    },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false },
                    onDrag = { change, _ ->
                        change.consume()
                        val maxTravel = (scrollbarHeight - thumbHeightPx).coerceAtLeast(1f)
                        dragY = (change.position.y - thumbHeightPx / 2f).coerceIn(0f, maxTravel)
                        val proportion = (dragY / maxTravel).coerceIn(0f, 1f)
                        val targetIndex = (proportion * (itemCount - 1)).toInt().coerceIn(0, itemCount - 1)
                        coroutineScope.launch { listState.scrollToItem(targetIndex) }
                    }
                )
            }
    ) {
        // Scrollbar Track - 4dp wide, offset slightly inwards from right screen edge
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 4.dp)
                .fillMaxHeight()
                .width(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.15f))
        )

        // Thumb - uses lambda offset to avoid recomposition passes on every pixel
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 2.dp)
                .offset {
                    val y = if (isDragging) dragY else thumbY.value
                    androidx.compose.ui.unit.IntOffset(0, y.toInt())
                }
                .size(width = 8.dp, height = thumbHeightDp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (isDragging) accentColor else accentColor.copy(alpha = 0.85f))
        )

        // Popup indicator - clearly positioned to the left of the scrollbar
        if (isDragging && popupText != null) {
            val text = popupText(currentItemIndex.value)
            if (text.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset {
                            androidx.compose.ui.unit.IntOffset(
                                (-40.dp).roundToPx(),
                                (dragY - 6.dp.toPx()).coerceAtLeast(0f).toInt()
                            )
                        }
                        .clip(CircleShape)
                        .background(accentColor)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = text,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }
        }
    }
}
