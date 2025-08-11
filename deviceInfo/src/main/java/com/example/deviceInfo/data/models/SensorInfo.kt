package com.example.deviceInfo.data.models

data class SensorInfo(
    val name: String,
    val type: String,
    val vendor: String,
    val version: Int,
    val maximumRange: Float,
    val resolution: Float,
    val power: Float
)