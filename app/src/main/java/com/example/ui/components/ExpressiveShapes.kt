package com.example.ui.components

import android.graphics.Matrix
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.tan

object ExpressivePaths {
    fun getCookie6Sided(bounds: RectF): Path {
        val points = listOf(PointNRound(0.723f, 0.884f, 0.394f), PointNRound(0.500f, 1.099f, 0.398f))
        return generatePolygonPath(bounds, points, reps = 6)
    }

    fun getSoftBurst(bounds: RectF): Path {
        val points = listOf(PointNRound(0.193f, 0.277f, 0.053f), PointNRound(0.176f, 0.055f, 0.053f))
        return generatePolygonPath(bounds, points, reps = 10)
    }

    private data class PointNRound(val x: Float, val y: Float, val r: Float)
    private data class Vertex(val x: Float, val y: Float, val r: Float)
    private data class Corner(val start: PointF, val end: PointF)

    private fun generatePolygonPath(bounds: RectF, rawPoints: List<PointNRound>, reps: Int): Path {
        val centerX = 0.5f
        val centerY = 0.5f
        val vertices = ArrayList<Vertex>()
        val numPoints = rawPoints.size

        for (i in 0 until numPoints * reps) {
            val pointIndex = i % numPoints
            val p = rawPoints[pointIndex]
            val repIndex = i / numPoints
            val rotationDeg = (repIndex * 360f) / reps
            val (rx, ry) = rotatePoint(p.x, p.y, centerX, centerY, rotationDeg)
            vertices.add(Vertex(rx, ry, p.r))
        }

        val normalizedPath = createRoundedPath(vertices)
        val pathBounds = RectF()
        normalizedPath.computeBounds(pathBounds, true)

        val matrix = Matrix()
        matrix.setRectToRect(pathBounds, bounds, Matrix.ScaleToFit.CENTER)
        normalizedPath.transform(matrix)
        return normalizedPath
    }

    private fun rotatePoint(x: Float, y: Float, cx: Float, cy: Float, degrees: Float): Pair<Float, Float> {
        val rad = Math.toRadians(degrees.toDouble())
        val cos = cos(rad)
        val sin = sin(rad)
        val dx = x - cx
        val dy = y - cy
        val nx = dx * cos - dy * sin + cx
        val ny = dx * sin + dy * cos + cy
        return Pair(nx.toFloat(), ny.toFloat())
    }

    private fun createRoundedPath(vertices: List<Vertex>): Path {
        val path = Path()
        val n = vertices.size
        if (n < 3) return path

        val corners = List(n) { i ->
            calculateCorner(vertices[(i - 1 + n) % n], vertices[i], vertices[(i + 1) % n])
        }

        path.moveTo(corners[0].start.x, corners[0].start.y)
        for (i in 0 until n) {
            val curr = vertices[i]
            val corner = corners[i]
            val nextCorner = corners[(i + 1) % n]
            path.quadTo(curr.x, curr.y, corner.end.x, corner.end.y)
            path.lineTo(nextCorner.start.x, nextCorner.start.y)
        }
        path.close()
        return path
    }

    private fun calculateCorner(prev: Vertex, curr: Vertex, next: Vertex): Corner {
        val v1x = prev.x - curr.x
        val v1y = prev.y - curr.y
        val len1 = hypot(v1x, v1y)
        val v2x = next.x - curr.x
        val v2y = next.y - curr.y
        val len2 = hypot(v2x, v2y)
        val dot = (v1x * v2x + v1y * v2y)
        val angleRad = acos((dot / (len1 * len2)).coerceIn(-1.0f, 1.0f))
        val tanHalf = tan(angleRad / 2.0)
        var dist = if (tanHalf != 0.0) (curr.r / tanHalf).toFloat() else 0f
        val limit = min(len1, len2) / 2.0f
        if (dist > limit) dist = limit
        val startX = curr.x + (v1x / len1) * dist
        val startY = curr.y + (v1y / len1) * dist
        val endX = curr.x + (v2x / len2) * dist
        val endY = curr.y + (v2y / len2) * dist
        return Corner(PointF(startX, startY), PointF(endX, endY))
    }
}

class CookieShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = ExpressivePaths.getCookie6Sided(RectF(0f, 0f, size.width, size.height))
        return Outline.Generic(path.asComposePath())
    }
}

class SoftBurstShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = ExpressivePaths.getSoftBurst(RectF(0f, 0f, size.width, size.height))
        return Outline.Generic(path.asComposePath())
    }
}
