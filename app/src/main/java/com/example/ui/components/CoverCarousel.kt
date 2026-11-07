package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.PagerState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.abs

/**
 * "Masked carousel" transition for the now-playing cover pager.
 *
 * Every page is pinned in place, and the *artwork* is revealed through an animated window: the
 * outgoing cover's window shrinks toward the side it leaves on, the incoming cover's window grows from
 * the side it enters from, a small gap opens early in the swipe and closes at the end, and the art
 * inside each window drifts slightly (parallax).
 *
 * The window is computed in the artwork's own coordinates and shares its corner radius, so mid-swipe
 * you see two rounded covers side by side (the cut edges are rounded too), and the shadow follows the
 * visible slice. Masking the whole page instead (artwork + padding + title) gave hard straight cuts and
 * a shadowed "slot" between covers. Titles cross-fade rather than being sliced.
 *
 * Technique inspired by the Auxio music player's cover swiper (GPL-3.0); this is an independent Compose
 * implementation. Everything runs inside graphicsLayer (draw phase), so swiping causes no recomposition.
 */
private const val GAP_RAMP = 0.18f         // fraction of the swipe over which the gap grows / shrinks
private const val MAX_GAP_FRACTION = 0.03f // widest gap, as a fraction of the artwork width
private const val PARALLAX = 0.2f          // how far the art drifts toward its window's centre

/** Signed distance of [page] from the settled position: <0 = leaving/left side, >0 = entering/right side. */
@OptIn(ExperimentalFoundationApi::class)
private fun PagerState.offsetOf(page: Int): Float = (page - currentPage) - currentPageOffsetFraction

/** Horizontal visible window [left, right] of content [width] px wide at [position] (see [offsetOf]). */
private fun visibleWindow(position: Float, width: Float): Pair<Float, Float> {
    if (position == 0f) return 0f to width
    // Progress through the transition: 0 = fully shown ... 1 = fully hidden.
    val s = if (position < 0f) -position else 1f - position
    val maxGap = width * MAX_GAP_FRACTION
    val gap = when {
        s < GAP_RAMP -> maxGap * (s / GAP_RAMP)
        s > 1f - GAP_RAMP -> maxGap * ((1f - s) / GAP_RAMP)
        else -> maxGap
    }
    // Share of the space owned by the incoming cover; held at 0/1 while the gap ramps.
    val incomingShare = ((s - GAP_RAMP) / (1f - 2f * GAP_RAMP)).coerceIn(0f, 1f)
    val available = width - gap
    val incomingWidth = available * incomingShare
    return if (position < 0f) 0f to (available - incomingWidth) else (width - incomingWidth) to width
}

/** The artwork's own shape intersected with a rounded window [left, right]. */
private class ArtWindowShape(
    private val artShape: Shape,
    private val left: Float,
    private val right: Float,
    private val cornerPx: Float
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val r = minOf(cornerPx, (right - left) / 2f, size.height / 2f).coerceAtLeast(0f)
        val window = RoundRect(left, 0f, right, size.height, CornerRadius(r, r))
        val art = artShape.createOutline(size, layoutDirection, density)
        // Plain rectangle art: the window alone is the answer (cheapest, and keeps a convex outline
        // so shadows render on every Android version).
        if (art is Outline.Rectangle) return Outline.Rounded(window)
        val artPath = Path().apply { addOutline(art) }
        val windowPath = Path().apply { addRoundRect(window) }
        return Outline.Generic(Path().apply { op(artPath, windowPath, PathOperation.Intersect) })
    }
}

/**
 * Apply to each pager page's root: pins the page in place during a swipe. [pageSpacingPx] must match
 * the pager's pageSpacing.
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.coverCarouselPin(pagerState: PagerState, page: Int, pageSpacingPx: Float): Modifier = graphicsLayer {
    val position = pagerState.offsetOf(page)
    if (abs(position) >= 1f) {
        // Not part of the transition: leave it off screen where the pager put it. Pinning it over the
        // current page would let it steal touches (CD scrub, long-press) even while invisible.
        translationX = 0f
        alpha = 0f
    } else {
        translationX = -position * (size.width + pageSpacingPx)
        alpha = 1f
    }
}

/**
 * Apply to the artwork box *in place of* `.shadow(elevation, shape).clip(shape)`. At rest it is exactly
 * that; mid-swipe the clip and shadow follow the visible slice of the cover.
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.coverCarouselArt(
    pagerState: PagerState,
    page: Int,
    artShape: Shape,
    windowCornerPx: Float,
    shadowElevationPx: Float
): Modifier = graphicsLayer {
    val position = pagerState.offsetOf(page)
    shadowElevation = shadowElevationPx
    clip = true
    shape = if (position == 0f || abs(position) >= 1f) {
        artShape
    } else {
        val (left, right) = visibleWindow(position, size.width)
        ArtWindowShape(artShape, left, right, windowCornerPx)
    }
}

/** Apply to the image inside the artwork box for the subtle parallax drift. */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.coverCarouselParallax(pagerState: PagerState, page: Int): Modifier = graphicsLayer {
    val position = pagerState.offsetOf(page)
    if (size.width <= 0f || position == 0f || abs(position) >= 1f) {
        translationX = 0f
        return@graphicsLayer
    }
    val (left, right) = visibleWindow(position, size.width)
    translationX = ((left + right) / 2f - size.width / 2f) * PARALLAX
}

/** Apply to the title/artist block: the two pinned titles cross-fade instead of being sliced. */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.coverCarouselFade(pagerState: PagerState, page: Int): Modifier = graphicsLayer {
    alpha = (1f - 2f * abs(pagerState.offsetOf(page))).coerceIn(0f, 1f)
}
