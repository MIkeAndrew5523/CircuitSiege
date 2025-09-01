package com.example.circuitcity_2.engine

/**
 * Manages the current active screen in CircuitCity 2, handling transitions and disposal.
 */
class ScreenManager {
    private var current: Screen? = null

    /**
     * Sets the current screen, disposing the previous one if necessary.
     * @param screen The new Screen to activate
     */
    fun set(screen: Screen) { current?.dispose(); current = screen }

    /**
     * Gets the current active screen.
     * @return The current Screen, or null if none is set
     */
    fun get(): Screen? = current
}
