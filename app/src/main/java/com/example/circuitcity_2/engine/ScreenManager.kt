package com.example.circuitcity_2.engine

class ScreenManager {
    private var current: Screen? = null
    fun set(screen: Screen) { current?.dispose(); current = screen }
    fun get(): Screen? = current
}
