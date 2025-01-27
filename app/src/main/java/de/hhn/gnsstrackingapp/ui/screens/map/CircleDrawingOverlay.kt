package de.hhn.gnsstrackingapp.ui.screens.map

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import de.hhn.gnsstrackingapp.data.CircleData
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay

class CircleDrawingOverlay(
    private val currentCircle: CircleData? = null,
    private val onClick: ((CircleData) -> Unit)? = null,
    private val context: Context
) : Overlay() {

    private var longPressHandler: Runnable? = null
    private var name: String? = null

    var isInside = false
    var isNear = false

    var lastCheckTime: Long = 0
    val checkInterval: Long = 5 * 60 * 1000

    var radius: Float = 0f;

    fun getCurrentCircle(): CircleData? {
        return currentCircle
    }

    fun setName(name: String) {
        this.name = name
    }

    override fun draw(pCanvas: Canvas?, pMapView: MapView?, pShadow: Boolean) {
        currentCircle?.let { circle ->
            val paint = Paint().apply {
                color = Color.RED
                style = Paint.Style.STROKE
                strokeWidth = 5f
                alpha = 128
            }
            val fillPaint = Paint().apply {
                color = Color.RED
                style = Paint.Style.FILL
                alpha = 64
            }
            val centerPoint = pMapView?.projection?.toPixels(circle.center, null)
            val radiusPoint= pMapView?.projection?.toPixels(circle.radiusPoint, null)
            if(centerPoint != null && radiusPoint != null) {
                radius = Math.sqrt(
                    Math.pow((centerPoint.x - radiusPoint.x).toDouble(), 2.0)
                            + Math.pow((centerPoint.y - radiusPoint.y).toDouble(), 2.0)
                ).toFloat()
                val left = centerPoint.x - radius
                val top = centerPoint.y - radius
                val right = centerPoint.x + radius
                val bottom = centerPoint.y + radius
                pCanvas?.drawOval(left, top, right, bottom, fillPaint)
                pCanvas?.drawOval(left, top, right, bottom, paint)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent?, mapView: MapView?): Boolean {
        val centerPoint = mapView?.projection?.toPixels(currentCircle?.center, null)
        val radiusPoint = mapView?.projection?.toPixels(currentCircle?.radiusPoint, null)
        if (centerPoint != null && radiusPoint != null) {
            val radius = Math.sqrt(
                Math.pow((centerPoint.x - radiusPoint.x).toDouble(), 2.0)
                        + Math.pow((centerPoint.y - radiusPoint.y).toDouble(), 2.0)
            ).toFloat()
            val distance = Math.sqrt(
                Math.pow((centerPoint.x - event?.x?.toInt()!! ?: 0).toDouble(), 2.0)
                        + Math.pow((centerPoint.y - event?.y?.toInt()!! ?: 0).toDouble(), 2.0)
            ).toFloat()
            if (distance <= radius) {
                when(event?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        longPressHandler = Runnable {
                            currentCircle?.let { circle ->
                                onClick?.invoke(circle)
                            }
                            //currentCircle?.let { onClick?.invoke(it) }
                        }
                        mapView?.postDelayed(longPressHandler, 3000)
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        mapView?.removeCallbacks(longPressHandler)
                    }
                }
                return true
            }
        }
        return false
    }
}