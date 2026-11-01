package com.example.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Responsive layout that places leftContent and rightContent side-by-side if there is sufficient
 * horizontal space; otherwise, shifts rightContent gracefully to the next line below leftContent.
 */
@Composable
fun ResponsiveWrapRow(
  leftContent: @Composable () -> Unit,
  rightContent: @Composable () -> Unit,
  modifier: Modifier = Modifier,
  horizontalSpacing: Dp = 8.dp,
  verticalSpacing: Dp = 4.dp
) {
  Layout(
    content = {
      Box(modifier = Modifier.wrapContentSize()) { leftContent() }
      Box(modifier = Modifier.wrapContentSize()) { rightContent() }
    },
    modifier = modifier.fillMaxWidth()
  ) { measurables, constraints ->
    val horizontalSpacingPx = horizontalSpacing.roundToPx()
    val verticalSpacingPx = verticalSpacing.roundToPx()
    val maxAvailableWidth = constraints.maxWidth

    val rightMeasurable = measurables[1]
    val rightPlaceable = rightMeasurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
    val rightWidth = rightPlaceable.width

    val leftMeasurable = measurables[0]
    val leftIntrinsicWidth = leftMeasurable.maxIntrinsicWidth(constraints.maxHeight)

    val canFitTogether = (leftIntrinsicWidth + rightWidth + horizontalSpacingPx) <= maxAvailableWidth

    if (canFitTogether) {
      val leftPlaceable = leftMeasurable.measure(
        constraints.copy(
          minWidth = 0,
          maxWidth = maxAvailableWidth - rightWidth - horizontalSpacingPx
        )
      )
      val rowHeight = maxOf(leftPlaceable.height, rightPlaceable.height)
      layout(maxAvailableWidth, rowHeight) {
        leftPlaceable.placeRelative(0, (rowHeight - leftPlaceable.height) / 2)
        rightPlaceable.placeRelative(maxAvailableWidth - rightWidth, (rowHeight - rightPlaceable.height) / 2)
      }
    } else {
      val leftPlaceable = leftMeasurable.measure(
        constraints.copy(minWidth = 0, maxWidth = maxAvailableWidth)
      )
      val totalHeight = leftPlaceable.height + verticalSpacingPx + rightPlaceable.height
      layout(maxAvailableWidth, totalHeight) {
        leftPlaceable.placeRelative(0, 0)
        rightPlaceable.placeRelative(0, leftPlaceable.height + verticalSpacingPx)
      }
    }
  }
}
