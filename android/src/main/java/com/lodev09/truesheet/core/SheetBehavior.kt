package com.lodev09.truesheet.core

import android.graphics.Point
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.ScrollView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior

data class SizeInfo(val index: Int, val value: Float)

class SheetBehavior<T : ViewGroup> : BottomSheetBehavior<T>() {
  var maxSheetSize: Point = Point()
  var maxSheetHeight: Int? = null

  var contentView: ViewGroup? = null
  var footerView: ViewGroup? = null

  override fun onInterceptTouchEvent(parent: CoordinatorLayout, child: T, event: MotionEvent): Boolean {
    contentView?.let {
      val isDownEvent = (event.actionMasked == MotionEvent.ACTION_DOWN)
      val expanded = state == STATE_EXPANDED

      if (isDownEvent && expanded) {
        for (i in 0 until it.childCount) {
          val contentChild = it.getChildAt(i)
          val scrolled = (contentChild is ScrollView && contentChild.scrollY > 0)

          if (!scrolled) continue
          if (isInsideSheet(contentChild as ScrollView, event)) {
            return false
          }
        }
      }
    }

    return super.onInterceptTouchEvent(parent, child, event)
  }

  private fun isInsideSheet(scrollView: ScrollView, event: MotionEvent): Boolean {
    val x = event.x
    val y = event.y

    val position = IntArray(2)
    scrollView.getLocationOnScreen(position)

    val nestedX = position[0]
    val nestedY = position[1]

    val boundRight = nestedX + scrollView.width
    val boundBottom = nestedY + scrollView.height

    return (x > nestedX && x < boundRight && y > nestedY && y < boundBottom) ||
      event.action == MotionEvent.ACTION_CANCEL
  }

  fun getSizeHeight(size: Any, contentHeight: Int): Int {
    val height =
      when (size) {
        is Double -> Utils.toPixel(size)

        is Int -> Utils.toPixel(size.toDouble())

        is String -> {
          when (size) {
            "auto" -> contentHeight

            "large" -> maxSheetSize.y

            "medium" -> (maxSheetSize.y * 0.50).toInt()

            "small" -> (maxSheetSize.y * 0.25).toInt()

            else -> {
              if (size.endsWith('%')) {
                val percent = size.trim('%').toDoubleOrNull()
                if (percent == null) {
                  0
                } else {
                  ((percent / 100) * maxSheetSize.y).toInt()
                }
              } else {
                val fixedHeight = size.toDoubleOrNull()
                if (fixedHeight == null) {
                  0
                } else {
                  Utils.toPixel(fixedHeight)
                }
              }
            }
          }
        }

        else -> (maxSheetSize.y * 0.5).toInt()
      }

    return minOf(height, maxSheetHeight ?: maxSheetSize.y)
  }

  fun configure(sizes: Array<Any>) {
    var contentHeight = 0

    contentView?.let { contentHeight = it.height }
    footerView?.let { contentHeight += it.height }

    // Configure sheet sizes
    apply {
      isFitToContents = true
      isHideable = true
      skipCollapsed = false

      when (sizes.size) {
        1 -> {
          maxHeight = getSizeHeight(sizes[0], contentHeight)
          skipCollapsed = true
        }

        2 -> {
          peekHeight = getSizeHeight(sizes[0], contentHeight)
          maxHeight = getSizeHeight(sizes[1], contentHeight)
        }

        3 -> {
          // Enables half expanded
          isFitToContents = false

          peekHeight = getSizeHeight(sizes[0], contentHeight)
          halfExpandedRatio = getSizeHeight(sizes[1], contentHeight).toFloat() / maxSheetSize.y.toFloat()
          maxHeight = getSizeHeight(sizes[2], contentHeight)
        }
      }
    }
  }

  fun getSizeInfoForState(sizeCount: Int, state: Int): SizeInfo? =
    when (sizeCount) {
      1 -> {
        when (state) {
          STATE_EXPANDED -> SizeInfo(0, Utils.toDIP(maxHeight))
          else -> null
        }
      }

      2 -> {
        when (state) {
          STATE_COLLAPSED -> SizeInfo(0, Utils.toDIP(peekHeight))
          STATE_EXPANDED -> SizeInfo(1, Utils.toDIP(maxHeight))
          else -> null
        }
      }

      3 -> {
        when (state) {
          STATE_COLLAPSED -> SizeInfo(0, Utils.toDIP(peekHeight))

          STATE_HALF_EXPANDED -> {
            val height = halfExpandedRatio * maxSheetSize.y
            SizeInfo(1, Utils.toDIP(height.toInt()))
          }

          STATE_EXPANDED -> SizeInfo(2, Utils.toDIP(maxHeight))

          else -> null
        }
      }

      else -> null
    }

  fun setStateForSizeIndex(sizeCount: Int, index: Int) {
    state =
      when (sizeCount) {
        1 -> STATE_EXPANDED

        2 -> {
          when (index) {
            0 -> STATE_COLLAPSED
            1 -> STATE_EXPANDED
            else -> STATE_HIDDEN
          }
        }

        3 -> {
          when (index) {
            0 -> STATE_COLLAPSED
            1 -> STATE_HALF_EXPANDED
            2 -> STATE_EXPANDED
            else -> STATE_HIDDEN
          }
        }

        else -> STATE_HIDDEN
      }
  }
}
