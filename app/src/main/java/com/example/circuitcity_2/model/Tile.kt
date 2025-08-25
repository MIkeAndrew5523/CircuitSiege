package com.example.circuitcity_2.model

data class Tile(val symbol: Char) {
    val solid: Boolean      get() = symbol == '#' || symbol == 'd'   // locked door is solid
    val hazard: Boolean     get() = symbol == 'X'
    val key: Boolean        get() = symbol == 'K'
    val checkpoint: Boolean get() = symbol == 'C'
    val exit: Boolean       get() = symbol == 'D'
    val lockedDoor: Boolean get() = symbol == 'd'
}
