package de.hhn.gnsstrackingapp.data

import org.osmdroid.util.GeoPoint

data class CircleData(
    val center: GeoPoint,
    var radiusPoint: GeoPoint
)