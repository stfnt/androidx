/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.ui.core

import androidx.ui.geometry.Size
import androidx.ui.graphics.ColorFilter
import androidx.ui.graphics.DefaultAlpha
import androidx.ui.graphics.painter.Painter
import androidx.ui.graphics.drawscope.withTransform
import androidx.ui.unit.IntSize
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Paint the content using [painter].
 *
 * @param sizeToIntrinsics `true` to size the element relative to [Painter.intrinsicSize]
 * @param alignment specifies alignment of the [painter] relative to content
 * @param contentScale strategy for scaling [painter] if its size does not match the content size
 * @param alpha opacity of [painter]
 * @param colorFilter optional [ColorFilter] to apply to [painter]
 *
 * @sample androidx.ui.core.samples.PainterModifierSample
 */
fun Modifier.paint(
    painter: Painter,
    sizeToIntrinsics: Boolean = true,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Inside,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null
) = this + PainterModifier(
    painter = painter,
    sizeToIntrinsics = sizeToIntrinsics,
    alignment = alignment,
    contentScale = contentScale,
    alpha = alpha,
    colorFilter = colorFilter
)

/**
 * [DrawModifier] used to draw the provided [Painter] followed by the contents
 * of the component itself
 */
private data class PainterModifier(
    val painter: Painter,
    val sizeToIntrinsics: Boolean,
    val alignment: Alignment = Alignment.Center,
    val contentScale: ContentScale = ContentScale.Inside,
    val alpha: Float = DefaultAlpha,
    val colorFilter: ColorFilter? = null
) : LayoutModifier, DrawModifier {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
        layoutDirection: LayoutDirection
    ): MeasureScope.MeasureResult {
        val placeable = measurable.measure(modifyConstraints(constraints))
        return layout(placeable.width, placeable.height) {
            placeable.place(0, 0)
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int,
        layoutDirection: LayoutDirection
    ): Int {
        return if (sizeToIntrinsics) {
            val constraints = Constraints(maxHeight = height)
            val layoutWidth =
                measurable.minIntrinsicWidth(modifyConstraints(constraints).maxHeight)
            val scaledSize = calculateScaledSize(Size(layoutWidth.toFloat(), height.toFloat()))
            max(scaledSize.width.roundToInt(), layoutWidth)
        } else {
            measurable.minIntrinsicWidth(height)
        }
    }

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int,
        layoutDirection: LayoutDirection
    ): Int {
        return if (sizeToIntrinsics) {
            val constraints = Constraints(maxHeight = height)
            val layoutWidth =
                measurable.maxIntrinsicWidth(modifyConstraints(constraints).maxHeight)
            val scaledSize = calculateScaledSize(Size(layoutWidth.toFloat(), height.toFloat()))
            max(scaledSize.width.roundToInt(), layoutWidth)
        } else {
            measurable.maxIntrinsicWidth(height)
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int,
        layoutDirection: LayoutDirection
    ): Int {
        return if (sizeToIntrinsics) {
            val constraints = Constraints(maxWidth = width)
            val layoutHeight =
                measurable.minIntrinsicHeight(modifyConstraints(constraints).maxWidth)
            val scaledSize = calculateScaledSize(Size(width.toFloat(), layoutHeight.toFloat()))
            max(scaledSize.height.roundToInt(), layoutHeight)
        } else {
            measurable.minIntrinsicHeight(width)
        }
    }

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int,
        layoutDirection: LayoutDirection
    ): Int {
        return if (sizeToIntrinsics) {
            val constraints = Constraints(maxWidth = width)
            val layoutHeight =
                measurable.maxIntrinsicHeight(modifyConstraints(constraints).maxWidth)
            val scaledSize = calculateScaledSize(Size(width.toFloat(), layoutHeight.toFloat()))
            max(scaledSize.height.roundToInt(), layoutHeight)
        } else {
            measurable.maxIntrinsicHeight(width)
        }
    }

    private fun calculateScaledSize(dstSize: Size): Size {
        return if (!sizeToIntrinsics) {
            dstSize
        } else {
            val intrinsicWidth = painter.intrinsicSize.width
            val intrinsicHeight = painter.intrinsicSize.height
            val srcWidth = if (intrinsicWidth == Float.POSITIVE_INFINITY) {
                dstSize.width
            } else {
                intrinsicWidth
            }

            val srcHeight = if (intrinsicHeight == Float.POSITIVE_INFINITY) {
                dstSize.height
            } else {
                intrinsicHeight
            }

            val srcSize = Size(srcWidth, srcHeight)
            srcSize * contentScale.scale(srcSize, dstSize)
        }
    }

    private fun modifyConstraints(constraints: Constraints): Constraints {
        if (!sizeToIntrinsics || (constraints.hasFixedWidth && constraints.hasFixedHeight)) {
            // If we have fixed constraints or we are not attempting to size the
            // composable based on the size of the Painter, do not attempt to
            // modify them. Otherwise rely on Alignment and ContentScale
            // to determine how to position the drawing contents of the Painter within
            // the provided bounds
            return constraints
        }

        val intrinsicSize = painter.intrinsicSize
        val intrinsicWidth =
            if (intrinsicSize.width != Float.POSITIVE_INFINITY) {
                intrinsicSize.width.roundToInt()
            } else {
                constraints.minWidth
            }

        val intrinsicHeight =
            if (intrinsicSize.height != Float.POSITIVE_INFINITY) {
                intrinsicSize.height.roundToInt()
            } else {
                constraints.minHeight
            }

        // Scale the width and height appropriately based on the given constraints
        // and ContentScale
        val constrainedWidth = constraints.constrainWidth(intrinsicWidth)
        val constrainedHeight = constraints.constrainHeight(intrinsicHeight)
        val scaledSize = calculateScaledSize(
            Size(constrainedWidth.toFloat(), constrainedHeight.toFloat())
        )

        val minWidth = if (constraints.hasFixedWidth) {
            constraints.minWidth
        } else {
            scaledSize.width.roundToInt()
        }

        val minHeight = if (constraints.hasFixedHeight) {
            constraints.minHeight
        } else {
            scaledSize.height.roundToInt()
        }
        return constraints.copy(minWidth = minWidth, minHeight = minHeight)
    }

    override fun ContentDrawScope.draw() {
        val intrinsicSize = painter.intrinsicSize
        val srcWidth = if (intrinsicSize.width != Float.POSITIVE_INFINITY) {
            intrinsicSize.width
        } else {
            size.width
        }

        val srcHeight = if (intrinsicSize.height != Float.POSITIVE_INFINITY) {
            intrinsicSize.height
        } else {
            size.height
        }

        val srcSize = Size(srcWidth, srcHeight)
        val scale = contentScale.scale(srcSize, size)

        val alignedPosition = alignment.align(
            IntSize(
                ceil(size.width - (srcWidth * scale)).toInt(),
                ceil(size.height - (srcHeight * scale)).toInt()
            )
        )

        val dx = alignedPosition.x.toFloat()
        val dy = alignedPosition.y.toFloat()

        withTransform({
            translate(dx, dy)
            scale(scale, scale, 0.0f, 0.0f)
        }) {
            with(painter) {
                draw(size = srcSize, alpha = alpha, colorFilter = colorFilter)
            }
        }
    }
}