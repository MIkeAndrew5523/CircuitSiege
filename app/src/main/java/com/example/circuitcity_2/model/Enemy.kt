package com.example.circuitcity_2.model

open class Enemy(
    xTiles: Float,
    yTiles: Float,
    wTiles: Float = 0.8f,
    hTiles: Float = 0.9f
) : Entity(xTiles, yTiles, wTiles, hTiles) {
    open fun update(level: Level, player: Player, dt: Float) {}
    open fun onPlayerHit(player: Player) {}  // hook for damage, knockback, etc.
}
