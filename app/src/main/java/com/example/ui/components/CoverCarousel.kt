package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.PagerState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.abs

/**
 * "Masked carousel" transition for the now-playing cover pager.
 *
 * Instead of sliding covers sideways like a normal pager, every page is pinned in place and only its
 * visible window (a mask) changes as you swipe: the outgoing cover's window shrinks toward the side it
 * leaves on, the incoming cover's window grows from the side it enters from, a small gap opens between
 * them early in the swipe and closes at the end, and the artwork inside each window drifts slightly
 * (parallax). Because the windows track the finger 1:1, the result feels like one cover gliding into
 * the next.
 *
 * Technique inspired by the Auxio music player's cover swiper (GPL-3.0); this is an independent Compose
 * implementation. Everything runs inside graphicsLayer, i.e. in the draw phase, so swiping causes
 * no recomposition.
 */
private const val GAP_RAMP = 0.18f      // fraction of the swipe over which the gap grows / shrinks
private const val MAX_GAP_FRACTION = 0.08f // widest gap, as a fraction of the page width
private const val PARALLAX = 0.2f       // how far the art drifts toward its window's centre

/** Signed distance of [page] from the settled position: <0 = leaving/left side, >0 = entering/right side. */
@OptIn(ExperimentalFoundationApi::class)
private fun PagerState.offsetOf(page: Int): Float = (page - currentPage) - currentPageOffsetFraction

/** Horizontal visible window [left, right] of a page of [width] px at [position] (see [offsetOf]). */
private fun visibleWindow(position: Float, width: Float): Pair<Float, Float> {
    if (position == 0f) return 0f to width
    // Progress of this page through the transition, 0 = fully shown ... 1 = fully hidden.
    // For the outgoing page that is how far it has moved away; for the incoming one, how far it still is.
    val s = if (position < 0f) -position else 1f - position
    val maxGap = width * MAX_GAP_FRACTION
    val gap = when {
        s < GAP_RAMP -> maxGap * (s / GAP_RAMP)
        s > 1f - GAP_RAMP -> maxGap * ((1f - s) / GAP_RAMP)
        else -> maxGap
    }
    // How much of the shared space the *incoming* cover owns; held at 0/1 while the gap ramps.
    val incomingShare = ((s - GAP_RAMP) / (1f - 2f * GAP_RAMP)).coerceIn(0f, 1f)
    val available = width - gap
    val incomingWidth = available * incomingShare
    return if (position < 0f) {
        0f to (available - incomingWidth)       // outgoing: anchored to the left edge
    } else {
        (width - incomingWidth) to width        // incoming: anchored to the right edge
    }
}

/** A rounded rectangle covering only [left, right] of the layer. */
private class WindowShape(
    private val left: Float,
    private val right: Float,
    private val cornerPx: Float
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val r = minOf(cornerPx, (right - left) / 2f).coerceAtLeast(0f)
        return Outline.Rounded(RoundRect(left, 0f, right, size.height, CornerRadius(r, r)))
    }
}

/**
 * Apply to each pager page's root. [pageSpacingPx] must match the pager's pageSpacing so pages can be
 * pinned exactly; [cornerPx] rounds the window's edges while it is partially revealed.
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.coverCarouselPage(
    pagerState: PagerState,
    page: Int,
    pageSpacingPx: Float,
    cornerPx: Float
): Modifier = graphicsLayer {
    val position = pagerState.offsetOf(page)
    val width = size.width
    if (width <= 0f) return@graphicsLayer
    if (abs(position) >= 1f) {
        // Not part of the transition. Leave it at its natural off-screen spot: pinning it over the
        // current page would let it steal touches (CD scrub, long-press) even while invisible.
        translationX = 0f
        alpha = 0f
        clip = false
        return@graphicsLayer
    }
    // Pin the page over the current one; the pager would otherwise slide it.
    translationX = -position * (width + pageSpacingPx)
    alpha = 1f
    if (position != 0f) {
        val (left, right) = visibleWindow(position, width)
        shape = WindowShape(left, right, cornerPx)
        clip = true
    } else {
        clip = false
    }
}

/** Apply to the page's content (inside [coverCarouselPage]) for the subtle parallax drift. */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.coverCarouselParallax(pagerState: PagerState, page: Int): Modifier = graphicsLayer {
    val position = pagerState.offsetOf(page)
    val width = size.width
    if (width <= 0f || position == 0f || abs(position) >= 1f) {
        translationX = 0f
        return@graphicsLayer
    }
    val (left, right) = visibleWindow(position, width)
    translationX = ((left + right) / 2f - width / 2f) * PARALLAX
}
