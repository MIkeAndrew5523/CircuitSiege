package com.example.circuitcity_2.model

import android.content.Context
import kotlin.math.floor

/**
 * Represents a game level in CircuitCity 2. Handles tile grid, player spawn, exit location,
 * tile queries, pickups, doors, collision detection, and asset loading.
 * @property width Level width in tiles
 * @property height Level height in tiles
 * @property grid 2D array of tile characters
 * @property playerSpawnX Player spawn X coordinate
 * @property playerSpawnY Player spawn Y coordinate
 * @property exitX Exit tile X coordinate (nullable)
 * @property exitY Exit tile Y coordinate (nullable)
 */
class Level(
    val width: Int,
    val height: Int,
    private val grid: Array<CharArray>,
    val playerSpawnX: Int,
    val playerSpawnY: Int,
    val exitX: Int?,
    val exitY: Int?
) {
    // ---- Coordinate scale (world units -> tiles) ----
    // If your world positions are already expressed in tiles, keep this = 1f.
    // If they are pixels, set to your pixel tile size (e.g., 32f).
    private val TILE_SIZE: Float = 1f

    // ---- Tile accessors / queries ----
    /** Returns the tile at the given coordinates. */
    fun tileAt(tx: Int, ty: Int): Tile {
        if (ty !in 0 until height || tx !in 0 until width) return Tile('#') // out-of-bounds as wall
        return Tile(grid[ty][tx])
    }

    /** Returns true if the level has an exit. */
    fun hasExit(): Boolean = exitX != null && exitY != null

    /** Returns true if the tile is a hazard. */
    fun isHazard(tx: Int, ty: Int)     = tileAt(tx, ty).hazard
    /** Returns true if the tile is a key. */
    fun isKey(tx: Int, ty: Int)        = tileAt(tx, ty).key
    /** Returns true if the tile is a checkpoint. */
    fun isCheckpoint(tx: Int, ty: Int) = tileAt(tx, ty).checkpoint
    /** Returns true if the tile is a locked door. */
    fun isLockedDoor(tx: Int, ty: Int) = tileAt(tx, ty).lockedDoor
    /** Returns true if the tile is an enemy spawn. */
    fun isEnemySpawn(tx: Int, ty: Int) = tileAt(tx, ty).enemySpawn

    /** Returns true if the tile is solid. */
    fun isSolid(tx: Int, ty: Int): Boolean {
        // Treat out-of-bounds as solid to avoid falling through edges
        if (tx < 0 || ty < 0 || tx >= width || ty >= height) return true
        return tileAt(tx, ty).solid
    }

    /**
     * Consumes a pickup or checkpoint at the given coordinates, clearing the tile and returning its character.
     * @return The consumed character, or '\u0000' if none
     */
    fun consumeAt(tx: Int, ty: Int): Char {
        if (ty !in 0 until height || tx !in 0 until width) return '\u0000'
        val c = grid[ty][tx]
        if (c == 'K' || c == 'C' || c == 'E') {
            grid[ty][tx] = '.'
            return c
        }
        return '\u0000'
    }

    /**
     * Unlocks a locked door at the given coordinates, setting it to '.' and returning success.
     * @return true if the door was unlocked
     */
    fun unlockDoorAt(tx: Int, ty: Int): Boolean {
        if (ty !in 0 until height || tx !in 0 until width) return false
        if (grid[ty][tx] == 'd') {
            grid[ty][tx] = '.'
            return true
        }
        return false
    }

    // ---- World<->Tile helpers ----
    /** Converts world X coordinate to tile X index. */
    private fun worldToTileX(x: Float): Int = floor(x / TILE_SIZE).toInt()
    /** Converts world Y coordinate to tile Y index. */
    private fun worldToTileY(y: Float): Int = floor(y / TILE_SIZE).toInt()

    // ---- Collision helpers used by Physics.kt ----
    /** Returns true if the given world coordinates collide horizontally with a solid tile. */
    fun collidesHorizontally(x: Float, y: Float): Boolean {
        val tx = worldToTileX(x)
        val ty = worldToTileY(y)
        return isSolid(tx, ty)
    }

    /** Returns true if the given world coordinates collide vertically with a solid tile. */
    fun collidesVertically(x: Float, y: Float): Boolean {
        val tx = worldToTileX(x)
        val ty = worldToTileY(y)
        return isSolid(tx, ty)
    }

    companion object {
        /**
         * Loads a level from an asset file.
         * @param context Android context
         * @param path Asset path to the level file
         * @return Loaded Level instance
         */
        fun fromAsset(context: Context, path: String): Level {
            val lines = context.assets.open(path)
                .bufferedReader()
                .use { it.readLines().filter { s -> s.isNotEmpty() } }

            val h = lines.size
            val w = lines.maxOf { it.length }

            var spawnX = 1
            var spawnY = 1
            var ex: Int? = null
            var ey: Int? = null

            val grid = Array(h) { y ->
                val row = lines[y].padEnd(w, '#').toCharArray()
                for (x in row.indices) {
                    when (row[x]) {
                        '@' -> { spawnX = x; spawnY = y; row[x] = '.' }
                        'D' -> { ex = x; ey = y; /* keep 'D' so it can render; non-solid in Tile */ }
                        // 'd' (locked door) remains 'd' and is solid via Tile.solid
                    }
                }
                row
            }

            return Level(w, h, grid, spawnX, spawnY, ex, ey)
        }
    }
}
